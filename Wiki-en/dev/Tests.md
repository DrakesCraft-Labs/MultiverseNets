# 🧪 MultiverseNets plugin tests

This document explains how the project's tests work and what each one covers. Aimed at developers
who want to run them, understand them or extend them.

> Development area: [Structure](Structure.md) · [How the code works](Code.md) · **The tests**

---

## 1. Running the tests

The tests are **JUnit 5 (Jupiter)** and do not require a real Minecraft server. To run them:

```
mvn test
```

Or a full build with packaging (also runs the tests):

```
mvn clean package
```

Requirements: a JDK compatible with the project (Paper 1.21 / `api-version: '1.21'`) and Maven.

## 2. Test infrastructure: MockBukkit

Most tests use **[MockBukkit](https://github.com/MockBukkit/MockBukkit)**, which simulates a Bukkit
server in memory. The typical pattern is:

```java
@BeforeEach void setUp() {
    server = MockBukkit.mock();                    // create the fake server
    plugin = MockBukkit.load(MultiverseNets.class); // load the plugin (onEnable)
    world = server.addSimpleWorld("world");        // fake world
    player = server.addPlayer();                   // fake player
}

@AfterEach void tearDown() {
    MockBukkit.unmock();                            // cleanup between tests
}
```

This makes it possible to: place blocks, fire events (`server.getPluginManager().callEvent(...)`),
simulate inventory clicks, interactions, explosions and pistons, and verify the state persisted in
the chunk PDCs.

**Pure unit tests** (no MockBukkit): `PosUtilTest`, `SettingsCellCapacityTest`, `DeviceTypeTest`,
`NetworksCoexistenceTest`, `PluginResourcesTest`, `NewDevicesTest`, `ToolsTest` and `SlimefunBridgeTest`.

## 3. Overview of the 17 tests

| File (under `src/test/java/com/chagui68/multiversenets/`) | Type | Covers |
| --- | --- | --- |
| `BlockFlowsTest` | Integration | Block flows: break/place, pistons, explosions and interaction; node protection and persistence. |
| `CellGuiTest` | Integration | Quantum Cell GUI: item template, quick deposit, withdrawal and capacity safety. |
| `CrafterGuiTest` | Integration | Auto-Crafter GUI: installing/uninstalling blueprints and clearing recipes. |
| `DeviceTypeTest` | Unit | `DeviceType` classification and properties (filterable, cells, hand vs. placeable). |
| `FilterGuiTest` | Integration | Filter GUI: adding/removing items, whitelist/blacklist mode, shift-click and directional faces. |
| `GreedyCellTest` | Integration | Greedy Cell: multi-item storage, shared capacity, its menu and terminal enhancements. |
| `GuiDupeGuardTest` | Integration | Anti-duplication guards of the menus against dangerous clicks. |
| `GuiFlowsTest` | Integration | Integration of all network GUIs: terminal, encoder, crafter, crafting grid and monitor. |
| `InfinityBarrelTest` | Integration | Infinity Barrel: 2×10⁹ capacity, deposits/withdrawals and break/place persistence. |
| `NetworksCoexistenceTest` | Unit | Coexistence with the legacy Networks plugin: name, main class, commands, permissions and Slimefun soft dependency. |
| `NewDevicesTest` | Unit | Recent devices (Purger, Probe): placeable, filterable and never cells. |
| `PluginResourcesTest` | Unit | Essential resources (`plugin.yml`, `config.yml`) present on the classpath. |
| `PosUtilTest` | Unit | Packing/unpacking 3D coordinates into a 64-bit `long`. |
| `QuantumWorkbenchTest` | Integration | Quantum Workbench: cell upgrades preserving cargo and ingredient return. |
| `SettingsCellCapacityTest` | Unit | Capacity calculations against empty/missing/edge-case configs; defaults. |
| `SlimefunBridgeTest` | Unit | Safe bridge behavior when Slimefun is not present. |
| `ToolsTest` | Unit | Hand tools (Configurator, Rake, Crayon) and Receiver filtering. |

## 4. Per-test details

### `BlockFlowsTest`
Covers the "block flows" governed by `BlockListener`: the `BlockBreakEvent`, `BlockPlaceEvent`,
`BlockPistonExtendEvent`, `EntityExplodeEvent` and `PlayerInteractEvent` handlers. Verifies that
nodes cannot be pushed by pistons nor destroyed by explosions, that breaking a device drops the
proper item (with its preserved state) and that placing/breaking nodes correctly update the network
(controller registration, neighbor invalidation).

### `CellGuiTest`
Tests the Quantum Cell GUI (`CellMenu`, 18 slots): setting the item template, the **quick deposit**
from the cursor into the player's inventory, withdrawing items, and ensuring the cell's capacity is
never exceeded.

### `CrafterGuiTest`
Tests the Auto-Crafter GUI (`CrafterMenu`, 27 slots): **installing** a blueprint into a free slot,
**uninstalling** it and **clearing** the recipe list.

### `DeviceTypeTest`
Unit tests on the `DeviceType` enum: the **filterable** devices include the newer types; the
**Greedy Cell is not a storage cell** (it has no tier); and hand items (blueprint, wireless
terminal) are not placeable.

### `FilterGuiTest`
Tests `FilterMenu` (27 slots): adding items to the filter, removing filters, toggling
**whitelist/blacklist**, **shift-clicking** on the player inventory and configuring **directional
faces**.

### `GreedyCellTest`
Tests the Greedy Cell: **multi-item** storage (several templates at once) with **shared capacity**
(`greedy.capacity`), its dedicated menu (`GreedyMenu`, 54 slots with 36 storage slots) and the
purger/greedy enhancements over the terminal menu.

### `GuiDupeGuardTest`
Validates the **`GuiListener`**: dangerous clicks (double-click, middle, number keys, drop, hotbar,
collect-to-cursor, etc.) are cancelled inside custom menus to prevent item duplication or cloning.

### `GuiFlowsTest`
Integration of every network GUI:
- **Terminal**: opening it by right-clicking the block, withdrawing 1 item on left-click or a full
  stack on shift-click, saving input-slot items on close, depositing via shift-click from the player
  inventory, and withdrawing **custom items** (with PDC ID and lore) preserving their metadata.
- **Encoder**: encodes a 3×3 matrix into a Blueprint item (embedded `RecipeData`) and persists the
  matrix on the block.
- **Auto-Crafter**: crafts from the network **atomically** (insufficient ingredients → nothing is
  consumed; sufficient → ingredients are consumed and the result deposited).
- **Network Crafting Grid**: consumes 8 of 9 ingredients from the network and hands the result to
  the player.
- **Monitor**: opens its GUI when connected to a network; grabber/vacuum open `FilterMenu` and the
  crafter opens its `CrafterMenu`.

### `InfinityBarrelTest`
Tests the Infinity Barrel: **2,000,000,000 item capacity**, opening the menu and quick-depositing
with a set template, network-storage integration (bulk deposit/withdraw) and **break/place
persistence** (the embedded `CELL_CARGO` keeps both amount and type).

### `NetworksCoexistenceTest`
**Coexistence with the legacy Networks plugin** so both can run on the same server:
- The plugin name is not `NetworksV6-Drake`.
- The main class is not `io.github.sefiraat.networks.Networks`.
- The plugin's commands and aliases do not collide with the `networks` command.
- Permissions live under the `multiversenets.` prefix and never invade `networks.`.
- The Slimefun dependency is a **soft dependency** (the plugin works standalone).

It reads these values directly from `plugin.yml` with SnakeYAML.

### `NewDevicesTest`
Tests the recent utility devices:
- The **Purger** is placeable and **filterable** (so it does not delete items indiscriminately).
- The **Probe** is a hand tool: not placeable, not filterable, never a cell.
- Purger and Probe **never count as storage cells**.
- Every `DeviceType.values()` entry has a non-null material and a non-blank display name.

### `PluginResourcesTest`
Checks that `plugin.yml` and `config.yml` exist on the classpath (essential for startup) plus a
basic build-version sanity check.

### `PosUtilTest`
Pins the binary format of `PosUtil.pack/unpack`: positive and negative/world-boundary coordinates
(±30,000,000 in X/Z, negative Y) round-trip losslessly in a single `long`.

### `QuantumWorkbenchTest`
Tests `QuantumWorkbenchMenu` (45 slots):
- **Cell upgrade**: a Quantum Cell T1 with cargo (500 iron ingots) + diamonds around it → craft
  button → output **T2 with the cargo preserved** in `CELL_CARGO`, and the matrix consumed.
- **Closing**: uncrafted ingredients are **returned to the player**.

### `SettingsCellCapacityTest`
Tests `Settings` by reflectively injecting mock configurations:
- Uses capacities declared in `cells.capacities`.
- Empty list or missing key → **geometric fallback** without throwing.
- Undeclared tier → clamps to the **last** configured capacity.
- Invalid/negative tier → safe positive capacity.
- Barrel: 2,000,000,000 by default or the custom `barrel.capacity`.
- `crafter.max-recipes` clamps between 1 and 18.
- `long` capacities up to 2×10⁹ without 32-bit overflow.
- With `cfg == null` every getter returns its **default** (scan 20, max nodes 16,384, transfer 5,
  vacuum 10, craft 20, 64 items/op, HT multiplier 8, greedy 262,144, barrel 2×10⁹, blueprints 18,
  radius 4.0, `compat.slimefun` true, `debug` false, rake 250, cell 65,536).

### `SlimefunBridgeTest`
Without Slimefun present: the bridge reports `isAvailable()`/`disponible()` = false; querying null
blocks/items does not throw (`isMachine`, `getId`, `isSlimefunItem` and their Spanish aliases);
`extract`/`extraer` return null and `insert`/`insertar` return 0 safely.

### `ToolsTest`
Tests the hand tools: **Configurator, Rake and Crayon** are not placeable, do not store and do not
filter; the **Receiver is filterable** (controlled wireless transport); and filters default to
**whitelist** mode (`filterBlacklist = false`).

---

To understand the functionality these tests cover, see [How the code works](Code.md).