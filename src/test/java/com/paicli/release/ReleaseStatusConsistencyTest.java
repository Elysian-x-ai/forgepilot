package com.paicli.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents public release and benchmark status copies from drifting apart. */
class ReleaseStatusConsistencyTest {
    private static final String VERSION = "16.1.0";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void productVersionAndCurrentEvaluationStatusAreConsistent() throws Exception {
        Path root = Path.of("");
        JsonNode status = JSON.readTree(root.resolve(
                "benchmarks/paicli-native-agentbench-v0.1/status.json").toFile());

        assertEquals(VERSION, status.path("productVersion").asText());
        assertEquals(25, status.path("prototypeCount").asInt());
        assertEquals(28, status.path("prototypeTotal").asInt());
        assertEquals(88, status.path("originalWeight").asInt());
        assertEquals(100, status.path("originalWeightTotal").asInt());
        assertEquals(List.of("D5", "E3", "E4"),
                JSON.convertValue(status.path("missingCases"), List.class));
        assertTrue(status.path("formalScores").isNull());
        assertFalse(status.path("publishable").asBoolean());
        assertEquals("NOT_INTEGRATED", status.path("runnerIntegrationStatus").asText());

        String pom = Files.readString(root.resolve("pom.xml"));
        String main = Files.readString(root.resolve("src/main/java/com/paicli/cli/Main.java"));
        String readme = Files.readString(root.resolve("README.md"));
        String agents = Files.readString(root.resolve("AGENTS.md"));

        assertEquals(VERSION, mavenProperty(pom, "forgepilot.version"));
        assertTrue(main.contains("VERSION = \"" + VERSION + "\""));
        for (String document : List.of(readme, agents)) {
            assertTrue(document.contains("16.1.0"), "public docs must mention the product version");
            assertTrue(document.contains("25/28"), "public docs must expose current prototype coverage");
            assertTrue(document.contains("88/100"), "public docs must expose current original weight");
            assertTrue(document.contains("NOT_INTEGRATED"));
            assertTrue(document.contains("formalScores=null"));
            assertTrue(document.contains("publishable=false"));
        }
        assertTrue(readme.contains("target/paicli-16.1.0.jar"));
        assertTrue(agents.contains("target/paicli-16.1.0.jar"));
        assertFalse(readme.contains("24/28"), "README current status must not retain the stale aggregate");
        assertFalse(readme.contains("84/100"), "README current status must not retain the stale weight");
    }

    private static String mavenProperty(String pom, String property) throws Exception {
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(pom.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Element properties = (Element) document.getElementsByTagName("properties").item(0);
        return properties.getElementsByTagName(property).item(0).getTextContent().trim();
    }
}
