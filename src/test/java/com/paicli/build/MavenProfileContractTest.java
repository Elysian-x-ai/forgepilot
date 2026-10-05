package com.paicli.build;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the product/benchmark test boundary from accidental profile drift. */
class MavenProfileContractTest {
    @Test
    void profilesHaveDisjointSurefireSelectionRules() throws Exception {
        Path pom = Path.of("pom.xml");
        assertTrue(Files.isRegularFile(pom), "run this contract from the Maven project root");

        var document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(pom.toFile());
        Element profiles = firstElement(document.getDocumentElement(), "profiles");
        assertNotNull(profiles);

        Element core = profile(profiles, "core");
        Element quick = profile(profiles, "quick");
        Element benchmark = profile(profiles, "benchmark");
        assertFalse(core == null || quick == null || benchmark == null,
                "core, quick, and benchmark profiles are required");

        String coreConfig = surefireConfig(core);
        String quickConfig = surefireConfig(quick);
        String benchmarkConfig = surefireConfig(benchmark);

        assertTrue(coreConfig.contains("**/eval/benchmark/**"),
                "core must exclude benchmark tests");
        assertTrue(quickConfig.contains("**/eval/benchmark/**"),
                "quick must exclude benchmark tests");
        assertTrue(benchmarkConfig.contains("**/eval/benchmark/**/*Test.java"),
                "benchmark must include only benchmark tests");
        assertFalse(hasInclude(benchmark, "**/*Test.java"),
                "benchmark must not use the product-wide test include");
        assertTrue(coreConfig.contains("platform-seatbelt"),
                "core must exclude host-dependent Seatbelt JVM probes");
        assertTrue(quickConfig.contains("platform-seatbelt"),
                "quick must exclude host-dependent Seatbelt JVM probes");
    }

    private static Element profile(Element profiles, String id) {
        NodeList children = profiles.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element
                    && "profile".equals(element.getTagName())
                    && id.equals(text(firstElement(element, "id")))) {
                return element;
            }
        }
        return null;
    }

    private static String surefireConfig(Element profile) {
        Element plugin = surefirePlugin(profile);
        return plugin == null ? "" : plugin.getTextContent();
    }

    private static boolean hasInclude(Element profile, String value) {
        Element plugin = surefirePlugin(profile);
        Element configuration = firstElement(plugin, "configuration");
        Element includes = firstElement(configuration, "includes");
        if (includes == null) {
            return false;
        }
        NodeList children = includes.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element
                    && "include".equals(element.getTagName())
                    && value.equals(text(element))) {
                return true;
            }
        }
        return false;
    }

    private static Element surefirePlugin(Element profile) {
        Element build = firstElement(profile, "build");
        Element plugins = firstElement(build, "plugins");
        if (plugins == null) {
            return null;
        }
        NodeList children = plugins.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element plugin) || !"plugin".equals(plugin.getTagName())) {
                continue;
            }
            if ("maven-surefire-plugin".equals(text(firstElement(plugin, "artifactId")))) {
                return plugin;
            }
        }
        return null;
    }

    private static Element firstElement(Node parent, String name) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && name.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    private static String text(Element element) {
        return element == null ? "" : element.getTextContent().trim();
    }
}
