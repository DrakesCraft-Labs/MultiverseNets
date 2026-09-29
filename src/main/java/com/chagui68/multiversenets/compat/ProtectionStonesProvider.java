package com.chagui68.multiversenets.compat;

import com.chagui68.multiversenets.util.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
 *   <li>Every PS region is a personal claim. ProtectionStones has no "server region" concept to
 *       ask about, so {@code protection.allow-claims=true} cannot be honoured here. It is ignored
 *       and every claim stays protected, with a warning. {@code protection.exempt-locations} is
 *       the way to run a network inside your own claim.</li>
 *   <li>{@code fromLocationUnsafe} is preferred over {@code fromLocation} because the latter also
 *       returns null when a region's protect block type is missing from the config, which would
 *       silently un-protect land the player still owns. Failing closed is the right side to err
 *       on here.</li>
 * </ul>
 * <p>
 * Note that ProtectionStones regions are WorldGuard regions, so {@link WorldGuardProvider} already
 * covers this land as a side effect. This provider exists so the log tells the truth, so the claim
 * rule above is applied, and so the coverage does not depend on a second plugin's provider.
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
 *   <li>Toda región de PS es un reclamo personal. ProtectionStones no tiene concepto de "región del
 *       servidor", así que {@code protection.allow-claims=true} no se puede honrar aquí: se ignora
 *       y todos los reclamos quedan protegidos, con un aviso.</li>
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
     * The whole decision: ProtectionStones says "yes you are in a region" by returning an object
     * and "no" by returning null. Anything else (no method, a failure) is handled by the bridge,
     * which treats it as protected.
     */
    static boolean isInside(Method lookup, Location loc) throws Exception {
        return lookup != null && lookup.invoke(null, loc) != null;
    }
}
