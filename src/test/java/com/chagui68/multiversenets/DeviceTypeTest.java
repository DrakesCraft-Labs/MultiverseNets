package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceTypeTest {

    @Test
    void losFiltrablesIncluyenLosNuevos() {
        assertTrue(DeviceType.GRABBER.filterable());
        assertTrue(DeviceType.GRABBER_HT.filterable());
        assertTrue(DeviceType.PUSHER_HT.filterable());
        assertTrue(DeviceType.VACUUM.filterable());
        assertTrue(DeviceType.GREEDY_CELL.filterable());
        assertFalse(DeviceType.CONTROLLER.filterable());
    }

    @Test
    void greedyNoEsCeldaDeAlmacenamiento() {
        assertTrue(DeviceType.CELL_T1.isCell());
        assertFalse(DeviceType.GREEDY_CELL.isCell());
    }

    @Test
    void blueprintNoEsColocable() {
        assertFalse(DeviceType.BLUEPRINT.placeable());
        assertFalse(DeviceType.WIRELESS_TERMINAL.placeable());
    }
}
