package dev.espi.protectionstones;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

/**
 * [EN] Test-only stand-in for the real {@code dev.espi.protectionstones.PSRegion}.
 * <p>
 * This exists so the provider's reflection is exercised against the exact class name and the exact
 * signatures the Drake fork actually ships, rather than against whatever the provider assumes.
 * The signatures below are copied from
 * {@code ProtectionStones-Drake/src/main/java/dev/espi/protectionstones/PSRegion.java} (lines 58-81)
 * and from the public ProtectionStones API docs:
 * <pre>
 *   public static PSRegion fromLocation(Location l)         // null when not in a region
 *   public static PSRegion fromLocationUnsafe(Location l)   // also reports unconfigured block types
 * </pre>
 * The class is abstract and holds no state, exactly like the original. {@link #INSIDE} decides the
 * answer so each test can describe a world layout; {@link #LAST_UNSAFE_CALLS} and
 * {@link #LAST_SAFE_CALLS} record which entry point the provider chose.
 * <p>
 * Deliberately absent: a manager class, an {@code Optional}-based lookup, {@code isRegion()} and
 * {@code getFlagValue()}. They are absent from the real plugin too, which is the whole point of
 * pinning them out. If a future ProtectionStones adds one, the provider should be reconsidered
 * rather than this stub quietly growing a matching method.
 *
 * [ES] Dublete de test para la clase real {@code dev.espi.protectionstones.PSRegion}.
 * <p>
 * Existe para que la reflexión del provider se ejercite contra el nombre de clase exacto y las
 * firmas exactas que publica el fork de Drake, en vez de contra lo que el provider suppose. Las
 * firmas están copiadas de
 * {@code ProtectionStones-Drake/src/main/java/dev/espi/protectionstones/PSRegion.java} (líneas
 * 58-81) y de la documentación pública de ProtectionStones.
 * <p>
 * Deliberadamente ausentes: una clase manager, una búsqueda con {@code Optional},
 * {@code isRegion()} y {@code getFlagValue()}. Tampoco existen en el plugin real, que es
 * justamente lo que este archivo fija. Si una versión futura de ProtectionStones las añadiera,
 * habría que reconsiderar el provider en lugar de ampliar este dublete en silencio.
 */
public abstract class PSRegion {

    /** Blocks this fake world considers claimed. Cleared and set by each test. */
    public static final Set<String> INSIDE = new HashSet<>();

    public static int lastUnsafeCalls;
    public static int lastSafeCalls;

    private static String key(Location loc) {
        return (loc == null || loc.getWorld() == null ? "?" : loc.getWorld().getName())
                + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public static void reset() {
        INSIDE.clear();
        lastUnsafeCalls = 0;
        lastSafeCalls = 0;
    }

    /**
     * The fail-closed entry point. The real class returns null here only when the location is
     * outside every region, never merely because the block type is missing from the config.
     */
    public static PSRegion fromLocationUnsafe(Location l) {
        lastUnsafeCalls++;
        return INSIDE.contains(key(l)) ? new PSMergedRegion() : null;
    }

    public static PSRegion fromLocation(Location l) {
        lastSafeCalls++;
        return INSIDE.contains(key(l)) ? new PSStandardRegion() : null;
    }

    /** Only present so the returned objects are distinguishable if a test ever needs to be. */
    static final class PSStandardRegion extends PSRegion {
    }

    static final class PSMergedRegion extends PSRegion {
    }
}
