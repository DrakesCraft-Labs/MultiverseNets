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
 * MultiverseNets tiene que poder convivir con Networks en el mismo servidor.
 *
 * Es un requisito explicito: si un dia se instala este plugin y NetworksV6 sigue puesto, los dos
 * deben funcionar. Lo que rompe esa convivencia no es el codigo sino los identificadores, y son
 * faciles de romper sin darse cuenta al renombrar algo. Por eso se fijan aqui.
 *
 * Los valores de NetworksV6-Drake con los que se compara: plugin `NetworksV6-Drake`, clase
 * principal `io.github.sefiraat.networks.Networks`, comando `/networks`, permisos bajo `networks.`
 */
class ConvivenciaConNetworksTest {

    private static final String PLUGIN_NETWORKS = "NetworksV6-Drake";
    private static final String MAIN_NETWORKS = "io.github.sefiraat.networks.Networks";
    private static final String COMANDO_NETWORKS = "networks";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> pluginYml() {
        try (InputStream in = ConvivenciaConNetworksTest.class.getClassLoader()
                .getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "no se encontro plugin.yml");
            return new Yaml().load(in);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    @Test
    void elNombreDelPluginNoChocaConNetworks() {
        assertNotEquals(PLUGIN_NETWORKS, pluginYml().get("name"),
                "dos plugins con el mismo nombre hacen que Paper elija uno de forma ambigua");
    }

    @Test
    void laClasePrincipalNoChocaConNetworks() {
        assertNotEquals(MAIN_NETWORKS, pluginYml().get("main"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void niElComandoNiSusAliasPisanLosDeNetworks() {
        Map<String, Object> comandos = (Map<String, Object>) pluginYml().get("commands");
        assertNotNull(comandos);
        for (Map.Entry<String, Object> entrada : comandos.entrySet()) {
            assertNotEquals(COMANDO_NETWORKS, entrada.getKey(),
                    "el comando choca con el de Networks");
            Object alias = ((Map<String, Object>) entrada.getValue()).get("aliases");
            if (alias instanceof List<?> lista) {
                assertFalse(lista.contains(COMANDO_NETWORKS),
                        "un alias choca con el comando de Networks");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void losPermisosVanBajoNuestroPrefijo() {
        Map<String, Object> permisos = (Map<String, Object>) pluginYml().get("permissions");
        assertNotNull(permisos);
        for (String permiso : permisos.keySet()) {
            assertTrue(permiso.startsWith("multiversenets."),
                    "el permiso '" + permiso + "' se sale del prefijo propio");
            assertFalse(permiso.startsWith("networks."),
                    "el permiso '" + permiso + "' invade el espacio de Networks");
        }
    }

    @Test
    void laDependenciaDeSlimefunEsBlanda() {
        Map<String, Object> yml = pluginYml();
        assertFalse(yml.containsKey("depend"),
                "una dependencia dura de Slimefun rompe la promesa de funcionar standalone");
        assertNotNull(yml.get("softdepend"), "Slimefun debe cargar antes, pero sin ser obligatorio");
    }
}
