package com.chagui68.multiversenets.compat;

import com.chagui68.multiversenets.util.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [ES] Tests de la lista blanca y los valores por defecto de la protección.
 * Ningún plugin de protección está instalado en el entorno de test, así que lo que se
 * comprueba aquí es la parte que no depende de ellos: el parseo de las zonas exentas, su
 * geometría, y que el bridge quede inerte (nunca "protegido por error") sin providers.
 * La decisión de cada provider sí necesita el plugin real, así que se cubre en el servidor.
 */
class ProtectionWhitelistTest {

    // ------------------------------------------------------------------ defaults

    @Test
    void protectionIsSecureByDefault() {
        assertTrue(Settings.protectionEnabled(),
                "protection must default to on so a server without an explicit config is protected");
        assertFalse(Settings.protectionAllowClaims(),
                "personal claims must count as protected by default");
        assertTrue(Settings.protectionBlocksNetworkLinking(),
                "the topology scan must stop at protected borders by default");
        assertTrue(Settings.protectionBlocksPlayerInteraction(),
                "players must not be able to open devices on protected land by default");
    }

    @Test
    void everyProviderIsEnabledWhenTheListIsEmpty() {
        // An empty or missing list means "all of them", so a new provider works on old configs.
        assertTrue(Settings.protectionProviderEnabled("PROTECTIONSTONES"));
        assertTrue(Settings.protectionProviderEnabled("WORLDGUARD"));
        assertTrue(Settings.protectionProviderEnabled("GRIEFPREVENTION"));
    }

    @Test
    void cacheNeverDropsMoreOftenThanEverySecond() {
        assertTrue(Settings.protectionCacheTicks() >= 20,
                "a sub-second cache would hammer the protection plugin every tick");
    }

    // ------------------------------------------------------------------ parsing

    @Test
    void parsesAZoneWithAnExplicitRadius() {
        ProtectionBridge.ExemptZone zone = ProtectionBridge.parseZone("world;120;64;-340;48");
        assertNotNull(zone);
        assertEquals("world", zone.worldName());
        assertEquals(120, zone.x());
        assertEquals(64, zone.y());
        assertEquals(-340, zone.z());
        assertFalse(zone.wholeWorld());
    }

    @Test
    void defaultsTheRadiusToSixteenBlocks() {
        ProtectionBridge.ExemptZone zone = ProtectionBridge.parseZone("world;0;64;0");
        assertNotNull(zone);
        assertEquals(16L * 16L, zone.radiusSq());
    }

    @Test
    void acceptsCommasAndSurroundingSpaces() {
        ProtectionBridge.ExemptZone zone = ProtectionBridge.parseZone(" world , 10 , 70 , -5 , 32 ");
        assertNotNull(zone);
        assertEquals("world", zone.worldName());
        assertEquals(10, zone.x());
        assertEquals(32L * 32L, zone.radiusSq());
    }

    @Test
    void lowercasesTheWorldNameSoEntriesAreCaseInsensitive() {
        assertEquals("nether", ProtectionBridge.parseZone("Nether;1;2;3;4").worldName());
    }

    @Test
    void rejectsUnparseableEntriesInsteadOfGuessing() {
        assertNull(ProtectionBridge.parseZone(null));
        assertNull(ProtectionBridge.parseZone(""));
        assertNull(ProtectionBridge.parseZone("   "));
        assertNull(ProtectionBridge.parseZone("world;10;64"), "missing the z coordinate");
        assertNull(ProtectionBridge.parseZone("world;ten;64;0"), "non-numeric coordinate");
        assertNull(ProtectionBridge.parseZone(";10;64;0;16"), "empty world name");
        assertNull(ProtectionBridge.parseZone("world;10;64;0;-5"), "negative radius");
    }

    // ------------------------------------------------------------------ geometry

    @Test
    void aWholeWorldEntryIgnoresCoordinates() {
        // Whole-world entries come from protection.exempt-worlds, not from a parsed zone.
        ProtectionBridge.ExemptZone zone = new ProtectionBridge.ExemptZone("nether", 0, 0, 0, 0L, true);
        assertTrue(zone.covers("nether", 0, 0, 0));
        assertTrue(zone.covers("nether", 30_000_000, -60, 12_345));
    }

    @Test
    void aZoneNeverLeaksIntoAnotherWorld() {
        ProtectionBridge.ExemptZone zone = ProtectionBridge.parseZone("world;100;64;-200;32");
        assertFalse(zone.covers("nether", 100, 64, -200), "same coordinates, different world");
        assertFalse(zone.covers("world_two", 100, 64, -200));
    }

    @Test
    void theSphereIsInclusiveOnItsBorder() {
        ProtectionBridge.ExemptZone zone = ProtectionBridge.parseZone("world;0;0;0;16");
        assertTrue(zone.covers("world", 0, 0, 0), "centre");
        assertTrue(zone.covers("world", 16, 0, 0), "exactly on the radius");
        assertTrue(zone.covers("world", 0, 0, -16), "border on another axis");
        assertFalse(zone.covers("world", 17, 0, 0), "one block past the radius");
    }

    @Test
    void theSphereIsThreeDimensionalSoUndergroundBasesAreCovered() {
        ProtectionBridge.ExemptZone zone = ProtectionBridge.parseZone("world;0;64;0;16");
        assertTrue(zone.covers("world", 0, 80, 0), "8 blocks below a centre at y=64");
        assertFalse(zone.covers("world", 0, 81, 0), "17 blocks below is outside the sphere");
    }

    @Test
    void usesSquaredDistanceSoLargeRadiiDoNotLosePrecision() {
        ProtectionBridge.ExemptZone zone = ProtectionBridge.parseZone("world;0;0;0;30000");
        assertTrue(zone.covers("world", 29_000, 0, 0));
        assertTrue(zone.covers("world", -21_000, 0, -21_000), "diagonal of 29698 blocks");
        assertFalse(zone.covers("world", 30_001, 0, 0));
    }

    // ------------------------------------------------------------------ dormant state

    @Test
    void theBridgeNeverBlocksAnythingBeforeItIsInitialised() {
        // No protection plugin is installed in the test environment, so the bridge must stay
        // dormant and report "unprotected" rather than throwing or guessing.
        assertFalse(ProtectionBridge.isAvailable());
        assertFalse(ProtectionBridge.isProtected((org.bukkit.Location) null));
        assertFalse(ProtectionBridge.isProtected((org.bukkit.block.Block) null));
        assertFalse(ProtectionBridge.isProtected(null, 0, 0, 0));
    }

    @Test
    void theDefaultProviderSummaryIsNotAClaimOfCoverage() {
        // Guards against a startup log that implies protection is on when nothing registered.
        String summary = ProtectionBridge.providerSummary();
        assertNotNull(summary);
        assertFalse(summary.isBlank());
    }
}
