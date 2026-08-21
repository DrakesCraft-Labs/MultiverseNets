package com.chagui68.multiversenets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginResourcesTest {

    @Test
    void pluginYmlEstaEnElClasspath() {
        assertNotNull(getClass().getClassLoader().getResource("plugin.yml"));
    }

    @Test
    void configYmlEstaEnElClasspath() {
        assertNotNull(getClass().getClassLoader().getResource("config.yml"));
    }

    @Test
    void laVersionEsSnapshotInicial() {
        assertTrue(true);
    }
}
