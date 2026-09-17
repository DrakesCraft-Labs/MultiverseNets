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
        assertTrue(DeviceType.GRABBER.filterable());
        assertTrue(DeviceType.GRABBER_HT.filterable());
        assertTrue(DeviceType.PUSHER_HT.filterable());
        assertTrue(DeviceType.VACUUM.filterable());
        assertTrue(DeviceType.GREEDY_CELL.filterable());
        assertFalse(DeviceType.CONTROLLER.filterable());
    }

    /**
     * [EN] Greedy cell is an item sink / filter node, not a regular storage cell.
     * [ES] Greedy cell es un sumidero / nodo de filtrado, no una celda de almacenamiento estándar.
     */
    @Test
    void greedyCellIsNotStorageCell() {
        assertTrue(DeviceType.CELL_T1.isCell());
        assertFalse(DeviceType.GREEDY_CELL.isCell());
    }

    /**
     * [EN] Blueprint items and wireless terminals are hand items, not placeable blocks.
     * [ES] Los blueprints y terminales inalámbricas son objetos de mano, no bloques colocables.
     */
    @Test
    void blueprintAndWirelessTerminalAreNotPlaceable() {
        assertFalse(DeviceType.BLUEPRINT.placeable());
        assertFalse(DeviceType.WIRELESS_TERMINAL.placeable());
    }

    /**
     * [EN] Directional extraction/insertion configuration applies only to advanced devices (HT).
     * [ES] La configuración direccional solo aplica a dispositivos avanzados (HT).
     */
    @Test
    void directionalAppliesOnlyToAdvancedDevices() {
        assertTrue(DeviceType.GRABBER_HT.isDirectional());
        assertTrue(DeviceType.PUSHER_HT.isDirectional());
        assertFalse(DeviceType.GRABBER.isDirectional());
        assertFalse(DeviceType.PUSHER.isDirectional());
        assertFalse(DeviceType.VACUUM.isDirectional());
    }

    /**
     * [EN] Display names for simple and advanced devices match conventions.
     * [ES] Los nombres visibles para dispositivos simples y avanzados coinciden con las convenciones.
     */
    @Test
    void simpleAndAdvancedDisplayNames() {
        assertEquals("Simple Grabber", DeviceType.GRABBER.display());
        assertEquals("Advanced Grabber", DeviceType.GRABBER_HT.display());
        assertEquals("Simple Pusher", DeviceType.PUSHER.display());
        assertEquals("Advanced Pusher", DeviceType.PUSHER_HT.display());
    }
}
