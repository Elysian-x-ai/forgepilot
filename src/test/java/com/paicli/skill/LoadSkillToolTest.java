package com.paicli.skill;

import com.paicli.tool.LoadedSkillMessages;
import com.paicli.tool.ToolRegistry;
import com.paicli.tool.ToolRegistry.ToolExecutionResult;
import com.paicli.tool.ToolRegistry.ToolInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadSkillToolTest {

    @Test
    void loadsExistingSkillAndBuildsSameTurnMessage(@TempDir Path tempDir) throws IOException {
        ToolRegistry tools = toolsWith(registryWith(tempDir, "web-access", "决策手册",
                "# Body\nwhen to fetch\nwhen to browse\n"));

        List<ToolExecutionResult> results = load(tools, "{\"name\":\"web-access\"}");

        assertTrue(results.get(0).result().contains("已加载 skill 'web-access'"), results.get(0).result());
        assertFalse(results.get(0).result().contains("when to fetch"), "正文不应进入 untrusted 工具结果");
        String message = LoadedSkillMessages.from(results, tools.getSkillRegistry());
        assertTrue(message.contains("## 已加载 Skill：web-access"), message);
        assertTrue(message.contains("when to fetch"), message);
    }

    @Test
    void unknownSkillProducesNoInjection(@TempDir Path tempDir) throws IOException {
        ToolRegistry tools = toolsWith(registryWith(tempDir, "real-one", "desc", "body"));

        List<ToolExecutionResult> results = load(tools, "{\"name\":\"nonexistent\"}");

        assertTrue(results.get(0).result().contains("未找到"), results.get(0).result());
        assertEquals("", LoadedSkillMessages.from(results, tools.getSkillRegistry()));
    }

    @Test
    void disabledSkillProducesNoInjection(@TempDir Path tempDir) throws IOException {
        SkillStateStore state = new SkillStateStore(tempDir.resolve("skills.json"));
        state.disable("web-access");
        SkillRegistry registry = new SkillRegistry(null,
                writeUserSkill(tempDir, "web-access", "desc", "body").getParent().getParent(),
                null, state);
        registry.reload();
        ToolRegistry tools = toolsWith(registry);

        List<ToolExecutionResult> results = load(tools, "{\"name\":\"web-access\"}");

        assertTrue(results.get(0).result().contains("已被禁用"), results.get(0).result());
        assertEquals("", LoadedSkillMessages.from(results, tools.getSkillRegistry()));
    }

    @Test
    void truncatesOversizedBody(@TempDir Path tempDir) throws IOException {
        StringBuilder big = new StringBuilder();
        while (big.length() < 6 * 1024) big.append("0123456789");
        ToolRegistry tools = toolsWith(registryWith(tempDir, "huge", "desc", big.toString()));

        List<ToolExecutionResult> results = load(tools, "{\"name\":\"huge\"}");

        assertTrue(results.get(0).result().contains("已加载 skill 'huge'"));
        String message = LoadedSkillMessages.from(results, tools.getSkillRegistry());
        assertTrue(message.contains("(skill body truncated"), "应包含截断标记");
    }

    @Test
    void sameSkillLoadedTwiceInOneBatchIsInjectedOnce(@TempDir Path tempDir) throws IOException {
        ToolRegistry tools = toolsWith(registryWith(tempDir, "demo", "desc", "demo-body"));

        List<ToolExecutionResult> results = tools.executeTools(List.of(
                new ToolInvocation("call-1", "load_skill", "{\"name\":\"demo\"}"),
                new ToolInvocation("call-2", "load_skill", "{\"name\":\"demo\"}")));

        String message = LoadedSkillMessages.from(results, tools.getSkillRegistry());
        assertEquals(message.indexOf("## 已加载 Skill：demo"), message.lastIndexOf("## 已加载 Skill：demo"));
    }

    @Test
    void failsWhenNameMissing() {
        ToolRegistry tools = toolsWith(new SkillRegistry(null, null, null, null));

        String result = tools.executeTool("load_skill", "{}");
        assertTrue(result.contains("name 不能为空"), result);
    }

    private static ToolRegistry toolsWith(SkillRegistry registry) {
        ToolRegistry tools = new ToolRegistry();
        tools.setSkillRegistry(registry);
        return tools;
    }

    private static List<ToolExecutionResult> load(ToolRegistry tools, String argumentsJson) {
        return tools.executeTools(List.of(new ToolInvocation("call-1", "load_skill", argumentsJson)));
    }

    private static SkillRegistry registryWith(Path tempDir, String name, String desc, String body) throws IOException {
        Path userRoot = writeUserSkill(tempDir, name, desc, body).getParent().getParent();
        SkillStateStore state = new SkillStateStore(tempDir.resolve("skills.json"));
        SkillRegistry registry = new SkillRegistry(null, userRoot, null, state);
        registry.reload();
        return registry;
    }

    private static Path writeUserSkill(Path tempDir, String name, String desc, String body) throws IOException {
        Path userRoot = tempDir.resolve("user-skills");
        Path skillDir = userRoot.resolve(name);
        Files.createDirectories(skillDir);
        Path skillMd = skillDir.resolve("SKILL.md");
        Files.writeString(skillMd,
                "---\nname: " + name
                        + "\ndescription: " + desc
                        + "\n---\n" + body + "\n");
        return skillMd;
    }
}
