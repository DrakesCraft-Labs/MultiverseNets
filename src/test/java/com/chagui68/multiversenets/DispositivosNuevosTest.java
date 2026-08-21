package com.chagui68.multiversenets;

import com.chagui68.multiversenets.item.DeviceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los dos dispositivos traidos de Networks: purgador y sonda.
 *
 * Lo que se vigila aqui no es que existan, sino las dos propiedades de las que depende que no
 * hagan dano.
 */
class DispositivosNuevosTest {

    @Test
    void elPurgadorSeColocaYAceptaFiltro() {
        DeviceType purgador = DeviceType.parse("PURGER");
        assertNotNull(purgador, "el purgador tiene que resolverse por nombre");
        assertTrue(purgador.placeable(), "el purgador es un bloque, no un item de mano");

        // Sin filtro no borra nada, y esa decision vive en el ticker. Pero si no fuera
        // filtrable no habria manera de decirle QUE borrar, y entonces o no sirve o se lo come
        // todo. Las dos salidas son malas.
        assertTrue(purgador.filterable(), "sin filtro, un purgador o es inutil o es peligroso");
    }

    @Test
    void laSondaNoSeColoca() {
        DeviceType sonda = DeviceType.parse("PROBE");
        assertNotNull(sonda);
        // Se usa en la mano. Si fuera colocable, un jugador la pondria como bloque y la perderia
        // de vista, ademas de crear un nodo que no hace nada.
        assertFalse(sonda.placeable(), "la sonda es una herramienta de mano");
        assertFalse(sonda.filterable(), "la sonda no filtra nada");
        assertFalse(sonda.isCell());
    }

    @Test
    void ningunDispositivoNuevoSeConfundeConUnaCelda() {
        for (String nombre : new String[]{"PURGER", "PROBE"}) {
            DeviceType tipo = DeviceType.parse(nombre);
            assertFalse(tipo.isCell(), nombre + " no es una celda y no debe contar como almacenamiento");
            assertTrue(tipo.cellTier() < 1, nombre + " no puede declarar nivel de celda");
        }
    }

    @Test
    void todosLosTiposTienenMaterialYNombre() {
        for (DeviceType tipo : DeviceType.values()) {
            assertNotNull(tipo.material(), tipo + " sin material");
            assertNotNull(tipo.display(), tipo + " sin nombre visible");
            assertFalse(tipo.display().isBlank(), tipo + " con nombre vacio");
        }
    }
}
