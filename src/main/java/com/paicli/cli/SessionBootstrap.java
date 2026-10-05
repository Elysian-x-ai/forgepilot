package com.paicli.cli;

import com.paicli.agent.Agent;
import com.paicli.config.PaiCliConfig;
import com.paicli.history.ConversationLedger;
import com.paicli.llm.LlmClient;
import com.paicli.llm.LlmClientFactory;
import com.paicli.runtime.api.RuntimeApiServer;
import com.paicli.runtime.api.RuntimeThreadStore;
import com.paicli.runtime.task.DurableTaskManager;
import com.paicli.tool.CommandSandboxMode;
import com.paicli.tool.CommandSandboxStatus;
import com.paicli.tool.ToolRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Builds non-interactive runtime resources shared by CLI entry points.
 * Interactive renderer wiring remains in {@link Main}; this class owns the
 * headless agent and Runtime API lifecycle so those concerns can evolve
 * independently.
 */
final class SessionBootstrap {
    private SessionBootstrap() {
    }

    static void startRuntimeApiAndBlock(String[] args) {
        PaiCliConfig config = PaiCliConfig.load();
        LlmClient client = LlmClientFactory.createFromConfig(config);
        if (client == null) {
            System.err.println("❌ 错误: 未找到可用的 API Key");
            System.exit(1);
        }
        int port = CliCommandDispatcher.parseServePort(args, 8080);
        try {
            RuntimeThreadStore store = new RuntimeThreadStore(RuntimeThreadStore.defaultDbPath());
            RuntimeApiServer server = new RuntimeApiServer(
                    store,
                    prompt -> runHeadlessTask(prompt, client),
                    port,
                    RuntimeApiServer.configuredApiKey());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.close();
                store.close();
            }, "paicli-runtime-api-shutdown"));
            server.start();
            System.out.println("✅ ForgePilot Runtime API 已启动: http://127.0.0.1:" + server.port());
            System.out.println("   认证: Authorization: Bearer <FORGEPILOT_RUNTIME_API_KEY>");
            System.out.println("   兼容请求头: X-ForgePilot-API-Key（旧 X-PaiCLI-API-Key 仍可用）");
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Runtime API 启动失败: " + e.getMessage());
            System.exit(1);
        }
    }

    static String runHeadlessTask(String prompt, LlmClient llmClient) {
        ToolRegistry registry = new ToolRegistry();
        registry.setProjectPath(Path.of(".").toAbsolutePath().normalize().toString());
        CommandSandboxStatus sandboxStatus = registry.configureCommandSandbox(
                CommandSandboxMode.fromConfiguration(), Path.of(registry.getProjectPath()));
        if (!sandboxStatus.message().isBlank()) {
            System.err.println(sandboxStatus.message());
        }
        Agent agent = new Agent(llmClient, registry);
        try {
            agent.setConversationLedger(ConversationLedger.openDefault(
                    Path.of(System.getProperty("user.home"))));
        } catch (IOException ignored) {
            // A background task should still run if its audit directory is temporarily unavailable.
        }
        return agent.run(prompt);
    }

    static DurableTaskManager openTaskManager(AtomicReference<LlmClient> llmClientRef) {
        try {
            return DurableTaskManager.openDefault(prompt -> runHeadlessTask(prompt, llmClientRef.get()));
        } catch (Exception e) {
            throw new IllegalStateException("后台任务管理器初始化失败: " + e.getMessage(), e);
        }
    }
}
