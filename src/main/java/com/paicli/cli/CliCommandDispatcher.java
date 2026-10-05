package com.paicli.cli;

import com.paicli.wechat.WechatCommandMain;

/**
 * Classifies process-level CLI entry points before the interactive session is
 * assembled. Keeping this decision outside {@link Main} makes the startup
 * path testable without creating a terminal or an LLM client.
 */
final class CliCommandDispatcher {
    enum LaunchMode {
        WECHAT,
        RUNTIME_API,
        INTERACTIVE
    }

    private CliCommandDispatcher() {
    }

    static LaunchMode classify(String[] args) {
        if (WechatCommandMain.isWechatCommand(args)) {
            return LaunchMode.WECHAT;
        }
        if (isRuntimeServeCommand(args)) {
            return LaunchMode.RUNTIME_API;
        }
        return LaunchMode.INTERACTIVE;
    }

    static boolean isRuntimeServeCommand(String[] args) {
        return args != null
                && args.length >= 1
                && "serve".equalsIgnoreCase(args[0])
                && java.util.Arrays.stream(args).anyMatch("--http"::equalsIgnoreCase);
    }

    static int parseServePort(String[] args, int defaultPort) {
        if (args == null) {
            return defaultPort;
        }
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equalsIgnoreCase(args[i])) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException ignored) {
                    return defaultPort;
                }
            }
        }
        return defaultPort;
    }
}
