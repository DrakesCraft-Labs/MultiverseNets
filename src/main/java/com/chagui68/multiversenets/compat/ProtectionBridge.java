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
         * [EN] Whether the backing plugin keeps any data model for this world at all. This is the
         * third answer, and it is the one that keeps networks alive outside the overworld.
         * <p>
         * Protection plugins are configured world by world. WorldGuard only has a
         * {@code RegionManager} for worlds an admin actually configured, GriefPrevention and Towny
         * likewise only track the worlds they were pointed at, so a query in the nether or the end
         * raises {@code NullPointerException} or returns a query object with nothing behind it.
         * That is not a failed protection check, it is the plugin saying "I have never heard of
         * this dimension".
         * <p>
         * The default is true, which keeps the old fail-closed behaviour for every provider that
         * cannot tell the difference. A provider must only return false when it is <em>certain</em>
         * it has no data for the world: a wrong false silently un-protects a player's land, which
         * is the one failure this whole bridge exists to prevent.
         *
         * [ES] Si el plugin de detrás mantiene algún modelo de datos para este mundo. Es la tercera
         * respuesta, y la que mantiene vivas las redes fuera del mundo normal.
         * <p>
         * Los plugins de protección se configuran mundo por mundo. WorldGuard solo tiene
         * {@code RegionManager} para los mundos que un admin configuró, y GriefPrevention y Towny
         * solo siguen los mundos que se les indicó, así que una consulta en el Nether o en el End
         * lanza excepción. Eso no es una comprobación fallida: es el plugin diciendo "no conozco
         * esta dimensión".
         * <p>
         * El valor por defecto es true, que conserva el comportamiento fail-closed para todo
         * provider que no distinga el caso. Solo debe devolverse false cuando se tiene
         * <em>certeza</em> de que no hay datos: un false erróneo desprotegería tierra de un jugador,
         * que es justo el fallo que este bridge existe para evitar.
         */
        default boolean supports(World world) {
            return true;
        }

        /**
         * @return true when the location sits inside land claimed or owned by somebody.
         * @throws Exception Reflective calls into another plugin's API can fail; the caller
         *         treats any failure as "protected" so a broken API never leaks items.
         */
        boolean test(Location loc) throws Exception;

        /**
         * [EN] The player-aware question, for hand-held access like the wireless terminal.
         * @return {@code Boolean.TRUE} when the plugin can certify that this player is the owner or
         * a member of the land at that location; {@code null} when it cannot be certain. A null
         * never unlocks anything: the anonymous {@link #test} still has the final word.
         * <p>
         * This third answer exists because {@code test} alone cannot tell "a network device sits in
         * someone's claim" from "the very owner of that claim is holding the device". Claims in
         * ProtectionStones are personal by design, so without this the owner herself is locked out
         * of her own machines.
         *
         * [ES] La pregunta consciente del jugador, para acceso manual como la terminal inalámbrica.
         * {@code Boolean.TRUE} si el plugin puede certificar que el jugador es dueño o miembro de la
         * tierra en esa ubicación; {@code null} si no puede tener certeza. Un null nunca desbloquea
         * nada: el {@link #test} anónimo sigue teniendo la última palabra.
         */
        default Boolean allowsPlayer(Player player, Location loc) {
            return null;
        }

        /**
         * [EN] The same question for an actor that is not online: the UUID baked into a network's
         * Controller. A network has no {@link Player} to ask about, so without this the whole item
         * loop is an anonymous stranger inside every claim, including the one it was built in.
         * <p>
         * {@code TRUE} only when the provider can certify that this UUID owns or is a member of
         * the land; {@code null} otherwise. A provider that cannot answer (no owner model, missing
         * method) leaves its claims closed to every network, which is the same fail-closed default
         * as {@link #allowsPlayer}.
         *
         * [ES] La misma pregunta para un actor que no está conectado: el UUID grabado en el
         * Controlador de una red. Una red no tiene {@link Player} al que preguntar, así que sin esto
         * todo el bucle de ítems es un extraño anónimo dentro de cada reclamo, incluido aquel en el
         * que se construyó.
         * <p>
         * {@code TRUE} solo si el provider puede certificar que ese UUID es dueño o miembro de la
         * tierra; {@code null} en caso contrario. Un provider que no sabe responder (sin modelo de
         * propietarios, método ausente) deja sus reclamos cerrados a toda red, que es el mismo
         * comportamiento fail-closed de {@link #allowsPlayer}.
         */
        default Boolean allowsActor(java.util.UUID who, Location loc) {
            return null;
        }
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
    private static final Map<String, Boolean> UNMANAGED = new ConcurrentHashMap<>();
    /**
     * Memoised answers to {@link #ownsAt}: "is this specific actor the owner here?". It lives
     * apart from {@link #CACHE} because that one is keyed by position only, and the same spot can
     * be somebody's claim for one network and a stranger's for the next.
     */
    private static final Map<String, Boolean> OWNED = new ConcurrentHashMap<>();
    private static final AtomicInteger CACHE_ENTRIES = new AtomicInteger();
    private static final AtomicInteger OWNER_ENTRIES = new AtomicInteger();
    private static final int CACHE_LIMIT = 60_000;
    private static final int OWNER_LIMIT = 20_000;

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
        UNMANAGED.clear();
        OWNED.clear();
        CACHE_ENTRIES.set(0);
        OWNER_ENTRIES.set(0);
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
        UNMANAGED.clear();
        if (!CACHE.isEmpty()) {
            CACHE.clear();
        }
        if (!OWNED.isEmpty()) {
            OWNED.clear();
        }
        CACHE_ENTRIES.set(0);
        OWNER_ENTRIES.set(0);
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
        return query(ACTIVE, world, x, y, z);
    }

    /**
     * [EN] Runs the provider list against one position. Takes the list as a parameter so the
     * unmanaged-world rule is unit testable without a server or a real protection plugin installed.
     * <p>
     * A provider that does not manage the world is skipped before it is ever called. That is what
     * keeps the nether and the end usable: their queries fail, a failure means "protected", and
     * without this filter every single block of those dimensions reads as somebody's claim.
     *
     * [ES] Ejecuta la lista de providers contra una posición. Recibe la lista por parámetro para que
     * la regla de mundo no gestionado se pueda testear sin servidor ni plugin de protección real.
     */
    static boolean query(List<Provider> providers, World world, int x, int y, int z) {
        Location loc = new Location(world, x, y, z);
        for (Provider provider : providers) {
            if (!supports(provider, world)) {
                continue;
            }
            if (evaluate(provider, loc, world.getName() + " " + x + "," + y + "," + z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * [EN] A provider gets one chance to say it has no data for a world, and the answer is cached
     * per provider and world so the reflective call is not repeated on every block of a chunk.
     * <p>
     * {@code supports} itself is called inside a try/catch for the same reason {@code test} is: a
     * provider that cannot even answer this question has no business being consulted at all, and
     * treating that as "not supported" would be the un-protecting direction. So a failure here falls
     * back to true and lets {@code test} decide, which is fail-closed as before.
     *
     * [ES] Cada provider tiene una oportunidad de decir que no tiene datos de un mundo, y la
     * respuesta se cachea por provider y mundo para no repetir la llamada reflectiva en cada bloque.
     */
    private static boolean supports(Provider provider, World world) {
        String key = provider.id() + ' ' + world.getUID();
        Boolean cached = UNMANAGED.get(key);
        if (cached != null) {
            return !cached;
        }
        boolean supported;
        try {
            supported = provider.supports(world);
        } catch (Throwable error) {
            java.util.logging.Logger logger = logger();
            if (logger != null && Settings.debug()) {
                logger.warning("Protection: " + provider.id() + " could not report whether it manages "
                        + world.getName() + "; assuming it does. " + error);
            }
            return true;
        }
        if (!supported) {
            UNMANAGED.putIfAbsent(key, Boolean.TRUE);
            java.util.logging.Logger logger = logger();
            if (logger != null) {
                logger.info("Protection: " + provider.id() + " does not manage '" + world.getName()
                        + "', so networks run there unchecked. Configure that world in the plugin, or list it "
                        + "under protection.exempt-worlds to silence this.");
            }
        }
        return supported;
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
        return mayPlayerAccess(ACTIVE, player, loc);
    }

    /**
     * [EN] The provider loop, extracted so the owner/stranger rule is unit testable without a
     * server running. First every provider gets to vouch for the player; if none does, the
     * anonymous fail-closed rule answers the question instead.
     *
     * [ES] El bucle de providers, extraído para que la regla dueño/extraño sea testeable sin
     * servidor. Primero cada provider puede certificar al jugador; si ninguno lo hace, contesta la
     * regla anónima fail-closed.
     */
    static boolean mayPlayerAccess(List<Provider> providers, Player player, Location loc) {
        for (Provider provider : providers) {
            Boolean allowed;
            try {
                allowed = provider.allowsPlayer(player, loc);
            } catch (Throwable ignored) {
                continue;
            }
            if (Boolean.TRUE.equals(allowed)) {
                return true;
            }
        }
        for (Provider provider : providers) {
            if (!supports(provider, loc.getWorld())) {
                continue;
            }
            if (evaluate(provider, loc, "player " + player.getUniqueId() + " at "
                    + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ())) {
                return false;
            }
        }
        return true;
    }

    /**
     * [EN] The question the network loop actually asks: may a network owned by {@code actor} touch
     * this block?
     * <p>
     * Three answers, in order:
     * <ol>
     *   <li>No protection plugin is wired up: yes, nothing is claimed.</li>
     *   <li>A provider certifies that {@code actor} owns or is a member of this spot: yes. That is
     *       what lets a player's own network run inside the player's own claim — the case that used
     *       to leave a controller sitting in an empty network with no visible reason.</li>
     *   <li>Otherwise the anonymous rule decides, exactly as before.</li>
     * </ol>
     * A null actor (a controller placed before this field existed) skips rule 2 and is therefore
     * treated as a stranger, never as an owner.
     *
     * [ES] La pregunta que realmente hace el bucle de red: ¿puede una red cuyo dueño es
     * {@code actor} tocar este bloque?
     * <p>
     * Tres respuestas, en orden:
     * <ol>
     *   <li>No hay ningún plugin de protección: sí, no hay nada reclamado.</li>
     *   <li>Un provider certifica que {@code actor} es dueño o miembro de este punto: sí. Eso es lo
     *       que permite que la red de un jugador funcione dentro de su propio reclamo, el caso que
     *       dejaba un controlador en una red vacía sin motivo visible.</li>
     *   <li>En otro caso decide la regla anónima, igual que antes.</li>
     * </ol>
     * Un actor nulo (controlador colocado antes de que existiera este campo) se salta la regla 2 y
     * por tanto es un extraño, nunca un dueño.
     */
    public static boolean mayActorUse(World world, int x, int y, int z, UUID actor) {
        if (!initialised || ACTIVE.isEmpty() || world == null) {
            return true;
        }
        return mayActorUse(ACTIVE, world, x, y, z, actor);
    }

    /**
     * [EN] The provider loop, extracted for the same reason as {@link #mayPlayerAccess(List, Player,
     * Location)}: the owner rule has to be exercisable without a server or a real protection plugin.
     *
     * [ES] El bucle de providers, extraído por la misma razón que
     * {@link #mayPlayerAccess(List, Player, Location)}: la regla de propietario tiene que poder
     * probarse sin servidor ni plugin de protección real.
     */
    static boolean mayActorUse(List<Provider> providers, World world, int x, int y, int z, UUID actor) {
        if (providers == null || providers.isEmpty() || world == null) {
            return true;
        }
        if (ownsAt(providers, world, x, y, z, actor)) {
            return true;
        }
        return !query(providers, world, x, y, z);
    }

    public static boolean mayActorUse(Block block, UUID actor) {
        return block == null
                || mayActorUse(block.getWorld(), block.getX(), block.getY(), block.getZ(), actor);
    }

    public static boolean mayActorUse(Location loc, UUID actor) {
        return loc == null
                || mayActorUse(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), actor);
    }

    /**
     * [EN] Whether this specific actor is certified as owner/member here, memoised apart from
     * {@link #CACHE} because the same spot can be somebody's land for one network and a stranger's
     * for the next.
     *
     * [ES] Si este actor concreto está certificado como dueño/miembro aquí, memorizado aparte de
     * {@link #CACHE} porque el mismo punto puede ser tierra de uno para una red y de un extraño para
     * la siguiente.
     */
    static boolean ownsAt(World world, int x, int y, int z, UUID actor) {
        return ownsAt(ACTIVE, world, x, y, z, actor);
    }

    static boolean ownsAt(List<Provider> providers, World world, int x, int y, int z, UUID actor) {
        if (actor == null || world == null) {
            return false;
        }
        String key = world.getUID() + "|" + actor + "|" + PosUtil.pack(x, y, z);
        Boolean hit = OWNED.get(key);
        if (hit != null) {
            return hit;
        }
        boolean result = queryOwner(providers, world, x, y, z, actor);
        OWNED.put(key, result);
        if (OWNER_ENTRIES.incrementAndGet() > OWNER_LIMIT) {
            OWNED.clear();
            OWNER_ENTRIES.set(0);
        }
        return result;
    }

    /**
     * [EN] Asks every provider whether this actor owns the spot. A provider that throws is not an
     * owner: ownership <em>unlocks</em> land, so a broken API must never grant it — the mirror image
     * of {@link #evaluate}, where a throw protects.
     *
     * [ES] Pregunta a cada provider si este actor posee el punto. Un provider que lanza no es dueño:
     * la propiedad <em>desbloquea</em> tierra, así que una API rota nunca debe concederla — la imagen
     * especular de {@link #evaluate}, donde lanzar protege.
     */
    private static boolean queryOwner(List<Provider> providers, World world, int x, int y, int z, UUID actor) {
        Location loc = new Location(world, x, y, z);
        for (Provider provider : providers) {
            if (!supports(provider, world)) {
                continue;
            }
            try {
                if (Boolean.TRUE.equals(provider.allowsActor(actor, loc))) {
                    return true;
                }
            } catch (Throwable error) {
                java.util.logging.Logger logger = logger();
                if (logger != null && Settings.debug()) {
                    logger.warning("Protection: " + provider.id() + " failed the owner check at "
                            + world.getName() + " " + x + "," + y + "," + z + ": " + error);
                }
            }
        }
        return false;
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
