package com.paicli.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliCommandDispatcherTest {
    @Test
    void classifiesProcessEntryPointsWithoutBuildingInteractiveState() {
        assertEquals(CliCommandDispatcher.LaunchMode.INTERACTIVE,
                CliCommandDispatcher.classify(new String[0]));
        assertEquals(CliCommandDispatcher.LaunchMode.RUNTIME_API,
                CliCommandDispatcher.classify(new String[]{"serve", "--http", "--port", "9000"}));
        assertEquals(CliCommandDispatcher.LaunchMode.WECHAT,
                CliCommandDispatcher.classify(new String[]{"wechat", "status"}));
    }

    @Test
    void invalidPortFallsBackToConfiguredDefault() {
        assertEquals(8080, CliCommandDispatcher.parseServePort(
                new String[]{"serve", "--http", "--port", "bad"}, 8080));
        assertEquals(9000, CliCommandDispatcher.parseServePort(
                new String[]{"serve", "--http", "--port", "9000"}, 8080));
    }
}
