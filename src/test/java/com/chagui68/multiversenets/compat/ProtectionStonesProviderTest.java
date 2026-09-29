package com.chagui68.multiversenets.compat;

import dev.espi.protectionstones.PSRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

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

    /**
     * [EN] A World that answers only its name and uid. Implementing the interface by hand would
     * mean two hundred methods nobody calls, so a dynamic proxy keeps the fixture down to the two
     * the bridge actually reads. Anything else fails loudly instead of returning a default.
     */
    private static World world(String name) {
        UUID uid = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (World) java.lang.reflect.Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUID" -> uid;
                    case "toString" -> "World[" + name + "]";
                    case "hashCode" -> name.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(
                            "WorldStub." + method.getName() + " is not stubbed");
                });
    }

    /**
     * [EN] A Player that answers only its UUID and has no permissions. Same proxy trick as
     * {@link #world(String)}: the mayPlayerAccess logic reads nothing else from the player, so a
     * full Bukkit player is dead weight here.
     */
    private static org.bukkit.entity.Player player(UUID id) {
        return (org.bukkit.entity.Player) java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.entity.Player.class.getClassLoader(),
                new Class<?>[]{org.bukkit.entity.Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "hasPermission" -> false;
                    case "getName" -> "Player[" + id + "]";
                    case "toString" -> "Player[" + id + "]";
                    case "hashCode" -> id.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(
                            "PlayerStub." + method.getName() + " is not stubbed");
                });
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

    @Test
    @DisplayName("an owner can use devices in her own claim, a stranger cannot")
    void ownerAccessInsideAClaim() throws Exception {
        // Bug report: the wireless terminal refused to open in protected zones because the bridge
        // only knew how to answer anonymously, and ProtectionStones claims are always personal.
        // The real PSRegion API (Drake fork, lines 661-669) exposes isOwner/isMember(UUID), which
        // is exactly the distinction the anonymous check cannot make.
        Method lookup = PSRegion.class.getMethod("fromLocationUnsafe", Location.class);
        Method isOwner = PSRegion.class.getMethod("isOwner", UUID.class);
        Method isMember = PSRegion.class.getMethod("isMember", UUID.class);

        PSRegion.INSIDE.add("?:7,64,7");
        Location claim = at(7, 64, 7);
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        PSRegion.OWNERS.add(owner);
        PSRegion.MEMBERS.add(member);

        assertEquals(Boolean.TRUE,
                ProtectionStonesProvider.allowsPlayer(lookup, isOwner, isMember, owner, claim),
                "the region's owner must be told apart from its land: she can use her terminal");
        assertEquals(Boolean.TRUE,
                ProtectionStonesProvider.allowsPlayer(lookup, isOwner, isMember, member, claim),
                "members are also the people the claim exists for");
        assertNull(ProtectionStonesProvider.allowsPlayer(lookup, isOwner, isMember, stranger, claim),
                "a stranger gets no opinion: the anonymous fail-closed rule still blocks him");
    }

    @Test
    @DisplayName("outside any claim the provider has nothing to say")
    void outsideClaimsThereIsNoOpinion() throws Exception {
        Method lookup = PSRegion.class.getMethod("fromLocationUnsafe", Location.class);
        Method isOwner = PSRegion.class.getMethod("isOwner", UUID.class);
        Method isMember = PSRegion.class.getMethod("isMember", UUID.class);

        assertNull(ProtectionStonesProvider.allowsPlayer(
                        lookup, isOwner, isMember, UUID.randomUUID(), at(30, 64, 30)),
                "unclaimed land is decided by the anonymous rule, not by player identity");
    }

    @Test
    @DisplayName("the bridge lets the owner in, keeps the stranger out of someone's claim")
    void bridgeSeparatesOwnerFromStranger() {
        UUID owner = UUID.randomUUID();
        World claimWorld = world("world");
        Location claimSpot = new Location(claimWorld, 7, 64, 7);
        ProtectionBridge.Provider ps = new ProtectionBridge.Provider() {
            @Override
            public String id() {
                return "ProtectionStones";
            }

            @Override
            public boolean setup(java.util.logging.Logger logger) {
                return true;
            }

            @Override
            public boolean test(Location loc) {
                return loc.equals(claimSpot);
            }

            @Override
            public Boolean allowsPlayer(org.bukkit.entity.Player player, Location loc) {
                return loc.equals(claimSpot) && player.getUniqueId().equals(owner)
                        ? Boolean.TRUE : null;
            }
        };
        List<ProtectionBridge.Provider> providers = List.of(ps);

        assertTrue(ProtectionBridge.mayPlayerAccess(providers, player(owner), claimSpot),
                "the terminal bug: the owner inside her own protected claim must get through");
        assertFalse(ProtectionBridge.mayPlayerAccess(providers, player(UUID.randomUUID()), claimSpot),
                "a stranger inside someone else's claim still cannot touch the devices");
        assertTrue(ProtectionBridge.mayPlayerAccess(providers, player(UUID.randomUUID()),
                        new Location(claimWorld, 99, 64, 99)),
                "unclaimed land answers the same way regardless of who is asking");
    }

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
    @DisplayName("a provider that does not manage a world is skipped, not treated as protecting it")
    void aWorldTheProviderDoesNotManageIsNotProtected() {
        // Reproduce the real nether/end bug: WorldGuard has no RegionManager for a world it was
        // never configured for, so getApplicableRegions throws there. The bridge used to read that
        // crash as "protected", which froze every network in that dimension.
        ProtectionBridge.Provider worldGuard = new ProtectionBridge.Provider() {
            @Override
            public String id() {
                return "WorldGuard";
            }

            @Override
            public boolean setup(java.util.logging.Logger logger) {
                return true;
            }

            @Override
            public boolean supports(org.bukkit.World candidate) {
                return "world".equals(candidate.getName());
            }

            @Override
            public boolean test(Location loc) {
                throw new IllegalStateException("no RegionManager for this world");
            }
        };
        List<ProtectionBridge.Provider> providers = List.of(worldGuard);

        assertFalse(ProtectionBridge.query(providers, world("world_nether"), 10, 64, 10),
                "a world the provider does not manage must not be reported as protected, or every "
                        + "network in the nether is frozen solid by a provider that has no data there");
        assertTrue(ProtectionBridge.query(providers, world("world"), 10, 64, 10),
                "fail-closed still applies to a world the provider does manage");
    }

    @Test
    @DisplayName("an unresolvable WorldGuard keeps the fail-closed default")
    void worldGuardRegionLookupFailsSafe() {
        WorldGuardRegions.reset();
        // No WorldGuard on the test classpath, so the container cannot resolve. "I cannot tell"
        // must read as "managed", not as "no regions anywhere".
        assertTrue(WorldGuardRegions.manages(world("world")),
                "if the region manager lookup is unavailable the provider must stay fail-closed");
        assertTrue(new ProtectionStonesProvider().supports(world("world_nether")),
                "same for ProtectionStones, which is backed by the same WorldGuard data");
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
