package com.chagui68.multiversenets.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class Text {

    public static final String PREFIX_RAW = "[MVN] ";

    private Text() {
    }

    public static Component prefix(Component body) {
        return Component.text(PREFIX_RAW, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .append(body);
    }

    public static Component msg(String text, NamedTextColor color) {
        return prefix(Component.text(text, color).decoration(TextDecoration.ITALIC, false));
    }
}
