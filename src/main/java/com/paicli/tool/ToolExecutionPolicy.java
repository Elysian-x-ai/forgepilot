package com.paicli.tool;

import com.paicli.runtime.CancellationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Batch scheduling policy for one model response's tool calls.
 *
 * <p>The registry owns tool lookup and execution. This class owns only the
 * ordering and concurrency contract: a small allow-list of read-only tools
 * may run in parallel, while every other invocation remains serial.</p>
 */
final class ToolExecutionPolicy {
    private static final int MAX_PARALLEL_TOOLS = 4;
    private static final Set<String> PARALLEL_SAFE_TOOLS = Set.of(
            "read_file", "list_dir", "glob_files", "grep_code", "search_code",
            "web_search", "web_fetch", "load_skill");

    private ToolExecutionPolicy() {
    }

    static boolean isParallelSafeTool(String toolName) {
        return toolName != null && PARALLEL_SAFE_TOOLS.contains(toolName);
    }

    static List<ToolRegistry.ToolExecutionResult> executeBatch(
            List<ToolRegistry.ToolInvocation> invocations,
            Function<ToolRegistry.ToolInvocation, ToolOutput> executor,
            long batchTimeoutSeconds) {
        if (invocations == null || invocations.isEmpty()) {
            return List.of();
        }
        if (CancellationContext.isCancelled()) {
            return invocations.stream()
                    .map(invocation -> ToolRegistry.ToolExecutionResult.failed(
                            invocation, "用户取消了此次工具调用"))
                    .toList();
        }
        if (invocations.size() == 1
                || invocations.stream().anyMatch(invocation -> TurnToolPolicy.isBrowserToolName(invocation.name()))) {
            return executeSerially(invocations, executor);
        }
        if (invocations.stream().allMatch(invocation -> isParallelSafeTool(invocation.name()))) {
            return executeInParallel(invocations, executor, batchTimeoutSeconds);
        }

        List<ToolRegistry.ToolExecutionResult> results = new ArrayList<>(invocations.size());
        List<ToolRegistry.ToolInvocation> readOnlyRun = new ArrayList<>();
        for (ToolRegistry.ToolInvocation invocation : invocations) {
            if (isParallelSafeTool(invocation.name())) {
                readOnlyRun.add(invocation);
                continue;
            }
            results.addAll(flushReadOnlyRun(readOnlyRun, executor, batchTimeoutSeconds));
            results.addAll(executeSerially(List.of(invocation), executor));
        }
        results.addAll(flushReadOnlyRun(readOnlyRun, executor, batchTimeoutSeconds));
        return results;
    }

    private static List<ToolRegistry.ToolExecutionResult> flushReadOnlyRun(
            List<ToolRegistry.ToolInvocation> readOnlyRun,
            Function<ToolRegistry.ToolInvocation, ToolOutput> executor,
            long batchTimeoutSeconds) {
        if (readOnlyRun.isEmpty()) {
            return List.of();
        }
        List<ToolRegistry.ToolInvocation> run = List.copyOf(readOnlyRun);
        readOnlyRun.clear();
        return run.size() == 1
                ? executeSerially(run, executor)
                : executeInParallel(run, executor, batchTimeoutSeconds);
    }

    private static List<ToolRegistry.ToolExecutionResult> executeSerially(
            List<ToolRegistry.ToolInvocation> invocations,
            Function<ToolRegistry.ToolInvocation, ToolOutput> executor) {
        List<ToolRegistry.ToolExecutionResult> results = new ArrayList<>(invocations.size());
        for (ToolRegistry.ToolInvocation invocation : invocations) {
            if (CancellationContext.isCancelled()) {
                results.add(ToolRegistry.ToolExecutionResult.failed(
                        invocation, "用户取消了此次工具调用"));
                continue;
            }
            long startedAt = System.nanoTime();
            ToolOutput output = executor.apply(invocation);
            results.add(ToolRegistry.ToolExecutionResult.completed(
                    invocation, output, elapsedMillis(startedAt)));
        }
        return results;
    }

    private static List<ToolRegistry.ToolExecutionResult> executeInParallel(
            List<ToolRegistry.ToolInvocation> invocations,
            Function<ToolRegistry.ToolInvocation, ToolOutput> executor,
            long batchTimeoutSeconds) {
        int parallelism = Math.min(invocations.size(), MAX_PARALLEL_TOOLS);
        ExecutorService pool = Executors.newFixedThreadPool(parallelism, runnable -> {
            Thread thread = new Thread(runnable, "paicli-tool-executor");
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<Callable<ToolRegistry.ToolExecutionResult>> tasks = invocations.stream()
                    .<Callable<ToolRegistry.ToolExecutionResult>>map(invocation -> () -> {
                        if (CancellationContext.isCancelled()) {
                            return ToolRegistry.ToolExecutionResult.failed(
                                    invocation, "用户取消了此次工具调用");
                        }
                        long startedAt = System.nanoTime();
                        ToolOutput output = executor.apply(invocation);
                        return ToolRegistry.ToolExecutionResult.completed(
                                invocation, output, elapsedMillis(startedAt));
                    })
                    .toList();
            List<Future<ToolRegistry.ToolExecutionResult>> futures =
                    pool.invokeAll(tasks, batchTimeoutSeconds, TimeUnit.SECONDS);
            List<ToolRegistry.ToolExecutionResult> results = new ArrayList<>(invocations.size());
            for (int i = 0; i < futures.size(); i++) {
                ToolRegistry.ToolInvocation invocation = invocations.get(i);
                Future<ToolRegistry.ToolExecutionResult> future = futures.get(i);
                if (future.isCancelled()) {
                    results.add(ToolRegistry.ToolExecutionResult.timedOut(invocation, batchTimeoutSeconds));
                    continue;
                }
                try {
                    results.add(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    results.add(ToolRegistry.ToolExecutionResult.failed(invocation, "工具执行被中断"));
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    String message = cause == null || cause.getMessage() == null
                            ? "未知错误"
                            : cause.getMessage();
                    results.add(ToolRegistry.ToolExecutionResult.failed(invocation, message));
                }
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return invocations.stream()
                    .map(invocation -> ToolRegistry.ToolExecutionResult.failed(
                            invocation, "工具批次执行被中断"))
                    .toList();
        } finally {
            pool.shutdownNow();
        }
    }

    private static long elapsedMillis(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}
