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

    @Test
    void direccionalSoloAvanzados() {
        assertTrue(DeviceType.GRABBER_HT.isDirectional());
        assertTrue(DeviceType.PUSHER_HT.isDirectional());
        assertFalse(DeviceType.GRABBER.isDirectional());
        assertFalse(DeviceType.PUSHER.isDirectional());
        assertFalse(DeviceType.VACUUM.isDirectional());
    }

    @Test
    void nombresSimplesYAvanzados() {
        org.junit.jupiter.api.Assertions.assertEquals("Simple Grabber", DeviceType.GRABBER.display());
        org.junit.jupiter.api.Assertions.assertEquals("Advanced Grabber", DeviceType.GRABBER_HT.display());
        org.junit.jupiter.api.Assertions.assertEquals("Simple Pusher", DeviceType.PUSHER.display());
        org.junit.jupiter.api.Assertions.assertEquals("Advanced Pusher", DeviceType.PUSHER_HT.display());
    }
}
