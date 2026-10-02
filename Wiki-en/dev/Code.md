# ⚙️ How the MultiverseNets code works

This document explains the plugin's internals: how a network is represented, where and how the
state is persisted, how items and fluids flow, and how the code is organized by layer. It is aimed
at developers who want to read or modify the code. For the player-facing behaviour of each machine
see the [wiki README](../README.md).

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
│   + crafting                  │  com.chagui68.multiversenets.craft
├───────────────────────────────┤
│  Persistence layer            │  com.chagui68.multiversenets.persist
│   (NodeBlob / NodeStore)      │
├───────────────────────────────┤
│  Foundation                   │  util / item / command / compat / api
└───────────────────────────────┘
```

All network logic runs on the server's main thread (synchronous), avoiding race conditions with the
world. The plugin is **not Folia-compatible** for that reason.

## 2. Plugin lifecycle

Main class: `MultiverseNets extends JavaPlugin` (singleton via `MultiverseNets.instance()`; exposes
`networks()`, `ticker()` and `blockListener()`).

**`onEnable()`**, in order:
1. `saveDefaultConfig()`, `Keys.init(this)`, `Settings.refresh(this)`.
2. `SlimefunBridge.registerSerializationAliases()` and `SlimefunBridge.init(...)` — the Slimefun
   integration activates only if Slimefun is installed.
3. `ProtectionBridge.init(...)` — registers every protection provider whose plugin is present.
4. `Items.registerRecipes(this)` and `NodeStore.init(this)` (controller registry).
5. `new NetworkManager(this); networks.load()` — recreates the networks from the saved controllers.
6. Registers `BlockListener`, `GuiListener`, `ChatPrompts` and `CraftingListener`.
7. Re-registers the recipes one tick later and again after 100 ticks (so datapack reloads cannot
   drop them) and unlocks them for online players.
8. `NetworkHologramManager.init`, a repeating task that drops the protection cache every
   `protection.cache-ticks`, then starts `NetworkTicker` and registers `/mvnets`.

**`onDisable()`**: stops the ticker and the protection task, removes every hologram, and runs
`networks.saveAll()` (controller registry).

## 3. Persistent keys (`util/Keys`)

Single registry for every `NamespacedKey` used in the `PersistentDataContainer` (PDC) of chunks and
items (namespace `multiversenets:`):

| Constant | Key | Usage |
| --- | --- | --- |
| `DEVICE_TYPE` | `device_type` | Device type on items (`DeviceType` enum name). |
| `WIRELESS_BIND` | `wireless_bind` | Controller coordinates bound to a Wireless Terminal. |
| `RECEIVER_BIND` | `receiver_bind` | Bridge link on a Receiver or Transmitter item (`world;x;y;z` of the other end). |
| `BLUEPRINT_RECIPE` | `blueprint_recipe` | Legacy recipe key stored on an old blueprint. |
| `CHUNK_HAS_NODES` | `chunk_has_nodes` | Fast "this chunk may contain nodes" marker. |
| `TERMINAL_DISPLAY` | `terminal_display` | Terminal search/sort display settings. |
| `CELL_CARGO` | `cell_cargo` | Serialized (Base64) `NodeBlob` embedded in a broken device's item. |
| `BLUEPRINT_DATA` | `blueprint_data` | `RecipeData` encoded in Base64 on a Blueprint. |
| `CONFIG_DATA` | `config_data` | Wrench: copied materials plus `WL:`/`BL:` mode. |
| `CONFIG_ITEMS` | `config_items` | Wrench: exact filter templates (encoded `NodeBlob` carrying `filterItems`). |
| `RAKE_USES` | `rake_uses` | Remaining uses of the Network Rake. |
| `SF_BLUEPRINT` | `sf_blueprint` | Marks a Blueprint encoded from a Slimefun recipe. |

## 4. Coordinates (`util/PosUtil`)

A 3D block position is packed into a **single 64-bit `long`**:

- **X → 26 bits** (bits 38–63), mask `0x3FFFFFF`, range ±33,554,431.
- **Z → 26 bits** (bits 12–37), same range.
- **Y → 12 bits** (bits 0–11), range −2048 … +2047.

`pack(x, y, z) = (x & 0x3FFFFFF) << 38 | (z & 0x3FFFFFF) << 12 | (y & 0xFFF)`. Negatives are handled
by arithmetic shifts in `unpack`. Packed positions are the keys of `Network.nodes` and locate chunks
(`PosUtil.unpackX(pos) >> 4`). `PosUtilTest` pins the format.

## 5. Persistence (`persist/NodeBlob` and `persist/NodeStore`)

### 5.1 Node state: `NodeBlob`
`Serializable` (UID fixed at `1L`, so new fields deserialize as defaults on old blobs) with public
fields:

| Field | Type | Used by |
| --- | --- | --- |
| `typeName` | `String` | Every node (`DeviceType` name). |
| `cellSample` / `cellAmount` | `ItemStack` / `long` | Quantum Cells and Infinity Barrel (the barrel keeps `cellSample` when empty). |
| `filterMaterials` / `filterItems` / `filterBlacklist` | `List<String>` / `List<ItemStack>` / `boolean` | Filterable devices. `filterItems` (exact templates) wins over `filterMaterials` when non-empty. |
| `targetFace` | `String` | Advanced Grabber/Pusher face (`NORTH`… or `ALL`). |
| `transitBuffer` | `ItemStack` | Grabbers and Pushers: items waiting because neither the network nor the source took them. |
| `recipes` / `blueprintData` | `List<String>` | Crafters: legacy recipe keys / installed `RecipeData` (Base64). |
| `craftingMatrix` | `ItemStack[9]` | Encoder and Crafting Grid template. |
| `encoderBlank` / `encoderOutput` | `ItemStack` | Blueprints left in the Recipe Encoder's slots. |
| `txWorld` / `txX` / `txY` / `txZ` | `String` / `int` | Bridge link of a Receiver or Transmitter (the other end). |
| `greedySamples` / `greedyAmounts` | `List<ItemStack>` / `List<Long>` | Greedy Cell multi-item buffer. |
| `virtualCacheTier` / `virtualSamples` / `virtualAmounts` | `int` / lists | Controller CPU Virtual Cache. |
| `quotaSample` / `quotaLimit` / `quotaActive` | `ItemStack` / `long` / `boolean` | Quota Limiter. |
| `fluidType` / `fluidAmount` | `String` / `long` | Quantum Fluid Cell (mB). |
| `pumpFluid` (`pumpMode` legacy) | `String` | Liquid Pump filter (`WATER`, `LAVA`, null = any). |
| `ownerUuid` | `String` | Controller only: the network's owner. |
| `crayon` | `boolean` | Legacy field, unused. |

### 5.2 Per-chunk read/write: `NodeStore`
State lives on the **chunk PDC**, per block:
- `"n" + x + "_" + y + "_" + z` → full blob (Base64).
- `"t" + x + "_" + y + "_" + z` → type name only, so the scan can classify a block without
  deserializing.

Every `put()` also stamps `CHUNK_HAS_NODES` on the chunk.

API: `put` / `get` (copy-on-read; null if the chunk is not loaded) / `canonical` / `getType` /
`hasNode` / `remove` / `countNodesInChunk` / `encode` / `decode`.
- **`canonical(Block)`** returns a shared, already-decoded instance. `NetworkStorage` reads every
  cell on every deposit and withdrawal, and decoding Base64 + `BukkitObjectInputStream` there was the
  plugin's heaviest cost. Every `put` replaces the shared instance, so it is never older than the last
  write (`NodeStoreCanonicalTest`). The cache is dropped wholesale past 8,192 entries.
- **`decode`** treats a corrupt entry as missing, silently and cheaply (`NodeStoreCorruptionTest`);
  it also migrates legacy blobs (null lists, single-item Greedy Cells).

### 5.3 Controller registry (`networks.yml`)
`Map<UUID, List<String>>` (world → `"x,y,z"`) persisted to `<dataFolder>/networks.yml`. `save()`
writes asynchronously when called from the main thread. `NetworkManager.load()` rebuilds the
networks from it at startup.

## 6. The network (`net/Network` and `net/NetworkManager`)

### 6.1 Topology: `Network`
- Identified by the packed controller position. `nodes: Map<Long, DeviceType>` plus the reverse index
  `byType: Map<DeviceType, Set<Long>>`, so the ticker only iterates the types it needs.
- `scan()` — **BFS** from the controller over the 6 axis-aligned neighbours. It:
  - never loads chunks (unloaded neighbours are skipped; an unloaded controller leaves the network
    untouched);
  - clears the network if the controller block no longer has a blob (`controller missing`);
  - takes the owner from the controller blob and refuses to expand into land that owner cannot use
    (`ProtectionBridge.mayActorUse`), counting the refusals (`linksBlockedByProtection()`);
  - stops at another controller (`foreign controller at x,y,z`) and sets
    `touchesForeignController()`;
  - with Slimefun, treats Slimefun blocks whose id contains `CABLE`/`BRIDGE` as cables and records
    touching Slimefun barrels (`slimefunBarrels()`);
  - honours `network.max-nodes`; bumps `version` and invalidates the storage cache.
- Queries: `contains`, `typeAt`, `forEach(type, consumer)` (defensive copy), `count(type)`, `block(pos)`,
  `ownerUuid()`, `error`.

### 6.2 Manager: `NetworkManager`
- `networksByWorld: Map<UUID, Map<Long, Network>>`.
- `registerController` / `removeController`, `networkAt(Block)` (linear search of the world's
  networks), `networkByController(Location)`.
- `invalidateNear(Block)` — rescans the block's network and its 6 neighbours' (place/break/rake).
- Filter helpers: `filterPredicate(blob)` (empty filter → accepts everything; otherwise whitelist or
  blacklist), `matchesFilter(template, item)` (order: DeviceType → Slimefun id → display name →
  material), `extractMatching(Inventory, …)` (one item type, merging every slot up to the quota),
  `insertInto(Inventory, …)`.

## 7. Item storage: `NetworkStorage`

One "vault" over every storage of the network: the controller's **CPU Virtual Cache**, **Quantum
Cells**, **Infinity Barrels**, **Greedy Cells** and **Slimefun barrels**. All methods are
`synchronized`; blobs are read with `NodeStore.canonical` and only dirty ones are written back.

- **`deposit(ItemStack)` → leftover**. First the **Quota Limiters** cap the amount (lowest limit wins),
  then 8 passes: (1) Greedy Cells whose filter matches or that already hold the item → (2) virtual
  cache holding that type → (3) Slimefun barrels holding it → (4) cells/barrels holding it → (5)
  virtual cache free space → (6) empty Slimefun barrels → (7) empty cells/barrels (they adopt the
  type) → (8) Greedy Cells without a filter. Never mutates the argument.
- **`withdraw(matcher, want, excludePos, includeGreedy)`** — virtual cache → cells and barrels →
  Slimefun barrels → Greedy Cells (only if `includeGreedy`). Returns a single item type. Pushers,
  Greedy suction and both bridge directions pass `includeGreedy = false`; terminals, crafting and the
  API use the 2-argument form (Greedy included). An Infinity Barrel keeps its `cellSample` when it
  reaches 0; a cell forgets it.
- `count`, `remainingQuota`, `view()` (500 ms cache, merged with `StackUtils.itemsMatch`),
  `getPurgedItemsView`, `isItemPurged`, counters.

## 8. Fluid storage: `NetworkFluidStorage`

Separate from items: the sum of every `MVN_FLUID_CELL` (one fluid per cell, `fluids.cell-capacity-mb`
each). `deposit(fluid, mB)` is **all-or-nothing**: it returns `0` if everything fit and the full
amount (storing nothing) otherwise, because every caller — pump, terminal, input slot — consumes a
whole bucket, bottle or source block only on `0`. `withdraw`, `count`, `getFluids`, `totalCapacity`,
`totalStored`.

## 9. The heartbeat: `NetworkTicker`

- `runTaskTimer(plugin, run, 20L, 5L)`; per-family countdowns (`scanIn`, `transferIn`, `vacuumIn`,
  `craftIn`) fire each family at its configured interval.
- Each run: networks are sorted by world and controller position; dirty or due networks are
  scanned; if any operation is due, `assignSharedNodes` gives every node shared by two networks
  (`touchesForeignController`) to the first one, so **each device works once per cycle**; then
  transfers, vacuum and crafting run through `forEachWorked`; finally the hologram is updated inside
  a try/catch (a hologram failure is logged once and never stops the loop).
- **`doTransfers`** (`items-per-op` = 128, HT = ×`ht-multiplier`):
  - **Grabber** (`grabOnce`): retries its transit buffer first; then the first face that yields a
    match — Slimefun machine output slots first, then vanilla containers. Overflow → Pushers that
    accept it (`streamToPushers`) → back to the source → transit buffer. Idle grabbers back off
    (only every third cycle after an empty one, up to 30).
  - **Pusher** (`pushOnce`): retries its transit buffer first; empty whitelist = idle; does nothing
    without an adjacent container; withdraws one type (no Greedy) and inserts; the rest goes back to
    the network or into the transit buffer.
  - **Greedy Cell** (`greedyTick`): suction up to `4 × items-per-op`, distribution up to
    `2 × items-per-op` into adjacent non-network containers.
  - **Purger**: only with a non-empty filter.
  - **Receiver** (`bridgeOnce`) / **Transmitter** (`transmitOnce`): the end that holds the link
    moves items across. Filter rule `bridgeFilterSet`: non-empty whitelist or any blacklist. Both
    ends pass the protection check with their own network's owner; leftovers go back to the source
    and, failing that, drop next to the device.
  - **Liquid Pump** (`pumpTick`): one source block below, only if `deposit` returns 0.
- **`doVacuum`**: dropped `Item` entities without pickup delay within `vacuum.radius`.
- **`doCrafting`**: every Auto-Crafter (and Slimefun Auto-Crafter if `sf-crafter.enabled`) tries each
  installed Blueprint once.
- Every block touched passes `denied(net, block)` → `ProtectionBridge.mayActorUse(block, owner)`.

## 10. Devices and items (`item/DeviceType`, `item/Items`)

`DeviceType` enumerates the **43** devices, modules and tools; each constant has `material`,
`display`, `placeable` and `cellTier`. Derived properties: `isCell()`, `isBarrel()`, `isFluidCell()`,
`isLiquidPump()`, `isRequestTerminal()`, `isAutoCrafter()`, `isRequestCrafter()`,
`isSlimefunCrafter()`, `filterable()` (grabbers, pushers, vacuum, greedy cell, purger, receiver,
transmitter), `isImporter()`/`isExporter()`, `isDirectional()`, `isRouter()`, `isCacheModule()`,
`cacheTier()`. `parse(name)` accepts `MVN_…`, the unprefixed form and `wireless`.

| Constant | Material | Display name | Placeable |
| --- | --- | --- | --- |
| `MVN_CONTROLLER` | LODESTONE | Network Controller | ✔ |
| `MVN_CABLE` | GLASS | Network Cable | ✔ |
| `MVN_TERMINAL` | BEACON | Network Terminal | ✔ |
| `MVN_MONITOR` | RESPAWN_ANCHOR | Network Monitor | ✔ |
| `MVN_ROUTER` | LIGHTNING_ROD | Network Router | ✔ |
| `MVN_CACHE_L1` … `MVN_CACHE_QUANTUM` | COPPER_INGOT, GOLD_INGOT, DIAMOND, NETHERITE_INGOT, NETHER_STAR | CPU Cache Modules | ✘ (hand) |
| `MVN_CELL_T1` … `MVN_CELL_T6` | Terracotta per tier | Quantum Cell T1…T6 | ✔ |
| `MVN_GREEDY_CELL` | SLIME_BLOCK | Greedy Cell | ✔ |
| `MVN_INFINITY_BARREL` | BARREL | Infinity Barrel | ✔ |
| `MVN_GRABBER` / `MVN_GRABBER_HT` | OBSERVER / STICKY_PISTON | Simple / Advanced Grabber | ✔ |
| `MVN_PUSHER` / `MVN_PUSHER_HT` | TARGET / PISTON | Simple / Advanced Pusher | ✔ |
| `MVN_VACUUM` | SPONGE | Network Vacuum | ✔ |
| `MVN_PURGER` | MAGMA_BLOCK | Network Purger | ✔ |
| `MVN_LIMITER` | TARGET | Network Quota Limiter | ✔ |
| `MVN_PROBE` | SPYGLASS | Network Probe | ✘ (hand) |
| `MVN_CRAFTER` / `MVN_SF_CRAFTER` | CRAFTING_TABLE / CRYING_OBSIDIAN | Auto-Crafter / Slimefun Auto-Crafter | ✔ |
| `MVN_REQUEST_CRAFTER` / `MVN_SF_REQUEST_CRAFTER` | FLETCHING_TABLE / PURPUR_PILLAR | Request Crafter / Slimefun Request Crafter | ✔ |
| `MVN_REQUEST_TERMINAL` | LECTERN | Request Terminal | ✔ |
| `MVN_ENCODER` / `MVN_SF_ENCODER` | SMITHING_TABLE / ENCHANTING_TABLE | Recipe Encoder / Slimefun Recipe Encoder | ✔ |
| `MVN_CRAFTING_GRID` | CARTOGRAPHY_TABLE | Network Crafting Grid | ✔ |
| `MVN_QUANTUM_WORKBENCH` | BRAIN_CORAL_BLOCK | Quantum Workbench | ✔ |
| `MVN_TRANSMITTER` / `MVN_RECEIVER` | CONDUIT / REDSTONE_LAMP | Wireless Transmitter / Receiver | ✔ |
| `MVN_WIRELESS_TERMINAL` | NETHER_STAR | Wireless Terminal | ✘ (hand) |
| `MVN_BLUEPRINT` | BOOK | Blueprint | ✘ (hand) |
| `MVN_CONFIGURATOR` | COMPARATOR | Configuration Wrench | ✘ (hand) |
| `MVN_RAKE` | DEAD_BUSH | Network Rake | ✘ (hand) |
| `MVN_FLUID_CELL` | PRISMARINE_BRICKS | Quantum Fluid Cell | ✔ |
| `MVN_LIQUID_PUMP` | BLUE_STAINED_GLASS | Liquid Pump | ✔ |

`Items`:
- `create(type)` builds the item (name, lore, `DEVICE_TYPE`); `typeOf(item)` reads it back.
- `capacityOf(type)` — cells, barrel, greedy cell and cache modules from `Settings`.
- Tools: `rake()`/`rakeUses`/`spendRakeUse`; `saveConfig`/`readConfig` and
  `saveConfigItems`/`readConfigItems` (wrench); `linkReceiver`/`readReceiverBind` (bridge link,
  used for both Receiver and Transmitter items); `bindWireless`/`readWirelessBind`.
- `registerRecipes(plugin)` — the **43** shaped recipes (40 always, plus the Slimefun encoder and the
  two Slimefun crafters while their `enabled` flags are on). Patterns: [Recipes.md](../Recipes.md).
- `GuideBook` builds the in-game guide (`/mvnets guide en|es|both`).

## 11. Crafting (`craft/Blueprints` and `craft/CraftingSupport`)

- `RecipeData` — `inputs[9]` (quantity 1, `null` = empty) and `output`. `Blueprints.encode/decode`
  (Java serialization + Base64), `toItem`, `read`, `isBlueprint`.
- `Blueprints.resolve(matrix, world)` resolves the matrix against the server's vanilla recipes
  (cached); `matchesOutput` requires that recipe to still yield the recorded output, so **if the server
  changes the recipe, the blueprint stops working**.
- `CraftingSupport.tryCraftBlueprint(net, data)` — **all-or-nothing**:
  1. Resolve the result (vanilla recipe; with Slimefun, the Slimefun recipe; the recorded output is
     trusted only for Slimefun items).
  2. Aggregate needs by item, pre-check with `count`, then `withdraw` each one; a failure midway
     returns everything taken (`returnOrDrop`: deposits back and drops **only** what did not fit next
     to the controller).
  3. Deposit the result. If it only partly fits, the stored part is withdrawn again and the
     ingredients are returned — the craft either happens whole or not at all.
- `tryCraftOnce` / `tryCraftAll` — legacy recipe-key variant with the same rollback.
- `RequestTerminalMenu` plans chains with a simulated stock (`planCraft`), executes step by step with
  an intermediate buffer, and returns any intermediate leftovers to the network.

## 12. GUI layer (`gui/`)

### 12.1 Base: `MenuHolder`
Abstract `InventoryHolder`. `open(size, title)` creates the inventory, calls `draw()` and opens it;
`refresh()` redraws keeping the `vanillaSlots()` contents. Helpers: `giveOrDrop`,
`playerInventorySlot(event)`. Each menu implements `draw()` and `click(event)`; `onClose` is optional.

| Menu | Size | Usage / details |
| --- | --- | --- |
| `TerminalMenu` | 54 | Terminal (block, wireless, transmitter/receiver buttons). Input `INPUT_SLOT=8`, purger view `17`, sort `26`, fluids page `35`, pages `44`/`53`; 48 items per page. Fluid deposits/withdrawals with buckets and bottles. |
| `ControllerMenu` | 27 | Controller status, CPU cache usage, router status. |
| `MonitorMenu` | 27 | Live diagnostics (refresh task while open). |
| `FilterMenu` | 27 | Grabbers, pushers, vacuum, purger, greedy cell, receiver and transmitter: up to 17 templates, mode `17`, clear `25`, help `26`. Slot `24`: face selector for Advanced devices, "open adjacent block" for simple ones, "open terminal" for Transmitter/Receiver. |
| `CellMenu` / `BarrelMenu` | 18 | Template `4`, deposit all `11`, set item `13`, extract all `15`. The barrel's *Set Item* right-click clears the registration when empty. |
| `GreedyMenu` | 54 | 36 storage slots with pages, filter `45`, deposit `46`, monitor `49`, direction `50`, info `53`. |
| `CrafterMenu` | 27 | Auto/Request/Slimefun crafters: up to 18 Blueprints, status `24`, clear all `25` (returns them), help `26`. Installing consumes the Blueprint; uninstalling or replacing returns it. |
| `EncoderMenu` / `SfEncoderMenu` | 45 | Template grid, blueprint slot `19`, encode `16`, preview `25`, output `34`. The Recipe Encoder stores Blueprints left in `19`/`34` in the block; while a menu is open they live only in that menu. |
| `CraftingGridMenu` | 54 | Network crafting: result `31`, craft one `33`, craft all `35`, clear `38`, pages `27`/`29`, info `41`. |
| `RequestTerminalMenu` | 54 | 45 options per page, delivery toggle `49`, refresh `51`, pages `45`/`53`. |
| `QuotaLimiterMenu` | 36 | Target item `13`, on/off `22`, chat limit `31`, ±1/10/64/1,000 buttons. |
| `FluidCellMenu` | 27 | Tank `13`, bucket interaction `10`, extract one bucket `15`, void tank `16` (shift+right-click). |
| `LiquidPumpMenu` | 27 | Fluid filter `12` (ANY/WATER/LAVA), network fluids `14`. |
| `QuantumWorkbenchMenu` | 45 | Cell upgrade: centre `20`, craft `23`, output `25`; ingredients returned on close. |
| `ChatPrompts` | — | Chat-driven numeric prompts (request terminal, limiter). |

### 12.2 Security: `GuiListener` (dupe guard)
For every `MenuHolder` top inventory: cancels double-click, middle click, number keys, offhand swap,
drops, creative actions, `COLLECT_TO_CURSOR`, hotbar moves and `UNKNOWN`; only shift-clicks from the
player inventory reach the menu; non-vanilla top slots are cancelled and forwarded to `click`; drags
over non-vanilla slots are cancelled; `onClose` is forwarded.

## 13. Events (`listen/`)

`BlockListener` owns the event wiring; `DeviceInteractions` decides what each device opens and the
player access gate (`canAccessNetwork`, static: admin bypass, protection providers, BentoBox island
membership), installs cache modules and handles the fluid-cell quick interaction.

| Event | Behaviour |
| --- | --- |
| `BlockPlaceEvent` | Blocked worlds and `max-nodes-per-chunk` checked; registers the node, restores the embedded state (`CELL_CARGO`), applies a bridge link from the item (Receiver or Transmitter), records the Controller's owner, then registers the controller or rescans the neighbours. |
| `BlockBreakEvent` | Drops the Encoder's stored Blueprints, drops the device with its state embedded (nothing in creative), removes the node and rescans. |
| `PlayerInteractEvent` | Air click with a Wireless Terminal (combat lock, range/world unless Router, access check). Block click: access check, then Probe, Rake (returns the device), Wrench, bindings (wireless on controller/terminal; Receiver item on Transmitter and Transmitter item on Receiver), sneaking never opens menus, cable status message, cache module install, fluid cell quick interact, device menu. |
| `InventoryMoveItemEvent` | Hoppers can insert into and pull from an Infinity Barrel (its registered item); every other node refuses hoppers. |
| Pistons / explosions | Nodes cannot be moved and are removed from explosion block lists. |
| `EntityDamageByEntityEvent` | Records combat time for the Wireless Terminal lock. |

`CraftingListener` re-registers recipes after reloads, unlocks them on join, and upgrades a cell with
cargo in a regular crafting table keeping the cargo.

## 14. Command `/mvnets` (`command/MvnetsCommand`)

Subcommands: `help`, `info`, `guide`, `devices` (open) and `give <id> [n]`, `doctor`, `stats`,
`inspect`, `repair`, `recipes`, `reload` (**`multiversenets.admin`**). The tab completer completes
subcommands, `en|es|both` for `guide`, and device ids for `give` (`type.id()`, e.g. `mvn_controller`;
`give` also accepts the unprefixed form).

## 15. Public API (`api/MultiverseNetsAPI`)

Static, null-safe methods for other plugins, all keyed by any block of a network:
`isNetworkBlock(block)`, `extract(block, matcher, amount)`, `insert(block, stack)` (returns the
leftover) and `count(block, matcher)`. They go straight to `NetworkStorage`, so quotas and storage
order apply.

## 16. Slimefun integration (`compat/SlimefunBridge`)

- Slimefun machines keep their inventory in a `BlockMenu`, not an `InventoryHolder`; without the bridge
  they look like decorative blocks.
- **Reflection only** — probes `com.github.drakescraft_labs.slimefun4.legacy` and
  `io.github.thebusybiscuit.slimefun4.legacy`. With `compat.slimefun: false` or no Slimefun it stays
  dormant (`isAvailable()` = false).
- API: `isMachine`, `getId(Block)` / `getId(ItemStack)`, `extract` (output slots +
  `WITHDRAW`), `insert` (input slots + `INSERT`), `isNetworkCable`, `isBarrel` and barrel
  deposit/withdraw, `findSlimefunRecipe`, `openSlimefunMenu`. Spanish legacy aliases remain
  (`disponible`, `esMaquina`, `idDe`, `esItemSlimefun`, `extraer`, `insertar`).

## 17. Land protection (`compat/ProtectionBridge`)

- **Why**: a network is an anonymous actor; without this, a grabber in public land could read a chest
  inside somebody's region.
- **Owner**: the Controller stores its placer's UUID (`ownerUuid`); `mayActorUse(location, owner)`
  allows the network inside that owner's land. A provider certifies ownership through the optional
  `allowsActor(UUID, Location)` (implemented by ProtectionStones); the others return `null`, keeping
  their claims closed to every network. A throwing provider never grants access.
- **Legacy controllers** without an owner adopt the first player `mayPlayerAccess` allows
  (`DeviceInteractions.adoptControllerOwner`).
- **Where it applies**: `Network.scan()` (the BFS stops at land the owner cannot use and counts the
  cut), and `NetworkTicker` before every block it touches — grabbers, pushers, overflow to pushers,
  greedy distribution, vacuum, pump and both ends of the bridge in either direction. Players opening
  devices go through `canAccessNetwork`, including the remote terminal button of a Receiver.
- **Providers** (one file each, registered only if their plugin and API resolve):
  `ProtectionStonesProvider` (`PSRegion.fromLocationUnsafe`), `WorldGuardProvider`, `LandsProvider`,
  `TownyProvider`, `GriefPreventionProvider`. Methods are bound by exact signature, and only region
  containment is checked, never flags.
- **ProtectionStones caveat**: its API cannot tell a claim from a server region, so
  `protection.allow-claims` does not apply to it.
- **Performance**: answers are memoised per world and position (and per owner for `ownsAt`) and dropped
  every `protection.cache-ticks`.
- **Escape hatches**: `protection.exempt-worlds`, `protection.exempt-locations`
  (`world;x;y;z;radius`, radius 16 by default), `protection.block-network-linking: false`, and the
  `multiversenets.protection.bypass` permission for players.

## 18. Configuration (`util/Settings`)

All reads go through `Settings` over `plugin.getConfig()` (refreshed in `onEnable` and
`/mvnets reload`).

| Method | `config.yml` key | Default | Bounds |
| --- | --- | --- | --- |
| `scanIntervalTicks()` | `network.scan-interval-ticks` | 20 | ≥ 5 |
| `maxNodes()` | `network.max-nodes` | 16,384 | ≥ 16 |
| `maxNodesPerChunk()` | `network.max-nodes-per-chunk` | 64 | ≥ 1 |
| `transferIntervalTicks()` | `network.op-interval-ticks.transfer` | 5 | ≥ 1 |
| `vacuumIntervalTicks()` | `network.op-interval-ticks.vacuum` | 10 | ≥ 1 |
| `craftIntervalTicks()` | `network.op-interval-ticks.craft` | 20 | ≥ 1 |
| `itemsPerOp()` | `transfer.items-per-op` | 128 | ≥ 1 |
| `htMultiplier()` | `transfer.ht-multiplier` | 8 | ≥ 1 |
| `cellCapacity(tier)` | `cells.capacities` | list below | clamped to last tier |
| `virtualCacheCapacity(tier)` | `virtual-cache.tier-1` … `tier-5` | 2,048 … 524,288 | — |
| `greedyCapacity()` | `greedy.capacity` | 262,144 | ≥ 1 |
| `barrelCapacity()` | `barrel.capacity` | 2,000,000,000 | ≥ 1 |
| `fluidCellCapacity()` | `fluids.cell-capacity-mb` | 64,000 | ≥ 1,000 |
| `maxBlueprints()` | `crafter.max-recipes` | 18 | 1 – 18 |
| `vacuumRadius()` | `vacuum.radius` | 4.0 | ≥ 1.0 |
| `rakeUses()` | `rake.uses` | 250 | ≥ 1 |
| `wirelessLocalRange()` | `wireless.local-range-without-router` | 64 | ≥ 1 |
| `wirelessCombatCooldownSeconds()` | `wireless.combat-cooldown-seconds` | 10 | ≥ 1 |
| `blockedWorld(world)` | `blocked-worlds` | `[]` | case-insensitive |
| `compatSlimefun()` | `compat.slimefun` | `true` | — |
| `sfEncoderEnabled()` / `sfCrafterEnabled()` | `sf-encoder.enabled` / `sf-crafter.enabled` | `true` | — |
| `protectionEnabled()` | `protection.enabled` | `true` | — |
| `protectionProviderEnabled(id)` | `protection.providers` | empty list = all | case-insensitive |
| `protectionAllowClaims()` | `protection.allow-claims` | `false` | absent config = `false` |
| `protectionBlocksNetworkLinking()` | `protection.block-network-linking` | `true` | — |
| `protectionBlocksPlayerInteraction()` | `protection.deny-player-interaction` | `true` | — |
| `protectionBypassPermission()` | `protection.bypass-permission` | `multiversenets.protection.bypass` | empty removes the bypass |
| `protectionCacheTicks()` | `protection.cache-ticks` | 100 | ≥ 20 |
| `protectionExemptWorlds()` / `protectionExemptLocations()` | `protection.exempt-worlds` / `exempt-locations` | `[]` | unparseable entries dropped |
| `debug()` | `debug` | `false` | — |

**Cell capacities** (`cells.capacities`, `long` list): default `[65536, 262144, 1048576, 16777216,
268435456, 2000000000]`. Missing or empty key → `65536 × 2^(tier−1)`; an undeclared tier clamps to the
last one with a one-time warning (`SettingsCellCapacityTest`).
