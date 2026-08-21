package com.chagui68.multiversenets;

import com.chagui68.multiversenets.util.PosUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosUtilTest {

    @Test
    void roundtripPositivo() {
        long pos = PosUtil.pack(123, 64, -456);
        assertEquals(123, PosUtil.unpackX(pos));
        assertEquals(64, PosUtil.unpackY(pos));
        assertEquals(-456, PosUtil.unpackZ(pos));
    }

    @Test
    void roundtripNegativo() {
        long pos = PosUtil.pack(-30000000, -64, 29999999);
        assertEquals(-30000000, PosUtil.unpackX(pos));
        assertEquals(-64, PosUtil.unpackY(pos));
        assertEquals(29999999, PosUtil.unpackZ(pos));
    }
}
