> ⚠️ **CONSOLIDADO EN DRAKES-SUITES (SUITE MULTIVERSE POR CHAGUI68):**  
> Todo el desarrollo activo y soporte para Paper/Purpur 1.21.11+ se realiza oficialmente en el monorepo [`Drakes-Suites`](https://github.com/DrakesCraft-Labs/Drakes-Suites) dentro del módulo oficial `drakes-multiverse`, preservando la autoría y diseño soberano de **Chagui68**.

<div align="center">

<img src="docs/banner-en.svg" alt="MultiverseNets" width="100%"/>

# 🌌 MultiverseNets

**Standalone digital logistics networks and massive storage for Paper — no Slimefun.**

<img src="https://img.shields.io/badge/Paper-1.21.11%20%7C%2026.1%20%7C%2026.2-38BDF8?style=for-the-badge&logo=minecraft&logoColor=white" alt="Paper 1.21.11 | 26.1 | 26.2"/>
<img src="https://img.shields.io/badge/Java-21-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
<img src="https://img.shields.io/badge/License-GPLv3-blue?style=for-the-badge" alt="GPLv3"/>
<img src="https://img.shields.io/badge/Author-Chagui68-22C55E?style=for-the-badge" alt="Chagui68"/>

</div>

---

> Wiki: [English](Wiki-en/README.md) · [Español](Wiki-es/README.md) · Recipes: [EN](Wiki-en/Recipes.md) · [ES](Wiki-es/Recipes.md)

## 📖 What is MultiverseNets?

**MultiverseNets** implements a Networks-style logistics network **100% standalone**: no Slimefun and
no other dependency. Every device is a custom item (identified by PDC), every placed device is a
tagged block whose state lives in the chunk, and the network's storage is virtual and persistent.

A network is **one Network Controller plus every MultiverseNets block connected to it**, face to
face. Cables are the cheap way to connect things, but every device conducts: a grabber touching a
cell touching the controller is already a network.

## 🎮 Quick start

1. Place a **Network Controller**. Whoever places it becomes the network's owner.
2. Connect **Cables** and devices to it. Add at least one **Quantum Cell** (or an Infinity Barrel)
   — without storage the network has nowhere to put items.
3. Right-click a **Network Terminal** (or the controller's menu) to see and use the storage.
4. Put a **Grabber** next to a chest to import, a **Pusher** next to a chest to export. Configure
   their filters with right-click.
5. Check `/mvnets doctor` or a **Network Probe** if something does not connect.

Every device id below can be given with `/mvnets give <id>`; `/mvnets devices` lists them all.
Crafting recipes for every device are in [Recipes (EN)](Wiki-en/Recipes.md) /
[Recetas (ES)](Wiki-es/Recipes.md).

---

## 🧩 Machine reference

Rates are the defaults from `config.yml`; every one of them is configurable (see
[Configuration](#configuration) below). "Cycle" means one transfer cycle (`network.op-interval-ticks.transfer`,
5 ticks by default).

### 🖥️ Core and access

| Device | id · block | What it does | How to use it |
|---|---|---|---|
| **Network Controller** | `mvn_controller` · Lodestone | The network's root. Every scan starts here (breadth-first through connected blocks, up to `network.max-nodes`). Records its owner (the placer) and shows a floating hologram with status, nodes and stored totals. Memory modules **no longer** go in the controller: they go in a **DRAM Bay**. | Right-click: status and router status. It does not accept modules. A module that was inside an old controller is taken out automatically **with its items** and waits in the **Terminal** as a temporary item: click it there and install it in a DRAM Bay. Breaking the controller drops any module nobody collected. Only one controller per network: a second one wired to the same cables is reported as `foreign controller` (see [Shared buses](#shared-buses)). |
| **Network Cable** | `mvn_cable` · Glass | Connects devices. No logic of its own. | Right-click it to see whether it reaches a controller (and the network size). Holding a block while right-clicking places the block instead. |
| **Network Terminal** | `mvn_terminal` · Beacon | The storage grid: every item in the network (cache, cells, barrels, greedy cells, Slimefun barrels) and a second page for fluids. | Left-click takes 1, right-click a stack, shift+click sends to your inventory. Shift+left-click your items (or drop them in the input slot) to store them. Search, sort and page buttons. Buckets/honey bottles go to fluid storage. Each item shows its **total in the network** and a breakdown of where it is kept (DRAM, Quantum Cells, Infinity Barrels, Greedy Buffer, Slimefun barrels); the parts add up to the total, nothing is counted twice. Recovered modules from an old controller appear first as temporary items. |
| **Wireless Terminal** | `mvn_wireless_terminal` · item (Nether Star) | Opens the Network Terminal remotely. | Shift+right-click a Controller or a Terminal to bind it, then right-click in the air. Without a **Network Router** it works only in the same world and within `wireless.local-range-without-router` (64) blocks. Never within 10 s of combat, and only if you may access the network's land. |
| **Network Router** | `mvn_router` · Lightning Rod | Lifts the Wireless Terminal limits for its network: any distance and any world. | Connect it anywhere in the network. |
| **Network Monitor** | `mvn_monitor` · Respawn Anchor | Live diagnostic panel: node counts by type, storage usage, errors. | Right-click; it refreshes while open. |
| **Network Probe** | `mvn_probe` · item (Spyglass) | Answers "is this connected?": network size, controller position, scan errors and protection cuts for the block you click. | Right-click any block. |

### 🧠 Memory: DRAM Bay and modules

| Device | id · block | What it does | How to use it |
|---|---|---|---|
| **DRAM Bay** | `mvn_dram_bay` · Waxed Copper Bulb | Network block that holds **one memory module**. The module is the storage: while installed, its stock is part of the network. Place as many bays as you like. | Right-click an empty bay with a module in hand to install it, or right-click to open its menu: see the stock, install from the cursor or with shift+click, and **Eject Module**. Breaking it (or the Rake) drops the bay and the module **with its whole stock**. |
| **Item memory modules** | `mvn_cache_l1` · `_l2` · `_l3` · `_dram` · `_quantum` · items | Multi-item storage: 2,048 / 8,192 / 32,768 / 131,072 / 524,288 items in total (`virtual-cache.tier-1` … `tier-5`), any mix of types. | Install it in a DRAM Bay. **Taking it out takes all its items with it**: they disappear from that network and appear in the network of the DRAM Bay you install it in. Upgrading a loaded module in a crafting table keeps its items. |
| **Fluid DRAM Module** | `mvn_fluid_dram` · item (Heart of the Sea) | **Fluid-only** module: holds several fluids at once, up to `fluids.dram-capacity-mb` (512,000 mB = 512 buckets) in total. | Like the item modules: it goes in a DRAM Bay, adds its capacity to the network's fluid storage, and taking it out carries its fluids to another network. |

### 📦 Item storage

| Device | id · block | What it does | How to use it |
|---|---|---|---|
| **Quantum Cell T1–T6** | `mvn_cell_t1` … `mvn_cell_t6` · Terracotta (plain, orange, yellow, lime, cyan, purple) | Stores **one item type** each: 65,536 / 262,144 / 1,048,576 / 16,777,216 / 268,435,456 / 2,000,000,000 items. An empty cell takes the first item type that has nowhere else to go. | Connect it. Right-click to see or manage its contents. Breaking it keeps the cargo in the item. Upgrade one tier with the Quantum Workbench or in a crafting table (cell surrounded by 8 diamonds); the cargo is kept. |
| **Infinity Barrel** | `mvn_infinity_barrel` · Barrel | Same as a cell — one item type — with `barrel.capacity` (2,000,000,000), but it **stays registered** to its item when it empties. Items with their own id (Slimefun and other plugins) are recognised by that id and their visible text, so an item you take out always goes back in, even if its name or lore was stored differently. | Right-click: click *Set Item* with an item on the cursor to register it; right-click *Set Item* with an empty cursor clears the registration (only when empty). Hoppers do **not** interact with it (see below). |
| **Greedy Cell** | `mvn_greedy_cell` · Slime Block | With a filter: a **priority sink**. Incoming items matching its filter go to it before any other storage, every cycle it pulls up to 512 more of them from the network, and it pushes up to 256 per cycle into adjacent **non-network** containers (chests, Slimefun machines). Holds several types, up to `greedy.capacity` (262,144) in total. Without a filter: general overflow storage, used only when everything else is full. | Right-click to set the filter and see its buffer. It works as an **internal filter**: an item defined in its filter always keeps **1 unit** inside, whatever takes it out (Pushers, terminal, crafting, its own push to containers). If a Pusher of the network has that item in its **whitelist**, the Greedy Cell releases **all** of it (the last unit too) to the cells and stops taking it while that Pusher exists. The wireless bridge never takes from it. |
| **Quantum Workbench** | `mvn_quantum_workbench` · Brain Coral Block | Upgrades a Quantum Cell T1–T5 to the next tier, keeping its cargo. | Cell in the centre, 8 diamonds around it, press *Entangle & Upgrade*, take the result. |

### 💧 Fluids

Fluids live in their own storage, separate from items: Quantum Fluid Cells and Fluid DRAM Modules
installed in DRAM Bays.

| Device | id · block | What it does | How to use it |
|---|---|---|---|
| **Quantum Fluid Cell** | `mvn_fluid_cell` · Prismarine Bricks | Holds one fluid — Water, Lava, Milk, Powder Snow or Honey — up to `fluids.cell-capacity-mb` (64,000 mB = 64 buckets). All fluid cells of a network form its fluid storage. | Right-click with a filled bucket / honey bottle to pour into that cell; with an empty bucket to fill it (Water, Lava, Milk, Powder Snow). Or use the Terminal's fluid page: deposit buckets/bottles, withdraw with an empty bucket (honey with a glass bottle). |
| **Liquid Pump** | `mvn_liquid_pump` · Blue Stained Glass | Each cycle drains one **source** block of water or lava directly **below** it (1,000 mB). The block is removed only if the whole 1,000 mB fit in the network. | Place it on top of the liquid. Right-click to choose ANY / WATER / LAVA. |

### 🔄 Item transport

| Device | id · block | What it does | How to use it |
|---|---|---|---|
| **Simple Grabber** | `mvn_grabber` · Observer | Imports from adjacent containers into the network: up to 128 items of one type per cycle, through **all six faces**. From a furnace it only takes the result. | Right-click for the filter (whitelist/blacklist). Empty filter = everything. |
| **Advanced Grabber** | `mvn_grabber_ht` · Sticky Piston | Same, ×8 (`transfer.ht-multiplier`): 1,024 per cycle, and it can be restricted to **one face**. | Filter menu + face selector. |
| **Simple Pusher** | `mvn_pusher` · Target | Exports from the network into adjacent containers: up to 128 items per cycle, through **all six faces** (it has no face selector: use an Advanced Pusher for a single side). It never puts anything into another network block. | The filter decides **which items** leave, not where they go. **Empty whitelist = idle** (so a fresh pusher never empties the network); **blacklist = everything except what is listed**: whatever you blacklist stays in the network. |
| **Advanced Pusher** | `mvn_pusher_ht` · Piston | Same, ×8, and with the face selector it delivers **only** to that side. | Filter menu + face selector. For a machine, pick the machine's face. |
| **Network Vacuum** | `mvn_vacuum` · Sponge | Every 10 ticks picks up dropped items within `vacuum.radius` (4 blocks) into the network. | Optional filter. Items that do not fit stay on the ground. |
| **Network Purger** | `mvn_purger` · Magma Block | Deletes up to 128 items per cycle that match its filter. | **Without a filter it deletes nothing**, on purpose. |
| **Network Quota Limiter** | `mvn_limiter` · Target | Caps how much of one item the network may hold. Every deposit (grabbers, vacuum, terminal, crafting results, wireless bridge) stops at the cap. | Right-click: set the target item, the limit and on/off. Several limiters on one item: the lowest wins. |
| **Wireless Transmitter** | `mvn_transmitter` · Conduit | One end of a wireless bridge. If it holds the link to a Receiver, it **pushes** up to 128 items per cycle that pass **its** filter into the receiver's network. | Right-click: filter menu, plus a button that opens its own network's terminal. Link: shift+right-click a placed Transmitter holding a Receiver item, or a placed Receiver holding a Transmitter item. |
| **Wireless Receiver** | `mvn_receiver` · Redstone Lamp | The other end. If it holds the link to a Transmitter, it **pulls** up to 128 items per cycle that pass **its** filter from the transmitter's network into its own. Works across distance and worlds as long as the other end's chunk is loaded. | Right-click: filter menu, plus a button that opens the **remote** network's terminal. **Empty whitelist = nothing crosses**, on purpose. |

Containers that Grabbers, Pushers and Greedy Cells work with: chests, trapped chests, barrels,
hoppers, dispensers, droppers, furnaces, blast furnaces, smokers, brewing stands, chiseled
bookshelves, shulker boxes, and — with Slimefun installed — Slimefun machines (only the slots the
machine declares for input/output). Never another MultiverseNets block.

**How a Pusher delivers** (designed for feeding machines such as a Slimefun Smeltery):

* At most **one stack per slot**. A 128 or 1,024 item cycle used to land in a single slot and
  everything above one stack was lost: that is what emptied whole networks.
* With a **whitelist of several items** (e.g. the three ingredients of an alloy), each item gets its
  share of the target's slots (9 slots / 3 ingredients = 3 each) and they are served in turn within
  the same cycle. The first ingredient no longer floods the machine and locks the recipe out.
* Into a **furnace**, blast furnace or smoker: from above everything goes to the input; from a side,
  fuel goes to the fuel slot and the rest to the input. Never into the result slot.
* What does not fit goes back to the network; nothing is lost.

**Tip for machines that take and return items in the same block** (a Smeltery puts its result in
the dispenser when it has no output chest): add an output chest, point the Advanced Grabber at that
chest and the Advanced Pusher at the dispenser. A grabber facing the dispenser also takes back the
ingredients you just pushed.

**Hoppers:** hoppers (and hopper minecarts) **do not interact with any MultiverseNets block**,
neither to insert nor to extract. A hopper on top of an Infinity Barrel used to be able to delete its
whole stack.

### 🛠️ Crafting

| Device | id · block | What it does | How to use it |
|---|---|---|---|
| **Blueprint** | `mvn_blueprint` · item (Book) | A recipe: full 3×3 grid + result in its PDC. Crafting never consumes it. | Encode it in a Recipe Encoder and install it in a crafter. Installing moves the Blueprint into the crafter; removing it, replacing it or *Clear All* gives it back. |
| **Recipe Encoder** | `mvn_encoder` · Smithing Table | Builds a recipe in a 3×3 template grid (clicking only marks slots, no items are spent) and writes it to a Blueprint (blank or already encoded). | Fill the grid, put a Blueprint in the blue slot, press *Encode*. Clicking an encoded Blueprint loads its recipe into the grid. Blueprints left in its slots stay stored in the block (one viewer at a time sees them). |
| **Slimefun Recipe Encoder** | `mvn_sf_encoder` · Enchanting Table | Same for Slimefun recipes. Blueprints left in its slots also stay stored in the block, so you do not have to bring blank Blueprints every time. | Needs Slimefun; disable with `sf-encoder.enabled`. |
| **Auto-Crafter** | `mvn_crafter` · Crafting Table | Every 20 ticks tries each installed vanilla Blueprint once (up to `crafter.max-recipes`, 18). All-or-nothing: if any ingredient is missing nothing is taken, and if the result does not fit the whole craft is undone. | Right-click, click Blueprints in. Refuses Slimefun Blueprints. |
| **Slimefun Auto-Crafter** | `mvn_sf_crafter` · Crying Obsidian | Same, and it accepts **Slimefun and vanilla Blueprints**: mix both in the same machine. | Disabled entirely (menu and crafting) with `sf-crafter.enabled: false`. |
| **Request Crafter** | `mvn_request_crafter` · Fletching Table | Holds Blueprints that are **only** crafted on demand from a Request Terminal (never automatically). | Install Blueprints like in an Auto-Crafter. |
| **Slimefun Request Crafter** | `mvn_sf_request_crafter` · Purpur Pillar | Same, with Slimefun and vanilla Blueprints. | — |
| **Request Terminal** | `mvn_request_terminal` · Lectern | Lists everything the network's Request Crafters can make and crafts it on demand, resolving chains (logs → planks → crafting table) with the network's stock. | Left-click 1 batch, shift+left-click 10, right-click 64, shift+right-click asks for a number in chat. Toggle delivery to your inventory or to the network. |
| **Network Crafting Grid** | `mvn_crafting_grid` · Cartography Table | A crafting table that pulls ingredients from the network; the template grid is stored in the block. | Right-click, set the grid, craft. |

### 🧰 Tools

| Device | id · block | What it does | How to use it |
|---|---|---|---|
| **Configuration Wrench** | `mvn_configurator` · item (Comparator) | Copies a filter (exact templates, materials and whitelist/blacklist mode) from one device to another. | Shift+right-click a filterable device to copy, right-click another to paste. |
| **Network Rake** | `mvn_rake` · item (Dead Bush) | Dismantles a node instantly and gives it back to you with its state (filter, Blueprints, binding). 250 uses (`rake.uses`). | Right-click a node. It refuses controllers and storage that still holds items or fluid. |

### 🐔 GeneticChickengineering

| Device | id · block | What it does | How to use it |
|---|---|---|---|
| **Genetic Chicken Sorter** | `mvn_chicken_sorter` · Hay Bale | A machine dedicated **only** to the pocket chickens of the GeneticChickengineering addon. It reads their genes and moves only chickens that meet **every** rule; any other item is ignored. Up to 16 chickens per cycle. | Right-click. **Push**: from the network into the block it faces (e.g. a Roost or one of the addon's machines). **Pull**: from that block into the network. Rules: product list (click with a chicken on the cursor or shift+click one in your inventory; empty = any), min/max tier (recessive genes; special species 7-9), minimum DNA strength (0-6), pure genes only (no Aa), known/unknown DNA and age (adult/baby). Face selector. **Starts stopped**: turn it on once it is set up. |

With this addon installed every chicken is a unique item (it carries its own DNA). The network no
longer merges two different chickens into one stack: a chicken could previously come out with
another one's DNA.

Filterable devices: Grabbers, Pushers, Vacuum, Purger, Greedy Cell, Wireless Transmitter and Receiver. All of
them support whitelist and blacklist mode, and in the Grabber/Pusher menus clicking a face button
opens the container on that face.

---

## 🔀 How items move inside and between networks

### 1. What belongs to a network

On every scan (every `network.scan-interval-ticks`, and immediately after placing or breaking a
node) the controller walks through every connected MultiverseNets block, face to face. The scan:

* never loads chunks — nodes in unloaded chunks are simply not part of the network until they load;
* stops at another controller (`foreign controller at x,y,z`);
* stops at land the network's owner cannot use (see [Land protection](#land-protection));
* with Slimefun installed, also walks through Slimefun blocks whose id contains `CABLE` or `BRIDGE`
  and adds touching **Slimefun barrels** to the storage.

There is no per-node "which network am I in" state: the topology is rebuilt from scratch every
scan, so a node can never point to a network that no longer exists.

### 2. The internal storages and their order

A network's item storage is the sum of five internal storages. Every deposit — from a grabber,
the vacuum, the terminal, a crafting result or the wireless bridge — first checks the
**Quota Limiters**, then fills in this order:

| # | Where the items go | Condition |
|---|---|---|
| 1 | **Greedy Cells** | their filter matches, or they already hold the item |
| 2 | **Memory modules** (DRAM Bays) | they already hold that item type |
| 3 | **Slimefun barrels** | they already hold that item type |
| 4 | **Quantum Cells / Infinity Barrels** | they already hold that item type |
| 5 | **Memory modules** (DRAM Bays) | free space, new item type |
| 6 | **Slimefun barrels** | empty |
| 7 | **Quantum Cells / Infinity Barrels** | empty (the cell adopts the item type) |
| 8 | **Greedy Cells without a filter** | general overflow |

Whatever does not fit is returned to whoever deposited it (it is never deleted).

Withdrawals read in this order: memory modules → Quantum Cells / Infinity Barrels → Slimefun
barrels → Greedy Cells. Pushers, terminals, crafting and the API can take from Greedy Cells, but a
Greedy Cell always keeps **1 unit** of every item defined in its filter (its internal filter). A
Pusher that has the item in its **whitelist** releases it instead: the Greedy Cell moves all of it
to the other storages and stops taking it while that Pusher exists. A Greedy Cell's own suction and
the wireless bridge never take from Greedy Cells.

A single withdrawal always returns **one** item type (the first one that matches). A pusher with a
whitelist of several items makes one withdrawal per item, in turn, within the same cycle.

### 3. One cycle, step by step

Every 5 ticks the ticker runs these per network (each on its own configurable interval):

| Interval | Device | Per device |
|---|---|---|
| transfer (5 t) | Grabber / Advanced Grabber | Up to 128 / 1,024 of one type from the first face that yields something. If the network refuses part of it: first offered directly to Pushers whose filter accepts it and, if some is still left, it waits in the grabber's **transit buffer** (the grabber pauses until it clears; the buffer survives breaking the block). It is never handed back to the machine: its input slots would process it again. |
| transfer | Pusher / Advanced Pusher | Only with a container next to it: takes up to 128 / 1,024 items (a multi-item whitelist in turn) and inserts them into the adjacent containers with the rules above; what does not fit goes back to the network, or waits in the pusher's transit buffer if the network is full meanwhile (retried first on the next cycle). |
| transfer | Genetic Chicken Sorter | Up to 16 chickens that meet its rules, in the chosen direction (push/pull). |
| transfer | Greedy Cell | Pulls up to 512 matching items, pushes up to 256 into adjacent non-network containers. |
| transfer | Purger | Deletes up to 128 matching items (only with a filter). |
| transfer | Wireless Receiver / Transmitter | Pulls / pushes up to 128 matching items across the bridge (see below). |
| transfer | Liquid Pump | Drains one source block (1,000 mB), only if it fits whole. |
| vacuum (10 t) | Vacuum | Picks up dropped items within the radius. |
| craft (20 t) | Auto-Crafter / Slimefun Auto-Crafter | Tries each installed Blueprint once. |

Idle grabbers and pushers back off: after finding nothing they only check every third cycle until
something moves again, so a thousand idle devices cost almost nothing.

### 4. Between networks: the wireless bridge

Two networks never merge through the air. The only way items cross is a **Transmitter / Receiver**
link, and the end that holds the link is the one that moves items:

```
 Network A                                         Network B
 [Controller]-[Cells]-[Transmitter]  ~~~~~~~~~~~  [Receiver]-[Cells]-[Controller]

 Receiver linked to the Transmitter:  B pulls from A what passes the RECEIVER's filter
 Transmitter linked to the Receiver:  A pushes into B what passes the TRANSMITTER's filter
```

* **Linking**: shift+right-click a placed Transmitter holding a Receiver item (the receiver pulls),
  or a placed Receiver holding a Transmitter item (the transmitter pushes). Link both for a
  two-way exchange.
* **Filter on the end that moves**: whitelist or blacklist, exact templates or materials. **An
  empty whitelist moves nothing**; an empty blacklist moves everything.
* Up to 128 items of one type per cycle and per linked device. **Greedy Cells are never drained.**
* Works across any distance and across worlds, but only while the other end's chunk is loaded.
* Both ends pass the protection check, each with the owner of its own network.
* If the destination cannot take the items they go back to the source; if the source cannot take
  them back either they drop next to the device that moved them — never into the void.
* The Receiver's menu has a button that opens **A's** terminal, so you can use A's storage by hand
  from B's base (only if you are allowed to access A's land).

<a id="shared-buses"></a>
### 5. Shared buses: two controllers on the same cables

If two controllers end up connected (a cable touching both), each one builds its own network and
both include the cables and devices between them. `/mvnets doctor`, the Probe and the hologram
report it as `foreign controller at x,y,z`. While that lasts:

* **Every shared device still works once per cycle**: it is assigned to the first of the two
  networks (stable order by world and controller position). Previously every shared grabber,
  pusher, purger, pump and crafter worked twice per cycle.
* Shared cells are visible from both terminals.
* Shared DRAM Bays are visible from both networks, like cells. A module recovered from an old controller
  waits in that network's Terminal.

The clean fix is to keep one controller per network, and use a Transmitter/Receiver pair if you
really want two networks to exchange items.

### 6. Fluids

Fluid storage is separate: Quantum Fluid Cells and Fluid DRAM Modules (in DRAM Bays) hold fluids, and
only the Liquid Pump, the Terminal's fluid page, the Terminal's input slot and direct right-clicks on
a fluid cell move them. A deposit fills cells that already hold that fluid first, then Fluid DRAMs,
then empty cells.
Deposits are **all-or-nothing**: a bucket, bottle or source block is consumed only if its whole
volume fits, so a nearly full network can never keep both the fluid and the bucket.

### 7. Nothing is lost, nothing is duplicated

* Breaking a node stores its state in the dropped item (cell cargo, filters, face, transit buffer,
  Blueprints, crafting grid, receiver link, limiter, fluid, pump filter, chicken sorter rules) and
  placing it restores it. The Rake does the same. A DRAM Bay drops its module separately, with the
  whole stock inside.
* Pushers never leave more than one stack in a slot and never put anything into another network
  node.
* Hoppers cannot touch any device.
* All GUIs have anti-dupe guards and return anything left in real slots when closed.
* Crafting is all-or-nothing, including when only part of the result fits.
* Nodes are immune to pistons and explosions.

---

<a id="land-protection"></a>
## 🛡️ Land protection

Supported: ProtectionStones, WorldGuard, Lands, Towny, GriefPrevention (plus BentoBox islands for
player access).

* The **Controller remembers who placed it**; that player is the network's owner. Controllers from
  before this existed adopt the first player allowed to open them.
* The scan does not extend the network into land its owner cannot use, and **every block a network
  reads from or writes to is checked** — grabbers, pushers, overflow to pushers, greedy cells,
  vacuum, pump and both ends of a wireless bridge, in either direction.
* Players cannot open devices on protected land they do not own (`multiversenets.protection.bypass`
  and `multiversenets.admin` skip it). That includes opening a remote network through a Receiver.
* Escape hatches: `protection.exempt-worlds`, `protection.exempt-locations`, and
  `protection.block-network-linking: false`.

## ⌨️ Commands

| Command | Description | Permission |
|---|---|---|
| `/mvnets help` | Command list | `multiversenets.use` |
| `/mvnets info` | Version | `multiversenets.use` |
| `/mvnets guide [en\|es]` | Opens the guide menu: every device with its recipe, what it does and how to use it, plus how networks work. English/Spanish toggle inside | `multiversenets.use` |
| `/mvnets devices` | List all device ids | `multiversenets.use` |
| `/mvnets give <id> [n]` | Give a device | `multiversenets.admin` |
| `/mvnets doctor` | Rescan and diagnose all networks (also says whether the Slimefun integration is active) | `multiversenets.admin` |
| `/mvnets stats` | Global statistics | `multiversenets.admin` |
| `/mvnets inspect` | Inspect the block you are looking at (type, network, contents, filter) | `multiversenets.admin` |
| `/mvnets repair` | Force a rescan of the network you are looking at | `multiversenets.admin` |
| `/mvnets recipes` | Re-register and re-unlock all crafting recipes | `multiversenets.admin` |
| `/mvnets reload` | Reload configuration, protection providers and recipes | `multiversenets.admin` |

<a id="configuration"></a>
## ⚙️ Configuration

The most relevant keys of `config.yml` (every key is documented in the file itself, in English and
Spanish):

| Key | Default | Effect |
|---|---|---|
| `network.scan-interval-ticks` | 20 | Topology rescan interval |
| `network.max-nodes` | 16384 | Maximum nodes per network |
| `network.max-nodes-per-chunk` | 64 | Maximum devices per chunk |
| `network.op-interval-ticks.transfer` / `vacuum` / `craft` | 5 / 10 / 20 | Intervals of each family of operations |
| `transfer.items-per-op` | 128 | Items per Grabber/Pusher/Purger/Receiver operation |
| `transfer.ht-multiplier` | 8 | Multiplier for Advanced Grabbers/Pushers |
| `cells.capacities` | 65,536 … 2,000,000,000 | Capacity per cell tier |
| `virtual-cache.tier-1` … `tier-5` | 2,048 … 524,288 | Capacity of each item memory module tier |
| `greedy.capacity` / `barrel.capacity` | 262,144 / 2,000,000,000 | Greedy Cell / Infinity Barrel capacity |
| `fluids.cell-capacity-mb` | 64,000 | Fluid cell capacity |
| `fluids.dram-capacity-mb` | 512,000 | Total capacity of a Fluid DRAM Module |
| `vacuum.radius` | 4.0 | Vacuum pickup radius |
| `crafter.max-recipes` | 18 | Blueprints per crafter |
| `wireless.local-range-without-router` | 64 | Wireless Terminal range without a Router |
| `wireless.combat-cooldown-seconds` | 10 | Wireless Terminal combat lock |
| `compat.slimefun` | true | Slimefun machines, cables and barrels integration |
| `sf-encoder.enabled` / `sf-crafter.enabled` | true | Slimefun encoder / crafters |
| `blocked-worlds` | [] | Worlds where devices cannot be placed |
| `protection.*` | — | See [Land protection](#land-protection) |

## 🔗 Slimefun integration (optional)

MultiverseNets **does not depend on Slimefun**. If it is installed it is detected at startup
(both the DrakesCraft fork and the original) and, with `compat.slimefun: true`:

* Grabbers, Pushers, Greedy Cells and crafters work with **Slimefun machines** like with a chest,
  using only the slots the machine declares for input and output.
* Slimefun blocks whose id contains `CABLE` or `BRIDGE` conduct a MultiverseNets network, and
  Slimefun barrels touching it become part of its storage.
* Slimefun encoder and crafters become usable.

Without Slimefun the bridge stays inert and nothing else changes. `/mvnets doctor` says on its first
line whether the integration is active.

## 🤝 Coexistence with Networks

**Both plugins can be installed at the same time.** Their identifiers never collide:

| | MultiverseNets | NetworksV6-Drake |
|---|---|---|
| Plugin name | `MultiverseNets` | `NetworksV6-Drake` |
| Main class | `com.chagui68.multiversenets.…` | `io.github.sefiraat.networks.…` |
| Command | `/mvnets` | `/networks` |
| Permissions | `multiversenets.*` | `networks.*` |
| Items | own, via PDC, with vanilla recipes | Slimefun's (`NTW_*`) |

Networks registers no vanilla recipes (theirs go through the Slimefun crafting table), so the 46
recipes here do not clash either. Five tests in `NetworksCoexistenceTest` pin the identifiers down,
because what breaks coexistence is an identifier renamed by accident, not the code.

**Interactions to keep in mind** with the Slimefun integration active: a MultiverseNets Grabber can
pull from a Networks block (it is a Slimefun item with its own menu), and a Networks block whose id
contains `CABLE` or `BRIDGE` (e.g. `NTW_BRIDGE`) conducts a MultiverseNets network. If you prefer
each network to stick to its own blocks:

```yaml
compat:
  slimefun: false
```

## 🔍 How it differs from Networks

| | Networks (Slimefun addon) | MultiverseNets |
|---|---|---|
| Dependencies | Slimefun + its chain | None, only the Paper API |
| Network membership | Each node stores its root | Recalculated by BFS from the controller |
| Orphan nodes | Possible | **Structurally impossible**: every scan rebuilds the topology |
| Diagnosis | Added later (`/networks doctor`) | `/mvnets doctor`, Probe, Monitor, hologram |
| Ticker | Slimefun's cycle | Own, with per-operation intervals in the config |

The price is that scanning costs one BFS over up to `max-nodes` blocks every
`scan-interval-ticks` — predictable, bounded work in exchange for no state that can be corrupted.

## 🛠️ Building

```bash
mvn clean package
```

That builds the release jar (1.21.11 API, Java 21 bytecode, runs on 1.21.11, 26.1 and 26.2). To
check the code against the newer APIs (needs JDK 25):

```bash
mvn -P api-26.1 clean compile
mvn -P api-26.2 clean compile
```

The jar is generated at `target/MultiverseNets-v<version>.jar`.

## 📋 Compatibility

| Parameter | Requirement |
|---|---|
| **Server** | Paper / Purpur **1.21.11**, **26.1** (26.1 – 26.1.2) and **26.2** — the same jar for all three |
| **Java** | The one your server needs: Java 21 on 1.21.11, Java 25 on 26.1 and 26.2 |

How each version is checked before a release (`.github/workflows/verify.yml`):

| Version | Check |
|---|---|
| 1.21.11 | The jar is built against this API and the whole test suite runs on it. |
| 26.1 | The same sources compile against the Paper 26.1.2 API (`mvn -P api-26.1 clean compile`, JDK 25). |
| 26.2 | The same sources compile against the Paper 26.2 API (`mvn -P api-26.2 clean compile`, JDK 25). |

The test server (MockBukkit) only exists for 1.21, so on 26.x the check is that every call the
plugin makes exists there; nothing it uses is marked for removal in those versions.
| **Dependencies** | None (Slimefun and protection plugins are optional) |

> **Not Folia-compatible.** The network ticker, the topology scan and every GUI run on Paper's
> global scheduler and assume the main thread owns the blocks they touch. Install it on Paper or
> Purpur.

---

## 📄 License & Sovereign Authorship

Copyright © 2026 [**Chagui68**](https://github.com/Chagui68) · [**DrakesCraft Labs**](https://github.com/DrakesCraft-Labs).

This project is an **original sovereign creation** engineered by **Chagui68** for the DrakesCraft network. All intellectual authorship belongs to Chagui68. Commercial resale, repackaging in paid setups, or removing creator attribution is strictly prohibited.
