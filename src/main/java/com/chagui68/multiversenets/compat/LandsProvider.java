package com.chagui68.multiversenets.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * [EN] Lands provider.
 * <p>
 * Lands has moved its API between packages and renamed its accessor several times, so this
 * provider walks a list of candidate class names and, for the area manager, a list of candidate
 * lookup shapes. A single non-null result means the position belongs to some land and is treated
 * as protected.
 * <p>
 * If none of the shapes resolve the provider simply never registers; Lands setups then fall back
 * to the other providers instead of silently half-working.
 *
 * [ES] Provider de Lands.
 * <p>
 * Lands ha movido su API entre paquetes y renombrado el accessor varias veces, así que este
 * provider recorre una lista de nombres de clase candidatos y, para el gestor de áreas, una lista
 * de formas de consulta. Un único resultado no nulo significa que la posición pertenece a alguna
 * tierra y se trata como protegida.
 */
public final class LandsProvider implements ProtectionBridge.Provider {

    private static final String PLUGIN = "Lands";

    private static final String[] PLUGINS = {"Lands", "LandsAPI"};
    private static final String[] PLUGIN_CLASSES = {
            "me.angeschossen.lands.api.LandsPlugin",
            "me.angeschossen.lands.LandsPlugin",
            "me.angeschossen.lands.api.Lands",
    };
    private static final String[] AREA_MANAGERS = {
            "me.angeschossen.lands.api.lands.AreaManager",
            "me.angeschossen.lands.api.areas.AreaManager",
            "me.angeschossen.lands.api.LandAreaManager",
    };

    private Method mLookup;
    private boolean lookupByLocation;
    private boolean lookupReturnsCollection;
    private Object areaManager;

    @Override
    public String id() {
        return PLUGIN;
    }

    @Override
    public boolean setup(Logger logger) {
        if (!anyEnabled(PLUGINS)) {
            return false;
        }
        for (String pluginName : PLUGIN_CLASSES) {
            for (String managerName : AREA_MANAGERS) {
                try {
                    Class<?> pluginClass = Class.forName(pluginName);
                    Object instance = pluginClass.getMethod("getInstance").invoke(null);
                    if (instance == null) {
                        continue;
                    }
                    Object manager = pluginClass.getMethod("getAreaManager").invoke(instance);
                    if (manager == null) {
                        continue;
                    }
                    Method lookup = findLookup(manager.getClass());
                    if (lookup == null) {
                        continue;
                    }
                    areaManager = manager;
                    mLookup = lookup;
                    logger.info("Protection: Lands hooked via " + managerName.substring(managerName.lastIndexOf('.') + 1)
                            + "#" + lookup.getName() + ".");
                    return true;
                } catch (Throwable ignored) {
                    // Try the next package layout.
                }
            }
        }
        return false;
    }

    /**
     * Any of these returning a land for the queried position is enough. Collections are checked
     * for emptiness because several builds expose {@code getLands} instead of a single result.
     * <p>
     * {@code getMethods()} has no defined order, so a single pass would make the chosen method vary
     * between JVMs on a build that ships both shapes. Names are ranked instead: a single-land
     * getter beats a collection getter, and a {@code Location} lookup beats a coordinate one
     * because it needs no world-UUID guess.
     */
    private Method findLookup(Class<?> managerClass) {
        Method best = null;
        int bestRank = Integer.MAX_VALUE;
        for (Method method : managerClass.getMethods()) {
            int rank = rank(method);
            if (rank < bestRank) {
                bestRank = rank;
                best = method;
            }
        }
        if (best == null) {
            return null;
        }
        lookupByLocation = best.getParameterCount() == 1;
        lookupReturnsCollection = Collection.class.isAssignableFrom(best.getReturnType());
        return best;
    }

    /**
     * Lower is better; {@link Integer#MAX_VALUE} means "not a land lookup at all".
     */
    private static int rank(Method method) {
        String name = method.getName();
        int nameRank;
        if (name.equals("getLand") || name.equals("getArea")) {
            nameRank = 0;
        } else if (name.equals("getLands") || name.equals("getAreas")) {
            nameRank = 2;
        } else {
            return Integer.MAX_VALUE;
        }
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 1 && params[0] == Location.class) {
            return nameRank;
        }
        if (params.length == 3 && params[0] == UUID.class && params[1] == int.class
                && params[2] == int.class) {
            return nameRank + 1;
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean test(Location loc) throws Exception {
        Object result;
        if (lookupByLocation) {
            result = mLookup.invoke(areaManager, loc);
        } else {
            World world = loc.getWorld();
            result = mLookup.invoke(areaManager, world == null ? null : world.getUID(),
                    loc.getBlockX(), loc.getBlockZ());
        }
        if (result == null) {
            return false;
        }
        if (lookupReturnsCollection) {
            return !((Collection<?>) result).isEmpty();
        }
        return true;
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
