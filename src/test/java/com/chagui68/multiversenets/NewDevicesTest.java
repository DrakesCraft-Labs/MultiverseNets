package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests for utility and diagnostic devices (Purger, Probe).
 * [ES] Pruebas para dispositivos utilitarios y de diagnóstico (Purgador, Sonda).
 */
class NewDevicesTest {

    /**
     * [EN] Purger is a placeable block and must accept filters to prevent unconstrained item deletion.
     * [ES] El purgador es un bloque colocable y debe aceptar filtros para evitar borrado indiscriminado.
     */
    @Test
    void purgerIsPlaceableAndAcceptsFilters() {
        DeviceType purger = DeviceType.parse("PURGER");
        assertNotNull(purger, "purger must resolve by name");
        assertTrue(purger.placeable(), "purger is a placeable block");
        assertTrue(purger.filterable(), "purger must be filterable to specify items to delete");
    }

    /**
     * [EN] Probe is a hand tool and must not be placeable as a block.
     * [ES] La sonda es una herramienta de mano y no debe colocarse como bloque.
     */
    @Test
    void probeIsNotPlaceable() {
        DeviceType probe = DeviceType.parse("PROBE");
        assertNotNull(probe);
        assertFalse(probe.placeable(), "probe is a hand-held tool");
        assertFalse(probe.filterable(), "probe does not filter items");
        assertFalse(probe.isCell());
    }

    /**
     * [EN] Utility devices (Purger, Probe) must never be identified as storage cells.
     * [ES] Los dispositivos utilitarios (Purgador, Sonda) nunca deben contar como celdas de almacenamiento.
     */
    @Test
    void utilityDevicesDoNotCountAsCells() {
        for (String name : new String[]{"PURGER", "PROBE"}) {
            DeviceType type = DeviceType.parse(name);
            assertFalse(type.isCell(), name + " is not a cell and must not store items");
            assertTrue(type.cellTier() < 1, name + " cannot declare a cell tier");
        }
    }

    /**
     * [EN] All device types must have a non-null material and a non-blank display name.
     * [ES] Todos los tipos de dispositivos deben tener un material no nulo y un nombre visible no vacío.
     */
    @Test
    void allDeviceTypesHaveMaterialAndDisplayName() {
        for (DeviceType type : DeviceType.values()) {
            assertTrue(type.name().startsWith("MVN_"), type.name() + " must start with MVN_ prefix");
            assertNotNull(type.material(), type + " missing material");
            assertNotNull(type.display(), type + " missing display name");
            assertFalse(type.display().isBlank(), type + " has blank display name");
        }
    }

    /**
     * [EN] Tests that DeviceType.parse resolves both modern MVN_ prefixed and legacy names.
     * [ES] Verifica que DeviceType.parse resuelva tanto los nombres modernos con prefijo MVN_ como los heredados.
     */
    @Test
    void parseSupportsModernAndLegacyNames() {
        assertEquals(DeviceType.MVN_CONTROLLER, DeviceType.parse("CONTROLLER"));
        assertEquals(DeviceType.MVN_CONTROLLER, DeviceType.parse("MVN_CONTROLLER"));
        assertEquals(DeviceType.MVN_CONTROLLER, DeviceType.parse("mvn_controller"));
        assertEquals(DeviceType.MVN_GREEDY_CELL, DeviceType.parse("GREEDY_CELL"));
        assertEquals(DeviceType.MVN_GREEDY_CELL, DeviceType.parse("MVN_GREEDY_CELL"));
        assertEquals(DeviceType.MVN_WIRELESS_TERMINAL, DeviceType.parse("wireless"));
    }
}
