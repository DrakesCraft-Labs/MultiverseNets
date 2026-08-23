package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.net.NetworkStorage;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Calco del modelo de la grilla de Networks (AbstractGrid/NetworkGrid), que es lo que el
 * servidor ya conoce:
 *
 *   - Columna derecha: hueco de entrada real (8), fondo (17), orden (26), filtro (35),
 *     pagina previa (44) y siguiente (53).
 *   - Los items guardados se pintan con "Amount: N" en el lore; SOLO esos stacks se pueden
 *     retirar (anti-dupe, mismo guard que isGridDisplayStack en Networks).
 *   - Retiro: izquierdo deja 1 en el cursor (suma al stack igual que ya tengas), derecho un
 *     stack completo y shift manda directo al inventario.
 *   - Ingreso: shift+izquierdo sobre un stack propio lo inserta (se quita del inventario antes,
 *     anti-dupe) o se estaciona en el hueco de entrada y la red lo absorbe sola.
 *   - Todo otro clic sobre el inventario propio queda vanilla.
 */
public class TerminalMenu extends MenuHolder {

    private static final int PAGE_SIZE = 48;
    private static final int INPUT_SLOT = 8;
    private static final int BACKGROUND_SLOT = 17;
    private static final int SORT_SLOT = 26;
    private static final int FILTER_SLOT = 35;
    private static final int PREV_SLOT = 44;
    private static final int NEXT_SLOT = 53;

    private static final int[] DISPLAY_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7,
            9, 10, 11, 12, 13, 14, 15, 16,
            18, 19, 20, 21, 22, 23, 24, 25,
            27, 28, 29, 30, 31, 32, 33, 34,
            36, 37, 38, 39, 40, 41, 42, 43,
            45, 46, 47, 48, 49, 50, 51, 52
    };

    /** Marca en el lore que identifica a los stacks pintados por la grilla. */
    private static final String MARCA_MONTO = "Amount: ";    private enum Orden {ALFABETICO, CANTIDAD}

    private final Network network;
    private int page = 0;
    private String query = "";
    private Orden orden = Orden.ALFABETICO;
    private BukkitTask absorber;

    public TerminalMenu(MultiverseNets plugin, Player player, Network network) {
        super(plugin, player);
        this.network = network;
    }

    public void openMenu() {
        open(54, Component.text("Network Terminal", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
        // La red absorbe el hueco de entrada por ticks, como el BlockTicker de la grilla.
        absorber = plugin.getServer().getScheduler().runTaskTimer(plugin, this::absorberEntrada, 10L, 10L);
    }

    @Override
    protected boolean cancelarClicsJugador() {
        return false;
    }

    @Override
    protected java.util.Set<Integer> vanillaSlots() {
        return java.util.Set.of(INPUT_SLOT);
    }

    @Override
    protected void refresh() {
        ItemStack entrada = inv != null ? inv.getItem(INPUT_SLOT) : null;
        super.refresh();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (entrada != null && !entrada.getType().isAir() && inv.getItem(INPUT_SLOT) == null) {
                inv.setItem(INPUT_SLOT, entrada);
            }
        });
    }

    @Override
    protected void draw() {
        ItemStack fondo = panel(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        inv.setItem(BACKGROUND_SLOT, fondo);
        inv.setItem(SORT_SLOT, panel(Material.BLUE_STAINED_GLASS_PANE,
                orden == Orden.ALFABETICO ? "Change Sort Order: A-Z" : "Change Sort Order: Amount"));
        inv.setItem(FILTER_SLOT, filtroIcono());
        inv.setItem(PREV_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "Previous Page"));
        inv.setItem(NEXT_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "Next Page"));

        List<NetworkStorage.View> lista = filtradas();
        int pages = Math.max(1, (lista.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) {
            page = pages - 1;
        }
        int start = page * PAGE_SIZE;
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            int indice = start + i;
            if (indice < lista.size()) {
                inv.setItem(DISPLAY_SLOTS[i], iconoGrilla(lista.get(indice)));
            } else {
                inv.setItem(DISPLAY_SLOTS[i], fondo);
            }
        }
    }

    private List<NetworkStorage.View> filtradas() {
        List<NetworkStorage.View> all = network.storage().view();
        Comparator<NetworkStorage.View> comparador = orden == Orden.CANTIDAD
                ? Comparator.comparingLong(NetworkStorage.View::amount).reversed()
                : Comparator.comparing(v -> nombreLegible(v.sample()));
        List<NetworkStorage.View> out = new ArrayList<>(all);
        out.sort(comparador);
        if (query.isBlank()) {
            return out;
        }
        String q = query.toLowerCase();
        out.removeIf(v -> !nombreLegible(v.sample()).toLowerCase().contains(q)
                && !v.sample().getType().name().toLowerCase().contains(q));
        return out;
    }

    private String nombreLegible(ItemStack item) {
        return item.getType().name();
    }

    /**
     * El icono visible de una entrada: sample de a 1 con el monto en el lore y una marca en el
     * PDC. La marca cumple el papel de isGridDisplayStack en Networks: SOLO los stacks pintados
     * por la grilla se dejan retirar, nunca un item que un jugador logre colar aca.
     */
    private ItemStack iconoGrilla(NetworkStorage.View view) {
        ItemStack icono = view.sample().clone();
        icono.setAmount(1);
        var meta = icono.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(MARCA_MONTO + Items.formatAmount(view.amount()), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(Keys.TERMINAL_DISPLAY, PersistentDataType.BYTE, (byte) 1);
        icono.setItemMeta(meta);
        return icono;
    }

    private ItemStack panel(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(nombre, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack filtroIcono() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(query.isBlank()
                        ? "Set Filter (Right Click to Clear)"
                        : "Filter: " + query + " (Right Click to Clear)",
                NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        switch (raw) {
            case PREV_SLOT -> {
                if (page > 0) {
                    page--;
                    refresh();
                }
                return;
            }
            case NEXT_SLOT -> {
                page++;
                refresh();
                return;
            }
            case SORT_SLOT -> {
                orden = orden == Orden.ALFABETICO ? Orden.CANTIDAD : Orden.ALFABETICO;
                page = 0;
                refresh();
                return;
            }
            case FILTER_SLOT -> {
                if (event.getClick() == ClickType.RIGHT) {
                    query = "";
                    page = 0;
                    refresh();
                } else {
                    ChatPrompts.ask(player, "Type your search term:", text -> {
                        query = text == null ? "" : text;
                        page = 0;
                        refresh();
                    });
                }
                return;
            }
            default -> {
            }
        }

        for (int slot : DISPLAY_SLOTS) {
            if (raw == slot) {
                retirarDeDisplay(event);
                return;
            }
        }

        if (raw >= event.getView().getTopInventory().getSize()) {
            insertarStackPropio(event);
        }
    }

    /**
     * Retiro desde un stack pintado por la grilla, mismas reglas que retrieveItem en Networks:
     * izquierdo 1 al cursor (sumando al stack igual que ya traigas), derecho un stack entero y
     * shift directo al inventario con devolucion del sobrante.
     */
    private void retirarDeDisplay(InventoryClickEvent event) {
        ItemStack icono = event.getCurrentItem();
        if (!esStackDeGrilla(icono)) {
            return;
        }
        // El matcher debe comparar contra el stack REAL de la celda: sin lore y sin la marca del
        // PDC, porque isSimilar los tiene en cuenta y si no, nunca encontraria nada.
        ItemStack limpio = stackLimpio(icono);
        Predicate<ItemStack> coincide = limpio::isSimilar;
        ClickType clic = event.getClick();
        boolean shift = clic == ClickType.SHIFT_LEFT || clic == ClickType.SHIFT_RIGHT;

        if (shift) {
            int want = Math.min(limpio.getMaxStackSize(), 64);
            ItemStack sacado = network.storage().withdraw(coincide, want);
            if (sacado != null) {
                int sobra = NetworkManager.insertInto(player.getInventory(), sacado);
                if (sobra > 0) {
                    sacado.setAmount(sobra);
                    network.storage().deposit(sacado);
                }
            }
            refresh();
            return;
        }

        var vista = event.getView();
        ItemStack cursor = vista.getCursor();
        boolean derecho = clic == ClickType.RIGHT;

        if (cursor == null || cursor.getType().isAir()) {
            int want = derecho ? Math.min(limpio.getMaxStackSize(), 64) : 1;
            ItemStack sacado = network.storage().withdraw(coincide, want);
            if (sacado != null) {
                vista.setCursor(sacado);
            }
        } else if (!derecho && limpio.isSimilar(cursor) && cursor.getAmount() < cursor.getMaxStackSize()) {
            // Mismo gesto que setCursor/canAddMore en Networks: sumar de a uno al stack en mano.
            ItemStack uno = network.storage().withdraw(coincide, 1);
            if (uno != null) {
                cursor.setAmount(Math.min(cursor.getMaxStackSize(), cursor.getAmount() + 1));
            }
        }
        refresh();
    }

    /**
     * Shift+izquierdo sobre un stack propio: se retira del inventario ANTES de entrar a la red
     * (anti-dupe #shift-click) y solo ese stack entra, no todo el inventario.
     */
    private void insertarStackPropio(InventoryClickEvent event) {
        ItemStack actual = event.getCurrentItem();
        if (actual == null || actual.getType().isAir()) {
            return;
        }
        int antes = actual.getAmount();
        player.getInventory().setItem(event.getSlot(), null);
        int sobra = network.storage().deposit(actual);
        if (sobra <= 0) {
            player.sendMessage(Text.msg("Deposited " + Items.formatAmount(antes) + " items.", NamedTextColor.GREEN));
        } else {
            if (sobra < antes) {
                player.sendMessage(Text.msg("Deposited " + Items.formatAmount(antes - sobra) + " items.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Text.msg("Nothing deposited: the network needs a Quantum Cell "
                        + "with free space for this item.", NamedTextColor.RED));
            }
            ItemStack devuelto = actual.clone();
            devuelto.setAmount(sobra);
            player.getInventory().setItem(event.getSlot(), devuelto);
        }
        refresh();
    }

    /** Vacia el hueco de entrada hacia la red; lo que no cupo se queda a la vista. */
    private void absorberEntrada() {
        if (inv == null || player.getOpenInventory().getTopInventory().getHolder() != this) {
            if (absorber != null) {
                absorber.cancel();
                absorber = null;
            }
            return;
        }
        ItemStack entrada = inv.getItem(INPUT_SLOT);
        if (entrada == null || entrada.getType().isAir()) {
            return;
        }
        int antes = entrada.getAmount();
        int sobra = network.storage().deposit(entrada);
        if (sobra <= 0) {
            inv.setItem(INPUT_SLOT, null);
        } else {
            entrada.setAmount(sobra);
        }
        if (sobra < antes) {
            refresh();
        }
    }

    private boolean esStackDeGrilla(ItemStack icono) {
        if (icono == null || icono.getType().isAir() || !icono.hasItemMeta()) {
            return false;
        }
        Byte marca = icono.getItemMeta().getPersistentDataContainer()
                .get(Keys.TERMINAL_DISPLAY, PersistentDataType.BYTE);
        return marca != null && marca == (byte) 1;
    }

    private ItemStack stackLimpio(ItemStack icono) {
        ItemStack limpio = icono.clone();
        var meta = limpio.getItemMeta();
        meta.lore((List<Component>) null);
        meta.getPersistentDataContainer().remove(Keys.TERMINAL_DISPLAY);
        limpio.setItemMeta(meta);
        return limpio;
    }
}
