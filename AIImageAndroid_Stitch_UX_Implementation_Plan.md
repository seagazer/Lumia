# AIImageAndroid — 按 Google Stitch（Ethereal Canvas）UX 实现方案

本文档基于已解压的 Stitch 导出目录 `AIImageAndroid/ux/`，规划 **Jetpack Compose + Material 3** 方向下的界面还原、导航与模块划分，以及与 ComfyUI 业务层的挂接点。端到端网络与 API 流程见同目录 `ComfyUI_Remote_Android_Implementation_Plan.md`。

---

## 1. 设计资产与权威来源

| 路径 | 内容 |
|------|------|
| `ux/lumina_ai/DESIGN.md` | **设计系统**：Soft Minimalism、色板（Primary `#6a1cf6` 等）、无 1px 描边分区、Manrope + Inter、云阴影、135° 渐变主按钮、AI Progress Shimmer 等。 |
| `ux/_1/code.html` | **创建主屏**：顶栏、`Prompt Narrative`、风格 chips、`Generate Art`、Recent 网格、**三 Tab 底栏**、Tune → **Generation Settings** Bottom Sheet（比例 / 画质 / 采样器 / Apply）。 |
| `ux/_2/code.html` | **结果详情**：大图、模型 Chip、Save / Share / Export、原文 Prompt、Adjust settings / Regenerate、SEED/SAMPLING/STEPS chips；底栏 **Gallery 选中**。 |
| `ux/_3/code.html` | 另一版创建 + **显式 Width/Height、画质滑块** 的 Bottom Sheet；底栏为 **四图标**，与 `_1` **不一致**。 |
| `ux/settings_screen/code.html` | **设置全页**：Account、App Settings、Information、底栏 **Settings 选中**。 |
| `ux/dialog_1/code.html` | **Network Error** 对话框（Retry / Cancel）。 |
| `ux/dialog_2/code.html` | **生成失败** 对话框 + 画布区加载占位（Try Again / Dismiss）。 |

**实现原则**：视觉与交互以 `DESIGN.md` + **`_1` / `_2` / `settings_screen` 三 Tab 结构**为主线；`_3` 中有价值的控件并入 Sheet 的「高级/扩展」区块，见第 7 节。

---

## 2. 工程现状（规划起点）

- 包名示例：`com.seagazer.aiimage`  
- 已有：`MainActivity.kt`、`ui/theme/Color.kt`、`Theme.kt`、`Type.kt`  
- **目标**：在不大改包结构的前提下引入 Navigation、各 Feature 模块或源码分包。

---

## 3. 设计系统落地（Compose）

| 类别 | 规划 |
|------|------|
| **颜色** | 在 `Color.kt` / `Theme.kt` 中落实 `DESIGN.md` 与 HTML tailwind 中的 surface 层级、`on_surface`、`primary`、`primary_container`、`error` 等；避免用纸面硬编码散落。 |
| **渐变** | 主 CTA：`primary` → `primary_container`，135°；与 HTML `bg-gradient-to-br` 视觉对齐。 |
| **圆角** | `DEFAULT` / `lg` / `xl` 对应约 16dp / 24dp（以设计密度校准）；图片容器 `lg`（约 16dp）。 |
| **字体** | Manrope：标题 / Display；Inter：正文与 Label；metadata 使用全大写 + letterSpacing（如 SEED、SAMPLING）。 |
| **图标** | 统一 **Material Symbols**（Outlined / Fill 与设计稿一致），与 HTML `material-symbols-outlined` 对齐。 |
| **顶栏 / 底栏** | 半透明白 + 模糊：Compose 使用合适 Blur API，低版本降级为纯色 + 轻阴影；阴影遵循「云阴影」（低透明度 `on_surface`）。 |
| **无描边** | 分区靠 surface 色阶；必要时 `outline_variant` 极低透明度，符合 DESIGN.md「Ghost Border」。 |
| **加载** | 生成中：`DESIGN.md` 的 **Shimmer / aura**，用于大图占位（参考 `dialog_2` 与 `_2` 中 pulse 层）。 |

---

## 4. 信息架构与导航

采用 **单 Activity + Navigation Compose**，底栏 **三 Tab**（与 `_1`、`_2`、`settings_screen` 一致）。

```text
BottomNav: Create | Gallery | Settings

CreateScreen
  ├─ 打开 Tune → GenerationSettingsSheet (Modal Bottom Sheet)
  ├─ Generate Art → 生成流程（Shimmer）→ ResultDetailScreen（可同栈 push 或全屏路由）
  └─ Recent / View Gallery → 列表或直达 Gallery Tab

GalleryScreen
  └─ 点击项 → ResultDetailScreen

SettingsScreen
  └─ 全页滚动（Account / App / Information）

全局
  ├─ NetworkErrorDialog（对齐 dialog_1）
  └─ GenerationFailedDialog（对齐 dialog_2）
```

- **ResultDetail** 非底栏 Tab，由 **导航栈** 进入；返回后根据产品决定是否回 Create 或留在 Gallery。  
- Header 上 **菜单（menu）** 按钮：HTML 无展开态 —— 需在实现时定案：**NavigationDrawer**、省略、或等价跳转 **Settings**。

---

## 5. 分屏实现对照表

### 5.1 Create（`ux/_1/code.html`）

| UI 区域 | 实现要点 | 数据绑定 |
|---------|-----------|----------|
| 顶栏 | menu、标题「Ethereal」、Tune、头像 | Tune → 打开 Sheet；头像可占位 |
| Hero 文案 | 大标题 + 副标题 | `strings.xml` |
| Prompt | 多行、`Prompt Narrative` label | `prompt` |
| 风格 chips | 可选中/标签云 | 映射 prompt 后缀或配置表 |
| Generate Art | 渐变主按钮 + `auto_awesome` | 触发生成；禁用条件：空 prompt 等 |
| Recent Studio Work | 2 列网格 + 一大图 | 本地历史或占位；点击进入 Result / Gallery |
| Tune Sheet | 比例 1:1/16:9/9:16/4:3；画质 Low/Med/High；Sampler 下拉；Apply | 写入 `GenerationSettings`；关闭 Sheet |
| BottomNav | Create 高亮 | 当前路由 |

### 5.2 Result Detail（`ux/_2/code.html`）

| UI 区域 | 实现要点 | 数据绑定 |
|---------|-----------|----------|
| 顶栏 | 返回、Ethereal、`more_vert`、头像 | 返回 popBackStack |
| 主图 | 圆角容器、可选 Shimmer 层 | 生成结果 URI / Bitmap |
| 模型 Chip | glass 风格条 | checkpoint 显示名或默认文案 |
| 主操作 | Save to Gallery（渐变） | MediaStore |
| 次操作 | Share、Export | FileProvider + Intent |
| 侧栏 / 下方信息卡 | Original Prompt、Adjust settings、Regenerate | prompt / 打开 Sheet / 新 `prompt_id` |
| Metadata chips | SEED、SAMPLING、STEPS | 自 ComfyUI history 解析 |
| BottomNav | Gallery 高亮 | 若从 Gallery 进入则保持 Gallery 选中语义 |

### 5.3 Settings（`ux/settings_screen/code.html`）

| 区块 | 实现要点 |
|------|----------|
| Account | 个人信息、Edit Profile、Subscription；MVP 可静态 + 占位 |
| **ComfyUI 连接（建议新增）** | **Base URL**、可选 Token、连接测试；样式与同页卡片一致（设计未画但为功能必需） |
| App Settings | Clear cache、Notification、Theme Light/Dark |
| Information | About、Privacy、Terms |
| 底部品牌 | 「LUMINA」与 Version —— 与顶栏「Ethereal」统一策略见第 7 节 |
| BottomNav | Settings 高亮 |

### 5.4 对话框

| 资源 | 触发 | 按钮 |
|------|------|------|
| `dialog_1` | 网络不可达、超时 | Retry、Cancel |
| `dialog_2` | 生成失败 | Try Again、Dismiss |

布局：`surface_container_lowest`、圆角 `xl`、图标圆形底、主按钮渐变、次按钮 Ghost，与 HTML 一致。

---

## 6. 与 ComfyUI 层的挂接点（仅界面侧职责）

| 用户操作 | UI 表现 | 下层接口（概念） |
|----------|---------|------------------|
| Apply / Update 设置 | 关闭 Sheet，持久化 | `GenerationSettingsRepository` |
| Generate | 按钮 loading、Shimmer | `ComfyRepository.submit(...)` |
| 进度 | Shimmer 或 indeterminate | Flow / WS |
| 成功 | 导航 Result + 解码图 | `download + cache` |
| 失败 | `dialog_2` | 错误类型映射 |
| 网络异常 | `dialog_1` | IOException / 超时 |

具体 REST/WS 见 `ComfyUI_Remote_Android_Implementation_Plan.md`。

---

## 7. Stitch 多版本差异 — 归并策略（拍板建议）

| 问题 | 建议 |
|------|------|
| `_1` 三 Tab vs `_3` 四 Tab | **统一三 Tab**；`_3` 不作为底栏权威。 |
| `_1` Sheet（比例档位）vs `_3` Sheet（像素宽高 + 滑块） | **默认**：比例档位 → 内部映射固定 `width×height` 表；**高级**：折叠「自定义宽高」或「与 `_3` 一致滑块」二选一，避免两套主界面。 |
| 品牌名 Ethereal / Ethereal Canvas / LUMINA | `strings.xml` 定 **单一产品名**；关于页可保留副品牌。 |
| `_3` 底栏 icon 与 `_1` 不一致 | 以 `_1` + `settings_screen` 图标与文案为准。 |

---

## 8. 源码结构建议（可渐进式）

```text
app/src/main/java/com/seagazer/aiimage/
├── MainActivity.kt
├── ui/
│   ├── theme/
│   ├── navigation/          # NavHost, BottomBar
│   ├── create/              # CreateScreen, GenerationSettingsSheet
│   ├── result/              # ResultDetailScreen
│   ├── gallery/             # GalleryScreen
│   ├── settings/            # SettingsScreen
│   └── dialogs/             # NetworkError, GenerationFailed
├── domain/                  # （可选）GenerationSettings, models
└── data/                    # （可选）ComfyApi, DataStore, cache
```

是否拆成多 module（`:core:designsystem`、`:feature:create`）由团队规模决定；MVP 可单 module 分包。

---

## 9. 依赖与工程级规划（实现阶段启用）

- Navigation Compose、ViewModel、Lifecycle、DataStore。  
- 网络：OkHttp（可选 Retrofit）；JSON：Kotlin Serialization 或 Moshi。  
- 图片：Coil。  
- Coroutines + Flow（UI 状态与 WS/轮询）。

具体版本与 `gradle/libs.versions.toml` 对齐，不在此文定死版本号。

---

## 10. 实施阶段（仅 Android + UX）

| 阶段 | 交付物 |
|------|--------|
| **P1** | 三 Tab 壳 + Create / Settings / Gallery 占位；Theme 贴合 `DESIGN.md`。 |
| **P2** | Create 完整布局 + Tune Sheet + Result + 两对话框；本地假数据与导航打通。 |
| **P3** | 接入 ComfyUI Repository；真生成与错误态。 |
| **P4** | Gallery 持久化、Recent 数据源、动态 `object_info`（可选）。 |

---

## 11. 参考路径

- Stitch 导出：`AIImageAndroid/ux/`  
- 远程 ComfyUI 流程：`ComfyUI_Remote_Android_Implementation_Plan.md`  
- Android 工程：`AIImageAndroid/`
