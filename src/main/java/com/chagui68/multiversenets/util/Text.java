package com.chagui68.multiversenets.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * [EN] Chat Text Formatting Utilities
 * Formats chat messages with the standard MultiverseNets prefix and Adventure components.
 *
 * [ES] Utilidades de Formato de Texto para Chat
 * Formatea mensajes de chat con el prefijo estándar de MultiverseNets y componentes de Adventure.
 */
public final class Text {

    /** EN: Default text prefix / ES: Prefijo de texto por defecto. */
    public static final String PREFIX_RAW = "[MVN] ";

    private Text() {
    }

    /**
     * EN: Prepends the plugin prefix to a Kyori Adventure Component.
 *
     * ES: Añade el prefijo del plugin a un Componente de Kyori Adventure.
     */
    public static Component prefix(Component body) {
        return Component.text(PREFIX_RAW, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .append(body);
    }

    /**
     * EN: Creates a formatted prefix message with a specific text color.
 *
     * ES: Crea un mensaje formateado con prefijo y un color de texto específico.
     */
    public static Component msg(String text, NamedTextColor color) {
        return prefix(Component.text(text, color).decoration(TextDecoration.ITALIC, false));
    }
}
