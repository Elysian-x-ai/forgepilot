package com.paicli.brand;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgePilotBrandTest {
    private final Map<String, String> values = new HashMap<>();

    @AfterEach
    void resetNoticeState() {
        ForgePilotBrand.resetLegacyNoticeForTests();
    }

    @Test
    void exposesApprovedProductIdentity() {
        assertEquals("ForgePilot", ForgePilotBrand.PRODUCT_NAME);
        assertEquals("◆", ForgePilotBrand.MARK);
        assertEquals("A controlled engineering agent for the terminal", ForgePilotBrand.POSITIONING);
    }

    @Test
    void modernAliasWinsWhenBothValuesArePresent() {
        values.put("FORGEPILOT_RENDERER", "inline");
        values.put("PAICLI_RENDERER", "plain");

        assertEquals("inline", ForgePilotBrand.firstNonBlank(
                "FORGEPILOT_RENDERER", "PAICLI_RENDERER", values::get));
    }

    @Test
    void legacyAliasRemainsAvailableWhenModernValueIsMissing() {
        values.put("PAICLI_RENDERER", "lanterna");

        assertEquals("lanterna", ForgePilotBrand.firstNonBlank(
                "FORGEPILOT_RENDERER", "PAICLI_RENDERER", values::get));
    }

    @Test
    void legacyNoticeIsEmittedOnlyOncePerProcess() {
        String first = ForgePilotBrand.legacyNotice("PAICLI_RENDERER", "FORGEPILOT_RENDERER");
        String second = ForgePilotBrand.legacyNotice("PAICLI_RENDERER", "FORGEPILOT_RENDERER");

        assertTrue(first.contains("PAICLI_RENDERER"));
        assertTrue(first.contains("FORGEPILOT_RENDERER"));
        assertEquals("", second);
    }
}
