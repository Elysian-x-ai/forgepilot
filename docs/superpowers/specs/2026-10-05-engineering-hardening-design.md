# ForgePilot 工程化收口设计

## 目标

让 ForgePilot 的日常开发验证、核心运行时边界、CI 入口和公开文档形成一致的交付面，降低 benchmark 夹具漂移和中心类耦合对维护工作的影响，并提供一个不依赖真实模型或网络的可重复端到端演示。

## 范围与约束

- 保持现有 Java 17、Maven、三条 Agent 执行路径和公开兼容契约（`com.paicli`、`.paicli`、`PAICLI_*`、benchmark 标识）。
- 不把 benchmark 评测实现迁出仓库或恢复真实模型/Docker 运行；只把它从日常产品回归中隔离，并统一当前状态说明。
- 不做一次性 Maven 多模块重写；先以 Surefire profile、包级职责和兼容门面完成低风险收口。
- 不改变现有工具名称、策略顺序、HITL 语义和用户 CLI 行为。

## 方案

### 测试边界

Surefire 提供三个明确入口：`core` 运行产品单元/集成测试，`quick` 运行 core 的快速子集，`benchmark` 显式运行 `eval.benchmark`。产品 profile 排除 `com.paicli.eval.benchmark`，benchmark profile 只选择该包；默认 `mvn clean package` 继续保持跳过测试的历史行为，但 README 明确给出验证命令。平台相关 Seatbelt/JVM 用例采用测试标签或 profile 排除，不能把环境限制误报为产品回归。

### Main 与 ToolRegistry

从 `Main` 提取 `CliCommandDispatcher`（斜杠命令解析后的路由与 handler 绑定）和 `SessionBootstrap`（renderer、HITL、sandbox、MCP、Agent 的组装）。`Main` 保留进程入口、输入循环和生命周期协调。

从 `ToolRegistry` 提取 `BuiltinToolRegistrar`（17 个内置工具的注册）与 `ToolExecutionPolicy`（顺序、并发白名单、结果边界和策略入口的协调）。`ToolRegistry` 保留公开的工具注册表、执行门面和兼容构造器；三条 Agent 路径继续只调用 `executeTools()`。

拆分以“移动现有逻辑 + 保留委托”为主，每次抽取都由现有测试证明行为不变，不顺手重写策略或工具协议。

### Mock LLM 端到端演示

新增测试专用 `MockLlmServer`，使用 JDK/现有 HTTP 依赖监听随机本地端口，按固定请求序列返回一个工具调用和一个最终文本响应。测试创建临时项目和确定性 `read_file` 场景，运行默认 ReAct 路径，断言工具实际执行、最终回答、请求次数和无外网行为。演示不读取 API Key、不访问真实 provider，支持单命令重复运行。

### CI 与版本/状态

新增 GitHub Actions workflow，在 Java 17 上执行编译、core、quick 和 mock E2E；benchmark 不在 push/PR 默认任务中运行，另提供手动 workflow 或命令说明。Maven 版本集中在单一属性，Banner/README/配置/API 参考/AGENTS 使用同一公开版本；benchmark 状态以一份机器可读 manifest 或集中状态段为准，README 和 AGENTS 统一采用当前 25/28、88/100、`NOT_INTEGRATED`、`formalScores=null`、`publishable=false` 口径，并明确这是开发评测基础设施而非正式榜单成绩。

## 验收标准

1. `mvn -q test -Pcore` 与 `mvn -q test -Pquick` 不选择 `com.paicli.eval.benchmark`；`mvn -q test -Pbenchmark` 才运行 benchmark 测试。
2. `Main` 和 `ToolRegistry` 的中心职责明显减少，旧构造器/公开方法和现有核心测试保持兼容。
3. mock LLM E2E 在无 API Key、无网络环境下重复运行结果一致。
4. CI workflow 能在 Java 17 上执行产品验证，并把 benchmark 作为显式入口。
5. README、AGENTS、pom、Banner 和 benchmark 状态没有互相矛盾的版本/题数/发布状态。
