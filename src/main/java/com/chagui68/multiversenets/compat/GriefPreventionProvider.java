package com.chagui68.multiversenets.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * [EN] GriefPrevention provider.
 * <p>
 * GriefPrevention keeps claims in a {@code DataStore} owned by its singleton, so the instance is
 * read reflectively and the store is pulled from it. The claim lookup has three known shapes:
 * <pre>
 *   Claim getClaimAt(Location)                                        // MinecraftPortCentral fork
 *   Claim getClaimAt(Location, boolean ignoreHeight, Claim cached)
 *   Claim getClaimAt(Location, boolean ignoreHeight, boolean ignoreSubclaims, Claim cached)
 * </pre>
 * The shapes are matched on their real parameter types rather than on argument count, because the
 * 4-argument form ends in a {@code Claim} and not in a {@code boolean}: counting arguments and
 * filling the tail positionally throws {@code IllegalArgumentException} on every single call.
 * <p>
 * {@code ignoreHeight} is always false so containment is exact. GriefPrevention itself passes true
 * in places, which answers "is this anywhere in the column of a claim"; that would mark a location
 * hundreds of blocks underground as claim land. Over-blocking is the safe direction, but a network
 * would then refuse to run in a cave, so accuracy is preferred here and the bridge still fails
 * closed if the lookup throws.
 * <p>
 * Two package layouts are known, {@code me.ryanhamshire} and the lowercase
 * {@code me.ryanhambre} used by the MinecraftPortCentral fork, so both are tried.
 *
 * [ES] Provider de GriefPrevention.
 * <p>
 * GriefPrevention guarda los reclamos en un {@code DataStore} que tiene su singleton. Las tres
 * formas conocidas de {@code getClaimAt} se reconocen por sus tipos reales de parámetro y no por
 * el número de argumentos, porque la forma de 4 argumentos termina en {@code Claim} y no en
 * {@code boolean}: contarlos y rellenarlos posicionalmente lanza una excepción en cada llamada.
 * <p>
 * {@code ignoreHeight} siempre es false para que la contención sea exacta. GriefPrevention usa true
 * en algunos sitios, lo que responde "¿está en alguna parte de la columna del reclamo?"; eso
 * marcaría como tierra protegida una cueva cientos de bloques abajo.
 * <p>
 * Se intentan los dos paquetes conocidos: {@code me.ryanhamshire} y el {@code me.ryanhambre} en
 * minúsculas del fork de MinecraftPortCentral.
 */
public final class GriefPreventionProvider implements ProtectionBridge.Provider {

    private static final String PLUGIN = "GriefPrevention";

    private static final String[] PLUGIN_CLASSES = {
            "me.ryanhamshire.GriefPrevention.GriefPrevention",
            "me.ryanhambre.griefprevention.GriefPrevention",
    };

    private Object dataStore;
    private Method mGetClaimAt;
    private int argCount;

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
        for (String pluginClassName : PLUGIN_CLASSES) {
            try {
                Class<?> pluginClass = Class.forName(pluginClassName);
                Object instance = readSingleton(pluginClass);
                if (instance == null) {
                    continue;
                }
                Object store = readDataStore(pluginClass, instance);
                if (store == null) {
                    continue;
                }
                Method lookup = findGetClaimAt(store.getClass());
                if (lookup == null) {
                    continue;
                }
                dataStore = store;
                mGetClaimAt = lookup;
                argCount = lookup.getParameterCount();
                logger.info("Protection: GriefPrevention hooked via " + lookup.getParameterCount()
                        + "-arg getClaimAt on " + store.getClass().getName() + ".");
                return true;
            } catch (Throwable ignored) {
                // Try the next package layout.
            }
        }
        return false;
    }

    /**
     * GriefPrevention exposes the singleton as a public static field. Newer builds also offer
     * {@code instanceOrNull()}, which is preferred because it never throws during a bad reload.
     */
    private static Object readSingleton(Class<?> pluginClass) {
        try {
            Method factory = pluginClass.getMethod("instanceOrNull");
            Object instance = factory.invoke(null);
            if (instance != null) {
                return instance;
            }
        } catch (Throwable ignored) {
            // Falls through to the field lookup below.
        }
        try {
            return pluginClass.getField("instance").get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object readDataStore(Class<?> pluginClass, Object instance) {
        try {
            Object store = pluginClass.getMethod("getDataStore").invoke(instance);
            if (store != null) {
                return store;
            }
        } catch (Throwable ignored) {
            // Older builds keep it as a public field on the singleton.
        }
        try {
            return pluginClass.getField("dataStore").get(instance);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Exact signatures, most specific first. The trailing {@code Claim} is a cache hint and is
     * passed as null, which every documented overload accepts.
     */
    private static Method findGetClaimAt(Class<?> dataStoreClass) {
        Class<?> claimClass;
        try {
            String packageName = dataStoreClass.getPackage() == null
                    ? "" : dataStoreClass.getPackage().getName() + ".";
            claimClass = Class.forName(packageName + "Claim");
        } catch (Throwable ignored) {
            return null;
        }
        for (Method method : dataStoreClass.getMethods()) {
            if (!method.getName().equals("getClaimAt")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0] == Location.class) {
                return method;
            }
            if (params.length == 3 && params[0] == Location.class && params[1] == boolean.class
                    && params[2] == claimClass) {
                return method;
            }
            if (params.length == 4 && params[0] == Location.class && params[1] == boolean.class
                    && params[2] == boolean.class && params[3] == claimClass) {
                return method;
            }
        }
        return null;
    }

    @Override
    public boolean test(Location loc) throws Exception {
        Object[] args = new Object[argCount];
        args[0] = loc;
        if (argCount == 3) {
            args[1] = Boolean.FALSE;
        } else if (argCount == 4) {
            args[1] = Boolean.FALSE;
            args[2] = Boolean.FALSE;
        }
        // Trailing Claim cache hint stays null.
        return mGetClaimAt.invoke(dataStore, args) != null;
    }
}
