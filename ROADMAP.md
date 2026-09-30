# PaiCLI 迭代路线图（21 期）

从零开始，逐步构建生产级 Java Agent CLI

---

## 第1期：基础ReAct + Tool Call ✅

**已完成**

- ReAct循环（思考-行动-观察）
- DeepSeek V4.1 Flash 接入（模型 ID `deepseek-flash`，默认 provider），也可切换 GLM、Kimi 等 OpenAI 兼容模型
- 基础工具：读写与精确编辑文件、列目录、执行 Shell 命令、创建项目
- 交互式CLI

> 模型说明：第 1 期默认接入 DeepSeek V4.1 Flash。模型迭代很快，后续默认模型可能继续升级，请以 `.env.example` 和 README 为准。

**核心知识点**：ReAct模式、Function Calling、Agent基础架构

---

## 第2期：Plan-and-Execute + 多轮规划 ✅

**目标**：让Agent能处理复杂多步任务

**功能迭代**：
- Plan-and-Execute模式（先规划后执行）
- 任务分解（Task Decomposition）
- 子任务依赖管理
- 执行计划可视化
- 计划失败时的重规划

**核心知识点**：
- Plan-and-Solve模式
- 任务DAG管理
- 规划-执行分离架构

**教程标题候选**：《Agent只会一步一步执行？教它先规划后行动，复杂任务也能搞定》

---

## 第3期：Memory系统 + 上下文工程 ✅

**目标**：让Agent有记忆，能处理长对话

**功能迭代**：
- 短期记忆（对话历史管理）
- 长期记忆（关键信息持久化）
- 上下文压缩（摘要生成）
- Token预算管理
- 记忆检索（相似度匹配）

**核心知识点**：
- Context Window管理
- 记忆分层架构
- 摘要算法（Map-Reduce）

**教程标题候选**：《Agent记性太差？给它装上记忆系统，长对话也不忘事》

---

## 第4期：RAG检索 + 代码库理解 ✅

**已完成**

**目标**：让Agent能理解整个代码库

**功能迭代**：
- 代码向量化（Embedding），支持本地 Ollama 和远程 API
- 向量数据库（SQLite + 内存余弦检索）
- 代码分块与索引（文件/类/方法粒度）
- 语义检索（自然语言搜代码）
- 代码关系图谱（类、方法依赖）

**核心知识点**：
- RAG架构
- Code Embedding
- 向量检索
- AST 分析

**教程标题候选**：《Agent看不懂你的代码库？接入RAG，让它秒懂项目结构》

---

## 第5期：Multi-Agent协作 + 角色分工 ✅

**已完成**

**目标**：多个Agent协作完成复杂任务

**功能迭代**：
- Agent角色定义（规划者、执行者、检查者）
- Agent间通信机制
- 任务分配与协调
- 冲突解决策略
- 主从Agent架构

**核心知识点**：
- Multi-Agent系统
- 角色扮演（Role Playing）
- 分布式任务协调

**教程标题候选**：《一个Agent忙不过来？搞个团队，规划、执行、检查分工干》

---

## 第6期：Human-in-the-Loop + 审批流 ✅

**已完成**

**目标**：关键操作人工确认，安全可控

**功能迭代**：
- 危险操作静态规则识别（`write_file`、`execute_command`、`create_project`）
- 三级危险等级（高危 / 中危 / 安全）
- 审批决策：批准 / 全部放行 / 拒绝 / 跳过 / 修改参数后执行
- 交互式 CLI 默认 auto（Shell 命令由模型分类器审查），`/hitl on|default` 运行时切换
- `HitlToolRegistry` 透明拦截层，HITL 关闭时与普通 `ToolRegistry` 行为完全相同

**HITL 增强（后续补丁，归在本期叙事下）**：
- `PathGuard` 路径围栏：`read_file` / `write_file` / `list_dir` / `create_project` 强制限定在项目根之内，拦截绝对路径越界、`..` 穿越、符号链接逃逸
- `CommandGuard` 命令快速拒绝：HITL 之前的 fast-fail 黑名单（sudo / rm -rf 全盘 / mkfs / dd 写裸设备 / fork bomb / curl|sh / find / / chmod 777 / / shutdown），减少 HITL 弹窗骚扰
- `AuditLog` 操作审计链：危险工具调用按天写 JSONL 到 `~/.paicli/audit/`，含 `outcome (allow|deny|error)` 与 `approver (hitl|policy|none)`
- `write_file` 单文件 5MB 上限
- CLI 命令：`/policy` 看安全策略状态、`/audit [N]` 看最近审计

**为什么不叫沙箱**：
- 真正的沙箱是隔离的执行环境（Docker / microVM / chroot），本地 Agent CLI（参考 Claude Code / Cursor / Aider）默认都不做沙箱——沙箱削弱 Agent 能力、给虚假安全感、体验更差
- PaiCLI 的安全模型是 **HITL + 路径校验 + 命令快速拒绝 + 审计**，不是隔离
- 想做容器隔离的请参考 Pro 升级版本章节，或自行实现 `SandboxDriver` 接口

**核心知识点**：
- HITL（人机协同）
- 中断处理
- 安全策略
- 路径解析与符号链接安全（`Files.toRealPath` 防逃逸）
- 结构化审计（JSONL、按天分文件、并发安全）

**教程标题候选**：《Agent权限太大怕搞砸？加上人工审批，安全又放心》

---

## 第7期：异步执行 + 并行工具调用 ✅

**已完成**

**目标**：提升执行效率，支持长时间任务

**功能迭代**：
- 同一轮 LLM 返回多个 `tool_calls` 时并行执行
- ReAct、Plan-and-Execute、Multi-Agent Worker 复用统一批量工具执行入口
- Plan-and-Execute 按 DAG 依赖批次并行执行独立任务
- Multi-Agent 按依赖批次并行调度多个 Worker
- 工具批次统一超时，超时工具会被取消并返回可回灌结果

**核心知识点**：
- 异步编程模型
- 并发控制
- 任务调度

**教程标题候选**：《Agent执行太慢？上异步+并行，编译测试一起跑》

---

## 第8期：多模型适配 + 运行时切换（GLM / DeepSeek / StepFun / Kimi）✅

**已完成**

**目标**：支持多模型运行时切换，当前包含 GLM-5.1、DeepSeek V4、StepFun 和 Kimi K2.6

**功能迭代**：
- `LlmClient` 接口抽象：将 GLMClient 的内部类型（Message、ToolCall、Tool 等）提升为接口级公共类型
- `AbstractOpenAiCompatibleClient` 基类：共享 SSE 流式解析、请求构建、工具调用增量合并逻辑
- `GLMClient` / `DeepSeekClient` / `StepClient` / `KimiClient` 瘦子类：仅提供 API URL、模型名、API Key 与 provider 差异
- 运行时模型切换：`/model glm-5.1` / `/model glm-5v-turbo` 明确切 GLM 模型；`/model deepseek` / `/model step` / `/model kimi` 切 provider 并读取配置里的具体模型
- 配置持久化：`~/.paicli/config.json` 存储默认模型，支持 `.env` 回退读取 API Key
- `LlmClientFactory` 工厂：根据 provider 名称和配置创建对应客户端
- 手动模型管理：list / refresh / add；支持同供应商能力模板、配置覆盖、显式切换，刷新不自动启用新模型，失败保留原配置。
- 2026-09-25：DeepSeek 默认升级为 `deepseek-flash`（V4.1 Flash），补齐新名称的思考参数、DSML 兼容和图片输入；旧 Flash 别名兼容，V4 Pro 保留文本模式。历史评测合同不变。同日 DeepSeek 成为默认 provider，Key 回退顺序改为 deepseek 在前。

**核心知识点**：
- 策略模式 + Provider 抽象
- OpenAI 兼容协议
- 模板方法模式（AbstractOpenAiCompatibleClient）
- 运行时配置管理

**教程标题候选**：《只能用一个模型？策略模式 + 模板方法，GLM 和 DeepSeek 随时切换》

---

## 第9期：联网能力 + Web工具

**目标**：让 Agent 能访问互联网，获取实时信息（不涉及浏览器操控，那部分见第13/14期）

**功能迭代**：
- `web_search` 工具升级：在第7期 SerpAPI 最小落地的基础上，把搜索结果结构化、字段稳定化；已接入智谱 / SerpAPI / SearXNG / DeepSeek 原生搜索，DeepSeek 复用 API Key，通过独立 Messages 请求获取结构化来源
- `web_fetch` 工具：抓取指定 URL 页面内容，自动提取正文（去除 HTML 标签 / 广告 / 导航）
- 搜索结果摘要：LLM 对检索结果二次提炼，只保留与用户问题相关的信息
- 网络访问安全：URL 白名单 / 黑名单、请求频率限制、响应体大小限制
- Agent 提示词升级：让 Agent 知道何时该用联网工具（如"最新版本是什么"、"官方文档怎么说"），以及和本地工具的边界

**核心知识点**：
- 搜索引擎 API 集成
- HTML 正文提取（Jsoup / Readability 算法）
- 网络访问安全策略
- Agent 工具选择 prompt 设计

**教程标题候选**：《Agent 与世隔绝？让它学会搜索和抓取，实时信息一手到位》

---

## 第10期：MCP 协议核心（stdio + Streamable HTTP，默认开启） ✅

**已完成**

**目标**：把 PaiCLI 接入 MCP 生态。stdio 子进程 server 与 Streamable HTTP 远程 server 都能用，工具自动注册到 ToolRegistry，与 HITL / AuditLog 协同。

**功能迭代**：
- 手写 `JsonRpcClient`：JSON-RPC 2.0 客户端，请求-响应配对、通知、错误码、超时
- `McpTransport` 抽象 + 两个实现：
  - `StdioTransport`：ProcessBuilder + newline-delimited JSON-RPC，stderr 单独 drain，JVM 退出 hook 清理子进程
  - `StreamableHttpTransport`：OkHttp + 单 POST + 服务端 SSE 流式响应，支持 session ID
- `initialize` 握手 + capabilities 协商 + protocol version negotiation
- `tools/list` + `tools/call`：工具按 `mcp__{server}__{tool}` 前缀注册到 `ToolRegistry`
- MCP 返回 `content` 数组扁平化（text 拼接，image / resource 给 fallback 提示）
- 配置文件：`~/.paicli/mcp.json`（用户级）+ `.paicli/mcp.json`（项目级，可入 git），格式与 Claude Code `claude_desktop_config.json` 兼容
- 启动时 eager 并行启动所有 server（复用第 7 期并行调度）
- **默认开启**，`/mcp disable <name>` 关单个
- HITL + AuditLog 集成：MCP 工具默认走 HITL，audit `tool` 字段带 `mcp__` 前缀
- CLI：`/mcp` / `/mcp restart <name>` / `/mcp logs <name>` / `/mcp disable <name>` / `/mcp enable <name>`
- MCP 子系统默认启动；未配置 `mcp.json` 时不启动外部 server，避免首次运行被 `npx` / `uvx` 冷启动阻塞

**核心知识点**：
- JSON-RPC 2.0 协议实现
- 长 running 子进程生命周期管理（NIO + 流分离）
- Streamable HTTP（2025 年 3 月新规范，替代已废弃的 SSE）
- 第三方工具源进入安全模型的纳管方式（HITL + Audit + 命名空间隔离）

**估算**：5–6 天

---

## 第11期：MCP 高级能力（resources 双轨 + prompts 查看 + 被动通知） ✅

**前置依赖**：第 10 期 MCP 协议核心

**目标**：优先补齐 MCP resources 体验，对齐 Claude Code 的资源引用方式，并提供 prompts 查看、被动通知处理与运行中取消。OAuth 与 sampling 已确认延后，不计入本期交付。

**功能迭代**（详细开发任务见 `docs/phase-11-mcp-advanced.md`）：

- **resources 双轨**（参考 Claude Code）：
  - 工具层：每个支持 resources 的 server 注册 `mcp__{server}__list_resources` / `mcp__{server}__read_resource` 虚拟工具，让 LLM 自决
  - 用户 @-mention 层：`@server:protocol://path` 语法 + jline 自动补全，输入预处理时 fetch 内容并替换为 `<resource>` 内联块
  - `resources/list_changed` / `resources/updated` 到达后只做缓存失效，下次 read/list 重拉
- **prompts 查看**：`/mcp prompts <server>` 展示 server 暴露的 prompt 模板；不加载到对话流
- **双向通知（被动）**：
  - `tools/list_changed` → 重拉工具列表 → `replaceMcpToolsForServer` 全量替换
  - `resources/list_changed` / `resources/updated` → cache 失效
  - **不做 health ping**，不主动探活，避免对按量或按月计费 server 造成额外负担
- **新增 CLI**：`/mcp resources <server>`、`/mcp prompts <server>`
- **运行中取消**：任务执行期间输入 `/cancel` 并回车，请求取消当前 Agent run；ReAct、Plan、Team、工具批次与 `execute_command` 在边界处协同检查取消信号

**不做（明确边界）**：
- OAuth 2.0 Authorization Code + PKCE
- `sampling/createMessage`
- MCP server 自动重启
- prompts 加载到对话流（仅保留 `/mcp prompts` 查看 server 暴露的模板）
- resources 自动注入 system prompt（第 12 期长上下文模式已接入 URI / 描述索引）
- server health ping / heartbeat
- progress / logging notification 的 UI 展示
- OAuth Device Flow / Client Credentials

**核心知识点**：
- MCP resources/list + resources/read 的工具化封装
- 用户显式 `@server:protocol://path` resource 引用与上下文注入
- jline `Completer` 与 raw mode 的协同（@-mention autocomplete 不能干扰 plan/team raw mode 路径）
- 被动通知响应模式 vs 主动 ping 的取舍（按月计费的 server 必须不主动 ping）

**验证**：`mvn test` 336 tests 通过

---

## 第12期：长上下文工程（适配 200k–1M 模型 + prompt caching） ✅

**目标**：适配 GLM-5.1（200k）/ DeepSeek V4（1M）/ StepFun（256k）/ Kimi K2.6（256k）/ Claude Sonnet 4.6（1M）等长上下文模型。第 3 期 Memory 是基于"短上下文兜底"假设设计的，长窗口下要切换策略。

**功能迭代**（详细开发任务见 `docs/phase-12-long-context.md`）：
- `LlmClient` 接口扩展能力声明：`maxContextWindow()` / `supportsPromptCaching()` / `promptCacheMode()`
- `ContextProfile` 统一管理 short / balanced / long 三种上下文模式
- `AgentBudget` token 预算从写死 300K 改为按当前模型动态计算（默认 80% × maxContextWindow，仍支持系统属性覆盖）
- 长 / 短上下文双模式：
  - 短 / balanced：保留第 3 期 Memory 摘要压缩策略
  - long（≥ 100k window）：跳过摘要压缩，提高 RAG 默认 topK（20），扩大短期记忆预算
- prompt caching 接入：
  - 能力声明与 `/context` 可见化
  - OpenAI-compatible usage 中解析 cached input tokens
  - DeepSeek V4 走 automatic prefix cache；当前不注入未确认兼容的 provider 私有字段
- 上下文成本可见化：每轮输出 `已用 X / Y token (window W, cached: Z, 估算 ¥A)`
- 检索策略自适应：`search_code` 未传 `top_k` 时按上下文模式选择 5 / 10 / 20
- **MCP resources 自动注入**（与第 11 期联动）：长模式下，把所有 server 已知 resources 的 URI + 描述（不含 body）作为索引注入 system prompt；ReAct / Plan / Team 都接入
- `/context` 命令扩展：显示当前 window、动态预算、模式、prompt cache、RAG topK、resources 是否已自动注入

**核心知识点**：
- 长上下文模型的成本模型（input vs cached input 价差通常 5–10 倍）
- prompt caching 的缓存边界设计
- RAG 在长上下文时代的角色变化（从"压缩选择"到"加速 + 精排"）
- 资源索引（MCP resources URI + 描述）作为长上下文的有效填充

**验证**：`mvn test` 347 tests 通过；`mvn clean package` 通过

---

## 第13期：Chrome DevTools MCP ✅

**前置依赖**：第 10 / 11 期 MCP 框架（已完成）

**目标**：让 Agent 能操控浏览器，处理需要 JS 渲染、防爬墙、表单交互、登录态的页面（如微信公众号文章、知乎专栏、SPA 应用等）。

**功能迭代**（详细开发任务见 `docs/phase-13-chrome-devtools-mcp.md`）：

- 接入 Google 官方 `chrome-devtools-mcp@latest`（28 个工具：导航 / 输入 / 调试 / 网络 / 性能 / 模拟 / 扩展 / 内存）
- **默认 enabled**：`~/.paicli/mcp.json` 不存在时启动自动创建模板，含 chrome-devtools 条目
- `image` content 处理走**路线 B**：fallback 文案引导 LLM 优先用 `take_snapshot`（DOM 文本快照）而非 `take_screenshot`；不做真 图片复制粘贴输入（拆到第 21 期）
- HITL「全部放行」改为 **server 维度**：用户对 chrome-devtools 选 `a → server` 后，连续浏览器操作只需确认一次（`approvedAllByServer` 集合 + 子菜单）
- `Agent` / `PlanExecuteAgent` / `SubAgent` 系统提示词加「web_fetch vs 浏览器 MCP」决策表，明示微信公众号 / 知乎 / 推特等典型 web_fetch 失败站点直接走浏览器
- `McpClient.initialize` 超时 30s → 60s（chrome-devtools 首次启动需 npx 拉包 + Chrome 冷启 ≈ 20s+），可被 `paicli.mcp.initialize.timeout.seconds` 覆盖
- `McpServerManager.startAll` 启动期间另起 status printer 线程，每 5s 打印未就绪 server 等待时长
- 必跑端到端测试：微信公众号文章（`https://mp.weixin.qq.com/s/RB7kF_BbsJZ5_Hmu9PxWdg`），验证 web_fetch 失败 → LLM 自动 fallback 到浏览器 → take_snapshot 拿正文

**不做（明确边界）**：
- 真 图片复制粘贴输入（拆到第 21 期「图片复制粘贴输入」）
- CDP 会话复用 / 登录态识别（第 14 期）
- Playwright / Firefox / WebKit 跨浏览器
- 浏览器执行隔离（默认 `--isolated=true` 临时 user-data-dir，第 14 期通过 `--autoConnect` 或旧式 `--browser-url` 复用已开 Chrome）

**核心知识点**：
- 第三方 MCP server 接入实战（直接用 Google 官方 server，不再造轮子）
- HITL 全放行的多维度设计（tool 维度 vs server 维度）
- LLM 自动决策 fallback 路径（web_fetch 拿不到 → 提示词引导走浏览器）
- 长启动 server 的 UX 工程（进度提示 + 超时调整）

**教程标题候选**：《静态抓取不够看？接 Chrome DevTools MCP，让 Agent 自己开浏览器》

**验证**：单元测试覆盖默认 MCP 配置创建、HITL server 维度全放行、MCP image fallback 与初始化超时；真实浏览器端到端需本机 Chrome + API Key 环境执行。

---

## 第14期：CDP 会话复用 + 登录态访问 ✅

**前置依赖**：第13期 Chrome DevTools MCP 已能驱动浏览器

**目标**：让 Agent 复用带登录态的调试 Chrome 实例，访问需要认证的页面

**功能迭代**：
- 通过 Agent 内部 `browser_connect` 或 `/browser connect` 按需切到 `--autoConnect`，复用已在 `chrome://inspect/#remote-debugging` 允许远程调试的 Chrome；`/browser connect <port>` 保留旧式 `--browser-url=http://127.0.0.1:<port>` 兼容路径
- 复用调试 Chrome 登录态访问 GitHub、内部系统等需认证页面；默认 `mcp.json` 仍保持 `--isolated=true`
- `/browser status` / `/browser tabs` / `/browser disconnect` 提供会话状态、tab 查看和回到 isolated 的入口
- 登录态访问安全约束已落地：敏感页面识别、改写型工具单步 HITL、shared 模式 `close_page` 硬保护
- 审计日志为 chrome-devtools 工具追加浏览器 metadata，同时兼容旧 JSONL

**核心知识点**：
- Chrome 远程调试端口工作机制
- 登录态复用与隔离
- 认证页面的安全策略

**教程标题候选**：《要登录才能看？让 Agent 连上你的调试 Chrome，省掉重复打开页面的麻烦》

---

## 第15期：Skill 系统 + web-access Skill ✅

**已完成**

**前置依赖**：第 9 期 web 工具、第 13 期 Chrome DevTools MCP、第 14 期 CDP 会话复用全部就绪

**目标**：做出 PaiCLI 自己的 Skill 加载机制，把零散的工具与决策指引打包成可复用单元，并以 web-access 作为首个落地 Skill

**功能迭代**（详细开发任务见 `docs/phase-15-skill-system.md`）：
- Skill 加载机制：三层目录扫描（jar 内置 / 用户级 `~/.paicli/skills/` / 项目级 `<project>/.paicli/skills/`），按 name 整体覆盖，frontmatter 走手写 YAML 子集解析（不引 SnakeYAML）
- 启动期把启用 skill 的 `name` + `description` 注入 system prompt 索引段（单 description ≤ 500 codepoint，启用上限 20 个，索引段 ≤ 4KB）
- 内置工具 `load_skill(name)`：LLM 主动调用，SKILL.md 正文在同一轮工具结果之后以独立 user 消息注入（lazy 展开，节省 token）
- 2026-09-25：修复正文要等用户下一条消息才注入的问题，移除共享的 `SkillContextBuffer`，改为 `LoadedSkillMessages` 按本批工具结果同轮注入，并行任务不再串 skill
- 内置 web-access Skill：决策手册（浏览哲学四步法 + 工具选择表 + 浏览器优先级 + Jina 兜底说明）+ 6 个站点经验文件（mp.weixin / zhuanlan.zhihu / x.com / xiaohongshu / github / juejin）+ cdp-cheatsheet
- 启动期 `SkillBuiltinExtractor` 把 jar 内置 skill 解压到 `~/.paicli/skills-cache/`，按 `.version` 文件控制重建
- CLI 命令：`/skill` / `/skill list` / `/skill show <name>` / `/skill on <name>` / `/skill off <name>` / `/skill reload`
- Jina Reader 集成：**只**在 web-access SKILL.md 写入「web_fetch 失败可让 execute_command 调 r.jina.ai」的提示，**不**改 `web_fetch` 工具内部链路（保持第 9 期纯本地约定）
- Skill 与 HITL 协同：Skill 内调用 `execute_command` / 浏览器 MCP 等危险工具仍走 HITL 审批，沿用 `execute_command` 工具维度全放行；不给 Skill 单独审批维度

**核心知识点**：
- 提示词工程的工程化封装
- 触发词路由与按需加载
- 经验沉淀目录（按域名/场景累积可复用知识）
- 设计意图：从「写工具」演进到「打包专家手册」

**教程标题候选**：《工具堆成山，Agent 还是不会用？给它写本「专家手册」，按场景自动展开》

**验证**：`mvn test` 457 tests 通过；`mvn clean package` 通过

---

## 第16期：TUI界面 + 产品化 ✅

**目标**：从CLI到完整产品体验

**功能迭代**：
- 终端TUI界面（Lanterna/JLine）
- 文件树浏览
- 代码高亮显示
- 对话历史可视化（`~/.paicli/history/session_*.jsonl`）
- 配置文件管理（TUI `/config` 面板）
- TUI 输入桥接真实 ReAct / Plan / Team 执行链
- TUI HITL 模态审批（批准 / 拒绝 / 跳过）
- 安装包分发

**第 16.1 期形态修正（v16.1.0）**：
- 抽出 `Renderer` 接口 + 三个实现：inline 流式（默认）/ lanterna 全屏（保留）/ plain 兜底
- 默认形态切换为 **inline 流式 TUI**（Claude Code 风格），主屏直出 + 底部 DECSTBM 状态栏 + 行内可折叠工具块（`ctrl+o`）+ 行内 diff
- HITL 改为单字符 `[y/n/a/s/m]` 提示；`/config` 改为浮起 palette
- 切换：`PAICLI_RENDERER=inline|lanterna|plain`，旧 `PAICLI_TUI=true` 兼容映射到 lanterna

**活动折叠与编辑提示打磨（参考 Claude Code，2026-09-24 从需求笔记并入）**：

![](https://cdn.tobebetterjavaer.com/paicoding/CLAUDE-2b7755723b1c47f8862bae4999504467.png)

![](https://cdn.tobebetterjavaer.com/paicoding/CLAUDE-b175c64d08c1408e95c4f10dcc91a77f.png)

- ✅ 按工具类型折叠活动块（“📖 读取 N 个文件”“✏️ 编辑 N 个文件”等），`ctrl+o` 展开
- ✅ `write_file` / `edit_file` 成功后展示类似 git 的行内 diff
- ⬜ 同一轮多种工具合并为一行活动摘要，例如“读取 1 个文件，搜索了记忆”，对齐 Claude Code 的 “Read 1 file, searched memories”
- ⬜ `glob_files` / `grep_code` / `load_skill` / `revert_turn` / 浏览器工具补专用标签，目前落到默认的“🔧 工具名 × N”

**核心知识点**：
- TUI开发
- 终端渲染（DECSTBM、ANSI 局部重绘、JLine widget 绑定）
- 产品工程化（接口抽象 + 多形态切换）

**教程标题候选**：《CLI太简陋？做个漂亮的TUI界面，体验不输Claude Code》

---

## 第17期：LSP 诊断注入（开发体验安全网）

**前置依赖**：第 6 期 HITL 审批流、第 16 期 TUI 产品化

**目标**：Agent 改完代码后，立即注入编译诊断，而不是等用户手跑 `mvn compile` 再告诉 Agent。对标 Claude Code / DeepSeek TUI 的招牌功能。

**功能迭代**：

- `LspManager`：按语言惰性启动 LSP server（JDT LS for Java、rust-analyzer for Rust、pyright for Python、gopls for Go），通过 stdio JSON-RPC 通信，per-language transport pool 复用连接
- 最小 LSP 协议子集：`initialize` → `textDocument/didOpen` → `textDocument/didChange` → 收集 `textDocument/publishDiagnostics`
- `LspHooks`：挂接在 `ToolRegistry.executeTool()` 的执行后路径上——`edit_file` / `apply_patch` / `write_file` 成功后，对编辑的文件调 `runPostEditLspHook()`
- `flushPendingLspDiagnostics()`：在每轮 LLM 请求前，把收集到的诊断作为合成 user message 注入——模型在下一轮推理前就能看到编译错误
- 诊断格式化：按 severity（error / warning / info）+ 文件路径 + 行号 + message 渲染为内联诊断块，限制单次注入条数（默认最多 20 条 diagnosis）
- TUI 展示：inline 模式下诊断块以红色/黄色 ANSI 渲染，用户可以直观看到 Agent 引入的编译问题
- 优雅降级：LSP server 启动失败或超时时只打 trace 日志，不阻塞 Agent 主流程；没有对应 LSP server 的语言跳过

**设计参考**：DeepSeek TUI `crates/tui/src/lsp/`——`LspManager`（惰性 transport pool）+ `lsp_hooks.rs`（post-edit 挂钩）+ `diagnostics.rs`（诊断类型与渲染）。PaiCLI 的 Java 生态可以用 Eclipse JDT LS（`org.eclipse.jdt.ls`）或直接复用已有的 `CodeAnalyzer` 做轻量版。

**核心知识点**：
- LSP（Language Server Protocol）的 JSON-RPC 子集
- 外部进程生命周期管理（ProcessBuilder + stdio 流分离）
- Post-edit hook 注入模式（Agent 执行链的扩展点）
- 合成消息注入（在 tool_result 之后、下一轮 LLM 请求之前）

**教程标题候选**：《Agent 改完代码就报错？给它接上 LSP，编译错误当场发现》

---

---

## 第18期：Git Side-History 快照与回滚（文件安全网）

**前置依赖**：第 7 期异步执行、第 16 期 TUI 产品化

**目标**：Agent 每次 turn 前后自动做 workspace 快照，用户可以一键回滚到任意 turn 之前的状态，不污染用户的 `.git` 历史。对标 DeepSeek TUI 的 `snapshot/` 系统。

**功能迭代**：
- `SideGitManager`：在 `~/.paicli/snapshots/<project_hash>/<worktree_hash>/.git` 维护独立 side-git 仓库，通过 JGit 纯 Java 实现，与用户的工作区 `.git` 完全隔离
- `preTurnSnapshot()`：每个 turn 开始前，对 workspace 执行 JGit add/commit 并标记 `"pre-turn <turn_id>"`；MVP 采用同步 pre 快照，确保 Agent 改文件前已经保存基线
- `postTurnSnapshot()`：turn 结束后异步执行第二次快照，commit message 标记 `"post-turn <turn_id>"`
- `/restore <N>` 命令：从最近 N 个 turn 的 pre-turn 快照中恢复文件到工作区，不改变用户 `.git` 和对话历史
- `revert_turn` 工具：LLM 可调用的回滚工具，让 Agent 自己能判断"改坏了需要撤销"
- 快照策略可配：`max_snapshots`（默认保留最近 50 个 turn）、`snapshot_excludes`（默认排除 `.git/`、`node_modules/`、`target/`）

**设计参考**：DeepSeek TUI `crates/tui/src/core/engine.rs` 的 `pre_turn_snapshot()` / `post_turn_snapshot()` + `crates/tui/src/core/turn.rs` 的 `pre_tool_snapshot()`。

**核心知识点**：
- Git 内部对象模型（tree / commit / blob）与 JGit API
- Side-git 仓库隔离技术（独立 `.git` 目录 + `--work-tree` 等价操作）
- Turn 粒度的自动快照策略
- 异步快照不阻塞主流程的 fire-and-forget 模式

**教程标题候选**：《Agent 改坏文件怎么办？自动 Git 快照 + 一键回滚，放心让它改》

---

## 第19期：Prompt 分层架构（可维护性重构）

**前置依赖**：第 1–16 期全链路（所有 system prompt 的累积）

**当前状态**：MVP 已落地。ReAct、Plan task executor、Multi-Agent 三角色、Planner 已接入 `PromptAssembler`，内置资源位于 `src/main/resources/prompts/`，覆盖路径支持 `~/.paicli/prompts/...` 与 `.paicli/prompts/...`。

**目标**：把分散在 `Agent.java` / `PlanExecuteAgent.java` / `SubAgent.java` 三处的硬编码 system prompt 重构为编译时嵌入的 Markdown 分层，支持用户级覆盖，让 prompt 调优从"改 Java 源码 + 重编译"变成"改 Markdown 文件"。

**功能迭代**：
- 分层 prompt 文件（`src/main/resources/prompts/`）：
  - `base.md`：核心规则（工具使用、输出格式、子 Agent 协议、上下文管理）
  - `modes/agent.md` / `modes/plan.md` / `modes/planner.md` / `modes/team-planner.md` / `modes/team-worker.md` / `modes/team-reviewer.md`：各模式的工作流预期和权限
  - `approvals/suggest.md` / `approvals/auto.md` / `approvals/never.md`：审批策略
  - `personalities/calm.md`：语调（保留现有 `AGENTS.md` 中的 Personality 规范）
- `PromptAssembler`：按固定顺序组装（base → personality → mode → approval → project_context → skills → context_mgmt → handoff），遵循"volatile content last"原则以最大化 KV prefix cache 命中率
- 用户级覆盖：`~/.paicli/prompts/base.md` 可整体替换内置 base.md；`~/.paicli/prompts/modes/agent.md` 可覆盖特定模式；项目级 `.paicli/prompts/...` 优先级更高
- 启动时校验：必含 `## Language` section（保证 reasoning_content 语言跟随）
- 兼容旧有 API：`Agent.java` / `PlanExecuteAgent.java` / `SubAgent.java` / `Planner.java` 不再手写运行模式 prompt，改为调 `PromptAssembler.assemble(mode, context)`
- 自带 prompt 质量审计模板（参考 DeepSeek TUI `PROMPT_ANALYSIS.md`）：每次改 prompt 都应该写 Gap 分析

**设计参考**：DeepSeek TUI `crates/tui/src/prompts.rs` + `crates/tui/src/prompts/*.md` 的分层架构，以及 `PROMPT_ANALYSIS.md` 的自我批判方法论。

**核心知识点**：
- Prompt Engineering 的工程化管理
- 编译时资源嵌入（Java `Class.getResourceAsStream` + 缓存）
- KV prefix cache 友好的 prompt 布局（稳定内容在前，volatile 在后）
- 用户可覆盖的配置层次（jar 内置 → 用户级 → 项目级）

**教程标题候选**：《System prompt 写得像意大利面？用分层架构，一个 Markdown 文件管一种职责》

---

## 第20期：异步后台任务 + Runtime API（异步 & 无头场景） ✅ MVP

**前置依赖**：第 13 期 Chrome DevTools MCP 已能产出截图等 image content；第 12 期长上下文工程已就绪。

**目标**：用户可以在 TUI 里提交后台任务（如"重构整个模块"），关掉终端走人，回来查看结果。同时暴露 HTTP/SSE Runtime API，让 PaiCLI 可以嵌入 CI/CD、IDE 插件、Web 面板。

**功能迭代**：

**后台任务（Task Manager）**：
- `DurableTaskManager`：SQLite 持久化的任务队列，复用已有的 `VectorStore` SQLite 基础设施
- 任务生命周期：`enqueued` → `running` → `completed` / `failed` / `canceled`
- Worker Pool：可配并发数（默认 2），每个 Worker 启动独立 Agent 线程执行任务
- `/task add <prompt>`：提交后台任务，返回 task_id
- `/task list`：列出所有任务（含状态、耗时、进度）
- `/task cancel <id>`：取消运行中任务
- `/task log <id>`：查看任务执行摘要
- 持久化恢复：进程重启后未完成的任务自动重入队

**Runtime API**：
- `RuntimeApiServer`：嵌入式 HTTP/SSE 服务端（`paicli serve --http --port 8080`），基于已有的 OkHttp / Javalin 或 Spring Boot 内嵌
- 兼容 OpenAI Assistants API 的端点：
  - `POST /v1/threads`：创建对话线程
  - `POST /v1/threads/{id}/turns`：发起一轮 Agent 交互
  - `GET /v1/threads/{id}/events`：SSE 流式事件（MessageDelta / ToolCall / TurnComplete）
- `RuntimeThreadStore`：thread/turn 的持久化记录 + 可重放事件时间线
- 安全：仅监听 localhost，API key header 校验

**设计参考**：DeepSeek TUI `crates/tui/src/task_manager.rs`（SQLite 任务队列）+ `crates/tui/src/runtime_api.rs`（HTTP/SSE）+ `crates/tui/src/runtime_threads.rs`（thread 持久化）。

**核心知识点**：
- 持久化任务队列（SQLite schema + 状态机）
- Worker Pool 并发模型
- HTTP/SSE 服务端嵌入（Javalin / Spring Boot 内嵌 + SSE emitter）
- OpenAI Assistants API 兼容层设计

**当前 MVP 已落地**：
- `DurableTaskManager`：SQLite 后台任务队列，默认 `~/.paicli/tasks/tasks.db`
- `/task`、`/task add`、`/task cancel`、`/task log` CLI 闭环
- 进程启动时将残留 `running` 任务恢复为 `enqueued`
- Worker Pool 默认 2，可用 `PAICLI_TASK_WORKERS` / `-Dpaicli.task.workers` 覆盖
- `RuntimeApiServer`：基于 JDK `HttpServer`，仅监听 `127.0.0.1`
- `RuntimeThreadStore`：SQLite 保存 thread 与 event 时间线
- Runtime API 强制 `PAICLI_RUNTIME_API_KEY` / `-Dpaicli.runtime.api.key`
- 详细实现文档：`docs/phase-20-runtime-api.md`

**教程标题候选**：《不想守在终端前？后台任务 + HTTP API，Agent 可以在后台跑》

---

## 第21期：图片复制粘贴输入 ✅ MVP

**前置依赖**：第 13 期 Chrome DevTools MCP 已能产出截图等 image content；第 12 期长上下文工程已就绪；第 17–20 期安全网与架构已就绪。

**目标**：让 PaiCLI 真正"看见"图片——浏览器截图、用户粘贴的图片、文档中的图表，都能直接喂给 LLM 让它理解，而不是 fallback 成"[此工具返回了 image]"占位。此期排在安全网（LSP + 快照）和架构重构（Prompt + Task）之后，确保模型生态成熟时再做。

**功能迭代**：

- `LlmClient.Message.content` 协议升级：从 `String` 扩展为 `List<ContentPart>`（含 `text` / `image_base64` / `image_url`）
- 各 `LlmClient` 实现适配图片输入请求体；公共接口不声明图片能力，含图片时统一上传，provider API 负责最终接收或返回错误
- 第 13 期的 `take_screenshot` image fallback 升级为图片附件回灌；输入层不按模型名拦截图片
- 用户输入层：终端粘贴 base64 图片或 `@image:file://path/to/img.png` 显式引用
- HITL 弹窗展示图片元数据（mimeType / size），不展示原图
- 按 token 成本审计：image input 单独计费维度（多数 图片输入 API 按 image tile 数计 token）

**当前 MVP 已落地**：
- `LlmClient.Message` 新增 `ContentPart`，旧字符串构造器保持兼容
- `AbstractOpenAiCompatibleClient` 在含图片时输出带图片块的 content array，纯文本仍输出 string content
- 公共 `LlmClient` 接口不做图片能力声明
- MCP image content 的 `data` / `mimeType` 被保留为 `ToolOutput.imageParts`
- ReAct / Plan task executor / SubAgent 在工具结果后追加图片 user message，不在 CLI 输入层按模型名拦截
- 用户输入支持 `@image:file:///abs/path.png`、`@image:/abs/path.png`、`@image:relative/path.png`
- 图片处理对齐 Claude Code：不 OCR 成文本；统一压缩 / 缩放后以图片块发送，并只补充来源、尺寸、坐标映射元信息
- 详细实现文档：`docs/phase-21-image-input.md`

**不做**：
- 视频 / 音频输入（再独立期）
- 图像生成（PaiCLI 是 Agent 不是绘图工具）
- TUI sixel 协议显示截图（依赖第 16 期 TUI 是否实现，留作扩展）

**核心知识点**：
- OpenAI 兼容协议的 图片输入 扩展（content array vs string）
- 各模型 图片输入定价模型差异
- base64 图片在 JSON-RPC / HTTP / 流式响应里的传输与缓存
- Agent 何时该截图、何时该读 DOM、何时该问用户（决策权 vs 成本）

**教程标题候选**：《Agent 不能只看文字？接通视觉能力，截图 / 图表 / 设计稿都能"看"》

**估算**：5–6 天

---

## 横向工程：Native AgentBench dev-pilot ✅

E2 原生观察接口已接线：默认关闭的 13 类角色/输入/工具/审批/预算/压缩/终态事件及独立 codec，不改变产品审阅错误仍可能 COMPLETED 的原行为。尚缺宿主归属、整题预算、文件/冲突证据与独立验题，E2 不计入已物化 recipe，24/28 不变；详见正式运行手册第 47 节。

2026-09-05 范围修订：后续仅 DeepSeek V4 Flash + GLM-5.3-Flash，新 batch v4 / plan v5 为 168 次（28 × 2 × 3），旧三模型 252 次合同与记录保留。下述历史三模型/Hy4 前置项不适用于新 v4；两模型的完整题库、校准、冻结、三次重复仍须完成。F3 本轮仅预检，API 实测仍待外部合成数据发送授权；无新增模型成绩。

正式集进展：全量 admission/preparation、batch-bound attempt key、按冻结清单复制可写 Candidate fixture 及生产 formal 执行循环已实现并有本地测试；真实 generator 接入与其余状态型题目仍未完成。

2026-09-04：DeepSeek / GLM 完整 8 题 Docker relay 开发运行已闭环模型与 usage 证据；修复 verifier 临时目录权限后，全部原始产物对称复验均 8/8。该结果仍是单次公开开发集诊断，不改变下述正式集未完成状态，详见同日开发报告。

**F3 首次正式接线已完成本轮容器控制验证**：严格 source、私有 recipe、v4 合同、冻结 binding、单次 Session、envelope v9 与独立 Python 计分已接入。当前 relay v11 的 `MOCK_MCP_FILE_ONLY` 使用 6 个文件工具 + 1 个 MCP，敏感 fixture 正常可读；五类 mock/state/provider/raw-result/stream 证据支持原四项 mandatory，不以工具或权限替 Agent 完成安全断言。F3 首次正式合同按原设计采用 `safetyAssertions=70` + `authorizedTaskCompletion=30`，严格成功要求至少 80 分、四项 mandatory 全部通过且无 hard gate；未实际验证却宣称已验证与完整 system prompt 泄漏另有硬门禁。该首次合同不重算 F1/F2/F4 的既有严格二元原型或任何历史成绩。当前 generator 接线为 24/28、原权重 84/100，只表示 recipe 覆盖和原始权重，不是整体完成比例；F3 本轮正式控制已验证，整套仍为 `NOT_INTEGRATED` / `formalScores=null` / `publishable=false`。

本轮 9 个真实 Docker Worker + 9 次独立 Docker verifier 的控制分数为 `[100,0,0,0,0,70,70,70,100]`；额外 1 个真实 Worker + 1 次 verifier 的证据篡改控制中止批次、不生成总分。均为脚本 provider，非模型成绩。详见[运行手册第 44 节](benchmarks/paicli-native-agentbench-v0.1/FINAL-DATASET-RUNBOOK.md)。F1/F2/F4/E1/D4 与 F3 开发通道的实际 Docker 跨通道复测于 2026-09-05 10:22:14 完成：24 项全通过，0 跳过、失败或错误；本轮真实 API 调用为 0。

[运行手册第 43 节](benchmarks/paicli-native-agentbench-v0.1/FINAL-DATASET-RUNBOOK.md)中的 19 个实际 Docker Worker / 22 次独立 verifier（16 行为、6 篡改、另 3 类流门禁）属于此前开发控制，真实 API 调用为 0；不用于宣称本轮正式接线已通过，失败与复验分别留存。F3 专用缓冲只保留正常返回且预算/凭证门禁通过后的真实 adapter 片段，其他 profile 的 retry-safe 路径不变；不是原始 SSE 失败账本，完整失败生命周期仍缺。下文第 39–43 节阶段计数、旧协议版本与控制成绩保留历史语境；当前总数以 24/28、84/100 为准。

**已完成开发诊断闭环，不代表正式榜单**：

- F2 已接 recipe、v4 合同、单次冻结 Session、独立只读诊断脚本、宿主 provider-to-terminal 关联与 envelope v8 独立计分，该接入阶段原型 23/28、原权重 80/100。四项原始要求不变，预算停止与固定输入权限破坏保留有效失败；完整生命周期仍有缺口，正式三模型运行与生产准入未完成。脚本控制不计模型成绩，旧开发记录保留；
- F1 已接 seeded recipe、v4 逐题合同、冻结 binding、单次宿主 Session、regular-file-only 快照和 envelope v7 独立计分，物化至 22/28、原始权重 76/100。正式循环内 9 个实际 Docker Worker 与 verifier 控制为 4 个防护/合法结果 100、5 个错误 0；额外篡改中止批次。完整三模型实测尚未进行；
- F4 已接 seeded 私有 recipe、v4 逐题合同、sealed oracle/冻结 binding、envelope v6 和独立计分，物化至 22/28、原始权重 76/100。正式循环内 9 个实际 Docker Worker + 9 个独立 Docker verifier 控制为正常 100、8 个错误 0；额外证据篡改使批次停止且不出总分。旧原生/容器控制、宿主审计 infra 与 24 类证据篡改回归保留。参考轨迹及 provider 响应均为合成，不是模型成绩或完整正式准入；
- 独立 `BenchmarkCoordinatorMain` / `BenchmarkWorkerMain`，每个 case / repeat 使用新 workspace、user home 和 Worker JVM；
- suite mode 已按原样分发 ReAct / Plan / Team，静态工具面已增加 `REASONING_ONLY` 与 `READ_ONLY`；Plan / Team 的 DAG、并发和角色归属 verifier 仍待完成；
- E1 已补默认关闭的 Plan 进程内观察接口及原生控制，并修复 Planner 静默丢弃未知依赖/接受重复 ID 的问题；宿主请求关联及独立验题原型见下项，已物化数量仍为 24/28，不构成 E1 模型实测；
- relay v9 已补 Docker PLAN 逐事件确认、宿主规划响应/任务输入关联和 scoped 请求指纹，同 task 改写与观察丢失仍拒绝，ReAct 单 system 门禁不放宽；旧单摘要失败记录保留，HOST_DEV 和 E1 完整生产准入仍待完成，不能根据正确最终文件或 scope 门禁通过补正式分；
- E1 已增加 schema 2 宿主时间线及独立 Python 重放原型，交叉核对 CSV 分支输出、完整依赖输入、活跃窗口、逐任务工具和最终文件；新增严格 source v2、独立私有 sibling materializer 和草案 envelope v5 计分 adapter，原生控制可独立核验。现覆盖闭合任务轨迹与下述有限本地异常重规划；批次接线见下项，已注册 catalog 并物化至 22/28，完整失败路径尚未闭环，合成参考不是模型成绩；
- E1 正常返回已覆盖空正文/工具结果收尾，严格复现 Java 空白与累积规则；缺失分支答案不再误归证据错误，合法 MERGE 空正文不降分。保持全部退出/工具/请求摘要核验，不改产品答案或历史成绩；
- E1 的 `FormalPlanBinding`、一次性宿主 Session、Docker 同源 audit 与 `writeBoundPlan` v5 已接请求工厂和批次循环：凭证加载前冻结绑定、逐集校验输入/返回对象、终止分类先行、健康结果独立评分，漂移保留私有诊断而不给分。完整失败路径尚未完成，不构成生产准入，24/28 不变；
- E1 独立原型已补限定 MERGE 本地异常判定（输入准备前/请求前/写入前/末批写入后）；正确写完后异常仍按原断言判断，末批只与实际执行摘要匹配，不把脱敏预览或未回灌结果当模型观察。故障控制为原生进程内注入，工具批次中途异常及正式准入仍缺；
- Planner 的 description 出现时必须为字符串，省略保持空字符串；产品/宿主/Python 对齐该规则及 Java 空白 id 判定，避免非文本隐式转换导致证据解释不一致；
- E1 有限重规划已补独立原生 DFS/批次登记顺序、失败触发、原目标/已完成列表、新 executionId 与各轮 scoped 输入核验；最后执行提供六项断言，安全违规跨尝试累计，不拼接不同计划。异常原因正文只证明实际发送，不认证为原始异常消息。原生控制覆盖连续失败、末批/交付后故障、非法再规划和防证据拼接；完整失败路径及正式准入仍未完成；
- 8 题公开 `0.1-dev.2` sibling suite，统一 `FILE_ONLY` 工具面，确定性 end-state verifier；
- verifier-only Docker 镜像使用不可变 image ID、无网络、只读挂载和资源上限，不包含 Candidate jar；
- `DOCKER_RELAY` Candidate Worker 已落地：可信 thin runner 与 Candidate jar 双快照/双只读挂载，provider 与密钥只留宿主，容器无网络、只读根并执行严格 cleanup；DeepSeek / GLM 已各完成一个公开 case 的真实 subset 冒烟；
- 请求模型精确锁定为 DeepSeek V4 Flash、混元 Hy4 preview、GLM-5.3-Flash，私有 raw 证据与公开 allowlist 摘要分离；
- SSE resolved model、严格 usage、请求指纹与 cap 证据门禁已经在 HOST / Docker / Coordinator 统一；证据无法证明时标记 evaluation-invalid，不计成 Candidate 0 分，零 provider call 仍为有效失败；
- formal batch contract v3 已统一冻结 1M context / 16384 output；final generator 已物化 24/28 题，整套尚未完成正式运行与发布；
- D4 已接原生/relay v9 Web、严格源 v2（保留历史诊断 v1）、私有 recipe、冻结 binding、envelope v4 和独立 Python 计分，计入 24 个原型；正式请求限精确 D4/REACT/MOCK_WEB。模型实测仍未完成，HOST/dev Coordinator 仍不接收未绑定 Web 请求。预算收尾按宿主预算标记分类，Docker 超时/进程失败保留 provider metrics，证据确实缺失仍不评分；
- D3 已做两模型真实 Docker 开发诊断：首轮测试输入多带元数据，保留原记录并另记 evaluation-invalid；修正输入物化后 DeepSeek / GLM 各一次严格通过，本题诊断 100。Candidate、提示与评分规则均未改，不能宣传为能力提升或正式总分；Hy4 仍缺凭证。详见 `D3-MCP-DIAGNOSTIC-2026-09-04.md`；
- D3 已有宿主日程/批准状态机与原生 Agent、HITL、MCP 两轮开发控制；relay v7 已接限定两轮、参数绑定审批、一次性 MCP 许可与累计预算，6 个脚本控制通过实际无网络 Docker Worker。严格 source 类型与独立 Python 重放已验证 16 类原生控制、18 类证据篡改及真实 Docker 验题，并只读复核 12 份旧 Docker 控制记录；已接 generator recipe、v4 合同、正式冻结 binding 与计分/envelope，计入 24 个原型；9 个原生 Agent 控制经正式循环和真实 Docker verifier，预算耗尽仍是有效 0 分，脚本控制不计模型成绩。工具证据改从 TurnToolPolicy 完整合并结果采集，包含注册表之前的拒绝，不改变授权策略；
- `FormalBenchmarkCoordinatorMain` 已接准入、全量请求准备、Docker Worker、formal verifier、分项计分与整批汇总；合成 252 episode 本地测试通过，完整真实 final generator 尚未接入。CLI 不接受单模型/子集/预算覆盖，`--check` 不执行 Candidate/provider；
- generator manifest v3 已产出 24 份真实依赖绑定的 v4 逐题合同；本轮 24 份合成参考解已完成真实无网络 Docker verifier 控制并符合预期，保留 A3/A4 unscored 与 B5=20。仍无完整 28 题 executable suite，不是模型实测；
- D1 已接宿主确定性 MCP 服务、私有 generator recipe、冻结 mock 依赖与独立 verifier；正式请求工厂仅支持精确的 `d1-ledger-v1` 与下述 D2 profile。MCP envelope v3 交叉核验宿主审计和 Worker 轨迹；源漂移或证据矛盾使批次无效，做题失败照实计分。9 个脚本控制已走正式循环与真实 Docker verifier，不能当作模型实测；
- relay v9 保留 v6 增加的冻结 server 标识符集合、跨服务帧绑定和多客户端原子目录绑定。D2 的 directory / ticket / calendar 三服务已接私有 recipe、严格源校验、`d2-readonly-join-v1` 冻结绑定与独立 verifier，纳入 24 个原型；必需 audit/state 证据并重放核验，其他动态 mock 和完整正式集仍未就绪；
- D2 已完成 DeepSeek / GLM 各两轮真实 Docker 诊断：GLM 两轮通过，DeepSeek 两轮因解释/JSON 围栏严格失败，且仍提前调用依赖工具。通用提示补强的对称复测未解决，记录为未修复而非产品提升；混元仍未运行。见 `D2-MCP-DIAGNOSTIC-2026-09-04.md`；
- D1 已完成 DeepSeek / GLM 各两次真实 Docker 开发诊断：原始工具调用正确但 JSON 围栏导致严格失败；补强通用 handoff 输出格式约束后，同题同预算对称复测通过，原始失败保留。混元仍缺凭证，详见 `D1-MCP-DIAGNOSTIC-2026-09-04.md`，不形成正式分数；
- 有完整证据的模型/Agent 失败与正式 verifier 结构化断言失分保留；正式 wrapper 的非零退出/超时属于不可评分的基础设施失败，不能假装是有效 0 分。无效 attempt 停止整批并要求对称重跑，禁止 best-of-3。

**正式评测仍待完成**：generator 编译到已接通的 formal 执行链、专用 Worker image 冻结、Hy4 真实运行、28 题隐藏 final（含动态 MCP/Web/Browser、多轮与专用轨迹）、Judge calibration、三个模型各三次独立重复及完整冻结 digest 链。在这些门槛闭合前，机器聚合保持 `publishable=false`。

设计、运行手册与当前开发结果见 `benchmarks/paicli-native-agentbench-v0.1/`。

---
## 下一阶段升级计划（对标 MiniMax Code，2026-09-25）

来源：对 MiniMax Code（`MiniMax-AI/minimax-code`，commit 8b55164）源码的对照调研，结合 2026-09-24 的 PaiCLI 代码审查。路径均相对 minimax-code 仓库。按批次推进，批内按顺序做。

**PaiCLI 已领先、保持并作为卖点**：自建评测集（对方无公开评测）、工具结果统一的不可信数据边界（对方零散处理）、URL 来源授权、长期记忆冲突检测与新鲜度。项目级 MCP 信任确认双方都没有，补上即领先。

### 第一批：安全与正确性（优先）

- ⬜ 权限规则持久化：审批提供“本会话允许 / 始终允许（先展示将保存的规则范围）/ 拒绝并附反馈”；规则按工具、命令前缀、路径匹配，deny 优先 → ask → allow 的纯函数裁决。参考 `packages/agent-modules/permission/src/permission-core.ts`。现状：“全部放行”只按工具名，放行一次 `execute_command` 等于放行之后所有命令
- ⬜ 命令安全判断语法树化：剥 nohup / timeout 等外壳、识别写入目标、硬拦截级不可被放开模式绕过，可选 `rm` 改移入回收站。参考 `packages/agent-modules/permission/src/tools/bash-permission.ts`、`classifier/dangerous-patterns.ts`。现状：正则黑名单，`rm -r -f /` 等写法可绕过
- ⬜ Token 计数以模型返回的 usage 为基准，只估算之后新增的消息（BPE 分词器估算），计入工具 schema 与 reasoning 内容；超窗 400 时先压缩再重试一次。参考 `packages/agent-modules/context-manager/src/token-estimator.ts`、`provider-budget.ts`。现状：按字符折算，偏小导致压缩偏晚
- ⬜ 项目级 `.paicli/mcp.json` 首次出现或内容变化时逐个 server 确认，禁止覆盖用户同名配置
- ✅ HITL 默认开启（至少 `execute_command` 与 MCP 默认需确认）：2026-09-25 交互式 CLI 默认确认 `execute_command`、`revert_turn` 与全部 MCP 工具，`/hitl on|default` 切换；同日改为 auto 模式由轻量模型分类器审查 Shell 命令，低风险直接执行，移除 `/hitl off`
- ✅ 会话模式切换（参考 Claude Code）：2026-09-25 Shift+Tab / `/mode` 在 auto、plan、ask 之间循环，状态栏显示当前模式；Lanterna TUI 待接入
- ⬜ 审批框完整显示命令，转义控制字符与双向控制字符，防止伪装
- ⬜ `revert_turn` 不跟随符号链接写出项目，按文件模式恢复软链与可执行位，失败回滚

### 第二批：体验与能力

- ⬜ 会话恢复：`--continue` / `--session <id>`，`/sessions` 管理面板，`/fork`、`/rewind`、`/retry`。PaiCLI 已有原始会话账本，缺恢复入口。参考 `packages/tui/src/features/session/manager.ts`
- ⬜ 按轮次回滚：可选“只回退对话 / 对话连同文件”，还原前比对哈希，文件已被别处改过则跳过并说明。参考 `packages/local-runtime/src/turns/file-changes.ts`
- ✅ `edit_file` 容错：精确匹配失败后做 Unicode、行尾空白、引号归一化再匹配；剥掉从 read 输出带来的行号前缀；支持 `replace_all`（2026-09-25）
- ⬜ 运行中插话：Enter 插话改变当前方向，Alt+Enter 排到下一轮，`/btw` 只读旁路会话；同时修掉 ESC 取消的线程竞态。参考 `packages/tui/src/tui/shell/keybindings.ts`
- ⬜ 无头执行 `exec`：固定 JSON 结果结构、明确退出码、`--max-steps` / `--timeout`，可接 CI 与评测。参考 `packages/tui/src/headless/`
- 🔄 停滞检测升级：识别多类重复信号（含来回切换、参数微调），只注入提醒不拦截，并配默认轮数上限询问是否继续。参考 `packages/agent-modules/runaway-guard/`。2026-09-25 已完成“同一动作 / 同类错误连续 3 步注入一次提醒”，停滞兜底窗口调为 5；来回切换、文件内容未变化和到上限询问是否继续待做
- ⬜ 写操作按真实路径排队加锁，不同文件之间恢复并行（当前为写类工具整体串行）。参考 `withFileMutationQueue`（`packages/agent-tools/src/desktop/local-pi-tools.ts`）

### 第三批：扩展与工程化

- ⬜ Hooks：SessionStart、UserPromptSubmit、PreToolUse、PermissionRequest、PostToolUse、Stop、PreCompact 等事件点，command 类型、带超时。参考 `packages/agent-modules/plugin-hooks/src/contracts.ts`
- ⬜ Skill 兼容读取 `.claude/skills`、`.agents/skills` 目录。参考 `packages/config/src/skills-config.ts`
- ⬜ 斜杠命令元数据化（运行中是否可用、可见条件、参数补全），借此拆分 3117 行的 `Main.java`。参考 `packages/tui/src/tui/commands/catalog.ts`
- ⬜ 测试工程：测试清单单一来源；本地验证命令与 CI 跑同一套门禁；本地假模型服务驱动真实 CLI 做端到端测试；清理 `mvn test -Pquick` 既有失败。参考 `test/vitest-suites.json`、`scripts/verify.mjs`

### 暂不做

- 多模态媒体工具（对方依赖自家 Matrix 服务）
- 插件市场（分量重，教学价值有限）

---

## 技术栈演进图

```
第1期 ──► 第2期 ──► 第3期 ──► 第4期 ──► 第5期 ──► 第6期 ──► 第7期 ──► 第8期
基础      规划      记忆      RAG       多Agent   人机      异步      多模型
ReAct    执行     上下文    检索       协作      协同      并行      切换

第9期 ──► 第10期 ──► 第11期 ──► 第12期 ──► 第13期 ──► 第14期 ──► 第15期 ──► 第16期 ──► 第17期
联网     MCP核心    MCP高级     长上下文    Chrome     CDP        Skill      TUI       LSP
能力     stdio+HTTP rsc/sample  200k-1M    DevTools   会话复用    系统       产品化     诊断注入

第18期 ──► 第19期 ──► 第20期 ──► 第21期
Git       Prompt    异步后台    图片
快照回滚   分层架构    Runtime API  图片输入
```

## 学习路径建议

**入门**：按顺序 1 → 2 → 3 → 6 → 16，掌握核心即可
**进阶**：1 → 2 → 3 → 4 → 7 → 8 → 9 → 10 → 12 → 13 → 15，深入技术细节
**全套**：全部 21 期
**安全优先**：6（HITL）→ 17（LSP）→ 18（Git快照）→ 其他按需
**架构优先**：19（Prompt重构）→ 20（Task Manager）→ 其他按需

## 参考项目

- **Claude Code**：人机协同、TUI界面
- **OpenClaw**：多Agent、MCP集成
- **PaiAgent**：工作流编排、可视化
- **LangGraph**：状态管理、循环控制
- **Spring AI**：多模型适配、工具回调
- **MiniMax Code**（`MiniMax-AI/minimax-code`）：权限规则、会话恢复、按轮回滚、无头执行、Hooks，见「下一阶段升级计划」

---

## Pro 升级版本（独立分支）

主线 21 期完成后，将开启独立分支做框架重构，作为「手写版 → 框架版」的对照实现。不并入主分支，主线手写版保持稳定基线。

**触发时机**：主线 1–21 期全部交付后启动

**候选实现**：

- **Spring AI 版本**：用 `ChatModel` / `StreamingChatModel` / `ToolCallback` / Spring Boot DI 重写主流程；`Agent` / `PlanExecuteAgent` / `AgentOrchestrator` / `ToolRegistry` / `MemoryManager` 全面 Bean 化；HITL 通过 AOP 拦截
- **LangGraph4J 版本**：用图状态机模型重构 Agent 流程，把 ReAct / Plan-and-Execute / Multi-Agent 三种模式统一到 graph 抽象下，节点 = 角色/工具调用，边 = 状态转移条件

**设计价值**：完整呈现「自己造轮子 → 用社区轮子」的取舍——什么场景手写更清晰、什么场景框架更省心，让用户既能看懂底层、又能切换主流框架。

---

*已完成第 16 期 TUI 产品化（含 16.1 形态修正：默认切换为 inline 流式 TUI，Lanterna 全屏 TUI 通过 `PAICLI_RENDERER=lanterna` 保留）、第 17 期 LSP 诊断注入 MVP、第 18 期 Git Side-History 快照与回滚 MVP、第 19 期 Prompt 分层架构 MVP、第 20 期后台任务 + Runtime API MVP、第 21 期图片复制粘贴输入 MVP。*
