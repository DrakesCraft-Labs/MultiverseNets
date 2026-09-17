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
}
