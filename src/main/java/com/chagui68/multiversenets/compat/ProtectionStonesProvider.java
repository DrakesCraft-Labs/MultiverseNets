package com.chagui68.multiversenets.compat;

import com.chagui68.multiversenets.util.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * [EN] ProtectionStones provider, built against the API that actually exists.
 * <p>
 * The public ProtectionStones API (2.10.x through current, and the Drake fork) exposes exactly one
 * containment question: {@code PSRegion.fromLocation(Location)}, a static factory returning the
 * innermost region or {@code null}. There is no manager class, no {@code Optional}-based lookup,
 * and no {@code isRegion()}. An earlier draft of this provider assumed a
 * {@code PSProtectionManager.getProtectionFromLocation()} shape that does not exist in any release,
 * which made the provider fail to register and left ProtectionStones land unprotected while looking
 * like it was covered; that is the failure this class is shaped to avoid.
 * <p>
 * Two consequences of the real API are worth stating plainly:
 * <ul>
 *   <li>Every PS region is a personal claim. For the <em>anonymous network loop</em> there is no
 *       player identity, so {@code protection.allow-claims=true} cannot be honoured there: a
 *       claimed cable stays untouchable either way, and a warning is logged.</li>
 *   <li>For <em>hand-held player access</em> the region itself knows its owners and members
 *       ({@code PSRegion.isOwner/isMember}), so {@link #allowsPlayer} lets the very players the
 *       claim exists for use their own devices inside it, while every stranger stays locked out by
 *       the anonymous rule.</li>
 *   <li>{@code fromLocationUnsafe} is preferred over {@code fromLocation} because the latter also
 *       returns null when a region's protect block type is missing from the config, which would
 *       silently un-protect land the player still owns. Failing closed is the right side to err
 *       on here.</li>
 * </ul>
 *
 * [ES] Provider de ProtectionStones, construido sobre la API que existe de verdad.
 * <p>
 * La API pública de ProtectionStones (2.10.x hasta la actual, y el fork de Drake) expone una sola
 * pregunta de contención: {@code PSRegion.fromLocation(Location)}, una factoría estática que
 * devuelve la región más interna o {@code null}. No hay clase manager, ni búsqueda con
 * {@code Optional}, ni {@code isRegion()}.
 * <p>
 * Dos consecuencias de la API real:
 * <ul>
 *   <li>En el <em>bucle anónimo de red</em> no hay identidad de jugador, así que
 *       {@code protection.allow-claims=true} no se puede aplicar ahí: los cables dentro de un
 *       reclamo quedan intocables igualmente, y se avisa en el log.</li>
 *   <li>Para el <em>acceso manual de jugadores</em> la región sí conoce a sus dueños y miembros
 *       ({@code PSRegion.isOwner/isMember}), así que {@link #allowsPlayer} deja a los legítimos
 *       usar sus propios dispositivos dentro de su reclamo, mientras cualquier extraño sigue
 *       bloqueado por la regla anónima.</li>
 *   <li>Se prefiere {@code fromLocationUnsafe} porque {@code fromLocation} además devuelve null
 *       cuando el tipo de bloque protector no está en la config, lo que desprotegería tierra que
 *       el jugador sigue poseyendo.</li>
 * </ul>
 */
public final class ProtectionStonesProvider implements ProtectionBridge.Provider {

    static final String PLUGIN = "ProtectionStones";
    static final String REGION_CLASS = "dev.espi.protectionstones.PSRegion";

    /**
     * Tried in order. {@code fromLocationUnsafe} also reports regions whose protect block type is
     * not configured, which is the safer answer for a protection check.
     */
    static final String[] LOOKUPS = {"fromLocationUnsafe", "fromLocation"};

    private Method mFromLocation;
    private Method mIsOwner;
    private Method mIsMember;

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
        Method resolved = resolveLookup();
        if (resolved == null) {
            return false;
        }
        mFromLocation = resolved;
        try {
            Class<?> regionClass = Class.forName(REGION_CLASS);
            // Real API (Drake fork, PSRegion.java lines 661-669): claims are personal, and the
            // region knows who its owner and members are. If these ever vanish, allowsPlayer just
            // returns null and the anonymous rule keeps the land closed, so nothing unlocks by
            // accident.
            mIsOwner = regionClass.getMethod("isOwner", java.util.UUID.class);
            mIsMember = regionClass.getMethod("isMember", java.util.UUID.class);
        } catch (Throwable ignored) {
            mIsOwner = null;
            mIsMember = null;
        }
        logger.info("Protection: ProtectionStones hooked via " + REGION_CLASS + "." + resolved.getName() + "().");
        if (Settings.protectionAllowClaims()) {
            logger.warning("Protection: protection.allow-claims=true is ignored for ProtectionStones. "
                    + "Its API cannot tell a personal claim from a server region, so every claim stays "
                    + "protected. Use protection.exempt-locations for land you own.");
        }
        return true;
    }

    /**
     * Pure reflection with no Bukkit dependency, so the resolution can be unit tested against a
     * stub of the real class. Returns null when the installed version exposes neither factory.
     */
    static Method resolveLookup() {
        try {
            return ProtectionBridge.findStatic(Class.forName(REGION_CLASS), new Class<?>[]{Location.class}, LOOKUPS);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean test(Location loc) throws Exception {
        return isInside(mFromLocation, loc);
    }

    /**
     * [EN] A ProtectionStones region is a WorldGuard region with a player attached, so a world with
     * no WorldGuard region manager holds no ProtectionStones region either. Asking anyway makes
     * {@code fromLocation} throw in the nether and the end, and a throw reads as "protected", which
     * stopped every network in those dimensions dead.
     * <p>
     * This is not a relaxation of the rule above: a world that WorldGuard does manage is still
     * queried, and a genuine failure inside a managed world is still treated as protected.
     *
     * [ES] Una región de ProtectionStones es una región de WorldGuard con un jugador asociado, así
     * que un mundo sin region manager de WorldGuard tampoco puede tener regiones de PS. Preguntar de
     * todos modos hace que {@code fromLocation} lance en el Nether y el End, y una excepción se lee
     * como "protegido", lo que dejó muertas todas las redes de esas dimensiones.
     */
    @Override
    public boolean supports(World world) {
        return WorldGuardRegions.manages(world);
    }

    /**
     * [EN] A ProtectionStones claim always belongs to actual players, so the region itself can say
     * whether the person asking is one of them. TRUE only when the owner or member list holds the
     * UUID; null for everyone else, which leaves the anonymous fail-closed check in charge.
     */
    @Override
    public Boolean allowsPlayer(org.bukkit.entity.Player player, Location loc) {
        try {
            return allowsPlayer(mFromLocation, mIsOwner, mIsMember, player.getUniqueId(), loc);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Static and parameter-injected so the rule can run against the test stub without a server.
     */
    static Boolean allowsPlayer(Method lookup, Method isOwner, Method isMember,
                                java.util.UUID who, Location loc) throws Exception {
        if (lookup == null || isOwner == null || isMember == null) {
            return null;
        }
        Object region = lookup.invoke(null, loc);
        if (region == null) {
            return null;
        }
        boolean their = Boolean.TRUE.equals(isOwner.invoke(region, who))
                || Boolean.TRUE.equals(isMember.invoke(region, who));
        return their ? Boolean.TRUE : null;
    }

    /**
     * The whole decision: ProtectionStones says "yes you are in a region" by returning an object
     * and "no" by returning null. Anything else (no method, a failure) is handled by the bridge,
     * which treats it as protected.
     */
    static boolean isInside(Method lookup, Location loc) throws Exception {
        return lookup != null && lookup.invoke(null, loc) != null;
    }
}
