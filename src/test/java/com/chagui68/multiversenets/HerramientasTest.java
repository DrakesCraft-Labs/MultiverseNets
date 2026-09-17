package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las tres herramientas traidas de NetworksV6 (configurator, rake, crayon) son de mano: nunca
 * deben ser colocables, o alguien las pondria en el suelo y las perderia.
 */
class HerramientasTest {

    @Test
    void lasHerramientasSonDeManoYNoColocables() {
        for (DeviceType tool : new DeviceType[]{DeviceType.CONFIGURATOR, DeviceType.RAKE, DeviceType.CRAYON}) {
            assertFalse(tool.placeable(), tool + " es herramienta de mano, no bloque");
            assertFalse(tool.isCell(), tool + " no almacena");
            assertFalse(tool.filterable(), tool + " no filtra");
        }
    }

    @Test
    void elReceptorEsFiltrableParaElPuenteInalambrico() {
        // El receptor decide QUE cruza desde la red del transmisor; sin filtro no cruzaria nada
        // (a proposito, como la plantilla obligatoria del transmisor en NetworksV6).
        assertTrue(DeviceType.RECEIVER.filterable());
    }

    @Test
    void losFiltrosSoportanModoBlacklist() {
        // filterBlacklist es campo nuevo del blob; por defecto whitelist (false).
        assertFalse(com.chagui68.multiversenets.persist.NodeBlob.create("X").filterBlacklist);
    }
}
