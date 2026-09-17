package com.chagui68.multiversenets.util;

/**
 * [EN] Coordinate Bit-Packing Utilities
 * Fast bitwise packing and unpacking of integer coordinates (X, Y, Z) into a single 64-bit {@code long}.
 * *   - X: 26 bits (range: ±33,554,431)
 *   - Z: 26 bits (range: ±33,554,431)
 *   - Y: 12 bits (range: -2048 to +2047, covers entire vanilla world height)
 * *
 * [ES] Utilidades de Empaquetado de Coordenadas
 * Empaquetado y desempaquetado de coordenadas tridimensionales enteras (X, Y, Z) en un único {@code long} de 64 bits.
 */
public final class PosUtil {

    private PosUtil() {
    }

    /**
     * EN: Packs 3D block coordinates into a single 64-bit long.
 *
     * ES: Empaqueta coordenadas 3D de bloque en un único entero long de 64 bits.
     *
     * @param x Block X coordinate / ES: Coordenada X del bloque.
     * @param y Block Y coordinate / ES: Coordenada Y del bloque.
     * @param z Block Z coordinate / ES: Coordenada Z del bloque.
     * @return Packed 64-bit long / ES: Valor long empaquetado.
     */
    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
    }

    /**
     * EN: Extracts the block X coordinate from a packed long.
 *
     * ES: Extrae la coordenada X del bloque de un long empaquetado.
     */
    public static int unpackX(long pos) {
        return (int) (pos >> 38);
    }

    /**
     * EN: Extracts the block Y coordinate from a packed long.
 *
     * ES: Extrae la coordenada Y del bloque de un long empaquetado.
     */
    public static int unpackY(long pos) {
        return (int) (pos << 52 >> 52);
    }

    /**
     * EN: Extracts the block Z coordinate from a packed long.
 *
     * ES: Extrae la coordenada Z del bloque de un long empaquetado.
     */
    public static int unpackZ(long pos) {
        return (int) (pos << 26 >> 38);
    }
}
