package com.paicli.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paicli.agent.Agent;
import com.paicli.render.Renderer;
import com.paicli.render.StatusInfo;
import com.paicli.hitl.ApprovalRequest;
import com.paicli.hitl.ApprovalResult;
import com.paicli.tool.ToolRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic, offline ReAct smoke test: a provider tool call, a real file read,
 * and a final provider answer all happen through the production execution path.
 */
class MockLlmReActE2ETest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void executesReadFileAndReturnsFinalAnswerWithExactlyTwoRequests(@TempDir Path workspace)
            throws Exception {
        Files.writeString(workspace.resolve("input.txt"), "fixture payload\n");
        try (MockWebServer server = new MockWebServer()) {
            enqueueToolCall(server);
            enqueueFinalAnswer(server);

            ToolRegistry registry = new ToolRegistry();
            registry.setProjectPath(workspace.toString());
            Agent agent = new Agent(new DeepSeekClient(
                    "mock-key", "deepseek-flash", server.url("/chat/completions").toString()), registry);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            agent.setRenderer(new SilentRenderer(output));
            agent.setReturnFinalResponseWhenStreamed(true);

            String answer = agent.run("Read input.txt and report its contents.");

            assertTrue(answer.contains("Mock read complete"), "final provider answer should be returned");
            assertEquals("fixture payload\n", Files.readString(workspace.resolve("input.txt")),
                    "the read_file tool must execute against the temporary workspace");

            RecordedRequest first = server.takeRequest();
            RecordedRequest second = server.takeRequest();
            assertEquals("POST", first.getMethod());
            assertEquals("POST", second.getMethod());
            JsonNode firstBody = JSON.readTree(first.getBody().readUtf8());
            assertTrue(firstBody.path("tools").toString().contains("read_file"),
                    "the first request must expose the production read_file tool schema");
            JsonNode secondBody = JSON.readTree(second.getBody().readUtf8());
            boolean hasToolResult = false;
            for (JsonNode message : secondBody.path("messages")) {
                if ("tool".equals(message.path("role").asText())
                        && message.path("content").asText().contains("fixture payload")) {
                    hasToolResult = true;
                    break;
                }
            }
            assertTrue(hasToolResult, "the second request must carry the real read_file result");
            assertEquals(2, server.getRequestCount(), "the scripted run must make exactly two LLM requests");
        }
    }

    private static void enqueueToolCall(MockWebServer server) {
        server.enqueue(sse("""
                {"choices":[{"delta":{"role":"assistant","tool_calls":[{"index":0,"id":"call_read_file","type":"function","function":{"name":"read_file","arguments":"{\\"path\\":\\"input.txt\\"}"}}]},"finish_reason":"tool_calls"}]}
                """));
    }

    private static void enqueueFinalAnswer(MockWebServer server) {
        server.enqueue(sse("""
                {"choices":[{"delta":{"role":"assistant","content":"Mock read complete: fixture payload"},"finish_reason":"stop"}]}
                """));
    }

    private static MockResponse sse(String event) {
        return new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("data: " + event.trim() + "\n\ndata: [DONE]\n\n");
    }

    private static final class SilentRenderer implements Renderer {
        private final PrintStream stream;

        private SilentRenderer(ByteArrayOutputStream output) {
            this.stream = new PrintStream(output, true, StandardCharsets.UTF_8);
        }

        @Override public void start() { }
        @Override public void close() { stream.close(); }
        @Override public PrintStream stream() { return stream; }
        @Override public void appendToolCalls(List<LlmClient.ToolCall> toolCalls) { }
        @Override public void appendDiff(String filePath, String before, String after) { }
        @Override public void updateStatus(StatusInfo status) { }
        @Override public ApprovalResult promptApproval(ApprovalRequest request) { return ApprovalResult.approve(); }
        @Override public int openPalette(String title, List<String> items) { return -1; }
    }
}
