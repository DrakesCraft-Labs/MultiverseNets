package com.chagui68.multiversenets.compat;

import org.bukkit.World;

import java.lang.reflect.Method;

/**
 * [EN] One shared, lazy handle on WorldGuard's region container.
 * <p>
 * Both {@link WorldGuardProvider} and {@link ProtectionStonesProvider} need the same answer to the
 * same question: does this world have a region manager at all? ProtectionStones regions *are*
 * WorldGuard regions, so a world with no region manager has nothing for either plugin to say, and
 * both would throw when asked. Resolving the container once keeps that check to a single reflective
 * lookup per world instead of one per provider.
 * <p>
 * The whole class degrades to "I cannot tell" if any part of it fails to resolve, and the callers
 * treat that as supported, which preserves the fail-closed default.
 *
 * [ES] Un único handle perezoso y compartido al contenedor de regiones de WorldGuard.
 * <p>
 * Tanto {@link WorldGuardProvider} como {@link ProtectionStonesProvider} necesitan la misma
 * respuesta a la misma pregunta: ¿este mundo tiene region manager? Las regiones de ProtectionStones
 * *son* regiones de WorldGuard, así que un mundo sin region manager no tiene nada que decir ninguno
 * de los dos, y ambos lanzarían al consultar. Resolver el contenedor una sola vez deja esa
 * comprobación en una búsqueda reflectiva por mundo en vez de una por provider.
 */
final class WorldGuardRegions {

    private static Object container;
    private static Method getRegionManager;
    private static boolean resolved;

    private WorldGuardRegions() {
    }

    /**
     * [EN] Resolves the container once. Returns false when WorldGuard is not usable at all, which
     * the callers read as "I cannot tell" rather than "no regions anywhere".
     */
    static synchronized boolean resolve() {
        if (resolved) {
            return container != null;
        }
        resolved = true;
        try {
            Class<?> worldGuard = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object instance = worldGuard.getMethod("getInstance").invoke(null);
            if (instance == null) {
                return false;
            }
            Object platform = worldGuard.getMethod("getPlatform").invoke(instance);
            if (platform == null) {
                return false;
            }
            Object found = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            if (found == null) {
                return false;
            }
            container = found;
            getRegionManager = ProtectionBridge.findMethod(
                    found.getClass(), new Class<?>[]{World.class}, "get");
            return true;
        } catch (Throwable ignored) {
            container = null;
            getRegionManager = null;
            return false;
        }
    }

    static Object container() {
        return resolve() ? container : null;
    }

    /**
     * [EN] True when WorldGuard tracks this world. False means "it has no data here", and true also
     * covers the case where the lookup itself was unavailable, so the caller stays fail-closed.
     */
    static boolean manages(World world) {
        if (world == null || !resolve() || getRegionManager == null) {
            return true;
        }
        try {
            return getRegionManager.invoke(container, world) != null;
        } catch (Throwable ignored) {
            return true;
        }
    }

    /**
     * Convenience for the tests: drops the memoised handle so a new WorldGuard layout is picked up
     * without a JVM restart.
     */
    static synchronized void reset() {
        container = null;
        getRegionManager = null;
        resolved = false;
    }
}
