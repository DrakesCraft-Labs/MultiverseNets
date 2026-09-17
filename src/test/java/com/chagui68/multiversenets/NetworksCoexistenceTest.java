package com.chagui68.multiversenets;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests coexistence and namespace isolation with the legacy Networks plugin on the same server.
 * [ES] Pruebas de convivencia y aislamiento de nombres con el plugin legacy Networks en el mismo servidor.
 */
class NetworksCoexistenceTest {

    private static final String PLUGIN_NETWORKS = "NetworksV6-Drake";
    private static final String MAIN_NETWORKS = "io.github.sefiraat.networks.Networks";
    private static final String COMMAND_NETWORKS = "networks";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> pluginYml() {
        try (InputStream in = NetworksCoexistenceTest.class.getClassLoader()
                .getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml not found");
            return new Yaml().load(in);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    /**
     * [EN] Plugin name must not collide with Networks.
     * [ES] El nombre del plugin no debe colisionar con Networks.
     */
    @Test
    void pluginNameDoesNotCollideWithNetworks() {
        assertNotEquals(PLUGIN_NETWORKS, pluginYml().get("name"),
                "two plugins with the same name cause Paper to load ambiguously");
    }

    /**
     * [EN] Main class must not collide with Networks.
     * [ES] La clase principal no debe colisionar con Networks.
     */
    @Test
    void mainClassDoesNotCollideWithNetworks() {
        assertNotEquals(MAIN_NETWORKS, pluginYml().get("main"));
    }

    /**
     * [EN] Commands and aliases must not override Networks commands.
     * [ES] Los comandos y alias no deben sobrescribir los comandos de Networks.
     */
    @Test
    @SuppressWarnings("unchecked")
    void commandsAndAliasesDoNotCollideWithNetworks() {
        Map<String, Object> commands = (Map<String, Object>) pluginYml().get("commands");
        assertNotNull(commands);
        for (Map.Entry<String, Object> entry : commands.entrySet()) {
            assertNotEquals(COMMAND_NETWORKS, entry.getKey(),
                    "command collides with Networks command");
            Object alias = ((Map<String, Object>) entry.getValue()).get("aliases");
            if (alias instanceof List<?> list) {
                assertFalse(list.contains(COMMAND_NETWORKS),
                        "an alias collides with Networks command");
            }
        }
    }

    /**
     * [EN] Permissions must reside under multiversenets prefix without invading networks prefix.
     * [ES] Los permisos deben estar bajo el prefijo multiversenets sin invadir el de networks.
     */
    @Test
    @SuppressWarnings("unchecked")
    void permissionsUseProperPrefix() {
        Map<String, Object> permissions = (Map<String, Object>) pluginYml().get("permissions");
        assertNotNull(permissions);
        for (String perm : permissions.keySet()) {
            assertTrue(perm.startsWith("multiversenets."),
                    "permission '" + perm + "' is outside plugin prefix");
            assertFalse(perm.startsWith("networks."),
                    "permission '" + perm + "' invades networks prefix namespace");
        }
    }

    /**
     * [EN] Slimefun dependency must remain soft for standalone compatibility.
     * [ES] La dependencia de Slimefun debe ser suave (softdepend) para compatibilidad standalone.
     */
    @Test
    void slimefunDependencyIsSoft() {
        Map<String, Object> yml = pluginYml();
        assertFalse(yml.containsKey("depend"),
                "a hard dependency on Slimefun breaks standalone functionality");
        assertNotNull(yml.get("softdepend"), "Slimefun should load before if present, but remain optional");
    }
}
