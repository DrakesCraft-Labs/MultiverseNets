package com.chagui68.multiversenets;

import com.chagui68.multiversenets.compat.SlimefunBridge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * El puente con Slimefun, cuando Slimefun no esta.
 *
 * Es la mitad del contrato que sostiene el plugin: MultiverseNets se vende como standalone, y un
 * servidor sin Slimefun tiene que poder usarlo entero. Si alguna de estas llamadas lanzara en vez
 * de responder en vacio, cada pasada del ticker moriria en el primer vecino que no fuera un cofre.
 *
 * No se llama a init() a proposito: sin servidor arrancado no hay PluginManager, y ese es
 * exactamente el estado en el que el puente debe comportarse como si Slimefun no existiera.
 */
class SlimefunBridgeTest {

    @Test
    void sinSlimefunElPuenteSeDeclaraNoDisponible() {
        assertFalse(SlimefunBridge.disponible(),
                "sin init() y sin servidor, el puente no puede darse por bueno");
    }

    @Test
    void consultarUnBloqueNuloNoLanza() {
        assertFalse(SlimefunBridge.esMaquina(null));
        assertNull(SlimefunBridge.idDe(null));
    }

    @Test
    void extraerSinSlimefunDevuelveNadaEnVezDeLanzar() {
        assertNull(SlimefunBridge.extraer(null, item -> true, 64));
        assertNull(SlimefunBridge.extraer(null, null, 0));
    }

    @Test
    void insertarSinSlimefunDevuelveTodoComoNoCabido() {
        // Devolver 0 seria mentir: diria que el item se entrego cuando no hay donde. El llamante
        // usa ese numero para devolverlo al almacen, asi que un 0 aqui borraria items.
        org.bukkit.inventory.ItemStack nulo = null;
        assertEquals(0, SlimefunBridge.insertar(null, nulo));
    }
}
