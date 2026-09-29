package com.chagui68.multiversenets.compat;

import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * [EN] Land Protection Bridge
 * Single entry point used by the network loop to ask "may this network touch this block?".
 * <p>
 * Without it, a grabber sitting in a public area can read a chest inside someone else's
 * ProtectionStones region, so two players can rob each other through their own networks.
 * Every container the network reads from or writes to is checked here first, and the
 * topology scan refuses to route cables through a protected region, so networks on opposite
 * sides of a border never end up merged into one item bus.
 * <p>
 * Why via reflection: MultiverseNets stays standalone. A provider whose plugin is missing,
 * disabled or exposing a different API simply never registers, and the others keep working.
 * A provider that throws at runtime is caught and skipped rather than allowed to leak items.
 * <p>
 * Performance: the network loop asks this question thousands of times per second, and every
 * plugin API behind it is a reflective call plus, usually, a config lookup. Results are
 * therefore memoised per world and position, and the whole cache is dropped every
 * {@code protection.cache-ticks} so a player claiming or releasing land is honoured quickly.
 *
 * [ES] Puente de Protección de Tierras
 * Punto de entrada único que consulta el bucle de red con "¿puede esta red tocar este bloque?".
 * <p>
 * Sin él, un grabber en una zona pública puede leer un cofre dentro de la región de otro
 * jugador con ProtectionStones, de modo que dos jugadores se roban entre sí a través de sus
 * propias redes. Aquí se comprueba cada contenedor del que la red lee o en el que escribe, y
 * el escaneo de topología se niega a tender cable por una región protegida, para que dos redes
 * a lados opuestos de una frontera nunca acaben fusionadas en un mismo bus de ítems.
 */
public final class ProtectionBridge {

    /**
     * [EN] A single protection plugin integration. Implementations resolve their API once in
     * {@link #setup(Logger)} and stay dormant (unregistered) if that fails.
     *
     * [ES] Una integración con un plugin de protección. Las implementaciones resuelven su API
     * una vez en {@link #setup(Logger)} y quedan inertes si eso falla.
     */
    public interface Provider {

        /** Stable id used by the {@code protection.providers} config list. */
        String id();

        /**
         * @return true when the backing plugin is present and its API could be resolved.
         */
        boolean setup(Logger logger);

        /**
         * @return true when the location sits inside land claimed or owned by somebody.
         * @throws Exception Reflective calls into another plugin's API can fail; the caller
         *         treats any failure as "protected" so a broken API never leaks items.
         */
        boolean test(Location loc) throws Exception;
    }

    /**
     * A whitelist entry: either a whole world ({@code wholeWorld}) or a sphere around a point.
     * Package-private so the geometry and the parser can be unit tested without a server.
     */
    record ExemptZone(String worldName, int x, int y, int z, long radiusSq, boolean wholeWorld) {

        boolean covers(World world, int x, int y, int z) {
            return covers(world.getName().toLowerCase(Locale.ROOT), x, y, z);
        }

        boolean covers(String worldName, int x, int y, int z) {
            if (!this.worldName.equals(worldName)) {
                return false;
            }
            if (wholeWorld) {
                return true;
            }
            double dx = x - (double) this.x;
            double dy = y - (double) this.y;
            double dz = z - (double) this.z;
            return dx * dx + dy * dy + dz * dz <= radiusSq;
        }
    }

    private static final List<Provider> CANDIDATES = List.of(
            new ProtectionStonesProvider(),
            new WorldGuardProvider(),
            new LandsProvider(),
            new TownyProvider(),
            new GriefPreventionProvider());

    private static final List<Provider> ACTIVE = new ArrayList<>();
    private static final List<ExemptZone> EXEMPT = new ArrayList<>();
    private static final Map<UUID, Map<Long, Boolean>> CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger CACHE_ENTRIES = new AtomicInteger();
    private static final int CACHE_LIMIT = 60_000;

    private static boolean initialised;
    private static String summary = "disabled";

    private ProtectionBridge() {
    }

    /**
     * [EN] Resolves every available provider. Safe to call again after a reload: the previous
     * registration and the memoised answers are dropped first.
     *
     * [ES] Resuelve todos los providers disponibles. Se puede llamar otra vez tras un reload.
     */
    public static void init(Logger logger) {
        ACTIVE.clear();
        CACHE.clear();
        CACHE_ENTRIES.set(0);
        initialised = false;
        loadExemptions();
        if (!Settings.protectionEnabled()) {
            summary = "disabled by config";
            logger.info("Protection bridge disabled by config.");
            return;
        }
        List<String> missing = new ArrayList<>();
        for (Provider provider : CANDIDATES) {
            String id = provider.id();
            if (!Settings.protectionProviderEnabled(id)) {
                continue;
            }
            try {
                if (provider.setup(logger)) {
                    ACTIVE.add(provider);
                } else {
                    missing.add(id);
                }
            } catch (Throwable error) {
                logger.warning("Protection: " + id + " failed to initialise and was disabled: " + error);
                missing.add(id);
            }
        }
        initialised = true;
        summary = ACTIVE.isEmpty() ? "no provider available" : String.join(", ", ids());
        logger.info("Protection bridge active. Providers: " + summary
                + (EXEMPT.isEmpty() ? "" : ". Exempt zones: " + EXEMPT.size())
                + (missing.isEmpty() ? "" : ". Not installed or unrecognised: " + String.join(", ", missing)));
    }

    private static List<String> ids() {
        List<String> out = new ArrayList<>(ACTIVE.size());
        for (Provider provider : ACTIVE) {
            out.add(provider.id());
        }
        return out;
    }

    /**
     * [EN] Resolves the first of several candidate method names to a public static method with an
     * exact parameter list. Shared by the providers so they all fail the same way on a renamed API.
     * <p>
     * Requiring {@code static} is deliberate: several of these lookups are static factories, and
     * silently binding an instance method of the same name would throw on every call instead of
     * failing once at startup.
     *
     * [ES] Resuelve el primer nombre de método candidato que exista como método público estático
     * con esa lista exacta de parámetros. Lo comparten los providers para que todos fallen igual
     * ante una API renombrada.
     */
    static Method findStatic(Class<?> owner, Class<?>[] parameters, String... names) {
        return find(owner, parameters, true, names);
    }

    /**
     * [EN] Same, for a method that must be called on an instance. A static method of a matching
     * name is rejected on purpose: passing the receiver to a static method throws.
     *
     * [ES] Igual, para un método que debe invocarse sobre una instancia. Un método estático con el
     * mismo nombre se rechaza a propósito: pasar el receptor a un método estático lanza excepción.
     */
    static Method findMethod(Class<?> owner, Class<?>[] parameters, String... names) {
        return find(owner, parameters, false, names);
    }

    private static Method find(Class<?> owner, Class<?>[] parameters, boolean wantsStatic, String... names) {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, parameters);
                if (Modifier.isStatic(method.getModifiers()) == wantsStatic) {
                    return method;
                }
            } catch (Throwable ignored) {
                // Try the next candidate name.
            }
        }
        return null;
    }

    /**
     * [EN] Drops every memoised answer. Scheduled on a timer so claiming, unclaiming and
     * flag edits are picked up without a restart.
     *
     * [ES] Descarta todas las respuestas memorizadas. Se programa en un temporizador para que los
     * reclamos, cesiones y cambios de flags se apliquen sin reiniciar.
     */
    public static void invalidate() {
        if (!CACHE.isEmpty()) {
            CACHE.clear();
            CACHE_ENTRIES.set(0);
        }
    }

    /**
     * [EN] True when at least one protection plugin is actually wired up. False means the bridge
     * is dormant and every location reports as unprotected.
     *
     * [ES] True si hay al menos un plugin de protección conectado. False significa que el puente
     * está inerte y toda ubicación se reporta como no protegida.
     */
    public static boolean isAvailable() {
        return initialised && !ACTIVE.isEmpty();
    }

    public static String providerSummary() {
        return summary;
    }

    /**
     * [EN] The check the whole plugin funnels through. Preferred over the {@link Location}
     * overload in hot loops: it does not allocate when the answer is already memoised.
     *
     * [ES] La comprobación central del plugin. Prefierela en bucles calientes: no reserva memoria
     * si la respuesta ya está memorizada.
     */
    public static boolean isProtected(World world, int x, int y, int z) {
        if (!initialised || ACTIVE.isEmpty() || world == null) {
            return false;
        }
        for (ExemptZone zone : EXEMPT) {
            if (zone.covers(world, x, y, z)) {
                return false;
            }
        }
        Map<Long, Boolean> memo = CACHE.get(world.getUID());
        long key = PosUtil.pack(x, y, z);
        if (memo != null) {
            Boolean hit = memo.get(key);
            if (hit != null) {
                return hit;
            }
        }
        boolean result = query(world, x, y, z);
        if (memo == null) {
            memo = CACHE.computeIfAbsent(world.getUID(), ignored -> new ConcurrentHashMap<>());
        }
        memo.put(key, result);
        // Cada tick el bucle de red pregunta por miles de posiciones, asi que un limite global
        // se dispara de continuo y un clear() completo aqui dejaria la cache inservible: se
        // memorizaria una entrada y se borraria en la misma llamada. Se descarta el mapa entero
        // de este mundo, que es la unidad de invalidacion natural, y se reinicia el contador.
        if (CACHE_ENTRIES.incrementAndGet() > CACHE_LIMIT) {
            CACHE.remove(world.getUID());
            CACHE_ENTRIES.set(0);
        }
        return result;
    }

    public static boolean isProtected(Block block) {
        return block != null && isProtected(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public static boolean isProtected(Location loc) {
        return loc != null && isProtected(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private static boolean query(World world, int x, int y, int z) {
        Location loc = new Location(world, x, y, z);
        for (Provider provider : ACTIVE) {
            if (evaluate(provider, loc, world.getName() + " " + x + "," + y + "," + z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * [EN] The one rule that matters: a provider that says yes protects the block, and a provider
     * that blows up protects the block too. A protection plugin that throws must never be read as
     * "no claim here", because that is the failure that lets a network run inside someone's land.
     * <p>
     * The logger and the position label are passed in rather than reached for through
     * {@code Bukkit} so this is testable, and so a provider failure is always reported even if the
     * server is mid-shutdown.
     *
     * [ES] La única regla que importa: un provider que dice sí protege el bloque, y un provider
     * que revienta también. Un plugin de protección que lanza una excepción nunca debe leerse como
     * "aquí no hay reclamo", porque ese es el fallo que deja correr una red dentro de la tierra de
     * otro jugador.
     */
    static boolean evaluate(Provider provider, Location loc, String where) {
        try {
            return provider.test(loc);
        } catch (Throwable error) {
            java.util.logging.Logger logger = logger();
            if (logger != null && Settings.debug()) {
                logger.warning("Protection: " + provider.id() + " failed at " + where + ": " + error);
            }
            return true;
        }
    }

    /**
     * [EN] {@code Bukkit} is unavailable in unit tests and during early startup, so the server
     * logger is looked up defensively instead of assumed.
     *
     * [ES] En tests unitarios y al arrancar temprano no hay servidor de Bukkit, así que el logger
     * se busca de forma defensiva en vez de asumirlo.
     */
    private static java.util.logging.Logger logger() {
        try {
            return org.bukkit.Bukkit.getLogger();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * [EN] Whether a player may open a network device by hand. Admins and the configured bypass
     * permission always pass; everyone else is subject to the same check as the network loop.
     *
     * [ES] Si un jugador puede abrir un dispositivo de red a mano. Los administradores y el
     * permiso de bypass siempre pasan; el resto sigue la misma comprobación que el bucle de red.
     */
    public static boolean mayPlayerAccess(Player player, Location loc) {
        if (player == null) {
            return !isProtected(loc);
        }
        if (player.hasPermission("multiversenets.admin")) {
            return true;
        }
        String bypass = Settings.protectionBypassPermission();
        if (!bypass.isEmpty() && player.hasPermission(bypass)) {
            return true;
        }
        return !isProtected(loc);
    }

    private static void loadExemptions() {
        EXEMPT.clear();
        for (String world : Settings.protectionExemptWorlds()) {
            if (world != null && !world.isBlank()) {
                EXEMPT.add(new ExemptZone(world.toLowerCase(Locale.ROOT), 0, 0, 0, 0L, true));
            }
        }
        for (String entry : Settings.protectionExemptLocations()) {
            ExemptZone zone = parseZone(entry);
            if (zone != null) {
                EXEMPT.add(zone);
            }
        }
    }

    /**
     * Parses {@code world;x;y;z;radius}. Radius is optional and defaults to 16 blocks.
     * Returns null for anything unparseable so a typo cannot deny the whole world.
     */
    static ExemptZone parseZone(String entry) {
        if (entry == null || entry.isBlank()) {
            return null;
        }
        String[] parts = entry.split("[;,]");
        if (parts.length < 4) {
            return null;
        }
        try {
            String world = parts[0].trim().toLowerCase(Locale.ROOT);
            int x = Integer.parseInt(parts[1].trim());
            int y = Integer.parseInt(parts[2].trim());
            int z = Integer.parseInt(parts[3].trim());
            int radius = parts.length >= 5 ? Integer.parseInt(parts[4].trim()) : 16;
            if (world.isEmpty() || radius < 0) {
                return null;
            }
            return new ExemptZone(world, x, y, z, (long) radius * radius, false);
        } catch (NumberFormatException error) {
            return null;
        }
    }
}
