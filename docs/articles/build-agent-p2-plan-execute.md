---
title: 手搓 Java 版 Claude Code 第 2 期，先出计划再动手，按 DAG 分批并行执行
shortTitle: Plan-and-Execute与DAG调度
description: ForgePilot 第 2 期，按最新源码拆解 Plan-and-Execute：任务和依赖怎么建模，模型给的计划怎么严格校验，DAG 怎么按轮并行执行，上游结果怎么交给下游，任务失败后怎么重规划和收场。
keywords: Plan-and-Execute, DAG, 拓扑排序, 任务规划, ForgePilot
tag:
  - Agent
  - Java
category:
  - AI
author: 沉默王二
date: 2026-04-19
---

大家好，我是二哥呀。

第 1 期的 ReAct 已经能干活了，读文件、改代码、跑命令，一步接一步。

可任务一长，比如“读 pom.xml、看看源码结构、再写一份升级方案”，问题就来了。我们在 Agent 动手之前不知道它打算怎么做，互不相干的几步也没法同时跑。中途哪一步错了，只能等它自己发现。

![](https://cdn.paicoding.com/stutymore/build-agent-p2-plan-execute-20260924182030-166934ea.png)

这一期我们给 ForgePilot 加上 Plan-and-Execute。模型先出一份带依赖关系的任务清单，我们确认之后，执行器按依赖分批执行，互不依赖的任务可以并行。

## 01、先规划再执行

Plan-and-Execute 把一次任务交给两个角色。规划器只调用一次模型，不给任何工具，要求模型输出一份 JSON 格式的计划。执行器拿着这份计划，按依赖关系一批一批地跑任务。

![](https://cdn.paicoding.com/stutymore/build-agent-p2-plan-execute-20260926174050.png)

每个任务内部，跑的还是第 1 期 ReAct。模型决定调用什么工具，工具结果交回模型，直到模型不再调用工具，这个任务就算完成。

在 Agent 开始动手之前，我们能看到完整的计划，可以确认、补充要求或者直接取消。读 pom.xml 和列出 src 目录这种没有先后关系的任务，可以同时跑。每个任务拿到的上下文也更干净，只有总目标、任务描述和直接依赖的结果。

两种模式放在一起比一下。

| 对比项 | ReAct | Plan-and-Execute |
| --- | --- | --- |
| 模型调用次数 | 由 Action 轮数决定 | 多一次规划，每个任务至少一次 |
| 耗时 | 取决于 Action 轮数 | 多一次规划等待，独立任务并行时可能更快 |
| 执行前能否看到步骤 | 不能 | 能，还能补充要求后修改计划 |
| 适合的任务 | 简单任务、边看边决定的探索 | 步骤多、依赖明确、想先看清流程的任务 |

## 02、计划长什么样

一个任务就是一个节点。

```java
// src/main/java/com/paicli/plan/Task.java
public class Task {
    private final String id;
    private final String description;
    private final TaskType type;
    private volatile TaskStatus status;
    private volatile String result;
    private volatile String error;
    private final List<String> dependencies;  // 依赖的其他任务ID
    private final List<String> dependents;    // 依赖此任务的其他任务ID
}
```

任务类型有 5 种，`FILE_READ`、`FILE_WRITE`、`COMMAND`、`ANALYSIS`、`VERIFICATION`。类型只作为提示词变量交给执行任务的模型，提醒它这一步是读任务、写任务还是分析任务，不限制它能用哪些工具。

`dependencies` 是我依赖谁，用来判断能不能开始执行；`dependents` 是谁依赖我，任务失败时顺着它找出受影响的下游。

![](https://cdn.paicoding.com/paicoding/7ec21fcc8f1031ffef6704fd6c9d8586.png)

任务状态有 5 种。

```text
PENDING → RUNNING → COMPLETED / FAILED
PENDING → SKIPPED（依赖的任务失败，或者重新规划达到了上限）
```

能不能开始执行，只看一个条件，所有依赖是否都已经完成。

```java
public boolean isExecutable(Map<String, Task> allTasks) {
    if (status != TaskStatus.PENDING) return false;
    for (String depId : dependencies) {
        Task dep = allTasks.get(depId);
        if (dep == null || dep.getStatus() != TaskStatus.COMPLETED) {
            return false;
        }
    }
    return true;
}
```

依赖失败或者被跳过，下游会等不到 `COMPLETED`，所以失败处理必须主动把下游标成 `SKIPPED`，不然会一直停在 `PENDING`。

状态和结果都由调度线程写，任务开始前标记为 `RUNNING`，收回结果后标记为完成或失败。工作线程只读依赖任务的结果。线程池的提交和 `Future.get()` 保证先写后读的可见性，字段上的 `volatile` 算多一层保险。

【截图：任务状态流转；风格：three-layer；截图目标：展示 PENDING、RUNNING、COMPLETED、FAILED、SKIPPED 之间的转换条件；关键词：任务状态、依赖完成、跳过下游】

### 拓扑排序和环检测

多个任务组成一个执行计划，任务按插入顺序存在 `LinkedHashMap` 里，另外维护一份拓扑排序后的顺序。

拓扑排序用的是 DFS 后序遍历。沿着依赖方向递归，先把所有依赖加进结果，再加自己，得到的顺序天然就是依赖在前。

```java
// src/main/java/com/paicli/plan/ExecutionPlan.java
private boolean topologicalSort(Task task, Set<String> visited, Set<String> visiting) {
    String id = task.getId();
    if (visiting.contains(id)) {
        return false;  // 有环
    }
    if (visited.contains(id)) {
        return true;
    }
    visiting.add(id);
    for (String depId : task.getDependencies()) {
        Task dep = tasks.get(depId);
        if (dep != null && !topologicalSort(dep, visited, visiting)) {
            return false;
        }
    }
    visiting.remove(id);
    visited.add(id);
    executionOrder.add(id);
    return true;
}
```

`visiting` 是当前递归栈上的节点，递归中又碰到它，说明依赖绕回来了，比如 A 依赖 B，B 依赖 C，C 又依赖 A。`visited` 是已经处理完的节点，避免重复遍历。

![](https://cdn.paicoding.com/stutymore/build-agent-p2-plan-execute-20260924182251-1c5c3b96.png)

排出来的顺序不直接决定谁先跑。调度靠每一轮重新筛可执行的任务，拓扑顺序只负责给同一轮的任务排先后，另外决定计划展示、跳过标记和失败汇总里的列出顺序。

计划本身也有状态，`CREATED`、`RUNNING`、`COMPLETED`、`FAILED`、`CANCELLED`，用户按 ESC 中断执行时标成 `CANCELLED`。

## 03、模型给的计划凭什么能信

计划是模型写的，写错是常事。规划器先把要求讲清楚，再对模型交回来的东西严格验收。

先看规划请求。

```java
// src/main/java/com/paicli/plan/Planner.java
public ExecutionPlan createPlan(String goal) throws IOException {
    out.println("📋 正在规划任务: " + goal + "\n");
    if (isSimpleGoal(goal)) {
        return createMinimalPlan(goal);
    }
    return requestPlan(goal, "请为以下任务制定执行计划：\n" + goal);
}

private ExecutionPlan requestPlan(String goal, String planningRequest) throws IOException {
    List<LlmClient.Message> messages = Arrays.asList(
            LlmClient.Message.system(promptAssembler.assemble(PromptMode.PLANNER, PromptContext.builder()
                    .projectMemoryContext(buildProjectMemoryContext())
                    .build())),
            LlmClient.Message.user(planningRequest));
    LlmClient.ChatResponse response = llmClient.chat(messages, null, streamRenderer);
    return parsePlan(goal, response.content());
}
```

`chat` 的第二个参数传 `null`，规划请求不带任何工具，模型只能用文字回答。终端上只流式显示模型的思考过程，JSON 正文不直接打出来，解析通过后以计划审阅的形式展示。开头的简单任务短路第 08 节再讲。

### 规划提示词

提示词放在 `prompts/modes/planner.md`，拼装时带上 PAI.md 项目记忆。里面给了 JSON 格式示例、5 种任务类型，最后是 9 条规则，结尾强调“只输出 JSON，不要有其他内容”。

```text
5. 简单任务允许只生成 1-3 个任务，不要为了凑步数引入无关步骤。
6. 复杂任务拆分为 5-10 个子任务。
7. 不要为了“保存中间结果”额外创建 FILE_WRITE / FILE_READ，除非用户明确要求落盘。
8. 如果一个任务一步就能完成，就保持最短计划。
9. 没有依赖关系的任务会并行执行。会写同一个文件的任务不能同时执行，必须让后一个依赖前一个。
```

第 5、7、8 条对付过度规划。模型很爱把“列出目录”拆成“列出目录、保存结果、读取结果、总结结果”四步，步数多了看着认真，每一步都是一次模型调用。

第 9 条对付并行写冲突。执行器没有文件锁，两个并行任务改同一个文件，后写的会把先写的盖掉，只能在规划阶段让模型把它们排成先后。

【截图：planner.md 的 9 条规则；风格：checklist-card；截图目标：展示防过度规划的 5、7、8 条和防并行写冲突的第 9 条；关键词：最短计划、不落盘、同文件串行】

### 严格校验

模型交回来的计划，任何一项不合格都直接失败。

````java
String cleaned = planJson.replaceAll("```json\\s*", "")
        .replaceAll("```\\s*", "")
        .trim();
JsonNode root;
try {
    root = mapper.readTree(cleaned);   // JSON 后面跟说明文字、键重复都算不合法
} catch (JsonProcessingException e) {
    throw new IOException("计划不是合法的 JSON：" + e.getOriginalMessage(), e);
}
// 第一遍：登记所有 id，重复即失败
if (idMapping.putIfAbsent(originalId, "task_" + taskIndex++) != null) {
    throw new IOException("计划中存在重复任务 id");
}
// 第二遍：建立依赖，引用未声明的 id 即失败
if (!depNode.isTextual() || !idMapping.containsKey(depNode.asText())) {
    throw new IOException("计划依赖必须引用已声明的任务 id");
}
// 最后：拓扑排序失败说明有环
if (!plan.computeExecutionOrder()) {
    throw new IOException("计划中存在循环依赖");
}
````

解析分两遍。模型可能先写 task_2，再写 task_1，而 task_2 依赖 task_1。第一遍只登记 id，第二遍再连依赖，前向引用就不会误判。不管模型给的 id 叫什么，最后都按数组顺序重新编号成 `task_1` 到 `task_N`，依赖里却必须写模型自己声明的原 id，写成新编号也算未声明。

JSON 解析开了两个严格选项，JSON 后面跟着一段说明文字、同一个键出现两次，都判为不合法。Jackson 默认会忽略这两种情况，ForgePilot 的评测重放器用 Python 的 `json.loads` 重新解析同一份回复，两边标准不一样，同一份计划就会一边能跑一边报错。

![](https://cdn.paicoding.com/stutymore/build-agent-p2-plan-execute-20260924182549-5438f8da.png)

为什么不悄悄修补？比如依赖里引用了一个不存在的 id，丢掉这条边，计划照样能跑，可执行顺序已经不是模型本来的意思了。某个本该等上游的任务提前开跑，拿不到它需要的结果，错误会在很后面才冒出来，还很难查。

解析失败也不自动重试，不退回 ReAct，整次执行直接返回“❌ 执行失败”和具体原因。只有任务类型是宽松的，写错或缺失都按 `ANALYSIS` 处理，它本来就只是个提示。

**计划形状都不对的时候，与其猜模型想干什么，不如把错误摆出来，让用户换个说法再来一次。**

## 04、任务按什么顺序跑，哪些能并行

计划通过审阅后，执行器按轮调度。每一轮取出所有依赖已完成的任务，这一轮全部跑完，再算下一轮。

```java
// src/main/java/com/paicli/agent/PlanExecuteAgent.java，executePlan 节选
while (!stopAfterBatch) {
    if (CancellationContext.isCancelled()) {
        plan.markCancelled();
        return CANCELLED_PLAN_MESSAGE;
    }
    List<Task> executableTasks = getExecutableTasksInOrder(plan);
    if (executableTasks.isEmpty()) {
        break;
    }
    List<TaskExecutionResult> batchResults = executeTaskBatch(
            plan, executableTasks, streamState, taskTrustedUrls, observation);
    // 取消会打断本轮正在执行的任务，这些中断不是任务本身失败，不能再触发重新规划
    if (CancellationContext.isCancelled()) {
        plan.markCancelled();
        return CANCELLED_PLAN_MESSAGE;
    }
    for (TaskExecutionResult batchResult : batchResults) {
        // 成功就标记完成；失败就判断是重新规划，还是跳过它的下游（第 07 节）
    }
}
```

一轮里只有一个任务时，直接在当前线程执行。多个任务就开一个线程池并行。

```java
out.println("⚡ 本轮并行执行 " + executableTasks.size() + " 个任务: " + parallelTaskIds);
ExecutorService executor = Executors.newFixedThreadPool(Math.min(executableTasks.size(), 4), r -> {
    Thread t = new Thread(r, "paicli-plan-executor");
    t.setDaemon(true);
    return t;
});
```

并发度取本轮任务数和 4 的较小值，每一轮新建线程池，结束时关闭。第 5 个任务要等前面有线程空出来。

【截图：按轮调度示意；风格：swimlane；截图目标：展示每一轮取出可执行任务、并行执行、整轮结束后再算下一轮；关键词：按轮执行、最多 4 路并行、轮次边界】

### 为什么不就绪一个派发一个

按轮调度有个明显的代价。某个任务的依赖早就完成了，也得等这一轮最慢的那个任务结束才能开始。改成事件驱动，哪个任务的依赖一到齐就立刻派发，总耗时能更短。

ForgePilot 选按轮，是因为每一轮的边界很清楚。哪些任务在同一轮开始，失败发生时哪些结果已经提交，都能按轮复现出来。ForgePilot 的评测重放器就按这套语义逐条核对执行记录，比如“触发重新规划之后不能再有新任务开始”。换成就绪即派发，失败时兄弟任务还在跑，重规划和跳过的规则都要重新定义。

对 ForgePilot 这种计划通常只有几个到十几个任务的场景，多等一轮的代价可以接受。

### 并行输出和写冲突

几个任务同时往终端打日志，内容会搅成一团。每个并行任务的输出先写进自己的缓冲区，整轮结束后按任务顺序一次性打印。代价是并行执行期间看不到这些任务的流式输出，只能看到“▶️ 并行任务 [task_x]”这样的开始提示。审批弹窗和自动审查的提示是例外，它们直接打到终端，不进缓冲区，要等用户确认的东西不能压到整轮结束。

![](https://cdn.paicoding.com/stutymore/build-agent-p2-plan-execute-20260924183208-2fe0d6d3.png)

写冲突先看任务内部。模型一次可能返回好几个工具调用，读文件、搜索这类只读工具最多 4 个并行，写文件、执行命令、MCP 调用一律按模型给出的顺序串行，第 1 期讲过原因。任务之间就没有文件锁了，只能靠规划提示词第 9 条，让模型把写同一个文件的任务排成先后。

## 05、上游的结果怎么交给下游

每个任务开始时，拿到的第一条 user 消息是执行器拼出来的任务上下文。

```java
// src/main/java/com/paicli/agent/PlanExecuteAgent.java，buildTaskContext 节选
context.append("总目标：").append(goal).append("\n");
context.append("当前任务：").append(task.getDescription()).append("\n");
if (task.getDependencies().isEmpty()) {
    context.append("依赖任务：无\n");
} else {
    context.append("依赖任务结果：\n");
    for (String depId : task.getDependencies()) {
        Task dep = plan.getTask(depId);
        context.append("- ").append(dep.getId())
                .append(" / ").append(dep.getDescription())
                .append(" / 状态=").append(dep.getStatus())
                .append("\n");
        if (dep.getResult() != null && !dep.getResult().isBlank()) {
            context.append(dep.getResult()).append("\n");
        }
    }
}
context.append("请执行此任务。如果是ANALYSIS或VERIFICATION类型，请基于以上上下文直接给出结果。");
```

只带直接依赖的结果，不带间接依赖，也不带无关的兄弟任务。下游如果需要更早的信息，只能靠直接依赖在结果里转述。

![](https://cdn.paicoding.com/stutymore/build-agent-p2-plan-execute-20260924182820-c5b9ffbb.png)

为什么不让所有任务共用一份对话历史？并行的几个任务同时往一份历史里追加消息，顺序说不清。就算串行，下游也会看到上游每一次工具调用的原始输出，历史越跑越长，每个任务都在为别人的过程付 Token。

依赖结果原样拼进来，不截断也不做摘要。下游常常需要上游读到的完整内容，截掉一半，它就只能再读一遍。上游读了一个大文件、下游又依赖好几个上游时，这条消息会很长，任务内部还有第 1 期讲的工具结果落盘和上下文压缩兜底。上下文最后还会追加和任务描述相关的长期记忆。

【截图：任务上下文的组成；风格：three-layer；截图目标：展示总目标、当前任务、直接依赖结果、依赖分支的可信 URL、长期记忆五部分；关键词：直接依赖、原样拼接、长期记忆】

### 联网权限也按依赖传

每个任务开始前，都从本轮工具策略复制一份副本。并行的兄弟任务各用各的，新发现的 URL 不互相共享。

```java
TurnToolPolicy taskToolPolicy = turnToolPolicy.forkWithTrustedUrls(dependencyUrls);
```

上游任务通过 `web_search` 拿到的 URL，只传给 DAG 里声明依赖它的下游，拼进任务上下文时单独列一段“依赖分支经 web_search 验证的 URL”。上游回复正文里写的网址不算数，下游想抓取也会被拒绝。

这样一个任务读到的网页再怎么诱导，也没法让另一个分支去访问它指定的地址。

**任务之间只传结果，不传过程；只传声明过的依赖，不传旁路。**

## 06、一个任务内部怎么干活

每个任务有一份独立的消息列表。system 是任务执行提示词，带上 PAI.md、Skill 索引和任务类型；user 是上一节拼好的任务上下文。之后就是第 1 期那套循环，每一轮依次做这几件事。

```java
// src/main/java/com/paicli/agent/PlanExecuteAgent.java，任务循环节选
while (true) {
    if (CancellationContext.isCancelled()) { /* 返回“已取消任务” */ }
    AgentBudget.ExitReason exitReason = budget.check();   // 停滞或预算命中就收尾
    injectPendingLspDiagnostics(messages, out, actor);    // 上一步改过代码，补上语法诊断
    maybeCompactHistory(messages, out, actor);            // 太长就先压缩
    TurnToolPolicy.ToolExposure toolExposure = taskToolPolicy.expose(toolDefinitions);
    LlmClient.ChatResponse response = llmClient.chat(
            messages, toolExposure.definitions(), streamRenderer);
    // 没有工具调用 → 任务完成，返回模型的回答
    // 有工具调用 → 执行工具，结果经安全边界包装后交回模型 → 注入本轮加载的 Skill → 防打转检查
}
```

防打转和第 1 期一致。同一个工具带同样的参数连续调用 3 次，或者同类错误连续出现 3 次，注入一次提醒，让模型换个思路。连续 5 轮调用完全相同，就判定原地打转，关掉工具再调一次模型，让它基于已有结果收尾，任务结果标“⚠️ 部分完成”。任务循环不设固定轮数上限，写死轮数的话，复杂一点的任务可能还没做完就被截断。

【截图：任务内部的防打转；风格：swimlane；截图目标：展示同一动作 3 次提醒、5 次停滞后关闭工具收尾；关键词：runaway guard、停滞检测、部分完成】

权限也和 ReAct 共用一套。计划任务和 ReAct 用同一个工具注册表，默认 auto 模式下，`execute_command` 先交给关闭思考的轻量模型审查，放行才执行，不放行就把原因交回模型。整份计划算一轮用户输入，所有任务、并行的兄弟任务、重规划后的新任务共用一个计数，连续被拦 3 次才弹给用户确认。

什么才算任务失败？只有执行过程中抛出异常，比如模型调用失败。工具报错会作为工具结果交回模型，让它自己处理；预算触发返回部分结果；用户取消返回“已取消”。这几种都不算失败，不会触发下一节的重规划。

## 07、任务失败了怎么收场

一个任务失败后，执行器先看整份计划的完成度。

```java
if (!stopAfterBatch && plan.getProgress() < 0.5) {
    int maxReplans = maxReplans();
    if (replansUsed < maxReplans) {
        out.println("🔄 尝试重新规划（第 " + (replansUsed + 1) + "/" + maxReplans + " 次）...\n");
        ExecutionPlan replanned = planner.replan(plan, errorMessage);
        return reviewAndExecutePlan(replanned, streamState, explicitTaskEnvelope, replansUsed + 1)
                .result();
    }
    String stopNotice = "已达到重新规划上限（" + maxReplans + " 次），停止执行剩余任务";
    out.println("🛑 " + stopNotice + "。\n");
    failureLines.add(stopNotice);
    stopAfterBatch = true;
    continue;
}
for (Task skipped : plan.skipDependentsOf(task.getId())) {
    failureLines.add("任务 " + skipped.getId() + " 已跳过: 依赖的任务 " + task.getId() + " 失败");
}
```

完成度低于一半，说明计划刚开头就出了问题，大概率是计划本身不对，重新规划。新计划同样要经过用户审阅。

重新规划最多 2 次，可以用 `PAICLI_PLAN_MAX_REPLANS` 调整。不设上限的话，新计划只要又在一半之前失败，就会一直规划下去。到了上限，不再启动任何新任务，只收完这一轮已经返回的结果，其余任务全部标 `SKIPPED`。

完成度过半，说明大部分工作已经做完，没必要推倒重来。失败任务的直接和间接下游标成 `SKIPPED`，不受影响的任务继续跑。

【截图：失败后的两条路；风格：whiteboard；截图目标：展示完成度低于一半时重规划、过半时跳过下游继续执行、到达上限时停止；关键词：完成度 50%、重规划上限、跳过下游】

### 重新规划时模型知道什么

```java
context.append("原任务: ").append(failedPlan.getGoal()).append("\n");
context.append("失败原因: ").append(failureReason).append("\n");
context.append("已完成的任务:\n");
for (Task task : failedPlan.getAllTasks()) {
    if (task.getStatus() == Task.TaskStatus.COMPLETED) {
        context.append("- ").append(task.getId())
                .append(": ").append(task.getDescription()).append("\n");
    }
}
context.append("\n请制定新的执行计划，避开之前的问题。");
return createPlan(context.toString());
```

模型只知道哪些任务做完了，看不到它们的结果。新计划是一份全新的计划，不继承旧计划的完成状态，所以没法保证只重做失败的那一步。审阅新计划时要留意有没有重复操作。

为什么不把结果也带上？规划请求要保持小而固定。结果一带上，读过大文件的任务会把规划请求撑得很长，而这份请求的格式也是评测重放器逐字核对的内容。这是 ForgePilot 目前明确保留的限制，要改得连重放器一起改。

### 用户取消的时候

用户按 ESC 取消时，正在跑的任务会被打断，这些任务会以异常的形式回来。如果按普通失败处理，完成度又不到一半，执行器就会打印“🔄 尝试重新规划”，在用户已经放弃的任务上再调一次模型。

所以一轮结果回来后，执行器先检查取消状态，已取消就把计划标成 `CANCELLED`，直接返回“⏹️ 已取消当前计划执行。”，不进入失败处理。

### 汇总

有任务失败时，汇总先列已完成任务的结果，再列失败和跳过的原因。已经流式显示过的结果不再重复打印。格式如下，内容为示意。

```text
⚠️ 计划部分完成，有任务失败。
已完成的任务结果:
[task_1] pom.xml 使用 Maven 构建，Java 17
[task_2] src/main/java 下共有 3 个包
任务 task_3 失败: 读取 README.md 超时
任务 task_5 已跳过: 依赖的任务 task_3 失败
```

![](https://cdn.paicoding.com/stutymore/build-agent-p2-plan-execute-20260924183403-4e7044a4.png)

**一个任务失败，不能让已经做完的工作跟着一起消失。**

## 08、怎么进入计划模式，计划怎么审

ForgePilot 启动后默认走 ReAct。进入 Plan-and-Execute 有三种方式。

- `/plan`：只有下一条任务走计划模式，执行完回到原来的模式
- `/plan <任务>`：这一条任务直接走计划模式
- Shift+Tab 切到 plan 模式，或者输入 `/mode plan`：之后每条普通输入都先规划再执行，状态栏显示“PLAN shift+tab to cycle”，直到切走

Shift+Tab 按 auto → plan → ask 循环。plan 模式的审批档位和 auto 一样，只是多了一个“普通输入走计划”的开关。在 plan 模式下显式输入 `/team`，仍然走 Multi-Agent，本条输入里显式写的命令优先。

【截图：Shift+Tab 切到 plan 模式；风格：data-board；截图目标：展示状态栏从 AUTO 切到 PLAN，普通输入直接进入规划；关键词：Shift+Tab、PLAN 模式、状态栏】

### 审阅

计划生成之后、执行之前，终端会停下来。默认显示折叠摘要，有目标、任务数、并行批次数、当前可执行和首批执行的任务。

```text
📝 计划已生成。
   - 回车：按当前计划执行
   - Ctrl+O：展开完整计划
   - ESC：折叠或取消本次计划
   - I：输入补充要求后重新规划
```

Ctrl+O 展开带状态图标的完整任务图，ESC 在展开时折叠回摘要，在摘要里就是取消。执行期间这张图不会刷新，只输出逐行日志，比如“▶️ 执行任务”“⚡ 本轮并行执行 N 个任务”“✅ 完成”“❌ 失败”。

【截图：计划审阅的折叠摘要和 Ctrl+O 展开图；风格：data-board；截图目标：真实运行中的摘要和展开后的任务图；关键词：计划审阅、并行批次、Ctrl+O】

按 I 输入的补充要求，会连同当前计划一起交给模型修改。

```java
public ExecutionPlan revisePlan(ExecutionPlan current, String feedback) throws IOException {
    String revisedGoal = current.getGoal() + "\n补充要求：" + feedback;
    String request = "请为以下任务制定执行计划：\n" + revisedGoal
            + "\n\n下面是用户刚审阅过的当前计划。请在它的基础上按补充要求修改，"
            + "不受补充要求影响的任务保持原样，输出修改后的完整计划：\n"
            + toPlanJson(current);
    return requestPlan(revisedGoal, request);
}
```

为什么不按补充后的目标从头规划？用户刚审过这份计划，只想改一两处，从头生成会把没问题的任务也换个说法，还得从头再审一遍。改完的计划仍然走同一套严格校验，然后再审一次。补充里如果写了“不要联网”，工具策略也会跟着收紧。补充要求不计入重规划次数。

终端读不到单个按键时会退回行模式。直接回车、`y`、`run` 都是执行，`/view` 展开，`cancel` 或 `esc` 取消，其他文字当作补充要求。

### 简单任务不调用模型规划

规划器开头有一个简单任务判断，三个条件同时满足就不调用模型，直接生成只有一个任务的计划。

- 不含“然后、并且、并、再、最后、同时、先、之后、接着、以及”这类多步提示词
- 去掉首尾空白后不超过 30 个字符
- 含“列出、查看、读取、显示、执行、运行、搜索、当前目录、文件”之一

命中后任务描述就是用户原文，这个计划仍然会经过审阅，执行时仍要调用模型，只是省掉了一次规划调用。

【截图：简单任务直接生成单任务计划；风格：data-board；截图目标：输入“列出当前目录的文件”后没有规划思考，直接出现单任务计划；关键词：简单任务、单任务计划、省一次规划】

## 09、跑起来看看

编译运行。

```bash
mvn clean package
java -jar target/paicli-1.0-SNAPSHOT.jar
```

先来一个串行的例子，每一步都依赖上一步。

```text
/plan 创建一个 Java 项目叫 demo，写一个 Hello 类输出 Hello World，然后编译运行
```

【截图：串行计划的审阅摘要；风格：data-board；截图目标：真实运行中生成的链式计划，任务依次依赖；关键词：/plan、链式依赖、计划审阅】

【截图：串行计划的执行过程；风格：data-board；截图目标：任务逐个执行、auto 模式放行编译命令、最后输出运行结果；关键词：逐个执行、auto 已放行、Hello World】

再来一个能并行的。

```text
/plan 请把任务拆成可并行的 DAG：
1. 读取 pom.xml
2. 列出 src/main/java 下的文件
3. 列出 src/test/java 下的文件
4. 读取 README.md 的前 80 行
5. 最后汇总以上结果，告诉我这个项目的构建方式、源码结构和测试情况。
要求：前 4 个任务彼此独立并行执行，最后 1 个任务依赖前 4 个任务。
```

【截图：并行计划的 Ctrl+O 展开图；风格：data-board；截图目标：前 4 个任务无依赖、第 5 个依赖前 4 个；关键词：DAG、并行批次、汇总任务】

【截图：并行执行日志；风格：data-board；截图目标：出现“本轮并行执行 4 个任务”，整轮结束后按顺序打印结果，再执行汇总任务；关键词：本轮并行执行、按序打印、汇总】

审阅时按 I，输入“汇总时用表格输出”，看模型怎么在原计划上修改。

【截图：按 I 补充要求后修改的计划；风格：data-board；截图目标：前 4 个任务保持不变，只有汇总任务的描述变了；关键词：补充要求、修改计划、再次审阅】

最后按 Shift+Tab 切到 plan 模式，直接输入一条普通任务，不用再敲 `/plan`。

【截图：plan 模式下直接输入任务；风格：data-board；截图目标：状态栏显示 PLAN，普通输入直接进入规划；关键词：plan 模式、Shift+Tab、普通输入】

## 简历怎么写

### ForgePilot｜Java 终端 Coding Agent｜核心开发 第 2 期

项目简介：在 ReAct Agent 的基础上实现 Plan-and-Execute 执行模式，由模型先生成带依赖关系的任务计划，用户审阅确认后按 DAG 分批并行执行，并支持计划修改、失败重规划与部分结果汇总，适用于步骤多、依赖明确的本地研发任务。

技术栈：Java 17、Maven、Jackson、JLine3、ExecutorService、DAG 拓扑排序、Plan-and-Execute

核心职责：

- 设计 Task 与 Execution Plan 模型，用双向依赖记录上下游关系，实现 PENDING、RUNNING、COMPLETED、FAILED、SKIPPED 五种任务状态流转；基于 DFS 后序遍历完成拓扑排序与环检测。

- 实现 Planner 规划器，规划请求不暴露工具，提示词约束简单任务 1-3 步、复杂任务 5-10 步，并要求写同一文件的任务串行；对模型输出做严格校验，尾随文字、重复键、重复 id、未声明依赖和循环依赖一律拒绝执行，不静默修补计划。

- 实现按轮 DAG 调度器，每轮筛选依赖已完成的任务，用线程池最多 4 路并行执行；并行任务输出先写入独立缓冲区，整轮结束后按拓扑顺序打印，审批提示直接输出不受缓冲影响。

- 设计任务间上下文传递机制，下游任务只接收直接依赖的完整结果，每个任务使用独立的工具策略副本，web_search 得到的可信 URL 只沿 DAG 声明的依赖向下游传递，防止跨分支的提示词注入扩大联网权限。

- 实现失败处理与重规划，完成度低于 50% 时基于已完成进度重新规划且默认最多 2 次，完成度过半时跳过失败任务的传递下游并继续执行；用户取消时不触发重规划，汇总保留已完成结果与失败、跳过原因。

- 集成 JLine3 实现计划审阅交互，支持回车执行、Ctrl+O 展开任务图、ESC 取消和补充要求后在原计划基础上修改，并接入 Shift+Tab 会话模式，plan 模式下普通输入自动走计划执行。
