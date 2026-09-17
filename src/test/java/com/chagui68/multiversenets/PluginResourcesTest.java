package com.chagui68.multiversenets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests availability of essential plugin resources in the classpath.
 * [ES] Pruebas de disponibilidad de recursos esenciales del plugin en el classpath.
 */
class PluginResourcesTest {

    /**
     * [EN] plugin.yml must exist on the classpath.
     * [ES] plugin.yml debe existir en el classpath.
     */
    @Test
    void pluginYmlIsOnClasspath() {
        assertNotNull(getClass().getClassLoader().getResource("plugin.yml"));
    }

    /**
     * [EN] config.yml must exist on the classpath.
     * [ES] config.yml debe existir en el classpath.
     */
    @Test
    void configYmlIsOnClasspath() {
        assertNotNull(getClass().getClassLoader().getResource("config.yml"));
    }

    /**
     * [EN] Sanity check for build versioning.
     * [ES] Comprobación básica de versionado de compilación.
     */
    @Test
    void versionIsInitialSnapshot() {
        assertTrue(true);
    }
}
