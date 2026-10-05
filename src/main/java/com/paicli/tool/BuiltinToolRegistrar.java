package com.paicli.tool;

/**
 * Wires the built-in tools into a registry in one deterministic order.
 * Tool implementations remain on the registry for now so existing private
 * state and extension hooks stay compatible while the registration boundary
 * is made explicit.
 */
final class BuiltinToolRegistrar {
    private BuiltinToolRegistrar() {
    }

    static void registerAll(ToolRegistry registry) {
        registry.registerFileTools();
        registry.registerShellTools();
        registry.registerCodeTools();
        registry.registerRagTools();
        registry.registerWebTools();
        registry.registerBrowserTools();
        registry.registerMemoryTools();
        registry.registerSkillTools();
        registry.registerSnapshotTools();
    }
}
