package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests device type classification, properties (filterable, directional, placeable), and display names.
 * [ES] Pruebas de clasificación de tipos de dispositivos, propiedades (filtrable, direccional, colocable) y nombres visibles.
 */
class DeviceTypeTest {

    /**
     * [EN] Filterable devices include basic and advanced grabbers, pushers, vacuum, and greedy cells.
     * [ES] Los dispositivos filtrables incluyen grabbers simples y avanzados, pushers, vacuum y greedy cells.
     */
    @Test
    void filterableDevicesIncludeNewTypes() {
        assertTrue(DeviceType.MVN_GRABBER.filterable());
        assertTrue(DeviceType.MVN_GRABBER_HT.filterable());
        assertTrue(DeviceType.MVN_PUSHER_HT.filterable());
        assertTrue(DeviceType.MVN_VACUUM.filterable());
        assertTrue(DeviceType.MVN_GREEDY_CELL.filterable());
        assertFalse(DeviceType.MVN_CONTROLLER.filterable());
    }

    /**
     * [EN] Greedy cell is an item sink / filter node, not a regular storage cell.
     * [ES] Greedy cell es un sumidero / nodo de filtrado, no una celda de almacenamiento estándar.
     */
    @Test
    void greedyCellIsNotStorageCell() {
        assertTrue(DeviceType.MVN_CELL_T1.isCell());
        assertFalse(DeviceType.MVN_GREEDY_CELL.isCell());
    }

    /**
     * [EN] Blueprint items and wireless terminals are hand items, not placeable blocks.
     * [ES] Los blueprints y terminales inalámbricas son objetos de mano, no bloques colocables.
     */
    @Test
    void blueprintAndWirelessTerminalAreNotPlaceable() {
        assertFalse(DeviceType.MVN_BLUEPRINT.placeable());
        assertFalse(DeviceType.MVN_WIRELESS_TERMINAL.placeable());
    }

    /**
     * [EN] Directional extraction/insertion configuration applies only to advanced devices (HT).
     * [ES] La configuración direccional solo aplica a dispositivos avanzados (HT).
     */
    @Test
    void directionalAppliesOnlyToAdvancedDevices() {
        assertTrue(DeviceType.MVN_GRABBER_HT.isDirectional());
        assertTrue(DeviceType.MVN_PUSHER_HT.isDirectional());
        assertFalse(DeviceType.MVN_GRABBER.isDirectional());
        assertFalse(DeviceType.MVN_PUSHER.isDirectional());
        assertFalse(DeviceType.MVN_VACUUM.isDirectional());
    }

    /**
     * [EN] Display names for simple and advanced devices match conventions.
     * [ES] Los nombres visibles para dispositivos simples y avanzados coinciden con las convenciones.
     */
    @Test
    void simpleAndAdvancedDisplayNames() {
        assertEquals("Simple Grabber", DeviceType.MVN_GRABBER.display());
        assertEquals("Advanced Grabber", DeviceType.MVN_GRABBER_HT.display());
        assertEquals("Simple Pusher", DeviceType.MVN_PUSHER.display());
        assertEquals("Advanced Pusher", DeviceType.MVN_PUSHER_HT.display());
    }
}
