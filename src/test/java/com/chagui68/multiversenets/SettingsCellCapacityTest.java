package com.chagui68.multiversenets;

import com.chagui68.multiversenets.util.Settings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Capacidad de las celdas cuando el config no colabora.
 *
 * Antes se calculaba el indice con Math.clamp(tier - 1, 0, caps.size() - 1). Con la lista vacia
 * ese maximo vale -1, y clamp con minimo mayor que maximo lanza IllegalArgumentException: el
 * return de respaldo escrito justo debajo no llegaba a ejecutarse jamas. Bastaba con borrar
 * cells.capacities del config para que reventara cada operacion sobre una celda.
 */
class SettingsCellCapacityTest {

    /** Settings guarda el config en un estatico privado que solo rellena el plugin. */
    private static void inyectar(YamlConfiguration config) throws Exception {
        Field campo = Settings.class.getDeclaredField("cfg");
        campo.setAccessible(true);
        campo.set(null, config);
    }

    @Test
    void usaLasCapacidadesDelConfigCuandoEstanTodas() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cells.capacities", List.of(100, 200, 300, 400, 500, 600));
        inyectar(config);

        assertEquals(100L, Settings.cellCapacity(1));
        assertEquals(600L, Settings.cellCapacity(6));
    }

    @Test
    void conLaListaVaciaCaeAlRespaldoEnVezDeReventar() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cells.capacities", List.of());
        inyectar(config);

        // Lo que importa es que NO lance. El valor es el respaldo geometrico: 65536 << (nivel-1).
        assertEquals(65536L, Settings.cellCapacity(1));
        assertEquals(65536L * 32, Settings.cellCapacity(6));
    }

    @Test
    void sinLaClaveSiquieraTampocoRevienta() throws Exception {
        inyectar(new YamlConfiguration());
        assertEquals(65536L, Settings.cellCapacity(1));
    }

    @Test
    void unNivelSinCapacidadDeclaradaUsaElUltimoYNoLanza() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cells.capacities", List.of(100, 200));
        inyectar(config);

        assertEquals(200L, Settings.cellCapacity(6), "deberia caer al ultimo nivel declarado");
    }

    @Test
    void unNivelAbsurdoNoRompeElCalculo() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("cells.capacities", List.of(100, 200, 300));
        inyectar(config);

        assertTrue(Settings.cellCapacity(0) > 0, "el nivel 0 no deberia dar capacidad negativa");
        assertTrue(Settings.cellCapacity(-5) > 0, "un nivel negativo no deberia dar capacidad negativa");
    }
}
