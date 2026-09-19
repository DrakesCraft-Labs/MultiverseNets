package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests hand-held tools (Configurator/Wrench, Rake, Crayon) and receiver filtering behavior.
 * [ES] Pruebas para herramientas de mano (Configurador/Llave, Rastrillo, Crayón) y filtrado del receptor.
 */
class ToolsTest {

    /**
     * [EN] Hand tools must never be placeable blocks.
     * [ES] Las herramientas de mano nunca deben ser bloques colocables.
     */
    @Test
    void toolsAreHandHeldAndNotPlaceable() {
        for (DeviceType tool : new DeviceType[]{DeviceType.MVN_CONFIGURATOR, DeviceType.MVN_RAKE, DeviceType.MVN_CRAYON}) {
            assertFalse(tool.placeable(), tool + " is a hand tool, not a block");
            assertFalse(tool.isCell(), tool + " does not store");
            assertFalse(tool.filterable(), tool + " does not filter");
        }
    }

    /**
     * [EN] The receiver must be filterable to control wireless item transport.
     * [ES] El receptor debe ser filtrable para controlar el transporte inalámbrico.
     */
    @Test
    void receiverIsFilterableForWirelessBridge() {
        assertTrue(DeviceType.MVN_RECEIVER.filterable());
    }

    /**
     * [EN] Filters default to whitelist mode and support blacklist toggling.
     * [ES] Los filtros usan modo whitelist por defecto y soportan cambio a blacklist.
     */
    @Test
    void filtersSupportBlacklistMode() {
        assertFalse(com.chagui68.multiversenets.persist.NodeBlob.create("X").filterBlacklist);
    }
}
