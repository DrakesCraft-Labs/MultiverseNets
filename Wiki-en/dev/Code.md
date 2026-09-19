# ⚙️ How the MultiverseNets code works

This document explains the plugin's internals: how a network is represented, where and how the
state is persisted, how items flow, and how the code is organized by layer. It is aimed at
developers who want to read or modify the code.

> Development area: [Structure](Structure.md) · **How the code works** · [Tests](Tests.md)

---

## 1. Overview (layers)

```
┌───────────────────────────────┐
│  Presentation layer (GUI)     │  com.chagui68.multiversenets.gui
├───────────────────────────────┤
│  Event layer (listeners)      │  com.chagui68.multiversenets.listen
├───────────────────────────────┤
│  Service layer (core)         │  com.chagui68.multiversenets.net
│   + crafting (compat/craft)   │
├───────────────────────────────┤
│  Persistence layer            │  com.chagui68.multiversenets.persist
│   (NodeBlob / NodeStore)      │
├───────────────────────────────┤
│  Foundation (util + item +    │  util / item / command / compat
│   command + compat)           │
└───────────────────────────────┘
```

All network logic runs on the server's main thread (synchronous), avoiding race conditions with the world.

## 2. Plugin lifecycle

Main class: `MultiverseNets extends JavaPlugin` (singleton via `MultiverseNets.instance()`; exposes
the network manager through `networks()`).

**`onEnable()`**, in order:
1. `saveDefaultConfig()` — writes `config.yml` if it does not exist yet.
2. `Keys.init(this)` — initializes the persistent `NamespacedKey`s.
3. `Settings.refresh(this)` — loads the configuration.
4. `SlimefunBridge.init(getLogger())` — activates the Slimefun integration only if Slimefun is installed.
5. `Items.registerRecipes(this)` — registers the recipes for every device.
6. `NodeStore.init(this)` — prepares per-chunk persistence and loads the controller registry.
7. `new NetworkManager(this); networks.load()` — recreates the networks from the saved controllers.
8. Registers `BlockListener`, `GuiListener` and `ChatPrompts`.
9. Starts `NetworkTicker` and registers the `/mvnets` command (alias `/mvn`).

**`onDisable()`**: stops the `NetworkTicker`, runs `networks.saveAll()` (saves the controller
registry) and logs the shutdown.

## 3. Persistent keys (`util/Keys`)

Single registry for every `NamespacedKey` used in the `PersistentDataContainer` (PDC) of chunks and items:

| Constant | Key (namespace `multiversenets:`) | Usage |
| --- | --- | --- |
| `DEVICE_TYPE` | `device_type` | Device type on items (`DeviceType` enum name). |
| `WIRELESS_BIND` | `wireless_bind` | Bound controller coordinates of a wireless terminal. |
| `RECEIVER_BIND` | `receiver_bind` | Linked transmitter coordinates of a receiver. |
| `BLUEPRINT_RECIPE` | `blueprint_recipe` | Legacy recipe stored on an old blueprint. |
| `CHUNK_HAS_NODES` | `chunk_has_nodes` | Fast "this chunk may contain nodes" marker. |
| `TERMINAL_DISPLAY` | `terminal_display` | Terminal search/sort display settings. |
| `CELL_CARGO` | `cell_cargo` | Serialized (Base64) state of a cell/node's cargo. |
| `BLUEPRINT_DATA` | `blueprint_data` | Recipe encoded in Base64 on the Blueprint item. |
| `CONFIG_DATA` | `config_data` | Filter configuration copied onto the wrench. |
| `RAKE_USES` | `rake_uses` | Remaining uses of the Network Rake. |

## 4. Coordinates (`util/PosUtil`)

A 3D block position is packed into a **single 64-bit `long`** (a very efficient map key):

- **X → 26 bits** (bits 38–63), mask `0x3FFFFFF`, range ±33,554,431.
- **Z → 26 bits** (bits 12–37), same range.
- **Y → 12 bits** (bits 0–11), range −2048 … +2047 (the full vanilla world height).

`pack(x, y, z) = (x & 0x3FFFFFF) << 38 | (z & 0x3FFFFFF) << 12 | (y & 0xFFF)`. Negatives are handled
by arithmetic (sign) shifts in the `unpack` methods. Packed positions are used as keys in
`Network.nodes`, `NetworkStorage.CellRef` and to locate chunks (`PosUtil.unpackX(pos) >> 4`).
`PosUtilTest` pins this format (positives and negative world boundaries).

## 5. Persistence (`persist/NodeBlob` and `persist/NodeStore`)

### 5.1 Node state: `NodeBlob`
A `Serializable` class (fixed UID `1L`) with **public fields** describing a network block's persistent state:

| Field | Type | Description |
| --- | --- | --- |
| `typeName` | `String` | `DeviceType` enum name. |
| `cellSample` | `ItemStack` | Sample item template stored in a Quantum Cell / Barrel. |
| `cellAmount` | `long` | Total stored quantity. |
| `filterMaterials` | `List<String>` | Filter material names/IDs. |
| `filterItems` | `List<ItemStack>` | Exact filter templates. |
| `filterBlacklist` | `boolean` | `true` = blacklist, `false` = whitelist. |
| `recipes` | `List<String>` | Legacy recipe keys (Auto-Crafter). |
| `blueprintData` | `List<String>` | Base64 `RecipeData` installed in the Auto-Crafter. |
| `craftingMatrix` | `ItemStack[9]` | Persistent 3×3 crafting template. |
| `crayon` | `boolean` | Controller particle effect. |
| `txWorld` / `txX` / `txY` / `txZ` | `String` / `int` | Receiver → Transmitter binding. |
| `targetFace` | `String` | Directional face (`NORTH`… or `ALL`). |
| `greedySamples` | `List<ItemStack>` | Multi-item templates of the Greedy Cell. |
| `greedyAmounts` | `List<Long>` | Per-sample quantities of the Greedy Cell. |

### 5.2 Per-chunk read/write: `NodeStore`
State is not stored on the block entity but on the **chunk PDC**, with per-block keys:
- `"n" + x + "_" + y + "_" + z` → full **blob** (Base64).
- `"t" + x + "_" + y + "_" + z` → just the **type name**, to classify a block without deserializing (the scanner's fast path).

Every `put()` also stamps the `CHUNK_HAS_NODES` marker (byte 1) on the chunk; the scanner uses it to skip empty chunks without reading PDCs.

Main API:
- `put(Block, NodeBlob)` / `get(Block)` (null if the chunk is not loaded) / `getType(Block)` / `hasNode(Block)` / `remove(Block)`.
- `encode(NodeBlob)` / `decode(String)` — Java serialization via `BukkitObjectOutputStream`/`BukkitObjectInputStream` + Base64.
- `normalize()` — repairs legacy blobs (null lists back to defaults) and **migrates legacy Greedy Cells**: if a Greedy held single-item cargo in `cellSample`/`cellAmount`, it is moved into `greedySamples`/`greedyAmounts`.

### 5.3 Controller registry (`networks.yml`)
`NodeStore` keeps `Map<UUID, List<String>> CONTROLLERS` in memory (world → `"x,y,z"`), persisted to
`<dataFolder>/networks.yml` under `controllers.<world-uuid>`. `save()` runs **asynchronously** when
called from the main thread (never blocks the tick). `NetworkManager.load()` rebuilds the networks
from this registry at startup.

## 6. The network (`net/Network` and `net/NetworkManager`)

### 6.1 Topology: `Network`
- Identified by the packed controller position (`controllerPos`).
- `nodes: Map<Long, DeviceType>` = membership set; `byType: Map<DeviceType, Set<Long>>` = type-indexed reverse index (for iterating only grabbers/pushers/etc. with thousands of nodes).
- `version` (long, volatile) — bumped on every `scan()`; `NetworkStorage` uses it to detect stale topologies.
- `scan()` — **BFS flood fill** from the controller over the 6 axis-aligned neighbors (±X/±Y/±Z). Prunes: unloaded chunk, chunk without the `CHUNK_HAS_NODES` marker, block without a type, and a **second controller** (rejected with `error = "foreign controller"`); honors `network.max-nodes`. Never forces chunk loads. Exposes `error` (human-readable warning), `markDirty()` and `needsScan(intervalMs)`.
- Queries: `contains(pos)`, `typeAt(pos)`, `forEach(type, consumer)` (defensive copy so blocks can be broken/placed during iteration), `count(type)`, `block(pos)`.

### 6.2 Manager: `NetworkManager`
- `networksByWorld: Map<UUID, Map<Long, Network>>` (world → controller → network).
- `registerController(Block)` / `removeController(Block)` — create/destroy the network and update `NodeStore`.
- `networkAt(Block)` — linear lookup over the world's networks containing the block; `networkByController(Location)` — direct controller lookup (wireless terminal).
- `invalidateNear(Block)` — rescans the block's network plus those of its **6 neighbors** (for connectivity changes). Called from `BlockListener` on place/break/rake/crayon.
- Filter helpers: `filterPredicate(blob)` (whitelist/blacklist), `matchesFilter(template, item)` (decision order: DeviceType → Slimefun ID → display name → material), `extractFirst(Inventory,…)`, `insertInto(Inventory,…)`.

## 7. Virtual storage: `NetworkStorage`

An aggregated view of every storage block in the network (Quantum Cells, Greedy Cells and Infinity
Barrels) as a **single "vault"**. GUIs and the ticker interact with the network, not individual cells.

- `sync()` — rebuilds the cell list `CellRef(pos, tier, greedy, barrel)` when `network.versionSnapshot() != boundVersion`; classifies each node (normal cell → `cellTier()`, greedy, barrel).
- `load()` / `flush()` — per operation, decodes blobs of loaded cells (skips unloaded chunks or cells whose real type no longer matches) and rewrites the dirty ones.
- **`deposit(ItemStack)` → leftover** — three priority passes: (1) **Greedy Cells** (preferred sink, if the item matches an existing sample or passes its filter); (2) **cells with the same template** (`cellSample`); (3) **empty cells** (they adopt the incoming type). Never mutates the argument stack.
- **`withdraw(matcher, amount[, excludePos])`** — two passes: normal cells + barrels first, **Greedy Cells last** (output buffer; `excludePos` stops a Greedy from withdrawing its own cargo). Result is a single item type.
- `count(predicate)` — total of items matching the predicate.
- `view()` — consolidated snapshot for GUIs with a **500 ms cache**; buckets by material and merges with `StackUtils.itemsMatch` (never `hashCode`).
- Capacities: cell → `Settings.cellCapacity(tier)`; greedy → `Settings.greedyCapacity()`; barrel → `Settings.barrelCapacity()`.
- Utilities: `getGreedyStoredAmount`, `isItemPurged`, `getPurgedItemsView`, `countActivePurgers`, `countActiveGreedyCells`, `isEmpty`.

## 8. The heartbeat: `NetworkTicker`

- Synchronous, `runTaskTimer(plugin, run, 20L, 5L)` — first tick at 20, then **every 5 ticks**.
- Per-family counters (`scanIn`, `transferIn`, `vacuumIn`, `craftIn`) — each family runs at its configured interval (multiples of 5).
- Per network: if `dirty` or scan due → `scan()`; transfer due → `doTransfers`; vacuum due → `doVacuum`; craft due → `doCrafting`.
- **`doTransfers`** (per-operation batching): `items-per-op` = 64 base; HT (Advanced Grabber/Pusher) uses `64 × ht-multiplier (8) = 512`. Processing order by type:
  1. `GRABBER` (imports 64) → `GRABBER_HT` (512) → `PUSHER` → `PUSHER_HT` → `GREEDY_CELL` → `PURGER` → `RECEIVER`.
  2. **Grabber**: extracts from vanilla containers (or Slimefun machines when `compat.slimefun`) against its filter and deposits into the network; overflow is returned or **dropped into the world** (`dropItemNaturally`).
  3. **Pusher**: withdraws from the network against its filter and inserts into containers/machines; what does not fit **goes back to the network**.
  4. **Purger**: only acts with **non-empty filters** (safe from indiscriminate deletion); withdraws and discards.
  5. **Greedy**: `greedyTick` = suction (withdraws up to `4 × items-per-op` with its own `excludePos`) if it has a filter and space, then **distributes** up to `2 × items-per-op` to adjacent containers/machines.
  6. **Receiver** (wireless bridge): only if the blob has `txWorld` and a **non-empty filter** (avoids accidental network merging); withdraws from the remote network and deposits into its own.
- **`doVacuum`**: radius `vacuum.radius`; collects ground `Item` entities (no `pickupDelay`, filter-passing) into the network.
- **`doCrafting`**: per Auto-Crafter, decodes each installed blueprint (`craft/Blueprints.decode`) and attempts to craft (see §10).
- Particles: when `net.crayon()`, cyan particles spawn on each operation (the Network Crayon feature).

## 9. Items and devices (`item/DeviceType` and `item/Items`)

- `DeviceType` — enum of **30 devices**. Each constant carries `material`, `display`, `placeable` and `cellTier` (1–6). Derived properties: `isCell()` (tier > 0), `filterable()` (grabbers, pushers, vacuum, greedy cell, purger, receiver), `isImporter()`/`isExporter()`, `isDirectional()` (advanced HT). `parse(name)` accepts `MVN_…`, the unprefixed form and `wireless`.

| Constant | Material | Display name | Placeable |
| --- | --- | --- | --- |
| `MVN_CONTROLLER` | LODESTONE | Network Controller | ✔ |
| `MVN_CABLE` | GLASS | Network Cable | ✔ |
| `MVN_TERMINAL` | BEACON | Network Terminal | ✔ |
| `MVN_MONITOR` | RESPAWN_ANCHOR | Network Monitor | ✔ |
| `MVN_CELL_T1`…`T6` | Terracotta per tier | Quantum Cell T1…T6 | ✔ |
| `MVN_GREEDY_CELL` | SLIME_BLOCK | Greedy Cell | ✔ |
| `MVN_INFINITY_BARREL` | BARREL | Infinity Barrel | ✔ |
| `MVN_GRABBER` / `MVN_GRABBER_HT` | OBSERVER / STICKY_PISTON | Simple / Advanced Grabber | ✔ |
| `MVN_PUSHER` / `MVN_PUSHER_HT` | TARGET / PISTON | Simple / Advanced Pusher | ✔ |
| `MVN_VACUUM` | SPONGE | Network Vacuum | ✔ |
| `MVN_PURGER` | MAGMA_BLOCK | Network Purger | ✔ |
| `MVN_PROBE` | SPYGLASS | Network Probe | ✘ (hand) |
| `MVN_CRAFTER` | CRAFTING_TABLE | Auto-Crafter | ✔ |
| `MVN_ENCODER` | SMITHING_TABLE | Recipe Encoder | ✔ |
| `MVN_CRAFTING_GRID` | CARTOGRAPHY_TABLE | Network Crafting Grid | ✔ |
| `MVN_QUANTUM_WORKBENCH` | BRAIN_CORAL_BLOCK | Quantum Workbench | ✔ |
| `MVN_TRANSMITTER` / `MVN_RECEIVER` | CONDUIT / REDSTONE_LAMP | Wireless Transmitter / Receiver | ✔ |
| `MVN_WIRELESS_TERMINAL` | NETHER_STAR | Wireless Terminal | ✘ (hand) |
| `MVN_BLUEPRINT` | BOOK | Blueprint | ✘ (hand) |
| `MVN_CONFIGURATOR` | COMPARATOR | Configuration Wrench | ✘ (hand) |
| `MVN_RAKE` | DEAD_BUSH | Network Rake | ✘ (hand) |
| `MVN_CRAYON` | CYAN_DYE | Network Crayon | ✘ (hand) |

- `Items.create(type)` — builds the `ItemStack` with its display name and stamps `Keys.DEVICE_TYPE = type.name()` in the PDC. `Items.typeOf(item)` reads it back (distinguishes plugin items from vanilla ones).
- `Items.capacityOf(type)` — declared capacity of a device (Barrel 2,000,000,000; cells from `Settings`).
- Tools: `rake()` (stamps `RAKE_USES`), `spendRakeUse` (spends a use, breaks at 0), `saveConfig`/`readConfig` (wrench: `CONFIG_DATA` with materials + `bl`/`wl`), `linkReceiver`/`readReceiverBind` (`RECEIVER_BIND`), `bindWireless`/`readWirelessBind` (`WIRELESS_BIND`), `blueprint`/`isBlueprint`/`readBlueprint` (legacy recipes via `BLUEPRINT_RECIPE`).
- `registerRecipes(plugin)` — registers **all** the plugin's shaped recipes. The 3×3 patterns are documented in [Recipes.md](../Recipes.md).

## 10. Crafting (`craft/Blueprints` and `craft/CraftingSupport`)

- `RecipeData` — `Serializable` structure with `inputs[9]` (quantity 1, `null` = empty) and `output`.
- `Blueprints.encode/decode` — Java serialization + Base64. A blueprint is a BOOK `ItemStack` with `Keys.BLUEPRINT_DATA`; `toItem` adds readable name/lore and `read` restores it.
- `Blueprints.resolve(matrix, world)` — normalizes the matrix (quantity-1, air→null) and resolves it against the server's vanilla recipes (`Bukkit.getCraftingRecipe`), cached per matrix. `matchesOutput` requires the currently registered recipe to still yield the recorded output (the NetworksV6 pattern): **if the server changes the recipe, the blueprint stops working**.
- `CraftingSupport.tryCraftBlueprint(net, data)` — **atomic** crafting from network storage:
  1. Guards: valid data and non-empty matrix.
  2. `resolve` + `matchesOutput`.
  3. Aggregates needs by type (`Need(sample, amount)`) using `StackUtils.itemsMatch`.
  4. Availability pre-check (via `count`) → if anything is missing, nothing is touched.
  5. Consumes with `withdraw`; if something fails midway, **everything taken is returned** (`depositAll`).
  6. Deposits the result; if it does **not fully fit** into the network, it rolls back the ingredients and fails.
- `tryCraftOnce(net, recipe)` — a Bukkit-recipe variant (legacy) with the same rollback pattern. `tryCraftAll` iterates the blob's legacy recipes. The Network Crafting Grid (`CraftingGridMenu`) uses the same network-backed resolution (see §11).

## 11. GUI layer (`gui/`)

### 11.1 Base: `MenuHolder`
Abstract class implementing `InventoryHolder`. `open(size, title)` creates the inventory, calls `draw()` (renders buttons/items) and opens it. `refresh()` redraws while preserving the `vanillaSlots()` contents. Provides `giveOrDrop` (returns items to the player, or drops them if the inventory is full) and `playerInventorySlot(event)` (robust player-slot resolution to prevent dupe glitches). Each menu implements `draw()` and `click(event)`; `onClose(event)` is optional.

Menus and inventories:

| Menu | Size | Usage / details |
| --- | --- | --- |
| `TerminalMenu` | 54 | Terminal (block, wireless and receiver). Per-item extraction boxes; input slot `INPUT_SLOT=8`; purger `17`, sort `SORT=26`, filter `35`, pages `PREV=44`/`NEXT=53`; 48 items per page. On close, saves anything left in the input into the network. |
| `EncoderMenu` | 45 | 3×3 matrix (`BLANK_SLOT=19`), encode button `ENCODE=16`, output `OUTPUT=34`, preview `PREVIEW=25`. Produces a Blueprint item with the recipe embedded. |
| `CrafterMenu` | 27 | Auto-Crafter: up to 18 installed blueprints (`MAX_BLUEPRINT_SLOTS=18`), status `STATUS=24`, clear `CLEAR=25`, help `HELP=26`. |
| `CraftingGridMenu` | 54 | Network Crafting Grid: own 3×3 + `RESULT=31`, `CRAFT_ONE=33`, `CRAFT_ALL=35`, `CLEAR=38`, pages `27/29`, info `41`. Consumes from the network and hands the result to the player. |
| `FilterMenu` | 27 | Filters (grabbers, pushers, vacuum, purger, greedy cell, receiver): up to 17 items, whitelist/blacklist toggle `MODE=17`, all-faces button `ALL_DIRECTIONS=24`, clear `25`, help `26`. |
| `MonitorMenu` | 27 | Network item counter. |
| `CellMenu` | 18 | Individual Quantum Cell: template `ITEM=4`, quick deposit `DEPOSIT_ALL=11`, set `SET=13`, extract all `EXTRACT_ALL=15`. |
| `GreedyMenu` | 54 | Greedy Cell: 36 storage slots (0–35), filter `45`, quick deposit `46`, monitor `49`, direction `50`, info `53`. |
| `BarrelMenu` | 18 | Infinity Barrel: template `ITEM=4`, deposit `11`, set `13`, extract all `15`. |
| `QuantumWorkbenchMenu` | 45 | Cell upgrade: 3×3 recipe (center `CENTER=20`), craft `CRAFT=23`, output `OUTPUT=25` (upgraded cell with preserved cargo). |
| `ChatPrompts` | — | Chat-driven prompts of the plugin (`isPending` status consulted by `GuiListener`). |

### 11.2 Security: `GuiListener` (dupe guard)
Global listener filtering ALL clicks whenever the top inventory is a `MenuHolder`:
- Cancels dangerous clicks: double-click, middle click, number keys, offhand swap, drop (simple/control), creative mode, `COLLECT_TO_CURSOR`, hotbar moves and `UNKNOWN`.
- Player inventory: only **shift-clicks** are forwarded to the menu (unless a `ChatPrompts` prompt is pending).
- Top inventory: if the slot is not in `vanillaSlots()`, it cancels and forwards to `menu.click(event)`.
- `onDrag`: cancels if the drag touches non-vanilla slots of the menu. `onClose`: notifies `menu.onClose(event)`.

## 12. Block listener (`listen/BlockListener`)

Handled events:

| Event | Behavior |
| --- | --- |
| `BlockPlaceEvent` | If the placed item is a device: registers it (`NodeStore.put`), restores embedded cargo (`CELL_CARGO`), links a Receiver if applicable, and either registers the controller (`registerController`) or invalidates the neighbors. |
| `BlockBreakEvent` | If the block is a node: drops the device item **with its state embedded in `CELL_CARGO`** (except controller/cable/empty state), removes the node and rescans. |
| `PlayerInteractEvent` | Hand tools (Probe → diagnostics, Rake → remove without breaking, Wrench → copy/paste filters, Crayon → particles); bindings (shift+click wireless terminal on controller/terminal; receiver on transmitter); and opening the matching menu per device (see menu table). |
| `BlockPistonExtendEvent` / `BlockPistonRetractEvent` | Cancelled if any moved block is a node (nodes cannot be pushed/pulled). |
| `EntityExplodeEvent` / `BlockExplodeEvent` | Nodes are removed from the destroyed-blocks list (explosion-proof). |

Notable tools:
- **Probe** (`probeNode`): reports the device, its network (or "NO NETWORK" if it cannot reach a controller) and any warnings from the last scan.
- **Rake** (`useRake`): instantly removes a node (never a controller, never storage with cargo), spending a use and breaking when exhausted.
- **Wrench** (`useWrench`): shift+click copies filters onto the item; normal click pastes them onto another filterable device.
- **Crayon** (`useCrayon`): toggles `blob.crayon` on the controller (network particles).

## 13. Command `/mvnets` (`command/MvnetsCommand`)

Subcommands: `help`, `info`, `reload`, `give <id> [n]`, `devices`, `doctor`, `stats`, `inspect`, `repair`. Admin-gated commands use **`multiversenets.admin`** (`reload`, `give`, `doctor`, `stats`, `inspect`, `repair`); `help`/`info`/`devices` are open. The `TabCompleter` completes subcommands and, for `give`, the device IDs (`type.id()`, lowercase without the `mvn_` prefix).

## 14. Slimefun integration (`compat/SlimefunBridge`)

- **Why it exists**: Slimefun machines store their inventory in a `BlockMenu`, not as an `InventoryHolder`; without a bridge they would look like decorative blocks to grabbers/pushers.
- **Via reflection**: the plugin stays fully standalone. It probes the packages `com.github.drakescraft_labs.slimefun4.legacy` and `io.github.thebusybiscuit.slimefun4.legacy`. If `compat.slimefun=false` or Slimefun is absent, it stays **dormant** (`isAvailable()` = false) and only vanilla containers are used.
- API: `isMachine(Block)` (own menu), `getId(Block)` (`checkID`) and `getId(ItemStack)` (reads the PDC tag `slimefun_item`), `extract` (pulls from output slots honoring `getSlotsAccessedByItemTransport` + `WITHDRAW`), `insert` (pushes to input slots + `INSERT`). Keeps Spanish legacy aliases: `disponible`, `esMaquina`, `idDe`, `esItemSlimefun`, `extraer`, `insertar`.

## 15. Configuration (`util/Settings`)

All reads go through `Settings` over `plugin.getConfig()` (refreshed in `onEnable` and `/mvnets reload`). Full table:

| Method | `config.yml` key | Default | Bounds |
| --- | --- | --- | --- |
| `scanIntervalTicks()` | `network.scan-interval-ticks` | 20 | ≥ 5 |
| `maxNodes()` | `network.max-nodes` | 16,384 | ≥ 16 |
| `transferIntervalTicks()` | `network.op-interval-ticks.transfer` | 5 | ≥ 1 |
| `vacuumIntervalTicks()` | `network.op-interval-ticks.vacuum` | 10 | ≥ 1 |
| `craftIntervalTicks()` | `network.op-interval-ticks.craft` | 20 | ≥ 1 |
| `itemsPerOp()` | `transfer.items-per-op` | 64 | ≥ 1 |
| `htMultiplier()` | `transfer.ht-multiplier` | 8 | ≥ 1 |
| `greedyCapacity()` | `greedy.capacity` | 262,144 | ≥ 1 |
| `barrelCapacity()` | `barrel.capacity` | 2,000,000,000 | ≥ 1 |
| `maxBlueprints()` | `crafter.max-recipes` | 18 | between 1 and 18 |
| `vacuumRadius()` | `vacuum.radius` | 4.0 | ≥ 1.0 |
| `cellCapacity(tier)` | `cells.capacities` | default list (below) | clamped to last tier |
| `compatSlimefun()` | `compat.slimefun` | `true` | — |
| `debug()` | `debug` | `false` | — |
| `rakeUses()` | `rake.uses` | 250 | ≥ 1 |

**Per-tier cell capacities** (`cells.capacities`, `long` list): default `[65536, 262144, 1048576, 16777216, 268435456, 2000000000]` for T1…T6. If the key is missing or empty, the fallback is the geometric formula `65536 × 2^(tier−1)` (only T1 matches the list). An undeclared tier clamps to the last available one (with a one-time console warning). `SettingsCellCapacityTest` covers all these edge cases.

---

Next page: [The plugin tests](Tests.md).