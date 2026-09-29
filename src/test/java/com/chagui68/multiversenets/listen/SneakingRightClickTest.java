package com.chagui68.multiversenets.listen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [ES] Regresión: agachado + click derecho no debe abrir la interfaz del dispositivo.
 * <p>
 * El acceso shift+right al bloque adyacente se añadió en {@code ed6142f} y se retiró en
 * {@code 1c58b5b}, pero al retirarlo se fue también el {@code return} que tenía justo debajo. El
 * efecto quedó peor que la funcionalidad que se quitaba: agachado + click derecho sobre un
 * dispositivo se saltaba la rama de shift y caía en {@code openDeviceMenu}, así que abría el GUI
 * exactamente igual que sin agacharse. Este test fija que el corte por agachado está antes de esa
 * llamada.
 * <p>
 * Es un guard sobre el texto fuente y no sobre el comportamiento porque el entorno de test no
 * levanta un servidor de Bukkit, así que un click derecho real no se puede simular. Se buscan
 * posiciones de líneas y no bloques literales para que reindentar el archivo no rompa el test.
 */
class SneakingRightClickTest {

    /** [ES] Abre el menu del dispositivo. Su posicion respecto al guard es lo que importa. */
    private static final Pattern OPEN_DEVICE_MENU =
            Pattern.compile("^\\s*if \\(openDeviceMenu\\(player, block, blob, type\\)\\)");

    /** [ES] Un corte por agachado que no hace nada mas que devolver. */
    private static final Pattern SNEAKING_CUT =
            Pattern.compile("^\\s*if \\(player\\.isSneaking\\(\\)\\) \\{\\s*$");

    private static String[] lines() throws IOException {
        Path path = Paths.get("src", "main", "java", "com", "chagui68", "multiversenets",
                "listen", "BlockListener.java");
        assertTrue(Files.exists(path), "BlockListener.java must be readable for this regression guard");
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).split("\r?\n", -1);
    }

    private static int lineOf(String[] lines, Pattern pattern, int fromLine) {
        for (int i = fromLine; i < lines.length; i++) {
            if (pattern.matcher(lines[i]).find()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * [ES] El corte por agachado debe existir y estar ANTES de la llamada que abre el menu, dentro
     * del mismo manejador de click derecho sobre un dispositivo.
     */
    @Test
    void sneakingIsCutBeforeTheDeviceMenuOpens() throws IOException {
        String[] lines = lines();
        int menu = lineOf(lines, OPEN_DEVICE_MENU, 0);
        assertTrue(menu > 0, "the device menu call must still exist for non-sneaking clicks");

        // Busca el corte mas cercano por encima de la llamada al menu: ese es el que le aplica.
        int cut = -1;
        for (int i = menu - 1; i >= 0; i--) {
            if (SNEAKING_CUT.matcher(lines[i]).find()) {
                cut = i;
                break;
            }
        }
        assertTrue(cut > 0, "there must be a sneaking early-return guarding the device menu");
        assertTrue(cut < menu,
                "the sneaking guard must come before openDeviceMenu, otherwise sneaking opens the GUI anyway");
    }

    /**
     * [ES] El codigo que abria la interfaz de un bloque adyacente no debe volver a este manejador.
     * El helper {@code openTargetBlockInterface} si sigue existiendo y lo usa {@code FilterMenu}
     * para inspeccion dentro de un GUI, que es otro feature y no el de este guard.
     */
    @Test
    void shiftRightAdjacentAccessIsNotCalledFromTheRightClickHandler() throws IOException {
        String src = String.join("\n", lines());
        assertFalse(src.contains("tryAccessBlockInterface"),
                "shift+right must not open the interface of an adjacent block");
        assertFalse(src.contains("tryAccessBlockInterface(") || src.contains("openTargetBlockInterface(player, candidate"),
                "no adjacent-interface helper may be invoked from the world right-click handler");
    }

    /**
     * [ES] Agachado sobre un bloque que no es un dispositivo no debe cancelar el evento: en vanilla
     * agachado + click derecho con un bloque en la mano lo coloca contra la cara.
     */
    @Test
    void sneakingOnANonDeviceStillAllowsVanillaPlacement() throws IOException {
        String[] lines = lines();
        int blobNull = lineOf(lines, Pattern.compile("^\\s*if \\(blob == null\\) \\{"), 0);
        assertTrue(blobNull > 0, "the non-device branch must still exist");
        int parse = lineOf(lines, Pattern.compile("DeviceType type = DeviceType\\.parse\\(blob\\.typeName\\);"), blobNull);
        assertTrue(parse > blobNull);

        StringBuilder branch = new StringBuilder();
        for (int i = blobNull; i < parse; i++) {
            branch.append(lines[i]).append('\n');
        }
        Matcher cancelled = Pattern.compile("setCancelled\\(true\\)").matcher(branch);
        assertFalse(cancelled.find(),
                "cancelling the non-device branch would break vanilla sneak-place against a face");
    }
}
