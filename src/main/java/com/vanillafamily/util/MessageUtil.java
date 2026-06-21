package com.vanillafamily.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtil {

    // 使用 builder API 兼容 Paper 1.21+ 各版本 Adventure
    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character(LegacyComponentSerializer.AMPERSAND_CHAR)
                    .hexColors()
                    .build();

    public static Component format(String message) {
        if (message == null) return Component.empty();
        return SERIALIZER.deserialize(message);
    }

    public static Component format(String message, String... replacements) {
        if (message == null) return Component.empty();
        String result = message;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                result = result.replace(replacements[i], replacements[i + 1]);
            }
        }
        return SERIALIZER.deserialize(result);
    }
}
