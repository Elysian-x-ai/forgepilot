package com.paicli.brand;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/** Shared public identity and compatibility helpers for the ForgePilot release. */
public final class ForgePilotBrand {
    public static final String PRODUCT_NAME = "ForgePilot";
    public static final String MARK = "◆";
    public static final String POSITIONING = "A controlled engineering agent for the terminal";

    private static final Set<String> EMITTED_LEGACY_NOTICES = new HashSet<>();

    private ForgePilotBrand() {
    }

    /** Returns the modern value first, then the legacy value, ignoring blank values. */
    public static String firstNonBlank(String modernKey,
                                       String legacyKey,
                                       Function<String, String> lookup) {
        String modern = lookup.apply(modernKey);
        if (modern != null && !modern.isBlank()) {
            return modern.trim();
        }
        String legacy = lookup.apply(legacyKey);
        return legacy == null || legacy.isBlank() ? null : legacy.trim();
    }

    /** Creates a one-time migration hint for a legacy setting. */
    public static synchronized String legacyNotice(String legacyKey, String modernKey) {
        String noticeKey = legacyKey + "->" + modernKey;
        if (!EMITTED_LEGACY_NOTICES.add(noticeKey)) {
            return "";
        }
        return "ForgePilot compatibility: " + legacyKey + " is supported; prefer " + modernKey + ".";
    }

    static synchronized void resetLegacyNoticeForTests() {
        EMITTED_LEGACY_NOTICES.clear();
    }
}
