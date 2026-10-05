package com.paicli.tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolExecutionPolicyTest {
    @Test
    void preservesInvocationOrderWhenReadOnlyCallsRunInParallel() {
        List<String> seen = new CopyOnWriteArrayList<>();
        List<ToolRegistry.ToolInvocation> invocations = List.of(
                new ToolRegistry.ToolInvocation("1", "read_file", "{}"),
                new ToolRegistry.ToolInvocation("2", "grep_code", "{}"));

        List<ToolRegistry.ToolExecutionResult> results = ToolExecutionPolicy.executeBatch(
                invocations,
                invocation -> {
                    seen.add(invocation.id());
                    return ToolOutput.text("ok-" + invocation.id());
                },
                1);

        assertEquals(List.of("1", "2"), results.stream().map(ToolRegistry.ToolExecutionResult::id).toList());
        assertEquals(2, seen.size());
    }

    @Test
    void keepsWriteLikeCallsSerialBetweenReadOnlyRuns() {
        List<String> seen = new CopyOnWriteArrayList<>();
        List<ToolRegistry.ToolInvocation> invocations = List.of(
                new ToolRegistry.ToolInvocation("read", "read_file", "{}"),
                new ToolRegistry.ToolInvocation("write", "write_file", "{}"),
                new ToolRegistry.ToolInvocation("grep", "grep_code", "{}"));

        List<ToolRegistry.ToolExecutionResult> results = ToolExecutionPolicy.executeBatch(
                invocations,
                invocation -> {
                    seen.add(invocation.id());
                    return ToolOutput.text("ok");
                },
                1);

        assertEquals(List.of("read", "write", "grep"), results.stream()
                .map(ToolRegistry.ToolExecutionResult::id)
                .toList());
        assertEquals(3, seen.size());
        assertEquals("write", seen.get(1));
    }
}
