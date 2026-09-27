# 🌌 MultiverseNets (English)

<div align="center">

<img src="../docs/banner-en.svg" alt="MultiverseNets" width="100%"/>

</div>

**Standalone digital logistics networks and massive storage for Paper — no Slimefun.**

> Wiki index: [README](README.md) · [Recipes & functions](Recipes.md) · **Development:** [Structure](dev/Structure.md) · [Code](dev/Code.md) · [Tests](dev/Tests.md)

---

## 📖 What is MultiverseNets?

**MultiverseNets** implements the full logic of a Networks-style logistics network, but **100% standalone**: no Slimefun and no other dependency. Everything works with the native Paper API using custom items (PDC), chunk-tagged blocks, and persistent virtual storage.

## ⚙️ Implemented systems

### 🖥️ Network
* **Network Controller**: the heart of the network; indexes nodes via BFS through cables.
* **Network Cable**: carries the signal between nodes.
* **Network Terminal** (block) and **Wireless Terminal** (item bindable with shift+click to the controller).
* **Network Monitor**: diagnostic panel with a breakdown of nodes, storage, and status.
* **Wireless Transmitter / Receiver**: bind a receiver (shift+click on the transmitter holding the item) and place it in another base or dimension. The receiver **opens the remote network's terminal** and, if you give it a filter, **bridges items** from the transmitter's network to its own every cycle (without a filter it crosses nothing, by design).

### 📦 Quantum storage
* **Cells T1–T6**: each cell stores a single item type up to its capacity (65k → 2,000M configurable).
* **Quantum Fluid Cell & Network Fluid Storage**: Stores fluids (Water, Lava, Milk, Honey, Powder Snow) in quantum capacity (default 64,000 mB / 64 buckets). Liquid levels are aggregated into network fluid storage.
* **Greedy Cell**: a smart buffer that claims its filtered item from the network and feeds it to adjacent containers (ideal for continuous lines).
* **Network Terminal**: Redesigned Terminal GUI with a 3rd toggle button (slot 35) for fluid storage. Deposit fluid buckets/bottles or withdraw fluids using matching empty containers (Bucket for Water/Lava/Milk/Powder Snow; Glass Bottle for Honey).
* The network's storage is the aggregate of all connected cells.
* Atomic per-chunk persistence (Paper region data), anti-dupe in all flows.

### 🔄 Transport
* **Importer (Grabber)**: extracts from adjacent containers into the network, with whitelist filter.
* **Exporter (Pusher)**: inserts from the network into adjacent containers, with filter.
* **HT (High-Throughput) variants**: fast versions ×8 (configurable) for massive factories.
* **Liquid Pump**: Dark blue stained glass device that extracts liquid source blocks (strictly Water and Lava) from the block directly below (`BlockFace.DOWN`) into network fluid storage.
* **Vacuum**: picks up ground items within a configurable radius, now with an optional whitelist filter.

### 🛠️ Auto-crafting
* **Auto-Crafter**: accepts **Blueprints** (real 3×3 grid) and result-based recipes (legacy mode). Each blueprint is attempted once per cycle with **atomic extraction**: either there are ingredients for everything or nothing is touched.
* **Request Crafter**: dedicated on-demand crafting node managed exclusively by Request Terminals. Blueprints placed here are not auto-crafted periodically, keeping manual crafting orders clean and isolated.
* **Request Terminal**: on-demand crafting console linked strictly to Request Crafters across the network. Resolves recursive chained dependencies (e.g., Oak Logs -> Planks -> Crafting Table). Left-click crafts 1x, right-click crafts 64x, and shift+right-click prompts in chat to specify an exact custom quantity with strict numeric validation.
* **Recipe Encoder**: builds the recipe in a persistent 3×3 template grid (click to fix slots, without spending items) and encodes a blank Blueprint with one click.
* **Slimefun Recipe Encoder**: Dedicated encoder for Slimefun recipes (toggleable via `sf-encoder.enabled`).
* **Network Quota Limiter**: Regulates maximum stock allowed in network storage for a specified target item.
* **Blueprints**: reusable plans that carry the full recipe (grid + result) in their PDC; they are installed in an Auto-Crafter with a click and are not consumed.
* **Crafting Grid**: manual crafting pulling from the network: the template grid is saved in the block, and each craft withdraws ingredients from the network transactionally.

### 🧰 Tools (brought over from NetworksV6)
* **Configuration Wrench**: shift+click on a device with a filter **copies** its configuration; normal click **pastes** it onto another.
* **Network Rake**: removes nodes instantly (250 uses by default, `rake.uses`); does not touch controllers or loaded cells.
* Filters with **whitelist/blacklist mode** on any device with a filter (grabbers, pushers, vacuum, purger, greedy cell, receiver).

### 🛡️ Reliability
* Protection against pistons and explosions on nodes.
* When you break a node, its state travels inside the item (like in Networks): cell cargo, filters, blueprints, grid matrix, and receiver binding. When you place it again, it is as it was.
* Networks-style anti-dupe guards in all menus (no double-click, no drags over painted slots, no shift+right-click into the void) and **recovery of anything left in the real slots on close**.
* `/mvnets doctor` rescans and diagnoses all networks; `/mvnets inspect` and `/mvnets repair` inspect and rescan the block you are looking at.

## 🍳 Recipes (crafting grid)

Each device is crafted on a standard 3×3 crafting table. `·` marks an empty slot.

| Device | Grid (3×3) | Ingredients |
|---|---|---|
| Controller | <pre>I I I<br/>I N I<br/>I I I</pre> | I = Iron Block · N = Nether Star |
| Cable ×16 | <pre>G G G<br/>G R G<br/>G G G</pre> | G = Glass · R = Redstone |
| Terminal | <pre>G E G<br/>E B E<br/>G E G</pre> | G = Glass · E = Ender Pearl · B = Beacon |
| Cell T1 | <pre>G G G<br/>G D G<br/>G G G</pre> | G = Glass · D = Diamond |
| Cell Tn+1 | <pre>D D D<br/>D P D<br/>D D D</pre> | D = Diamond · P = Previous cell (exact item) |
| Importer | <pre>I O I<br/>O R O<br/>I O I</pre> | I = Iron Ingot · O = Observer · R = Redstone Block |
| Exporter | <pre>I D I<br/>D R D<br/>I D I</pre> | I = Iron Ingot · D = Dropper · R = Redstone Block |
| Vacuum | <pre>S R S<br/>R H R<br/>S R S</pre> | S = String · R = Redstone · H = Hopper |
| Auto-Crafter | <pre>R C R<br/>I T I<br/>R C R</pre> | R = Redstone · C = Crafting Table · I = Iron Ingot · T = Target |
| Wireless Terminal | <pre>· P ·<br/>P N P<br/>· C ·</pre> | P = Ender Pearl · N = Nether Star · C = Compass |
| Network Monitor | <pre>G G G<br/>G C G<br/>G G G</pre> | G = Glass Pane · C = Comparator |
| Transmitter | <pre>I R I<br/>R C R<br/>I R I</pre> | I = Iron Ingot · R = Redstone Block · C = Conduit |
| Receiver | <pre>I P I<br/>P L P<br/>I P I</pre> | I = Iron Ingot · P = Ender Pearl · L = Redstone Lamp |
| Greedy Cell | <pre>G H G<br/>H S H<br/>G H G</pre> | G = Gold Ingot · H = Hopper · S = Slime Block |
| Grabber HT | <pre>O P O</pre> | O = Observer · P = Sticky Piston |
| Pusher HT | <pre>D P D</pre> | D = Dropper · P = Piston |
| Recipe Encoder | <pre>K P K<br/>P S P<br/>K P K</pre> | K = Ink Sac · P = Paper · S = Smithing Table |
| Crafting Grid | <pre>C R C<br/>R G R<br/>C R C</pre> | C = Crafting Table · R = Redstone · G = Cartography Table |
| Blueprint ×4 | <pre>P P P<br/>P B P<br/>P P P</pre> | P = Paper · B = Blue Dye |
| Configuration Wrench | <pre>I · I<br/>· C ·<br/>· I ·</pre> | I = Iron Ingot · C = Comparator |
| Network Rake | <pre>D · D<br/>· S ·<br/>· S ·</pre> | D = Dead Bush · S = Stick |
| Network Purger | <pre>I L I<br/>L H L<br/>I L I</pre> | I = Iron Ingot · L = Magma Block · H = Hopper |
| Network Probe | <pre>· A ·<br/>A S A<br/>· A ·</pre> | A = Amethyst Shard · S = Spyglass |
| Quantum Workbench | <pre>D D D<br/>D C D<br/>D D D</pre> | D = Diamond · C = Crafting Table |
| Infinity Barrel | <pre>N D N<br/>D B D<br/>N D N</pre> | N = Netherite Ingot · D = Diamond Block · B = Barrel |
| Network Router | <pre>· L ·<br/>· C ·<br/>· R ·</pre> | L = Lightning Rod · C = Network Cable · R = Redstone Block |
| L1 CPU Cache | <pre>C R C<br/>R C R<br/>C R C</pre> | C = Copper Ingot · R = Redstone Dust |
| L2 CPU Cache | <pre>G L G<br/>L P L<br/>G L G</pre> | G = Gold Ingot · L = Lapis Lazuli · P = L1 CPU Cache |
| L3 CPU Cache | <pre>D A D<br/>A P A<br/>D A D</pre> | D = Diamond · A = Amethyst Shard · P = L2 CPU Cache |
| DRAM Module | <pre>N E N<br/>E P E<br/>N E N</pre> | N = Netherite Ingot · E = Eye of Ender · P = L3 CPU Cache |
| Quantum Cache Matrix | <pre>N S N<br/>S P S<br/>N S N</pre> | N = Netherite Block · S = Nether Star · P = DRAM Module |
| Slimefun Recipe Encoder | <pre>E P E<br/>P B P<br/>E P E</pre> | E = Ender Pearl · P = Paper · B = Enchanting Table |
| Network Quota Limiter | <pre>R C R<br/>C T C<br/>R C R</pre> | R = Redstone · C = Comparator · T = Target |
| Quantum Fluid Cell | <pre>G B G<br/>G L G<br/>G G G</pre> | G = Glass · B = Bucket · L = Lapis Block |
| Liquid Pump | <pre>· G ·<br/>P B P<br/>· R ·</pre> | G = Blue Stained Glass · P = Piston · B = Bucket · R = Redstone |
| Request Terminal | <pre>G L G<br/>R C R<br/>G G G</pre> | G = Glass · L = Lectern · C = Crafting Table · R = Redstone |
| Request Crafter | <pre>R C R<br/>I L I<br/>R C R</pre> | R = Redstone · C = Crafting Table · I = Iron Ingot · L = Lectern |


## ⌨️ Commands

| Command | Description | Permission |
|---|---|---|
| `/mvnets guide [en\|es\|both]` | Receive official interactive guide book (English, Spanish, or both) | `multiversenets.use` |
| `/mvnets devices` | List the device IDs | `multiversenets.use` |
| `/mvnets give <id> [n]` | Give a device | `multiversenets.admin` |
| `/mvnets doctor` | Rescan and diagnose networks | `multiversenets.admin` |
| `/mvnets stats` | Global statistics | `multiversenets.admin` |
| `/mvnets inspect` | Inspect the block you are looking at (type, network, contents, filter) | `multiversenets.admin` |
| `/mvnets repair` | Force a rescan of the network of the block you are looking at | `multiversenets.admin` |
| `/mvnets reload` | Reload the configuration | `multiversenets.admin` |

## 🎮 Quick start

1. Place a **Controller**, surround the area with **Cables**, and connect **Cells**, **Grabbers/Pushers**, etc.
2. Right-click the controller or a **Terminal** to open the Grid.
3. In the terminal (the same conventions as the Networks grid): **left-click** takes 1 to the cursor, **right-click** a stack, **shift+click** sends to inventory; **shift+left-click** on your items inserts them into the network, or leave them in the **input slot** (right corner) and the network absorbs them. The magnifying glass/search label searches (right-click clears), the blue button changes the sort order, the **Network Fluids Storage** button opens digital liquid storage, and the arrows page.
4. Shift+click with a **Wireless Terminal** on the controller to bind it (then right-click in the air to open the network from a distance).
5. **Encoder**: build the recipe in the template grid, put a blank **Blueprint** in the blue slot, and press *Encode*. That Blueprint is installed in an Auto-Crafter with a click on its list.
6. **Receiver**: shift+click with the receiver item on a Transmitter, place it in another base and open it; give it a filter and it will also **bring items** from the transmitter's network.
7. **Request Terminal & Request Crafter**: Encode blueprints into a **Request Crafter** (using the Recipe Encoder). Open the **Request Terminal** to order crafting jobs on-demand. The system automatically resolves recursive chained crafting dependencies (e.g., crafting logs into planks, then planks into a crafting table) and supports custom chat amount entry via Shift+Right Click.

## 🤝 Coexistence with Networks

**Both plugins can be installed at the same time.** They don't step on each other at all:

| | MultiverseNets | NetworksV6-Drake |
|---|---|---|
| Plugin name | `MultiverseNets` | `NetworksV6-Drake` |
| Main class | `com.chagui68.multiversenets.…` | `io.github.sefiraat.networks.…` |
| Command | `/mvnets` | `/networks` |
| Permissions | `multiversenets.*` | `networks.*` |
| Items | own, via PDC, with vanilla recipes | Slimefun's (`NTW_*`) |

Networks doesn't register any vanilla recipe — theirs go through the Slimefun crafting table — so
the 20 here don't clash either. There are five tests (`ConvivenciaConNetworksTest`) that pin this
down; what breaks coexistence isn't the code but the identifiers.

**One interaction to keep in mind.** With the Slimefun integration active, a MultiverseNets Grabber
can pull from a Networks block, because those are Slimefun items with their own menu. That's
interoperability, not a bug, but if you prefer each network to stick to its own:

```yaml
compat:
  slimefun: false
```

## 🧹 Brought over from Networks

What the four Networks variants had and was missing here, chosen for real usefulness and not for
completing the checklist:

* **Network Purger** — discards from the network whatever matches its filter. Without something like
  this a network jams by itself. **Without a configured filter it removes nothing**, on purpose.
* **Network Probe** — right-click on a block (whether it is a node or not) and it tells you which
  network it belongs to, how many nodes it has, and where its controller is.

## 🔗 Slimefun integration (optional)

MultiverseNets **does not depend on Slimefun** and works fully without it. But if it's installed, it
detects it at startup and the **Grabbers, Pushers, and Auto-Crafters can work with Slimefun
machines** just like with a chest: pull the product out of an electric smeltery, feed an arc furnace,
empty a harvester. Everything is resolved via reflection at startup, works for both the DrakesCraft
fork and the original Slimefun, and only the machine's declared input/output slots are used.

To check whether the integration is active: `/mvnets doctor` says so on the first line.

## 🔍 How it differs from Networks

| | Networks (Slimefun addon) | MultiverseNets |
|---|---|---|
| Dependencies | Slimefun + its chain | None, only the Paper API |
| Network membership | Each node stores its root | Recalculated by BFS from the controller |
| Orphan nodes | Possible | **Structurally impossible** |
| Diagnosis | Added later (`/networks doctor`) | `/mvnets doctor` from day one |
| Ticker | Depends on the Slimefun cycle | Own, with per-operation intervals in the config |

## 🛠️ Building

```bash
mvn clean package
```

The jar is generated at `target/MultiverseNets-v<version>.jar`.

## 📋 Compatibility

| Parameter | Requirement |
|---|---|
| **Server** | Paper / Purpur / Folia 1.21.11 |
| **Java** | Java 21 LTS |
| **Dependencies** | None (standalone) |

## 📜 License

This project is licensed under the terms of the **GNU General Public License Version 3 (GPL-3.0)**. See the [LICENSE](../LICENSE) file for details.

---

**Author:** Chagui68 · Review and tuning: Jack · A [DrakesCraft Labs](https://github.com/DrakesCraft-Labs) project