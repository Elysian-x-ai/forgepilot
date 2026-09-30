package com.paicli.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paicli.skill.Skill;
import com.paicli.skill.SkillRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把本批工具结果里成功的 load_skill 整理成一条 user 消息，由调用方紧跟在工具结果之后追加，
 * 让同一轮的下一次 LLM 请求就能看到 SKILL.md 正文。
 *
 * <p>正文不放进工具结果本身：工具结果统一经 {@link ToolResultBoundary} 包成 untrusted-data，
 * 模型会被要求不执行其中的指令，而 Skill 恰恰是本地可信的操作指引。这里不持有任何共享状态，
 * 并行的 Plan 任务或 Team worker 只会拿到自己这一批工具结果里加载的 Skill。</p>
 */
public final class LoadedSkillMessages {

    static final String TOOL_NAME = "load_skill";
    static final int MAX_BODY_CHARS = 5 * 1024;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LoadedSkillMessages() {
    }

    /**
     * @return 可直接作为 user 消息内容的 Skill 指引；本批没有成功加载的 Skill 时返回空串
     */
    public static String from(List<ToolRegistry.ToolExecutionResult> results, SkillRegistry registry) {
        if (results == null || results.isEmpty() || registry == null) {
            return "";
        }
        Map<String, String> bodies = new LinkedHashMap<>();
        for (ToolRegistry.ToolExecutionResult result : results) {
            if (result == null || !TOOL_NAME.equals(result.name()) || !result.successful()) {
                continue;
            }
            String name = skillName(result.argumentsJson());
            Skill skill = name == null ? null : registry.findSkill(name);
            if (skill != null) {
                bodies.putIfAbsent(skill.name(), truncatedBody(skill));
            }
        }
        if (bodies.isEmpty()) {
            return "";
        }
        StringBuilder message = new StringBuilder("以下是刚才 load_skill 加载的 Skill 指引，请按指引继续当前任务。\n\n");
        bodies.forEach((name, body) -> message.append("## 已加载 Skill：").append(name).append('\n')
                .append(body.trim()).append("\n\n"));
        return message.append("---").toString();
    }

    static String truncatedBody(Skill skill) {
        String body = skill.body() == null ? "" : skill.body();
        if (body.length() <= MAX_BODY_CHARS) {
            return body;
        }
        return body.substring(0, MAX_BODY_CHARS)
                + "\n\n...(skill body truncated, full content via /skill show " + skill.name() + ")";
    }

    private static String skillName(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode name = MAPPER.readTree(argumentsJson).path("name");
            // 与 load_skill 执行器一致，按原值查找，避免工具报“未找到”却注入了正文
            return name.isTextual() && !name.asText().isBlank() ? name.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
