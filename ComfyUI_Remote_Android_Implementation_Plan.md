# ComfyUI 远程调用 + Android 客户端 — 实现流程方案

本文档描述「本地 ComfyUI + 本地模型」被 Android 应用远程调用的**端到端实现流程**，与具体 UI 实现细节解耦。Android 端 UI 与 Stitch 对齐见同目录 `AIImageAndroid_Stitch_UX_Implementation_Plan.md`。

---

## 1. 目标与边界

| 项目 | 说明 |
|------|------|
| **目标** | 手机端输入提示词与参数 → 请求家中电脑上的 ComfyUI → 使用本地模型推理 → 将生成图片拉回手机展示/保存。 |
| **推理位置** | 始终在 PC 端 ComfyUI；手机不跑扩散模型。 |
| **边界** | 需解决**网络可达性**（局域网 / VPN / 内网穿透）；需**工作流与参数**在 App 与 API JSON 之间可映射。 |

---

## 2. 可行性结论

- **技术可行**：ComfyUI 提供 HTTP 提交任务、WebSocket（或轮询）获知进度、HTTP 拉取输出文件等能力；Android 使用常规网络栈即可对接。
- **主要风险**：公网直连家庭内网需穿透或 VPN；需避免无鉴权暴露 ComfyUI。

---

## 3. 总体架构

```
┌─────────────────┐     LAN / VPN / 隧道(HTTPS)     ┌──────────────────────────┐
│  Android App    │ ◄──────────────────────────────► │  PC: ComfyUI + 本地模型   │
│  (提示词/参数)   │         REST + WebSocket          │  (队列 / 推理 / output)   │
└─────────────────┘                                   └──────────────────────────┘
```

- App：**配置、表单校验、提交工作流、展示进度、下载图片、本地缓存与相册**。
- ComfyUI：**唯一执行端**；模型文件与自定义节点均在 PC 维护。

---

## 4. 网络方案选型

| 场景 | 方案 | 备注 |
|------|------|------|
| **仅家中使用** | 手机与 PC 同一 Wi‑Fi；ComfyUI 监听 `0.0.0.0:端口`；Windows 防火墙放行 | 最简单；Base URL 使用 PC 局域网 IP。 |
| **外出访问** | **Tailscale 等 Mesh VPN** 或 **Cloudflare Tunnel / frp** 等 | 公网禁止开放「裸」ComfyUI；穿透建议 HTTPS + 令牌。 |

---

## 5. ComfyUI 侧准备流程

1. **固定可运行的 API 工作流**  
   - 在 ComfyUI 中搭好文生图（或目标能力）图；导出 **API 格式** JSON。  
   - 标明可参数化字段：`prompt`、负向、`width`/`height` 或 latent 尺寸、`steps`、`cfg`、`seed`、`sampler_name`、`scheduler`、checkpoint 等。

2. **节点与 App 控件映射表（建议维护一张表）**  
   - 例：`prompt` → 某 `CLIPTextEncode` 正向节点；`KSampler` → `seed`、`steps`、`cfg`、`sampler_name`。  
   - App 只修改白名单字段，避免误改图结构。

3. **运行参数**  
   - 启动时允许局域网访问；记录实际端口（默认常见为 `8188`，以本机为准）。

4. **安全（推荐）**  
   - 反向代理 + Basic Auth / Bearer Token；或仅通过 VPN IP 访问。  
   - 若必须 HTTP（纯内网），限定路由器与防火墙来源。

---

## 6. Android 端与 ComfyUI 的交互流程（逻辑顺序）

以下步骤与 ComfyUI 版本有关，具体路径以当前官方文档为准，**逻辑顺序**保持一致即可。

| 步骤 | 动作 | 说明 |
|------|------|------|
| 0 | （可选）探活 / `object_info` | 校验 Base URL；可选填充采样器、模型列表。 |
| 1 | 组装 **Prompt API 负载** | 在模板 JSON 上写入用户参数；生成稳定 `client_id`（UUID）。 |
| 2 | `POST` 提交任务 | 获得 `prompt_id`。 |
| 3 | **WebSocket** 订阅进度 | 使用与请求一致的 `client_id`；解析排队、执行中、完成、错误。若不用 WS，则**轮询 history/队列**。 |
| 4 | 任务完成 | 从 history 或 WS 消息解析**输出文件名**与类型。 |
| 5 | `GET` 下载图片 | 使用 ComfyUI 提供的查看/静态路径获取二进制。 |
| 6 | App 侧 | 解码为位图、展示、写入缓存或 MediaStore；失败走重试/错误态。 |

### 状态机（建议）

- **Idle** → **Submitting** → **Queued/Running**（进度/不确定进度）→ **Success** / **Failed**  
- 失败区分：**网络不可达**（超时、DNS）vs **业务失败**（HTTP 错误、队列拒绝、节点执行错误）。

---

## 7. 数据与安全要点

- **Base URL**、可选 **Token** 存 **DataStore** 或加密偏好；禁止硬编码进仓库。  
- ** cleartext HTTP**：仅内网可用时在 Manifest / Network Security Config 中明确声明；生产穿透用 HTTPS。  
- **大图与磁盘**：缓存目录策略、用户清理缓存（与设置页「Clear cache」对应）。

---

## 8. 与 Stitch / Android UI 的衔接

- 本方案只定义**业务能力与协议层**；界面与交互以 Stitch 导出的 **Ethereal Canvas** 体系为准，见 `AIImageAndroid_Stitch_UX_Implementation_Plan.md`。  
- UI 上所有「生成、设置、结果、错误对话框」仅作为上述状态机与 API 调用的**呈现层**。

---

## 9. 交付阶段建议（端到端）

| 阶段 | 内容 |
|------|------|
| **E1** | PC 固定 API JSON + 映射表；命令行或最小脚本验证 `POST` + 取图。 |
| **E2** | Android：设置 Base URL；单次固定参数打通「提交 → 出图 → 下载」。 |
| **E3** | 接入完整参数与进度表现；两种错误态（网络 / 生成失败）。 |
| **E4** | Gallery 持久化、可选动态 `object_info`、多端工作流版本管理。 |

---

## 10. 文档索引

- Android UI 与模块规划：`AIImageAndroid_Stitch_UX_Implementation_Plan.md`  
- Stitch 设计源文件：`AIImageAndroid/ux/`（`DESIGN.md` + 各 `code.html`）
