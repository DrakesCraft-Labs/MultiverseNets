package com.chagui68.multiversenets;

import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.util.Settings;
import com.chagui68.multiversenets.util.StackUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the grabber side of a transfer honours the full per-cycle quota instead of being
 * capped at a single stack, so {@code transfer.ht-multiplier} actually scales the grabbers.
 *
 * Verifica que el lado importador de una transferencia respete toda la cuota por ciclo en vez de
 * quedar topado a un único stack, de modo que {@code transfer.ht-multiplier} escale de verdad a
 * los importadores.
 */
class GrabberQuotaTest {

    private Inventory chest;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        chest = Bukkit.createInventory(null, 27);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack stack(Material material, int amount) {
        return new ItemStack(material, amount);
    }

    private static boolean empty(Inventory inv, int slot) {
        ItemStack it = inv.getItem(slot);
        return it == null || it.getType().isAir();
    }

    @Test
    void mergesEverySlotHoldingTheSameItemUpToQuota() {
        chest.setItem(0, stack(Material.IRON_INGOT, 64));
        chest.setItem(5, stack(Material.IRON_INGOT, 64));
        chest.setItem(9, stack(Material.IRON_INGOT, 64));

        ItemStack taken = NetworkManager.extractMatching(chest, item -> item.getType() == Material.IRON_INGOT, 128);

        assertNotNull(taken, "matching item must be extracted");
        assertEquals(128, taken.getAmount(), "must honour the full 128 unit quota, not stop at one 64 stack");
        assertTrue(empty(chest, 0), "fully drained slot must be cleared");
        assertTrue(empty(chest, 5), "fully drained slot must be cleared");
        assertEquals(64, chest.getItem(9).getAmount(), "third slot untouched once quota is reached");
    }

    @Test
    void honoursTheHtQuotaOfTheTicker() {
        int base = Settings.itemsPerOp();
        int ht = base * Settings.htMultiplier();

        for (int i = 0; i < 20; i++) {
            chest.setItem(i, stack(Material.GOLD_INGOT, 64));
        }

        ItemStack taken = NetworkManager.extractMatching(chest, item -> item.getType() == Material.GOLD_INGOT, ht);

        assertNotNull(taken, "HT grabber must extract something");
        assertEquals(ht, taken.getAmount(), "HT grabber must be able to move more than a single stack per cycle");
    }

    @Test
    void stopsAtTheQuotaOnAPartiallyConsumedSlot() {
        chest.setItem(0, stack(Material.DIAMOND, 64));
        chest.setItem(1, stack(Material.DIAMOND, 64));

        ItemStack taken = NetworkManager.extractMatching(chest, item -> item.getType() == Material.DIAMOND, 90);

        assertNotNull(taken);
        assertEquals(90, taken.getAmount());
        assertTrue(empty(chest, 0));
        assertEquals(38, chest.getItem(1).getAmount(), "second slot must keep only the 38 items above the quota");
    }

    @Test
    void drainsADifferentItemInTheSameCall() {
        chest.setItem(0, stack(Material.REDSTONE, 40));
        chest.setItem(1, stack(Material.REDSTONE, 25));

        ItemStack taken = NetworkManager.extractMatching(chest, item -> true, 100);

        assertNotNull(taken);
        assertEquals(65, taken.getAmount(), "two separate stacks of the same item must be merged");
        assertTrue(empty(chest, 0));
        assertTrue(empty(chest, 1));
    }

    @Test
    void leavesSlotsHoldingOtherItemsUntouched() {
        chest.setItem(0, stack(Material.IRON_INGOT, 64));
        chest.setItem(1, stack(Material.GOLD_INGOT, 64));
        chest.setItem(2, stack(Material.IRON_INGOT, 64));

        ItemStack taken = NetworkManager.extractMatching(chest, item -> item.getType() == Material.IRON_INGOT, 200);

        assertNotNull(taken);
        assertEquals(Material.IRON_INGOT, taken.getType());
        assertEquals(128, taken.getAmount(), "only iron ingots are drained");
        assertEquals(64, chest.getItem(1).getAmount(), "the gold slot belongs to a different item");
        assertTrue(empty(chest, 0));
        assertTrue(empty(chest, 2));
    }

    @Test
    void doesNotMergeItemsWithDifferentLore() {
        ItemStack plain = stack(Material.PAPER, 10);
        ItemStack named = stack(Material.PAPER, 10);
        var meta = named.getItemMeta();
        meta.displayName(Component.text("Special"));
        named.setItemMeta(meta);

        chest.setItem(0, plain);
        chest.setItem(1, named);

        ItemStack taken = NetworkManager.extractMatching(chest, item -> true, 100);

        assertNotNull(taken);
        assertEquals(10, taken.getAmount(), "only the first matching stack is drained when the second differs");
        assertEquals(10, chest.getItem(1).getAmount());
        assertTrue(StackUtils.itemsMatch(plain, taken), "the returned stack must be the one taken from the slot");
    }

    @Test
    void returnsNullWhenNothingMatches() {
        chest.setItem(0, stack(Material.IRON_INGOT, 64));

        assertNull(NetworkManager.extractMatching(chest, item -> item.getType() == Material.DIAMOND, 128),
                "a filter rejecting everything yields null");
    }

    @Test
    void returnsNullOnEmptyInventoryOrZeroQuota() {
        assertNull(NetworkManager.extractMatching(chest, item -> true, 128));
        assertNull(NetworkManager.extractMatching(null, item -> true, 128));

        chest.setItem(0, stack(Material.IRON_INGOT, 64));
        assertNull(NetworkManager.extractMatching(chest, item -> true, 0), "a zero quota must never touch the chest");
        assertEquals(64, chest.getItem(0).getAmount());
    }
}
