package com.chagui68.multiversenets;

import com.chagui68.multiversenets.util.Settings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Tests cell capacity calculations with empty, missing, or edge-case configuration values.
 * [ES] Pruebas del cálculo de capacidad de celdas ante valores de configuración vacíos, ausentes o casos límite.
 */
class SettingsCellCapacityTest {

    /**
     * [EN] Injects mock YamlConfiguration into static Settings field via reflection.
     * [ES] Inyecta una configuración YamlConfiguration simulada en el campo estático de Settings mediante reflexión.
     */
    private static void injectConfig(YamlConfiguration config) throws Exception {
        Field field = Settings.class.getDeclaredField("cfg");
        field.setAccessible(true);
        field.set(null, config);
    }

    /**
     * [EN] Uses configured capacity values when properly declared in configuration.
     * [ES] Utiliza las capacidades configuradas cuando están debidamente declaradas en el config.
     */
    @Test
    void usesConfiguredCapacitiesWhenPresent() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cells.capacities", List.of(100, 200, 300, 400, 500, 600));
        injectConfig(config);

        assertEquals(100L, Settings.cellCapacity(1));
        assertEquals(600L, Settings.cellCapacity(6));
    }

    /**
     * [EN] Falls back safely to geometric default when capacity list in configuration is empty.
     * [ES] Recurre de forma segura al valor por defecto geométrico cuando la lista de capacidades está vacía.
     */
    @Test
    void emptyListFallsBackToDefaultWithoutThrowing() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cells.capacities", List.of());
        injectConfig(config);

        assertEquals(65536L, Settings.cellCapacity(1));
        assertEquals(65536L * 32, Settings.cellCapacity(6));
    }

    /**
     * [EN] Falls back safely to default when the configuration key is entirely missing.
     * [ES] Recurre de forma segura al valor por defecto cuando la clave falta por completo.
     */
    @Test
    void missingKeyFallsBackGracefully() throws Exception {
        injectConfig(new YamlConfiguration());
        assertEquals(65536L, Settings.cellCapacity(1));
    }

    /**
     * [EN] Requests for tiers beyond the declared list length clamp to the highest available tier without throwing.
     * [ES] Niveles superiores a la lista declarada usan la última capacidad configurada sin lanzar error.
     */
    @Test
    void undeclaredTierUsesLastConfiguredTier() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cells.capacities", List.of(100, 200));
        injectConfig(config);

        assertEquals(200L, Settings.cellCapacity(6), "should fall back to last configured tier");
    }

    /**
     * [EN] Invalid or negative tier values do not break calculation or return negative capacities.
     * [ES] Niveles inválidos o negativos no rompen el cálculo ni devuelven capacidades negativas.
     */
    @Test
    void invalidTierCalculatesSafeCapacity() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cells.capacities", List.of(100, 200, 300));
        injectConfig(config);

        assertTrue(Settings.cellCapacity(0) > 0, "tier 0 should not yield negative capacity");
        assertTrue(Settings.cellCapacity(-5) > 0, "negative tier should not yield negative capacity");
    }

    /**
     * [EN] Barrel capacity returns 2 Billion by default or custom value when configured.
     * [ES] La capacidad de la barrica devuelve 2.000.000.000 por defecto o el valor configurado.
     */
    @Test
    void barrelCapacityHandlesDefaultAndCustom() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        injectConfig(config);
        assertEquals(2_000_000_000L, Settings.barrelCapacity());

        config.set("barrel.capacity", 500_000_000L);
        assertEquals(500_000_000L, Settings.barrelCapacity());
    }

    /**
     * [EN] Auto-Crafter max recipes clamps correctly to GUI bounds [1, 18].
     * [ES] El número máximo de recetas del Auto-Crafter se ajusta a los límites de la GUI [1, 18].
     */
    @Test
    void maxBlueprintsClampsCorrectly() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        injectConfig(config);
        assertEquals(18, Settings.maxBlueprints());

        config.set("crafter.max-recipes", 9);
        assertEquals(9, Settings.maxBlueprints());

        config.set("crafter.max-recipes", 50);
        assertEquals(18, Settings.maxBlueprints(), "values over 18 must clamp to 18 GUI slots");

        config.set("crafter.max-recipes", -5);
        assertEquals(1, Settings.maxBlueprints(), "non-positive values must clamp to 1");
    }

    /**
     * [EN] Long list capacities with values up to 2 Billion are preserved without 32-bit overflow.
     * [ES] Las capacidades de celdas con valores hasta 2.000.000.000 se preservan sin desbordamiento de 32 bits.
     */
    @Test
    void longCellCapacitiesHandledWithoutOverflow() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cells.capacities", List.of(65536L, 262144L, 1048576L, 16777216L, 268435456L, 2000000000L));
        injectConfig(config);

        assertEquals(65536L, Settings.cellCapacity(1));
        assertEquals(2000000000L, Settings.cellCapacity(6));
    }

    /**
     * [EN] Settings methods do not throw NullPointerException when config is uninitialized/null.
     * [ES] Los métodos de Settings no lanzan NullPointerException si el config es nulo.
     */
    @Test
    void nullConfigIsCompletelySafe() throws Exception {
        Field field = Settings.class.getDeclaredField("cfg");
        field.setAccessible(true);
        field.set(null, null);

        assertEquals(20, Settings.scanIntervalTicks());
        assertEquals(16384, Settings.maxNodes());
        assertEquals(5, Settings.transferIntervalTicks());
        assertEquals(10, Settings.vacuumIntervalTicks());
        assertEquals(20, Settings.craftIntervalTicks());
        assertEquals(64, Settings.itemsPerOp());
        assertEquals(8, Settings.htMultiplier());
        assertEquals(262144L, Settings.greedyCapacity());
        assertEquals(2_000_000_000L, Settings.barrelCapacity());
        assertEquals(18, Settings.maxBlueprints());
        assertEquals(4.0, Settings.vacuumRadius());
        assertEquals(65536L, Settings.cellCapacity(1));
        assertTrue(Settings.compatSlimefun());
        org.junit.jupiter.api.Assertions.assertFalse(Settings.debug());
        assertEquals(250, Settings.rakeUses());
    }
}
