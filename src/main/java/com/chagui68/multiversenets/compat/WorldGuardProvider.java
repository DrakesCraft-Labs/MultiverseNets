package com.chagui68.multiversenets.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * [EN] WorldGuard provider.
 * <p>
 * WorldGuard has no single "is this land owned" answer; membership of any region is the closest
 * thing, and it is what {@code getApplicableRegions(...).isEmpty()} reports. Region flags are
 * deliberately not evaluated: testing them per location and per tick is far more expensive, and a
 * region that a server opens up is already coverable through {@code protection.exempt-locations}.
 * <p>
 * Note that MVN never asks "may this player build here" here, because the network loop has no
 * player identity. That is the point: a network is an anonymous actor, so membership alone is
 * treated as off limits.
 *
 * [ES] Provider de WorldGuard.
 * <p>
 * WorldGuard no tiene una respuesta única de "¿esta tierra es de alguien?"; la pertenencia a
 * cualquier región es lo más parecido, y eso es lo que informa
 * {@code getApplicableRegions(...).isEmpty()}. Los flags de región no se evalúan a propósito:
 * probarlos por ubicación y por tick es mucho más caro, y una región que el servidor abre puede
 * cubrirse igualmente con {@code protection.exempt-locations}.
 */
public final class WorldGuardProvider implements ProtectionBridge.Provider {

    private static final String PLUGIN = "WorldGuard";
    private static final String[] PLUGINS = {"WorldGuard", "WorldEdit"};

    private Method mAdapt;
    private Method mGetApplicableRegions;
    private Method mIsEmpty;
    private Object query;

    @Override
    public String id() {
        return PLUGIN;
    }

    @Override
    public boolean setup(Logger logger) {
        if (!anyEnabled(PLUGINS)) {
            return false;
        }
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
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            if (container == null) {
                return false;
            }
            Object created = container.getClass().getMethod("createQuery").invoke(container);
            if (created == null) {
                return false;
            }
            Class<?> adapter = Class.forName("com.sk89q.worldguard.bukkit.util.BukkitAdapter");
            Class<?> regionQuery = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            Class<?> associable = Class.forName("com.sk89q.worldguard.protection.RegionAssociable");
            Class<?> applicable = Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
            mAdapt = adapter.getMethod("adapt", Location.class);
            mGetApplicableRegions = regionQuery.getMethod("getApplicableRegions", associable);
            mIsEmpty = ProtectionBridge.findMethod(applicable, new Class<?>[0], "isEmpty");
            query = created;
            return mIsEmpty != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean test(Location loc) throws Exception {
        Object associable = mAdapt.invoke(null, loc);
        Object regions = mGetApplicableRegions.invoke(query, associable);
        if (regions == null) {
            return false;
        }
        return !Boolean.TRUE.equals(mIsEmpty.invoke(regions));
    }

    private static boolean anyEnabled(String[] names) {
        for (String name : names) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
            if (plugin != null && plugin.isEnabled()) {
                return true;
            }
        }
        return false;
    }
}
