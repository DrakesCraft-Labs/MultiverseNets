# 📜 Recipes and functions of MultiverseNets items

Every item in the plugin, its **crafting recipe** as you see it on the crafting table (3×3 grid),
and what it **does** in the network. Numbers are the `config.yml` defaults; "cycle" means one
transfer cycle (`network.op-interval-ticks.transfer`, 5 ticks).

> In the grids, `·` marks an empty slot. Every item can also be given with `/mvnets give <id>`.
> Back to the [wiki index](README.md).

> **Devices as ingredients.** When a recipe asks for another device (previous cell, previous
> module, cable, fluid cell, Advanced Pusher) it must be that device: the plain material underneath
> (terracotta, copper ingot, glass, prismarine bricks, piston) does not count. Cell and module
> upgrades keep the cargo of the ingredient; every other recipe refuses a device that still stores
> something. No device can be used as an ingredient of a vanilla recipe.

---

## 🖥️ Core and access

### Network Controller · `mvn_controller`

```
I I I
I N I
I I I
```

> I = **Iron Block** · N = **Nether Star**

- **Result**: 1× Network Controller (Lodestone)
- **Function**: Root of the network. Every scan starts here and walks through every connected
  MultiverseNets block. Whoever places it becomes the network's **owner** (used by land protection).
  Shows a floating hologram with status, node count and stored totals. Memory modules go in a
  **DRAM Bay**, not in the controller; a controller that still holds an old module keeps using it, and
  its menu can take that module out with its items. Right-click opens its status menu. Only one
  controller per network: a second one wired to the same cables is reported as `foreign controller`.

---

### Network Cable (×16) · `mvn_cable`

```
G G G
G R G
G G G
```

> G = **Glass** · R = **Redstone**

- **Result**: 16× Network Cable (Glass)
- **Function**: Connects devices. Every MultiverseNets block conducts, cables are just the cheap way
  to cover distance. Right-click a cable to see whether it reaches a controller and how big the
  network is.

---

### Network Terminal · `mvn_terminal`

```
G E G
E B E
G E G
```

> G = **Glass** · E = **Ender Pearl** · B = **Beacon**

- **Result**: 1× Network Terminal (Beacon)
- **Function**: The storage grid. Shows every item in the network and has a second page for fluids.
  Left-click takes 1, right-click a stack, shift+click sends to your inventory; shift+left-click your
  own items (or leave them in the input slot) to store them. Buckets and honey bottles go to fluid
  storage. Search, sort and page buttons.

---

### Wireless Terminal · `mvn_wireless_terminal`

```
· P ·
P N P
· C ·
```

> P = **Ender Pearl** · N = **Nether Star** · C = **Compass**

- **Result**: 1× Wireless Terminal (hand item)
- **Function**: Opens a network's terminal remotely. Shift+right-click a Controller or a Terminal to
  bind it, then right-click in the air. Without a **Network Router** it only works in the same world
  and within 64 blocks (`wireless.local-range-without-router`). It is locked for 10 s after combat and
  checks that you may access the network's land.

---

### Network Router · `mvn_router`

```
· L ·
· C ·
· R ·
```

> L = **Lightning Rod** · C = **Network Cable** · R = **Redstone Block**

- **Result**: 1× Network Router (Lightning Rod)
- **Function**: Connected anywhere in a network, it lifts the Wireless Terminal limits for that
  network: any distance and any world.

---

### Network Monitor · `mvn_monitor`

```
G G G
G C G
G G G
```

> G = **Glass Pane** · C = **Comparator**

- **Result**: 1× Network Monitor (Respawn Anchor)
- **Function**: Live diagnostic panel: node counts by type, storage usage and scan errors. It refreshes
  while open.

---

### Network Probe · `mvn_probe`

```
· A ·
A S A
· A ·
```

> A = **Amethyst Shard** · S = **Spyglass**

- **Result**: 1× Network Probe (hand item)
- **Function**: Right-click any block (node or not) to see which network it belongs to, how many nodes
  it has, where its controller is, and any scan warning — including links cut by land protection.

---

### DRAM Bay · `mvn_dram_bay`

```
I Q I
R C R
I Q I
```

> I = **Iron Ingot** · Q = **Quartz** · R = **Redstone** · C = **Copper Block**

- **Result**: 1× DRAM Bay (Waxed Copper Bulb)
- **Function**: Network block that holds **one memory module** (any item module below, or the Fluid
  DRAM). While installed, the module's stock is part of the network. Right-click an empty bay with a
  module in hand to install it, or right-click to open its menu (stock, install from the cursor or
  with shift+click, **Eject Module**). An ejected module keeps its whole stock: install it in a DRAM
  Bay of another network and the stock appears there and is gone from the first one. Breaking the bay
  drops it and its module (with the stock) separately.

---

### Memory Modules · `mvn_cache_l1` … `mvn_cache_quantum`

Item modules for the **DRAM Bay**: multi-item storage, any mix of item types, capacity per tier in
`virtual-cache.tier-1` … `tier-5`. A module keeps its items when it is taken out of the bay. Each
tier is crafted from the previous one; upgrading a module that holds items keeps them.

#### L1 CPU Cache Module — 2,048 items

```
C R C
R C R
C R C
```

> C = **Copper Ingot** · R = **Redstone Dust**

#### L2 CPU Cache Module — 8,192 items

```
G L G
L P L
G L G
```

> G = **Gold Ingot** · L = **Lapis Lazuli** · P = **L1 CPU Cache Module**

#### L3 CPU Cache Module — 32,768 items

```
D A D
A P A
D A D
```

> D = **Diamond** · A = **Amethyst Shard** · P = **L2 CPU Cache Module**

#### DRAM Memory Module — 131,072 items

```
N E N
E P E
N E N
```

> N = **Netherite Ingot** · E = **Eye of Ender** · P = **L3 CPU Cache Module**

#### Quantum Cache Matrix — 524,288 items

```
N S N
S P S
N S N
```

> N = **Netherite Block** · S = **Nether Star** · P = **DRAM Memory Module**

---

### Fluid DRAM Module · `mvn_fluid_dram`

```
D B D
F E F
D B D
```

> D = **Diamond** · B = **Bucket** · E = **Eye of Ender** · F = **Quantum Fluid Cell** (empty)

- **Result**: 1× Fluid DRAM Module (hand item, Heart of the Sea)
- **Function**: **Fluid-only** memory module for the DRAM Bay. It holds several fluids at once, up to
  `fluids.dram-capacity-mb` (512,000 mB = 512 buckets) in total, and adds that to the network's fluid
  storage. Like the item modules, taking it out of the bay carries its fluids to whichever network
  you install it in next.

---

## 📦 Item storage

### Quantum Cell T1 – T6 · `mvn_cell_t1` … `mvn_cell_t6`

**T1:**

```
G G G
G D G
G G G
```

> G = **Glass** · D = **Diamond**

**Tn+1 (n ≥ 1):** the previous cell in the centre, surrounded by diamonds.

```
D D D
D P D
D D D
```

> D = **Diamond** · P = **Previous cell** (with or without cargo)

- **Result**: 1× cell of the next tier (terracotta: plain, orange, yellow, lime, cyan, purple)
- **Function**: Stores **one item type** each. An empty cell adopts the first item type that has
  nowhere else to go. Right-click to see or manage its contents; breaking it keeps the cargo inside
  the item. A cell **with cargo** is upgraded keeping that cargo (in the crafting table or the Quantum
  Workbench). Capacities (`cells.capacities`):

| Tier | Capacity |
|---|---|
| T1 | 65,536 |
| T2 | 262,144 |
| T3 | 1,048,576 |
| T4 | 16,777,216 |
| T5 | 268,435,456 |
| T6 | 2,000,000,000 |

---

### Infinity Barrel · `mvn_infinity_barrel`

```
N D N
D B D
N D N
```

> N = **Netherite Ingot** · D = **Diamond Block** · B = **Barrel**

- **Result**: 1× Infinity Barrel (Barrel)
- **Function**: Like a cell, one item type, with `barrel.capacity` (2,000,000,000). It **stays
  registered** to its item when it empties. In its menu, click *Set Item* with an item on the cursor
  to register it; right-click *Set Item* with an empty cursor clears the registration (only when it
  is empty). Items with their own id (Slimefun and other plugins) are recognised by that id and
  their visible text, so whatever you take out goes back in. Hoppers do not interact with it.

---

### Greedy Cell · `mvn_greedy_cell`

```
G H G
H S H
G H G
```

> G = **Gold Ingot** · H = **Hopper** · S = **Slime Block**

- **Result**: 1× Greedy Cell (Slime Block)
- **Function**: Multi-item buffer up to `greedy.capacity` (262,144) in total.
  - **With a filter** it is a priority sink: items entering the network that match go to it first;
    every cycle it pulls up to 512 more from the network and pushes up to 256 into adjacent
    **non-network** containers (chests, Slimefun machines). Ideal for feeding a machine line.
  - **Without a filter** it is general overflow storage, used only when everything else is full.
  - **Internal filter**: Pushers, terminals and crafting can take from it, but an item defined in
    its filter always keeps 1 unit inside. A Pusher with that item in its whitelist releases it: the
    Greedy Cell moves all of it to the cells and stops taking it. The wireless bridge never takes
    from it.

---

### Quantum Workbench · `mvn_quantum_workbench`

```
D D D
D C D
D D D
```

> D = **Diamond** · C = **Crafting Table**

- **Result**: 1× Quantum Workbench (Brain Coral Block)
- **Function**: Upgrades a Quantum Cell T1–T5 to the next tier keeping its cargo. Place the cell in
  the centre, 8 diamonds around it, press *Entangle & Upgrade* and take the result. Ingredients left
  in the grid are returned when you close the menu.

---

## 💧 Fluids

### Quantum Fluid Cell · `mvn_fluid_cell`

```
G B G
G L G
G G G
```

> G = **Glass** · B = **Bucket** · L = **Lapis Block**

- **Result**: 1× Quantum Fluid Cell (Prismarine Bricks)
- **Function**: Holds one fluid — Water, Lava, Milk, Powder Snow or Honey — up to
  `fluids.cell-capacity-mb` (64,000 mB = 64 buckets). All fluid cells of a network form its fluid
  storage. Right-click it with a filled bucket / honey bottle to pour, with an empty bucket to fill
  (Water, Lava, Milk, Powder Snow). Its menu shows the level, extracts one bucket, and has a *Void
  Fluid Tank* button (shift+right-click to confirm) that empties the cell permanently.

---

### Liquid Pump · `mvn_liquid_pump`

```
· G ·
P B P
· R ·
```

> G = **Blue Stained Glass** · P = **Piston** · B = **Bucket** · R = **Redstone**

- **Result**: 1× Liquid Pump (Blue Stained Glass)
- **Function**: Each cycle drains one **source** block of water or lava directly **below** it
  (1,000 mB) into the network's fluid cells. The source is removed only if the whole 1,000 mB fit.
  Right-click to choose ANY / WATER / LAVA.

---

## 🔄 Item transport

### Simple Grabber (Importer) · `mvn_grabber`

```
I O I
O R O
I O I
```

> I = **Iron Ingot** · O = **Observer** · R = **Redstone Block**

- **Result**: 1× Simple Grabber (Observer)
- **Function**: Imports from adjacent containers into the network: up to 128 items of one type per
  cycle, from all six faces. Whitelist/blacklist filter; an empty filter imports everything. If the
  network refuses part of it, the overflow goes first to Pushers that accept it, then back to the
  source, and only then waits in the grabber's transit buffer (kept even if you break it).

---

### Advanced Grabber · `mvn_grabber_ht`

```
O P O
```

> O = **Observer** · P = **Sticky Piston**

- **Result**: 1× Advanced Grabber (Sticky Piston)
- **Function**: Same as the Simple Grabber ×8 (`transfer.ht-multiplier`): 1,024 items per cycle. Its
  menu can restrict it to **one face**.

---

### Simple Pusher (Exporter) · `mvn_pusher`

```
I D I
D R D
I D I
```

> I = **Iron Ingot** · D = **Dropper** · R = **Redstone Block**

- **Result**: 1× Simple Pusher (Target)
- **Function**: Exports from the network into adjacent containers through **all six faces** (it has
  no face selector): up to 128 items per cycle, only when a container is next to it, never into
  another network block. **An empty whitelist does nothing** (a fresh pusher never empties the
  network); a **blacklist exports everything except what is listed** (the listed items stay in the
  network). At most one stack per slot; a whitelist of several items gives each item its share of
  the target's slots and serves them in turn, so one ingredient cannot fill a machine. Furnaces get
  fuel in the fuel slot and the rest in the input, never in the result slot. What does not fit goes
  back to the network.

---

### Advanced Pusher · `mvn_pusher_ht`

```
D P D
```

> D = **Dropper** · P = **Piston**

- **Result**: 1× Advanced Pusher (Piston)
- **Function**: Same as the Simple Pusher ×8 (1,024 per cycle). With a face selected it delivers
  **only** to that side; use it to feed one machine.

---

### Network Vacuum · `mvn_vacuum`

```
S R S
R H R
S R S
```

> S = **String** · R = **Redstone** · H = **Hopper**

- **Result**: 1× Network Vacuum (Sponge)
- **Function**: Every 10 ticks picks up dropped items within `vacuum.radius` (4 blocks) into the
  network. Optional filter. Items that do not fit stay on the ground.

---

### Network Purger · `mvn_purger`

```
I L I
L H L
I L I
```

> I = **Iron Ingot** · L = **Magma Block** · H = **Hopper**

- **Result**: 1× Network Purger (Magma Block)
- **Function**: Deletes up to 128 items per cycle that match its filter, so waste (gravel, seeds…)
  never jams the network. **Without a filter it deletes nothing**, on purpose.

---

### Network Quota Limiter · `mvn_limiter`

```
R C R
C T C
R C R
```

> R = **Redstone** · C = **Comparator** · T = **Target**

- **Result**: 1× Network Quota Limiter (Target)
- **Function**: Caps how much of one item the network may hold. Every deposit stops at the cap:
  grabbers, vacuum, terminal, crafting results and the wireless bridge. In its menu: click with an
  item to set the target, ±1/10/64/1,000 buttons or a chat prompt for the limit, and an on/off toggle.
  Several limiters on the same item: the lowest wins.

---

### Wireless Transmitter · `mvn_transmitter`

```
I R I
R C R
I R I
```

> I = **Iron Ingot** · R = **Redstone Block** · C = **Conduit**

- **Result**: 1× Wireless Transmitter (Conduit)
- **Function**: One end of a wireless bridge between two networks. Shift+right-click a placed
  Transmitter with a Receiver item to make that **receiver pull** from this network. Or link the other
  way: shift+right-click a placed Receiver with a Transmitter item, and this transmitter **pushes** up
  to 128 items per cycle that pass its filter into the receiver's network. Its menu is a filter menu
  with a button that opens its own network's terminal.

---

### Wireless Receiver · `mvn_receiver`

```
I P I
P L P
I P I
```

> I = **Iron Ingot** · P = **Ender Pearl** · L = **Redstone Lamp**

- **Result**: 1× Wireless Receiver (Redstone Lamp)
- **Function**: The other end of the bridge. When linked to a Transmitter it **pulls** up to 128 items
  per cycle that pass **its** filter from the transmitter's network into its own, across any distance
  and world while the other end's chunk is loaded. **An empty whitelist moves nothing**, on purpose;
  an empty blacklist moves everything. Greedy Cells are never drained. Its menu has a button that
  opens the remote network's terminal (if you may access that land).

---

## 🛠️ Crafting

### Blank Blueprint (×4) · `mvn_blueprint`

```
P P P
P B P
P P P
```

> P = **Paper** · B = **Blue Dye**

- **Result**: 4× Blueprint (Book)
- **Function**: Carries a recipe (3×3 grid + result) once written by a Recipe Encoder. Crafting never
  consumes it. Installing it moves it into the crafter; removing it, replacing it or *Clear All*
  gives it back.

---

### Recipe Encoder · `mvn_encoder`

```
K P K
P S P
K P K
```

> K = **Ink Sac** · P = **Paper** · S = **Smithing Table**

- **Result**: 1× Recipe Encoder (Smithing Table)
- **Function**: Build the recipe in its persistent 3×3 template grid (clicking only marks slots, no
  items are spent), put a Blueprint (blank or already encoded) in the blue slot and press *Encode*.
  Clicking an encoded Blueprint loads its recipe back into the grid. Blueprints left in its slots
  stay stored in the block; only one player at a time sees them.

---

### Slimefun Recipe Encoder · `mvn_sf_encoder`

```
E P E
P B P
E P E
```

> E = **Ender Pearl** · P = **Paper** · B = **Enchanting Table**

- **Result**: 1× Slimefun Recipe Encoder (Enchanting Table)
- **Function**: Same as the Recipe Encoder for Slimefun recipes. Blueprints left in its slots stay
  stored in the block when you close it. Needs Slimefun. Its recipe and menu exist only while
  `sf-encoder.enabled: true`.

---

### Auto-Crafter · `mvn_crafter`

```
R C R
I T I
R C R
```

> R = **Redstone** · C = **Crafting Table** · I = **Iron Ingot** · T = **Target**

- **Result**: 1× Auto-Crafter (Crafting Table)
- **Function**: Holds up to 18 vanilla Blueprints (`crafter.max-recipes`) and every 20 ticks tries
  each one once with the network's stock. All-or-nothing: if any ingredient is missing nothing is
  taken, and if the result does not fit the whole craft is undone. Slimefun Blueprints are refused
  (use the Slimefun Auto-Crafter).

---

### Slimefun Auto-Crafter · `mvn_sf_crafter`

```
R C R
I T I
R C R
```

> R = **Ender Pearl** · C = **Crying Obsidian** · I = **Iron Ingot** · T = **Target**

- **Result**: 1× Slimefun Auto-Crafter (Crying Obsidian)
- **Function**: Same as the Auto-Crafter and accepts **both Slimefun and vanilla Blueprints**.
  `sf-crafter.enabled: false` removes its recipe, its menu and its crafting.

---

### Request Crafter · `mvn_request_crafter`

```
R C R
I L I
R C R
```

> R = **Redstone** · C = **Crafting Table** · I = **Iron Ingot** · L = **Lectern**

- **Result**: 1× Request Crafter (Fletching Table)
- **Function**: Holds vanilla Blueprints that are **only** crafted on demand from a Request Terminal,
  never automatically.

---

### Slimefun Request Crafter · `mvn_sf_request_crafter`

```
R C R
I L I
R C R
```

> R = **Ender Pearl** · C = **Purpur Pillar** · I = **Iron Ingot** · L = **Lectern**

- **Result**: 1× Slimefun Request Crafter (Purpur Pillar)
- **Function**: Same as the Request Crafter, with Slimefun and vanilla Blueprints. Depends on
  `sf-crafter.enabled`.

---

### Request Terminal · `mvn_request_terminal`

```
G L G
R C R
G G G
```

> G = **Glass** · L = **Lectern** · C = **Crafting Table** · R = **Redstone**

- **Result**: 1× Request Terminal (Lectern)
- **Function**: Lists everything the network's Request Crafters can make and crafts it on demand,
  resolving chains with the network's stock (e.g. logs → planks → crafting table). Left-click 1
  batch, shift+left-click 10, right-click 64, shift+right-click asks for an exact number in chat
  (non-numeric or ≤ 0 cancels). A button toggles delivery to your inventory or to the network.
  Auto-Crafters are not listed here.

---

### Network Crafting Grid · `mvn_crafting_grid`

```
C R C
R G R
C R C
```

> C = **Crafting Table** · R = **Redstone** · G = **Cartography Table**

- **Result**: 1× Network Crafting Grid (Cartography Table)
- **Function**: A crafting table that pulls ingredients from the network transactionally. The
  template grid is stored in the block; *Craft 1* / *Craft All* hand you the result.

---

## 🐔 GeneticChickengineering

### Genetic Chicken Sorter · `mvn_chicken_sorter`

```
F E F
C P C
F E F
```

> F = **Feather** · E = **Egg** · C = **Comparator** · P = **Advanced Pusher**

- **Result**: 1× Genetic Chicken Sorter (Hay Bale)
- **Function**: Moves **only** the pocket chickens of the GeneticChickengineering addon, chosen by
  their genes; every other item is ignored. **Push** sends matching chickens from the network to the
  block it faces; **Pull** brings them from that block into the network (up to 16 per cycle). Rules,
  all of which must pass: accepted products (add one by clicking with a chicken on the cursor or
  shift-clicking one in your inventory; empty list = any), min/max tier, minimum DNA strength, pure
  genes only, known/unknown DNA, adult/baby. It starts **stopped** so it cannot empty a network
  before you configure it.

---

## 🧰 Tools

### Configuration Wrench · `mvn_configurator`

```
I · I
· C ·
· I ·
```

> I = **Iron Ingot** · C = **Comparator**

- **Result**: 1× Configuration Wrench (hand item)
- **Function**: Shift+right-click a filterable device to copy its filter (exact templates, materials
  and whitelist/blacklist mode); right-click another one to paste it, replacing its filter.

---

### Network Rake · `mvn_rake`

```
D · D
· S ·
· S ·
```

> D = **Dead Bush** · S = **Stick**

- **Result**: 1× Network Rake (hand item, 250 uses — `rake.uses`)
- **Function**: Dismantles a node instantly and gives it back to you with its state (filter,
  Blueprints, link…). It refuses controllers and storage that still holds items or fluid.
