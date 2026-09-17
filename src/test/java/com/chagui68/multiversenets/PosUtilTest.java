package com.chagui68.multiversenets;

import com.chagui68.multiversenets.util.PosUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * [EN] Tests bit-packing and unpacking of block 3D coordinates into a 64-bit primitive long.
 * [ES] Pruebas de empaquetado y desempaquetado de coordenadas 3D en un primitive long de 64 bits.
 */
class PosUtilTest {

    /**
     * [EN] Positive coordinates roundtrip cleanly.
     * [ES] Las coordenadas positivas se empaquetan y desempaquetan con precisión.
     */
    @Test
    void positiveCoordinatesRoundtrip() {
        long pos = PosUtil.pack(123, 64, -456);
        assertEquals(123, PosUtil.unpackX(pos));
        assertEquals(64, PosUtil.unpackY(pos));
        assertEquals(-456, PosUtil.unpackZ(pos));
    }

    /**
     * [EN] Large and negative world-boundary coordinates roundtrip cleanly.
     * [ES] Las coordenadas grandes y negativas en los límites del mundo se empaquetan y desempaquetan con precisión.
     */
    @Test
    void negativeCoordinatesRoundtrip() {
        long pos = PosUtil.pack(-30000000, -64, 29999999);
        assertEquals(-30000000, PosUtil.unpackX(pos));
        assertEquals(-64, PosUtil.unpackY(pos));
        assertEquals(29999999, PosUtil.unpackZ(pos));
    }
}
