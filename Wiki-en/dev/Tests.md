# 🧪 MultiverseNets plugin tests

This document explains how the project's tests work and what each one covers. Aimed at developers
who want to run them, understand them or extend them.

> Development area: [Structure](Structure.md) · [How the code works](Code.md) · **The tests**

---

## 1. Running the tests

The tests are **JUnit 5 (Jupiter)** and do not need a real Minecraft server:

```
mvn test
```

A full build (`mvn clean package`) runs them too. Requirements: JDK 21 and Maven.

## 2. Test infrastructure: MockBukkit

Most tests use **[MockBukkit](https://github.com/MockBukkit/MockBukkit)**, an in-memory Bukkit server:

```java
@BeforeEach void setUp() {
    server = MockBukkit.mock();                     // fake server
    plugin = MockBukkit.load(MultiverseNets.class); // runs onEnable
    world = server.addSimpleWorld("world");
    player = server.addPlayer();
}

@AfterEach void tearDown() {
    MockBukkit.unmock();
}
```

That allows placing blocks, firing events (`server.getPluginManager().callEvent(...)`), simulating
inventory clicks, explosions and pistons, running the ticker
(`new NetworkTicker(plugin, plugin.networks()).tick()`) and reading the state stored in chunk PDCs.

Things to know:
- A test that places a device by hand must also store its blob
  (`NodeStore.put(block, NodeBlob.create(type.name()))`); without it the block is not a node.
- MockBukkit does not implement `Display.setBillboard` or `HumanEntity.openWorkbench`. The ticker
  catches hologram failures, so ticker tests run normally; the one test that calls the hologram API
  directly (`FluidAndRequesterTest.testHologramRedesignNoFlowOrRouted`) is reported as **skipped**.
  Crafting tests open a `server.createInventory(player, InventoryType.WORKBENCH)` instead of
  `openWorkbench`.
- `src/test/java/dev/espi/protectionstones/PSRegion.java` is a stub of the ProtectionStones API that
  the provider tests load by reflection.

## 3. Overview: 35 classes, 262 tests

| Class (package `com.chagui68.multiversenets` unless stated) | Tests | Covers |
| --- | --- | --- |
| `BlockFlowsTest` | 24 | Break/place with embedded state, pistons, explosions, wireless binding, rake, wrench, sneaking guard, dimensions, bridge linking from the Transmitter item, cable status. |
| `BlueprintDupeTest` | 3 | Encoder Blueprints are never duplicated (two viewers, breaking with the menu open); installing a Blueprint consumes it and *Clear All* returns it. |
| `CellGuiTest` | 9 | Quantum Cell menu: template, quick deposit, withdrawal, capacity, no duplication when the ticker runs. |
| `ChickenSorterTest` | 3 | GeneticChickengineering genes read like the addon (product, tier, strength, purity, age, special species); every rule must pass; the sorter moves only matching chickens, only while running, and never other items. |
| `CrafterGuiTest` | 10 | Crafter menu: install/uninstall/clear; Slimefun crafters accept Slimefun and vanilla Blueprints, standard crafters refuse Slimefun ones. |
| `DramBayTest` | 8 | A module in a DRAM Bay stores items; an ejected module moves its stock to another network; breaking the bay drops the module with its stock; the Fluid DRAM holds several fluids and travels with them; a controller no longer takes modules; an old controller module waits in the Terminal as a temporary item and goes back with its items; the Terminal lore shows the total and its breakdown by storage (cells + Greedy add up, counted once); breaking a controller drops uncollected modules. |
| `DeviceTypeTest` | 7 | `DeviceType` classification: filterable devices, Greedy Cell is not a cell, hand items, directional devices, request and Slimefun crafters. |
| `FilterGuiTest` | 15 | Filter menu: add/remove templates, whitelist/blacklist, shift-click, faces, clear. |
| `FluidAndRequesterTest` | 13 | Fluid storage and fluid cell quick interaction, Liquid Pump, terminal fluid page, Request Terminal (orders, chat amount, recursive chains, ignores Auto-Crafters, Slimefun Request Crafter), Slimefun Auto-Crafter. |
| `GuideMenuTest` | 6 | Every device is documented in both languages and its recipe is found; `/mvnets guide en|es` opens the menu (no book); navigation to a device shows its real recipe; the language toggle keeps the page; device ingredients open their own page; nothing can be taken out. |
| `GrabberQuotaTest` | 8 | `extractMatching`: honours the full per-cycle quota (including HT), merges slots of one item, leaves other items alone. |
| `GreedyCellTest` | 8 | Greedy Cell multi-item storage, shared capacity, menu and terminal integration. |
| `GuiDupeGuardTest` | 3 | `GuiListener` cancels dangerous clicks (also in cell and barrel menus); shift-click deposits never duplicate. |
| `GuiFlowsTest` | 10 | Terminal, Encoder, Auto-Crafter (atomic crafting), Crafting Grid and Monitor flows. |
| `InfinityBarrelTest` | 4 | Barrel capacity, menu, network integration and break/place persistence. |
| `NetworksCoexistenceTest` | 5 | Plugin name, main class, commands and permissions never collide with NetworksV6; Slimefun is a soft dependency. |
| `NewDevicesTest` | 6 | Purger and Probe properties; every `DeviceType` has a material and a name. |
| `PluginResourcesTest` | 3 | `plugin.yml` and `config.yml` on the classpath; version sanity check. |
| `PosUtilTest` | 2 | Coordinate packing round-trips, including world borders and negative Y. |
| `QuantumWorkbenchTest` | 2 | Cell upgrade keeps the cargo; ingredients are returned on close. |
| `RecipeTest` | 7 | Every recipe registered once, cable and cell recipes craft, a cell with cargo matches its upgrade recipe and keeps the cargo. |
| `RecipeValidationTest` | 5 | Device ingredients are tracked per recipe; a plain material cannot stand in for a device; cell and module upgrades keep their cargo; a fluid cell that still holds fluid is refused; devices never feed vanilla recipes. |
| `ReportedIssuesTest` | 8 | Advanced Pusher never loses items (one stack per slot); a multi-item whitelist leaves room for every ingredient; blacklisted items stay in the network; nothing goes into an Infinity Barrel's block inventory; hoppers never touch a device; custom items match after their name is stored differently; the barrel takes back a custom item it handed out; the Slimefun Recipe Encoder keeps its Blueprints. |
| `SettingsCellCapacityTest` | 9 | `Settings` defaults and edge cases (capacities, clamps, null config). |
| `SlimefunBridgeTest` | 5 | The Slimefun bridge is inert and never throws without Slimefun. |
| `ToolsTest` | 3 | Wrench and Rake are hand tools; the Receiver is filterable; filters default to whitelist. |
| `TransmissionFixesTest` | 13 | Item and fluid transmission: all-or-nothing fluid deposits, the pump never duplicates fluid, bridge with template-only filter, bridge never drains Greedy Cells, a device shared by two controllers works once per cycle, partial crafting results are undone, wrench pastes exact templates, rake returns the device, filters/face/transit buffer survive break and place, transit buffers above 99 units are saved without crashing. |
| `UpgradedFeaturesTest` | 6 | Memory module in a DRAM Bay, Router, per-chunk node limit, grabber transit buffer, cache kept on break, creative breaking drops nothing. |
| `compat.NetworkOwnershipTest` | 9 | A network runs inside its owner's claim; other networks and a null owner are strangers; public land stays open; broken or unwired providers grant nothing; owner answers never leak between networks. |
| `compat.ProtectionStonesProviderTest` | 18 | The ProtectionStones provider against the real API shape (`PSRegion.fromLocation*`, exact signatures), owner/member certification, fail-closed behaviour; WorldGuard region lookup fails safe. |
| `compat.ProtectionWhitelistTest` | 15 | Protection defaults and `exempt-locations` parsing and geometry; the bridge is inert without providers. |
| `listen.SneakingRightClickTest` | 3 | Sneaking + right-click never opens a device menu and still allows vanilla placement. |
| `net.ScanCostTest` | 2 | The BFS neighbour walk allocates nothing per node and a large scan stays linear. |
| `persist.NodeStoreCanonicalTest` | 5 | The shared decoded blob is never older than the last write. |
| `persist.NodeStoreCorruptionTest` | 5 | Corrupt PDC entries read as missing, silently and cheaply, and can be overwritten. |

## 4. Notes on some suites

### `TransmissionFixesTest` and `BlueprintDupeTest`
Each test reproduces a loss or duplication that existed in the code and was fixed. They were checked
to **fail** against the code before the fix, so they guard against regressions rather than restate
the implementation. Examples: a purger shared by two controllers deleted 256 items per cycle instead
of 128; a nearly full fluid network kept part of a bucket and the bucket; two players opening the
same Recipe Encoder turned 16 stored Blueprints into 32.

### `BlockFlowsTest`
The three dimension tests (`networkExtractsInsideTheNether/End/Overworld`) build a real network with
a grabber and a chest and run the scheduler. They prove the scan, ticker and storage carry no
dimension check of their own, so whatever blocks a dimension at runtime is the protection bridge.

### `compat.*`
Protection plugins are not on the test classpath. What is tested is everything that does not need
them (defaults, whitelist geometry, inert bridge) plus the ProtectionStones provider against a stub of
its API; each provider's live decision is verified on a real server.

---

To understand the functionality these tests cover, see [How the code works](Code.md).
