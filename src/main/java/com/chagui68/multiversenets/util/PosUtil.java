package com.chagui68.multiversenets.util;

public final class PosUtil {

    private PosUtil() {
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
    }

    public static int unpackX(long pos) {
        return (int) (pos >> 38);
    }

    public static int unpackY(long pos) {
        return (int) (pos << 52 >> 52);
    }

    public static int unpackZ(long pos) {
        return (int) (pos << 26 >> 38);
    }
}
