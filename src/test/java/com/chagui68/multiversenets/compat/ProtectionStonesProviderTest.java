package com.chagui68.multiversenets.compat;

import dev.espi.protectionstones.PSRegion;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [ES] Tests del provider de ProtectionStones contra la API que el fork de Drake publica de verdad.
 * <p>
 * La versión anterior de este provider buscaba
 * {@code PSProtectionManager.getProtectionFromLocation(Location)} devolviendo un
 * {@code Optional}, un método {@code isRegion()} y un parámetro {@code allowClaims}. Ninguna de esas
 * tres cosas existe en ProtectionStones, ni en el fork de Drake ni en la API pública documentada. El
 * resultado era que {@code setup} devolvía false, el provider no se registraba, y la tierra de
 * ProtectionStones quedaba sin cubrir mientras el resto de providers sí funcionaban: un fallo
 * silencioso y grave.
 * <p>
 * Estos tests fijan la clase y las firmas correctas ({@code PSRegion.fromLocation(Location)} y su
 * variante {@code Unsafe}) para que no se pueda volver a inventar una API. El dublete de
 * {@code PSRegion} en {@code src/test/java/dev/espi/} replica la clase real, con las mismas firmas.
 */
class ProtectionStonesProviderTest {

    private static Location at(int x, int y, int z) {
        // El mundo es irrelevante para estos tests: la clase real solo lo usa para buscar la región.
        return new Location(null, x, y, z);
    }

    @BeforeEach
    void resetFakeWorld() {
        PSRegion.reset();
    }

    // ------------------------------------------------------------------ the API contract

    @Test
    @DisplayName("probes dev.espi.protectionstones.PSRegion, the class that really exists")
    void probesTheRealRegionClass() {
        assertEquals("dev.espi.protectionstones.PSRegion", ProtectionStonesProvider.REGION_CLASS,
                "the provider must target PSRegion; there is no PSProtectionManager or Protection class");
    }

    @Test
    @DisplayName("accepts fromLocation and fromLocationUnsafe, in that order of preference")
    void triesBothRealLookupNames() {
        assertEquals(2, ProtectionStonesProvider.LOOKUPS.length);
        assertEquals("fromLocationUnsafe", ProtectionStonesProvider.LOOKUPS[0]);
        assertEquals("fromLocation", ProtectionStonesProvider.LOOKUPS[1]);
    }

    @Test
    @DisplayName("resolves the lookup against the real class and picks the fail-closed one first")
    void resolvesTheRealLookupMethod() {
        Method lookup = ProtectionStonesProvider.resolveLookup();
        assertNotNull(lookup, "the fork ships PSRegion.fromLocation*, so resolution must succeed");
        assertEquals("fromLocationUnsafe", lookup.getName(),
                "fromLocationUnsafe is preferred: fromLocation also returns null when the protect "
                        + "block type is missing from the config, which would un-protect owned land");
        assertTrue(java.lang.reflect.Modifier.isStatic(lookup.getModifiers()));
        assertArrayEquals(new Class<?>[]{Location.class}, lookup.getParameterTypes());
    }

    @Test
    @DisplayName("uses the safe lookup when only that one exists, for older builds")
    void fallsBackToTheSafeLookup() {
        Method lookup = ProtectionBridge.findStatic(ResolverFixtures.LegacyOnly.class,
                new Class<?>[]{Location.class}, "fromLocationUnsafe", "fromLocation");
        assertNotNull(lookup);
        assertEquals("fromLocation", lookup.getName());
    }

    // ------------------------------------------------------------------ the decision

    @Test
    @DisplayName("land inside a claim is protected")
    void protectsLandInsideAClaim() throws Exception {
        PSRegion.INSIDE.add("?:10,64,20");
        assertTrue(ProtectionStonesProvider.isInside(ProtectionStonesProvider.resolveLookup(), at(10, 64, 20)));
    }

    @Test
    @DisplayName("free land is not protected")
    void leavesFreeLandAlone() throws Exception {
        assertFalse(ProtectionStonesProvider.isInside(ProtectionStonesProvider.resolveLookup(), at(10, 64, 20)));
    }

    @Test
    @DisplayName("calls the fail-closed entry point, never the one that hides unconfigured claims")
    void callsTheFailClosedEntryPoint() throws Exception {
        PSRegion.INSIDE.add("?:1,2,3");
        ProtectionStonesProvider.isInside(ProtectionStonesProvider.resolveLookup(), at(1, 2, 3));
        assertEquals(1, PSRegion.lastUnsafeCalls);
        assertEquals(0, PSRegion.lastSafeCalls,
                "falling through to fromLocation would let an unconfigured block type pass as free land");
    }

    @Test
    @DisplayName("a provider with no usable lookup reports unprotected, so it must not register")
    void noLookupIsNotProtected() throws Exception {
        assertFalse(ProtectionStonesProvider.isInside(null, at(0, 0, 0)));
        assertNull(ProtectionBridge.findStatic(ResolverFixtures.Empty.class,
                new Class<?>[]{Location.class}, "fromLocation"),
                "a class with no such method must resolve to null instead of throwing");
    }

    // ------------------------------------------------------------------ resolver guards

    @Test
    @DisplayName("rejects a lookup that lost its static modifier")
    void rejectsNonStaticLookups() {
        assertNull(ProtectionBridge.findStatic(ResolverFixtures.InstanceOnly.class,
                new Class<?>[]{Location.class}, "fromLocation"),
                "invoking a static target with an instance receiver throws on every call");
    }

    @Test
    @DisplayName("rejects a lookup whose parameters changed")
    void rejectsWrongParameters() {
        assertNull(ProtectionBridge.findStatic(ResolverFixtures.WrongParameters.class,
                new Class<?>[]{Location.class}, "fromLocation"));
    }

    @Test
    @DisplayName("separates static lookups from instance lookups")
    void keepsStaticAndInstanceLookupsApart() {
        Class<?>[] none = new Class<?>[0];
        assertNotNull(ProtectionBridge.findStatic(ResolverFixtures.StaticIsEmpty.class, none, "isEmpty"));
        assertNull(ProtectionBridge.findMethod(ResolverFixtures.StaticIsEmpty.class, none, "isEmpty"),
                "findMethod must not hand back a static target");
        assertNotNull(ProtectionBridge.findMethod(ResolverFixtures.InstanceIsEmpty.class, none, "isEmpty"));
        assertNull(ProtectionBridge.findStatic(ResolverFixtures.InstanceIsEmpty.class, none, "isEmpty"),
                "WorldGuard's ApplicableRegionSet.isEmpty() is an instance method, so findStatic "
                        + "must not claim it");
    }

    // ------------------------------------------------------------------ fail-closed rule

    @Test
    @DisplayName("a provider that throws protects the block instead of unprotecting it")
    void aFailingProviderProtectsTheBlock() {
        ProtectionBridge.Provider broken = new ProtectionBridge.Provider() {
            @Override
            public String id() {
                return "BROKEN";
            }

            @Override
            public boolean setup(java.util.logging.Logger logger) {
                return true;
            }

            @Override
            public boolean test(Location loc) {
                throw new IllegalStateException("plugin blew up");
            }
        };
        assertTrue(ProtectionBridge.evaluate(broken, at(0, 0, 0), "test"),
                "reading a crash as 'no claim here' is what lets a network run inside someone's land");
    }

    @Test
    @DisplayName("a provider that reports free land lets the block through")
    void aWorkingProviderCanStillSayNo() {
        ProtectionBridge.Provider permissive = new ProtectionBridge.Provider() {
            @Override
            public String id() {
                return "OPEN";
            }

            @Override
            public boolean setup(java.util.logging.Logger logger) {
                return true;
            }

            @Override
            public boolean test(Location loc) {
                return false;
            }
        };
        assertFalse(ProtectionBridge.evaluate(permissive, at(0, 0, 0), "test"));
    }

    private static void assertArrayEquals(Class<?>[] expected, Class<?>[] actual) {
        assertEquals(expected.length, actual.length, "parameter count");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "parameter " + i);
        }
    }
}
