package com.chagui68.multiversenets.compat;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests for the bug this feature exists to fix.
 * <p>
 * A player builds a MultiverseNets network inside their own ProtectionStones claim. The topology
 * scan refuses to route a cable through protected land — correctly, because a network has no player
 * identity — so the Controller ends up alone: the network reports {@code 1} node, every machine
 * answers "NO NETWORK. No controller reached", and nothing explains why. The land was the player's
 * own the whole time.
 * <p>
 * The fix is to give the network an identity: the Controller records the UUID of whoever placed it,
 * and the anonymous rule is replaced by "is this network's owner certified as owner or member of
 * the land?". These tests pin the three answers that rule can give, plus the fail-closed corners
 * that must never unlock anybody's land.
 *
 * [ES] Tests del bug que esta función existe para arreglar: un jugador construye su red dentro de su
 * propio reclamo de ProtectionStones, el escaneo corta en la frontera, y el controlador se queda solo
 * en una red de 1 nodo sin explicación. La solución es dar identidad a la red (el UUID de quien
 * colocó el Controlador) y cambiar la regla anónima por "¿está el dueño de esta red certificado como
 * dueño o miembro de la tierra?".
 */
class NetworkOwnershipTest {

    private static final int CLAIM_X = 100;
    private static final int CLAIM_Y = 64;
    private static final int CLAIM_Z = 200;

    @BeforeEach
    void resetBridgeCaches() {
        // The bridge memoises owner and world answers statically, and every stub here shares the id
        // "TEST", so without this a previous test's answer could satisfy the next one.
        ProtectionBridge.invalidate();
    }

    private static World world(String name) {
        UUID uid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
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
     * A protection plugin whose land is one block wide (the claim spot) and whose owner list holds
     * exactly one UUID. Anything else is a stranger, which is what the real providers do.
     */
    private static ProtectionBridge.Provider claimOf(UUID owner) {
        return new ProtectionBridge.Provider() {
            @Override
            public String id() {
                return "TEST";
            }

            @Override
            public boolean setup(java.util.logging.Logger logger) {
                return true;
            }

            @Override
            public boolean test(Location loc) {
                return isClaim(loc);
            }

            @Override
            public Boolean allowsActor(UUID who, Location loc) {
                return who != null && who.equals(owner) && isClaim(loc) ? Boolean.TRUE : null;
            }
        };
    }

    private static boolean isClaim(Location loc) {
        return loc != null
                && loc.getBlockX() == CLAIM_X
                && loc.getBlockY() == CLAIM_Y
                && loc.getBlockZ() == CLAIM_Z;
    }

    // ------------------------------------------------------------------ the rule itself

    @Test
    @DisplayName("the network's own owner may operate inside his claim")
    void ownerRunsInsideHisOwnClaim() {
        UUID owner = UUID.randomUUID();
        List<ProtectionBridge.Provider> providers = List.of(claimOf(owner));

        assertTrue(ProtectionBridge.mayActorUse(providers, world("world"), CLAIM_X, CLAIM_Y, CLAIM_Z, owner),
                "this is the bug: the player who built the network must be able to reach his own machines");
    }

    @Test
    @DisplayName("somebody else's network stays a stranger in that claim")
    void aDifferentNetworkIsBlockedInSomeoneElsesClaim() {
        List<ProtectionBridge.Provider> providers = List.of(claimOf(UUID.randomUUID()));

        assertFalse(ProtectionBridge.mayActorUse(providers, world("world"), CLAIM_X, CLAIM_Y, CLAIM_Z,
                        UUID.randomUUID()),
                "ownership must be per-network, or every base becomes public");
    }

    @Test
    @DisplayName("a controller with no recorded owner is a stranger, never an owner")
    void aNullOwnerIsAStranger() {
        List<ProtectionBridge.Provider> providers = List.of(claimOf(UUID.randomUUID()));

        assertFalse(ProtectionBridge.mayActorUse(providers, world("world"), CLAIM_X, CLAIM_Y, CLAIM_Z, null),
                "an unknown owner must fall back to the old fail-closed answer so nothing unlocks by accident");
    }

    @Test
    @DisplayName("owning one spot does not put the rest of the world off limits")
    void publicLandStaysAvailableToEveryNetwork() {
        List<ProtectionBridge.Provider> providers = List.of(claimOf(UUID.randomUUID()));

        assertTrue(ProtectionBridge.mayActorUse(providers, world("world"), 0, 64, 0, UUID.randomUUID()),
                "outside the claim the anonymous rule already said yes and must keep saying yes");
    }

    @Test
    @DisplayName("a provider that blows up never grants ownership")
    void aBrokenProviderCannotGrantLand() {
        ProtectionBridge.Provider broken = new ProtectionBridge.Provider() {
            @Override
            public String id() {
                return "TEST";
            }

            @Override
            public boolean setup(java.util.logging.Logger logger) {
                return true;
            }

            @Override
            public boolean test(Location loc) {
                return true;
            }

            @Override
            public Boolean allowsActor(UUID who, Location loc) {
                throw new IllegalStateException("API changed under us");
            }
        };

        assertFalse(ProtectionBridge.mayActorUse(List.of(broken), world("world"), CLAIM_X, CLAIM_Y, CLAIM_Z,
                        UUID.randomUUID()),
                "ownership unlocks land, so a throw must read as 'not the owner' even though test() protects");
    }

    @Test
    @DisplayName("with no protection plugin installed nothing is restricted")
    void noProviderMeansNothingIsClaimed() {
        assertTrue(ProtectionBridge.mayActorUse(List.of(), world("world"), CLAIM_X, CLAIM_Y, CLAIM_Z, null),
                "a dormant bridge must not start denying transfers");
    }

    @Test
    @DisplayName("the owner cache is per actor: one network's answer never answers for another")
    void ownerAnswersDoNotLeakBetweenActors() {
        UUID owner = UUID.randomUUID();
        World world = world("world");
        List<ProtectionBridge.Provider> providers = List.of(claimOf(owner));

        assertTrue(ProtectionBridge.ownsAt(providers, world, CLAIM_X, CLAIM_Y, CLAIM_Z, owner));
        assertFalse(ProtectionBridge.ownsAt(providers, world, CLAIM_X, CLAIM_Y, CLAIM_Z, UUID.randomUUID()),
                "a stranger asked at the same spot must not inherit the memoised 'yes'");
        assertTrue(ProtectionBridge.ownsAt(providers, world, CLAIM_X, CLAIM_Y, CLAIM_Z, owner),
                "and the owner's own answer must survive being interleaved with a stranger's");
    }

    // ------------------------------------------------------------------ provider wiring

    @Test
    @DisplayName("a ProtectionStones provider that never resolved its API grants nothing")
    void unwiredProviderGrantsNothing() {
        assertNull(new ProtectionStonesProvider().allowsActor(UUID.randomUUID(), new Location(null, 0, 64, 0)),
                "no resolved lookup must mean null, which the bridge reads as 'not the owner'");
    }

    // ------------------------------------------------------------------ the field itself

    @Test
    @DisplayName("the controller's owner field round-trips, and garbage reads as no owner")
    void ownerFieldParsesSafely() {
        com.chagui68.multiversenets.persist.NodeBlob blob =
                com.chagui68.multiversenets.persist.NodeBlob.create("MVN_CONTROLLER");
        assertNull(blob.owner(), "a fresh controller has no owner until one is placed");

        UUID id = UUID.randomUUID();
        blob.ownerUuid = id.toString();
        assertEquals(id, blob.owner(), "the owner must survive the PDC round-trip as a UUID");

        blob.ownerUuid = "not-a-uuid";
        assertNull(blob.owner(), "a corrupt owner must read as unknown, never as a valid claim on land");

        blob.ownerUuid = "   ";
        assertNull(blob.owner(), "blank is the same as absent");
    }
}
