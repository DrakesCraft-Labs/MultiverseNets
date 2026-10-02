package com.chagui68.multiversenets;

import com.chagui68.multiversenets.compat.ChickenGenetics;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkTicker;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EN] Genetic Chicken Sorter: reads GeneticChickengineering pocket chickens from their data and
 * moves only the ones that meet every rule.
 *
 * [ES] Genetic Chicken Sorter: lee los pollos de bolsillo de GeneticChickengineering desde sus
 * datos y solo mueve los que cumplen todas las reglas.
 */
class ChickenSorterTest {

    private static final NamespacedKey DNA = new NamespacedKey("geneticchickengineering", "gce_pocket_chicken_dna");
    private static final NamespacedKey ADAPTER = new NamespacedKey("geneticchickengineering", "gce_pocket_chicken_adapter");
    private static final NamespacedKey SPECIES = new NamespacedKey("geneticchickengineering", "gce_expanded_species");

    private ServerMock server;
    private MultiverseNets plugin;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(MultiverseNets.class);
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A pocket chicken with the given six gene states (0 aa, 1 Aa, 3 AA) and DNA-known flag. */
    private static ItemStack chicken(int[] genes, boolean known, boolean baby, String species) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        var meta = item.getItemMeta();
        int[] state = new int[7];
        System.arraycopy(genes, 0, state, 0, 6);
        state[6] = known ? 1 : 0;
        meta.getPersistentDataContainer().set(DNA, PersistentDataType.INTEGER_ARRAY, state);
        meta.getPersistentDataContainer().set(ADAPTER, PersistentDataType.STRING, "{\"baby\":" + baby + ",\"_health\":4.0}");
        if (species != null) {
            meta.getPersistentDataContainer().set(SPECIES, PersistentDataType.STRING, species);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Block place(int x, int y, int z, DeviceType type) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(type.material());
        NodeStore.put(block, NodeBlob.create(type.name()));
        if (type == DeviceType.MVN_CONTROLLER) {
            plugin.networks().registerController(block);
        } else {
            plugin.networks().invalidateNear(block);
        }
        return block;
    }

    @Test
    void genesAreReadLikeTheAddonDoes() {
        // Six dominant genes: typing 63 = Feather, tier 0, full strength, pure.
        ChickenGenetics.Chicken feather = ChickenGenetics.read(chicken(new int[]{3, 3, 3, 3, 3, 3}, true, false, null));
        assertNotNull(feather);
        assertEquals("TYPE:63", feather.product());
        assertEquals("Feather", ChickenGenetics.productName(feather.product()));
        assertEquals(0, feather.tier());
        assertEquals(6, feather.strength());
        assertTrue(feather.pure());

        // First gene recessive, second mixed: typing 31 = Bone, tier 1, strength 6-1-1 = 4, not pure.
        ChickenGenetics.Chicken bone = ChickenGenetics.read(chicken(new int[]{0, 1, 3, 3, 3, 3}, false, true, null));
        assertEquals("TYPE:31", bone.product());
        assertEquals(1, bone.tier());
        assertEquals(4, bone.strength());
        assertFalse(bone.pure());
        assertFalse(bone.known());
        assertFalse(bone.adult());

        ChickenGenetics.Chicken uranium = ChickenGenetics.read(chicken(new int[]{0, 0, 0, 0, 0, 0}, true, false, "URANIUM"));
        assertEquals("SPECIES:URANIUM", uranium.product());
        assertEquals(8, uranium.tier());

        assertNull(ChickenGenetics.read(new ItemStack(Material.PLAYER_HEAD)), "a plain head is not a chicken");
    }

    @Test
    void rulesMustAllPass() {
        NodeBlob rules = NodeBlob.create(DeviceType.MVN_CHICKEN_SORTER.name());
        ItemStack strongAdult = chicken(new int[]{3, 3, 3, 3, 3, 0}, true, false, null);
        ItemStack weakBaby = chicken(new int[]{1, 1, 3, 3, 3, 0}, false, true, null);
        assertTrue(ChickenGenetics.matches(rules, strongAdult), "default rules accept every chicken");
        assertFalse(ChickenGenetics.matches(rules, new ItemStack(Material.EGG)), "never anything that is not a chicken");

        rules.chickenMinStrength = 5;
        assertTrue(ChickenGenetics.matches(rules, strongAdult));
        assertFalse(ChickenGenetics.matches(rules, weakBaby));

        rules.chickenMinStrength = 0;
        rules.chickenAge = "ADULT";
        assertFalse(ChickenGenetics.matches(rules, weakBaby));
        rules.chickenAge = null;
        rules.chickenKnown = "KNOWN";
        assertFalse(ChickenGenetics.matches(rules, weakBaby));
        rules.chickenKnown = null;
        rules.chickenPureOnly = true;
        assertFalse(ChickenGenetics.matches(rules, weakBaby));
        assertTrue(ChickenGenetics.matches(rules, strongAdult));
        rules.chickenPureOnly = false;

        rules.chickenProducts.add(ChickenGenetics.read(strongAdult).product());
        assertTrue(ChickenGenetics.matches(rules, strongAdult));
        assertFalse(ChickenGenetics.matches(rules, chicken(new int[]{3, 3, 3, 3, 3, 3}, true, false, null)),
                "another product is rejected");

        rules.chickenProducts.clear();
        rules.chickenMinTier = 2;
        assertFalse(ChickenGenetics.matches(rules, strongAdult), "tier 1 is below the minimum");
    }

    @Test
    void theSorterPushesOnlyMatchingChickensAndOnlyWhenRunning() {
        Block controller = place(0, 64, 0, DeviceType.MVN_CONTROLLER);
        place(1, 64, 0, DeviceType.MVN_DRAM_BAY);
        NodeBlob bay = NodeStore.get(world.getBlockAt(1, 64, 0));
        com.chagui68.multiversenets.net.MemoryModules.install(bay,
                com.chagui68.multiversenets.item.Items.create(DeviceType.MVN_CACHE_L1));
        NodeStore.put(world.getBlockAt(1, 64, 0), bay);
        Block sorter = place(0, 64, 1, DeviceType.MVN_CHICKEN_SORTER);
        Block chest = world.getBlockAt(0, 64, 2);
        chest.setType(Material.CHEST);
        Network net = plugin.networks().networkByController(controller.getLocation());
        net.scan();

        ItemStack pure = chicken(new int[]{3, 3, 3, 3, 3, 3}, true, false, null);
        ItemStack mixed = chicken(new int[]{1, 3, 3, 3, 3, 3}, true, false, null);
        assertEquals(0, net.storage().deposit(pure));
        assertEquals(0, net.storage().deposit(mixed));
        assertEquals(0, net.storage().deposit(new ItemStack(Material.DIRT, 5)));

        new NetworkTicker(plugin, plugin.networks()).tick();
        Inventory inv = ((Container) chest.getState()).getInventory();
        assertTrue(inv.isEmpty(), "a stopped sorter moves nothing");

        NodeBlob rules = NodeStore.get(sorter);
        rules.chickenActive = true;
        rules.chickenPureOnly = true;
        rules.targetFace = "SOUTH";
        NodeStore.put(sorter, rules);
        new NetworkTicker(plugin, plugin.networks()).tick();

        int chickens = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null) {
                assertTrue(ChickenGenetics.read(it).pure(), "only the pure chicken left");
                chickens += it.getAmount();
            }
        }
        assertEquals(1, chickens);
        assertEquals(5, net.storage().count(i -> i.getType() == Material.DIRT), "other items are never touched");
        assertEquals(1, net.storage().count(ChickenGenetics::isPocketChicken), "the mixed chicken stays");
    }
}
