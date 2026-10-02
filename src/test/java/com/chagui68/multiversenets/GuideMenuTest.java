package com.chagui68.multiversenets;

import com.chagui68.multiversenets.gui.GuideMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.GuideContent;
import com.chagui68.multiversenets.item.Items;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] The guide menu that replaced the written book: every device is documented in both languages
 * and its page shows the real registered recipe.
 *
 * [ES] El menú de guía que sustituyó al libro: cada dispositivo está documentado en los dos idiomas y
 * su página muestra la receta registrada de verdad.
 */
class GuideMenuTest {

    private ServerMock server;
    private MultiverseNets plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(MultiverseNets.class);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Inventory top() {
        return player.getOpenInventory().getTopInventory();
    }

    private GuideMenu menu() {
        return (GuideMenu) top().getHolder();
    }

    private void click(int slot) {
        server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        server.getScheduler().performOneTick();
    }

    private static String name(ItemStack item) {
        return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
    }

    @Test
    void everyDeviceIsDocumentedInBothLanguages() {
        for (DeviceType type : DeviceType.values()) {
            GuideContent.Entry entry = GuideContent.of(type);
            assertNotNull(entry, type + " has a guide entry");
            assertFalse(entry.what(false).isBlank() || entry.what(true).isBlank(), type + " what");
            assertFalse(entry.use(false).isBlank() || entry.use(true).isBlank(), type + " use");
            assertFalse(entry.nameEs().isBlank(), type + " Spanish name");
        }
        for (GuideContent.Category category : GuideContent.Category.values()) {
            assertFalse(GuideContent.in(category).isEmpty(), category + " has devices");
            assertTrue(GuideContent.in(category).size() <= GuideMenu.LIST_SLOTS.length, category + " fits one page");
        }
    }

    @Test
    void everyCraftableDeviceHasItsRecipeFound() {
        for (DeviceType type : DeviceType.values()) {
            assertNotNull(GuideMenu.recipeKey(type), type + " recipe is found from the registered recipes");
        }
    }

    @Test
    void guideCommandOpensTheMenuInTheRequestedLanguage() {
        assertTrue(server.dispatchCommand(player, "mvnets guide es"));
        assertTrue(top().getHolder() instanceof GuideMenu, "the guide is a menu, not a book");
        assertTrue(menu().isSpanish());
        for (ItemStack it : player.getInventory().getContents()) {
            assertTrue(it == null || it.getType() != Material.WRITTEN_BOOK, "no book is handed out");
        }

        assertTrue(server.dispatchCommand(player, "mvnets guide en"));
        assertFalse(menu().isSpanish());
    }

    @Test
    void navigatingToADeviceShowsItsRecipeAndTheLanguageToggleKeepsThePage() {
        new GuideMenu(plugin, player, false).openMenu();
        int coreIndex = GuideContent.Category.CORE.ordinal();
        click(GuideMenu.CATEGORY_SLOTS[coreIndex]);
        assertEquals(GuideContent.Category.CORE, menu().currentCategory());

        click(GuideMenu.LIST_SLOTS[0]);
        assertEquals(DeviceType.MVN_CONTROLLER, menu().currentDevice());
        // Controller: I I I / I N I / I I I
        assertEquals(Material.IRON_BLOCK, top().getItem(GuideMenu.GRID_SLOTS[0]).getType());
        assertEquals(Material.NETHER_STAR, top().getItem(GuideMenu.GRID_SLOTS[4]).getType());
        assertEquals(DeviceType.MVN_CONTROLLER, Items.typeOf(top().getItem(GuideMenu.RESULT_SLOT)));
        assertEquals("What it does", name(top().getItem(GuideMenu.WHAT_SLOT)));

        click(GuideMenu.LANGUAGE_SLOT);
        assertTrue(menu().isSpanish());
        assertEquals(DeviceType.MVN_CONTROLLER, menu().currentDevice(), "switching language keeps the page");
        assertEquals("Qué hace", name(top().getItem(GuideMenu.WHAT_SLOT)));
        assertEquals("Controlador de Red", name(top().getItem(GuideMenu.DEVICE_SLOT)));
    }

    @Test
    void deviceIngredientsAreShownAsTheDeviceAndOpenTheirOwnPage() {
        new GuideMenu(plugin, player, false).openDevice(DeviceType.MVN_CELL_T2);
        ItemStack centre = top().getItem(GuideMenu.GRID_SLOTS[4]);
        assertEquals(DeviceType.MVN_CELL_T1, Items.typeOf(centre), "the previous cell is shown as the device");

        click(GuideMenu.GRID_SLOTS[4]);
        assertEquals(DeviceType.MVN_CELL_T1, menu().currentDevice());

        click(GuideMenu.NEXT_SLOT);
        assertEquals(DeviceType.MVN_CELL_T2, menu().currentDevice());
    }

    @Test
    void clicksNeverHandItemsOut() {
        new GuideMenu(plugin, player, false).openDevice(DeviceType.MVN_CONTROLLER);
        InventoryClickEvent take = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, GuideMenu.RESULT_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(take);
        assertTrue(take.isCancelled(), "the recipe display cannot be taken");
    }
}
