# AI 文生图应用 — 架构与实现设计文档

> 基于 Google Stitch 原型：创作页、相册页、设置页。  
> 用途：后续开发实现的单一参照文档。  
> 文档版本：1.0 · 更新日期：2026-04-05

---

## 目录

1. [产品目标与约束](#1-产品目标与约束)
2. [整体架构](#2-整体架构分层--模块化)
3. [功能模块划分](#3-功能模块划分)
4. [领域与数据模型](#4-领域与数据模型)
5. [远程 API 接口设计](#5-远程-api-接口设计)
6. [Repository 抽象（对内）](#6-repository-抽象对内)
7. [关键 UX 流程与状态机](#7-关键-ux-流程与状态机)
8. [设置与子功能](#8-设置与子功能)
9. [横切关注点](#9-横切关注点)
10. [依赖注入与导航](#10-依赖注入与导航)
11. [迭代交付顺序](#11-迭代交付顺序)
12. [与 Stitch 原型的对应检查清单](#12-与-stitch-原型的对应检查清单)
13. [待补充信息（收紧规格用）](#13-待补充信息收紧规格用)

---

## 1. 产品目标与约束

### 核心路径

提示词 →（可配置参数）→ 远程生成 API → 任务/结果 → 相册展示 → 分享 / 下载。

### 体验约束

- 生成可能**异步**、耗时、失败需可重试。
- 参数来自**底部弹窗**，需与「创建」动作解耦，但与「当前生成配置」共用同一份状态。

### 平台

当前工程为 **Android（Kotlin + Jetpack）**；下文分层与模块可直接映射到具体实现（如 Compose、Room、Coroutines）。

---

## 2. 整体架构（分层 + 模块化）

```
┌─────────────────────────────────────────────────────────┐
│ Presentation（UI：Compose 等）                           │
│  • CreateScreen  • GalleryScreen  • SettingsScreen       │
│  • GenerationOptionsSheet（底部弹窗）                    │
└───────────────────────┬─────────────────────────────────┘
                        │ 单向数据流（MVVM / MVI）
┌───────────────────────▼─────────────────────────────────┐
│ Domain（用例 + 领域模型，不依赖 Android Framework）       │
│  • GenerateImage  • ObserveGenerations  • ExportImage  │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│ Data                                                     │
│  • Remote：ImageGen API / WebSocket（可选）               │
│  • Local：Room / 文件缓存 / DataStore（偏好）             │
└─────────────────────────────────────────────────────────┘
```

**推荐模式**：**MVVM + UseCase**（或 **MVI** 若需要可重放状态）。Domain 层保持纯 Kotlin，便于测试与替换后端。

---

## 3. 功能模块划分

| 模块 | 职责 | 主要界面/组件 |
|------|------|----------------|
| **create** | 提示词输入、触发生成、打开参数弹窗、进行中/错误态 | 创作页、`GenerationOptionsBottomSheet` |
| **gallery** | 列表/网格、详情（可选）、下载、分享 | 相册页、图片 Cell |
| **settings** | 用户资料、应用偏好、关于、缓存清理 | 设置页多 Section |
| **generation（核心领域）** | 参数校验、任务提交、轮询/订阅结果、落库 | UseCase + Repository |
| **media** | 缩略图、缓存目录、MIME、分享 Intent、下载到 MediaStore | `MediaRepository` / `ShareManager` |
| **config** | API Base URL、密钥策略、功能开关 | BuildConfig / Remote Config（可选） |

**依赖原则**：`UI → ViewModel → UseCase → Repository`。`gallery` 与 `create` 均依赖 `generation` 的查询能力，避免 ViewModel 间互相直接引用。

---

## 4. 领域与数据模型

### 4.1 核心实体

| 名称 | 说明 |
|------|------|
| **PromptDraft** | 当前输入框文案；可仅存内存，或持久为草稿。 |
| **GenerationParams** | 与底部弹窗对应：`resolution`（预设档位或 WxH）、`quality`、`sampler`；可扩展 `seed`、`steps`、`cfg` 等。 |
| **GenerationJob** | 单次任务：`jobId`、`status`（queued / running / succeeded / failed / cancelled）、`promptSnapshot`（提示词+参数快照）、`errorCode`/`message`、`createdAt`/`updatedAt`。 |
| **GeneratedImage** | 展示用：`localId`（UUID）、`remoteUrl` 或 `remotePath`、`localUri`（缓存后）、`thumbUri`（可选）、`width`/`height`/`fileSize`、关联 `jobId`。 |

### 4.2 用户与偏好

| 名称 | 说明 |
|------|------|
| **UserProfile** | 无账号体系时可占位：`displayName`、`avatarUri`。 |
| **AppPreferences**（DataStore） | 主题（system/light/dark）、通知总开关与渠道粒度（后期）、默认/上次生成参数；**不存 API 明文密钥**。 |

### 4.3 缓存与存储策略

- **元数据**：Room（或 SQLDelight）持久化 `GenerationJob`、`GeneratedImage`。
- **图片二进制**：应用缓存目录；大图 LRU；**清除缓存**：通常删除缓存文件；是否同时清理 DB 记录需产品定（常见：删文件后保留记录并显示需重新拉取/下载）。

---

## 5. 远程 API 接口设计

以下为逻辑接口（REST 示例），可按实际服务（自建 Comfy、SD API、云厂商）调整字段名。

### 5.1 提交生成

- **`POST /v1/generations`**
  - **Request**：`prompt`，`params`（resolution, quality, sampler, …），可选 `clientRequestId`（幂等）。
  - **Response**：`jobId`，`status`（多为 `queued`），可选 `estimatedSeconds`。

### 5.2 查询任务

二选一或并存：

- **`GET /v1/generations/{jobId}`** — 轮询。
- **`WebSocket /v1/generations`** — 推送进度（可选）。

**Response** 建议包含：`status`、`progress`（0–100）、`result`（成功时 `imageUrls[]` 或单 URL）、`error`。

### 5.3 签名 URL 刷新（可选）

若结果为对象存储签名 URL：返回 `expiresAt`；可加 **`POST /v1/generations/{jobId}/refresh-url`** 避免过期。

---

## 6. Repository 抽象（对内）

以下为对内接口清单（实现类在 Data 层），供实现时对齐边界。

```text
ImageGenerationRepository
  suspend fun submit(prompt, params): Result<JobId>
  fun observeJob(jobId): Flow<GenerationJob>
  suspend fun syncPendingJobs(): Result<Unit>   // 冷启动恢复未完成任务

GalleryRepository
  fun observeImages(): Flow<List<GeneratedImage>>
  suspend fun deleteImage(localId)

PreferencesRepository
  val appPreferences: Flow<AppPreferences>
  suspend fun updateTheme(...)
  // 其他偏好更新方法
```

---

## 7. 关键 UX 流程与状态机

### 7.1 创作 → 相册

1. 用户输入 `prompt`，底部弹窗设置 `GenerationParams`。
2. 点击「创建」：校验（空提示、长度、敏感词策略若需要）→ `submit` → 本地可选插入 `GenerationJob(running)`（乐观 UI）。
3. 轮询或 WebSocket → `succeeded` 时写入 `GeneratedImage` 并加载缩略图；失败则展示重试。
4. 相册页订阅 DB `Flow`，自动刷新列表。

### 7.2 相册：分享 / 下载

- **下载**：优先使用已缓存 `localUri`；否则网络下载 → 写入 `MediaStore`（如 `Pictures/YourApp/`）或应用目录并提示用户。
- **分享**：`FileProvider` + `ACTION_SEND`，`type = image/*`；注意分区存储与 URI 读权限（Android 10+）。

### 7.3 失败与重试

- 区分网络错误、业务错误（4xx）、服务端错误（5xx）；对可重试错误采用退避策略。
- 限制并发任务数（如 1～2），避免滥用 API。

---

## 8. 设置与子功能

| 子项 | 行为要点 |
|------|----------|
| 用户设置 | 展示/编辑 `UserProfile`；无后端时仅存本地。 |
| 清除缓存 | 删除图片缓存目录、取消进行中的下载；**preferences 默认保留**。 |
| 通知设置 | 若后续「生成完成」推送：通知渠道 + Android 13+ `POST_NOTIFICATIONS`。 |
| 主题设置 | `DataStore` + Material 主题 / DayNight。 |
| 应用信息 | 版本号、开源许可、隐私政策链接等。 |

---

## 9. 横切关注点

| 主题 | 建议 |
|------|------|
| **并发** | Kotlin Coroutines + 结构化并发；UseCase 内统一调度。 |
| **网络** | Retry + 指数退避；明确错误类型映射到 UI 文案。 |
| **安全** | API Key 优先走**服务端代理**或短时 Token；客户端禁止长期明文存储密钥。 |
| **隐私** | 提示词上传合规、日志脱敏、支持用户删除本地历史（若需要）。 |
| **可测试性** | Repository 接口 + Fake；Domain UseCase 单元测试。 |
| **可观测性** | 埋点：生成开始/成功/失败/下载/分享。 |

---

## 10. 依赖注入与导航

- **DI**：Hilt 或 Koin；按模块提供 Repository、UseCase、IO Dispatcher。
- **导航**：单 Activity + BottomNav（创作 / 相册 / 设置）；底部参数弹窗使用 `ModalBottomSheet`（Compose）或 `BottomSheetDialogFragment`。

---

## 11. 迭代交付顺序

1. **数据层 + Domain**：模型、`submit`/`poll`、Room、最小相册列表。
2. **创作页**：输入 + 创建 + 简单参数（可先写死或仅 DataStore）。
3. **底部弹窗**：与 `GenerationParams` 绑定并持久化「上次使用」。
4. **下载 / 分享**：FileProvider、存储权限与分区存储。
5. **设置页**：主题、缓存、关于；通知占位。
6. **健壮性**：失败重试、冷启动恢复未完成任务、限流与错误文案。

---

## 12. 与 Stitch 原型的对应检查清单

- [ ] 创作页「创建」→ 调用 `submit` + 任务状态 UI。
- [ ] 右上角设置 → 编辑 `GenerationParams` + 持久化（与创作共享状态）。
- [ ] 相册 → `observeImages()` 驱动 UI。
- [ ] 分享 / 下载 → `media` 模块，URI 来源与缓存解耦。
- [ ] 设置 → `preferences` + 关于 + 清除缓存。

---

## 13. 待补充信息（收紧规格用）

实现前建议明确以下三点，以便将本文 API 与存储细节固化为对接文档：

1. **目标 Android `minSdk` / 权限策略**（尤其通知与媒体写入）。
2. **后端形态**：自建网关 / 第三方；是否已有 OpenAPI 规范。
3. **生成模式**：同步直出图片 vs 异步 `jobId` + 轮询/推送。

---

## 附录：原型与工程内 UX 参考路径

- 工程内可参考：`ux/` 目录下 Stitch 导出 HTML 与 `ux/lumina_ai/DESIGN.md`（若有冲突以本文架构为准，UI 细节以 Stitch 为准）。
