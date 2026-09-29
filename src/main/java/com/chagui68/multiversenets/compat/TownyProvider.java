package com.chagui68.multiversenets.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * [EN] Towny provider.
 * <p>
 * Towny splits the map into towns and the wilderness, so "not wilderness" is the containment
 * test. {@code getTown(Location)} is preferred because it is unambiguous about unclaimed land;
 * {@code isWilderness(Location)} is kept as the fallback for older builds.
 *
 * [ES] Provider de Towny.
 * <p>
 * Towny divide el mapa en pueblos y el desierto, así que "no es desierto" es la prueba de
 * contención. Se prefiere {@code getTown(Location)} porque es inequívoco sobre tierra sin reclamar;
 * {@code isWilderness(Location)} queda como respaldo para versiones antiguas.
 */
public final class TownyProvider implements ProtectionBridge.Provider {

    private static final String PLUGIN = "Towny";

    private Method mGetTown;
    private Method mIsWilderness;

    @Override
    public String id() {
        return PLUGIN;
    }

    @Override
    public boolean setup(Logger logger) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN);
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            Class<?> townyApi = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            // getInstance() doubles as a load check: a partially initialised Towny returns a usable
            // class but no methods, and the findStatic calls below are what actually gate setup.
            if (townyApi.getMethod("getInstance").invoke(null) == null) {
                return false;
            }
            // getTown is preferred: isWilderness only knows about towns, so a claimed Towny plot
            // outside every town would read as public.
            mGetTown = ProtectionBridge.findStatic(townyApi, new Class<?>[]{Location.class}, "getTown");
            mIsWilderness = ProtectionBridge.findStatic(townyApi, new Class<?>[]{Location.class}, "isWilderness");
            return mGetTown != null || mIsWilderness != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean test(Location loc) throws Exception {
        // Both lookups are static on TownyAPI, so the target is null and not the API instance.
        if (mGetTown != null) {
            // A null town means unclaimed land, which is public by design.
            return mGetTown.invoke(null, loc) != null;
        }
        return !Boolean.TRUE.equals(mIsWilderness.invoke(null, loc));
    }
}
