<div align="center">

<img src="docs/banner-en.svg" alt="MultiverseNets" width="100%"/>

# 🌌 MultiverseNets

**Standalone digital logistics networks and massive storage for Paper — no Slimefun.**

<img src="https://img.shields.io/badge/Paper-1.21.11-38BDF8?style=for-the-badge&logo=minecraft&logoColor=white" alt="Paper 1.21.11"/>
<img src="https://img.shields.io/badge/Java-21-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
<img src="https://img.shields.io/badge/Author-Chagui68-22C55E?style=for-the-badge" alt="Chagui68"/>

</div>

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
* **Greedy Cell**: a smart buffer that claims its filtered item from the network and feeds it to adjacent containers (ideal for continuous lines).
* The network's storage is the aggregate of all connected cells.
* Atomic per-chunk persistence (Paper region data), anti-dupe in all flows.

### 🔄 Transport
* **Importer (Grabber)**: extracts from adjacent containers into the network, with whitelist filter.
* **Exporter (Pusher)**: inserts from the network into adjacent containers, with filter.
* **HT (High-Throughput) variants**: fast versions ×8 (configurable) for massive factories.
* **Vacuum**: picks up ground items within a configurable radius, now with an optional whitelist filter.

### 🛠️ Auto-crafting
* **Auto-Crafter**: accepts **Blueprints** (real 3×3 grid) and result-based recipes (legacy mode). Each blueprint is attempted once per cycle with **atomic extraction**: either there are ingredients for everything or nothing is touched.
* **Recipe Encoder**: builds the recipe in a persistent 3×3 template grid (click to fix slots, without spending items) and encodes a blank Blueprint with one click.
* **Blueprints**: reusable plans that carry the full recipe (grid + result) in their PDC; they are installed in an Auto-Crafter with a click and are not consumed.
* **Crafting Grid**: manual crafting pulling from the network: the template grid is saved in the block, and each craft withdraws ingredients from the network transactionally.

### 🧰 Tools (brought over from NetworksV6)
* **Configuration Wrench**: shift+click on a device with a filter **copies** its configuration; normal click **pastes** it onto another.
* **Network Rake**: removes nodes instantly (250 uses by default, `rake.uses`); does not touch controllers or loaded cells.
* **Network Crayon**: marks the controller and the network shows particles when its machines work.
* Filters with **whitelist/blacklist mode** on any device with a filter (grabbers, pushers, vacuum, purger, greedy cell, receiver).

### 🛡️ Reliability
* Protection against pistons and explosions on nodes.
* When you break a node, its state travels inside the item (like in Networks): cell cargo, filters, blueprints, grid matrix, and receiver binding. When you place it again, it is as it was.
* Networks-style anti-dupe guards in all menus (no double-click, no drags over painted slots, no shift+right-click into the void) and **recovery of anything left in the real slots on close**.
* `/mvnets doctor` rescans and diagnoses all networks; `/mvnets inspect` and `/mvnets repair` inspect and rescan the block you are looking at.

## 🍳 Recipes

| Device | Recipe |
|---|---|
| Controller | 8 iron blocks + nether star |
| Cable x16 | 8 glass + redstone |
| Terminal | beacon + ender pearls + glass |
| Cell T1 | 8 glass + diamond |
| Cell Tn+1 | 8 diamonds + previous cell |
| Importer | 4 observers + 4 iron + redstone block |
| Exporter | 4 droppers + 4 iron + redstone block |
| Vacuum | sponge + hopper + string + redstone |
| Auto-Crafter | crafting table + target + iron + redstone |
| Wireless Terminal | nether star + pearl + compass |
| Network Monitor | 8 glass panes + comparator |
| Transmitter | conduit + 4 redstone blocks + 4 iron |
| Receiver | redstone lamp + 4 pearls + 4 iron |
| Greedy Cell | slime block + 2 hoppers + 4 gold |
| Grabber HT | observer + sticky piston + observer |
| Pusher HT | dropper + piston + dropper |
| Recipe Encoder | smithing table + paper + ink |
| Crafting Grid | cartography table + crafting tables + redstone |
| Blueprint x4 | 8 paper + blue dye |
| Configuration Wrench | 4 iron + comparator |
| Network Rake | 2 dead bushes + 2 sticks |
| Network Crayon | 2 cyan dyes + stick |

## ⌨️ Commands

| Command | Description | Permission |
|---|---|---|
| `/mvnets devices` | List the device IDs | `multiversenets.use` |
| `/mvnets give <id> [n]` | Give a device | `multiversenets.admin` |
| `/mvnets doctor` | Rescan and diagnose networks | `multiversenets.admin` |
| `/mvnets stats` | Global statistics | `multiversenets.admin` |
| `/mvnets inspect` | Inspect the block you are looking at (type, network, contents, filter) | `multiversenets.admin` |
| `/mvnets repair` | Force a rescan of the network of the block you are looking at | `multiversenets.admin` |
| `/mvnets reload` | Reload the configuration | `multiversenets.admin` |

Alias: `/mvn`

## 🎮 Quick start

1. Place a **Controller**, surround the area with **Cables**, and connect **Cells**, **Grabbers/Pushers**, etc.
2. Right-click the controller or a **Terminal** to open the Grid.
3. In the terminal (the same conventions as the Networks grid): **left-click** takes 1 to the cursor, **right-click** a stack, **shift+click** sends to inventory; **shift+left-click** on your items inserts them into the network, or leave them in the **input slot** (right corner) and the network absorbs them. The magnifying glass/search label searches (right-click clears), the blue button changes the sort order, the arrows page.
4. Shift+click with a **Wireless Terminal** on the controller to bind it (then right-click in the air to open the network from a distance).
5. **Encoder**: build the recipe in the template grid, put a blank **Blueprint** in the blue slot, and press *Encode*. That Blueprint is installed in an Auto-Crafter with a click on its list.
6. **Receiver**: shift+click with the receiver item on a Transmitter, place it in another base and open it; give it a filter and it will also **bring items** from the transmitter's network.

## 🤝 Coexistence with Networks

**Both plugins can be installed at the same time.** They don't step on each other at all:

| | MultiverseNets | NetworksV6-Drake |
|---|---|---|
| Plugin name | `MultiverseNets` | `NetworksV6-Drake` |
| Main class | `com.chagui68.multiversenets.…` | `io.github.sefiraat.networks.…` |
| Command | `/mvnets` (alias `/mvn`) | `/networks` |
| Permissions | `multiversenets.*` | `networks.*` |
| Items | own, via PDC, with vanilla recipes | Slimefun's (`NTW_*`) |

Networks doesn't register any vanilla recipe — theirs go through the Slimefun crafting table — so
the 20 here don't clash either.

There are **five tests** that pin this down (`ConvivenciaConNetworksTest`). They aren't there for
show: what breaks coexistence isn't the code but the identifiers, and those get changed by accident
when renaming something.

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
  this a network jams by itself: any machine that produces waste (quartz gravel, seeds from a
  harvester) ends up filling the cells and blocking what really matters. Networks spread this
  between `TRASH` and `PURGER`; here one is enough because the filter already decides what goes.
  **Without a configured filter it removes nothing**, on purpose: a purger that ate everything by
  default would be an inventory shredder waiting for someone to place it without looking.
* **Network Probe** — right-click on a block and it tells you which network it belongs to, how many
  nodes it has, and where its controller is. `/mvnets doctor` summarizes the health of all networks;
  the probe answers the specific question you ask yourself standing in front of a stopped machine:
  *is this connected to anything?*. It also works on blocks that are **not** nodes, which is exactly
  when it's most needed.

## 🔗 Slimefun integration (optional)

MultiverseNets **does not depend on Slimefun** and works fully without it. But if it's installed, it
detects it at startup and the **Grabbers, Pushers, and Auto-Crafters can work with Slimefun
machines** just like with a chest: pull the product out of an electric smeltery, feed an arc furnace,
empty a harvester.

Details that matter:

* **Adds no dependency.** Everything is resolved via reflection at startup. Without Slimefun the
  bridge stays inert and the rest of the plugin doesn't know.
* **Works for both Slimefun flavors.** It recognizes the repackaged DrakesCraft fork and the
  original by thebusybiscuit, so the same jar works on both.
* **Respects each machine's design.** Only the slots that the machine itself declares for input and
  output are used, not all the menu slots. Putting coal in a smeltery's output slot jams it, and
  pulling from its input robs what it was processing.

To check whether the integration is active: `/mvnets doctor` says so on the first line.

## 🔍 How it differs from Networks

MultiverseNets is not a trimmed-down Networks: it solves the same problem with a different
architecture, and that decision has concrete consequences.

| | Networks (Slimefun addon) | MultiverseNets |
|---|---|---|
| Dependencies | Slimefun + its chain | None, only the Paper API |
| Network membership | Each node stores its root | Recalculated by BFS from the controller |
| Orphan nodes | Possible: a node can keep pointing to a root its controller already replaced | **Structurally impossible**: every scan rebuilds the whole topology |
| Diagnosis | Added later (`/networks doctor`) | `/mvnets doctor` from day one |
| Ticker | Depends on the Slimefun cycle | Own, with per-operation intervals in the config |

The core difference is in the third row. In Networks, "I have everything connected and the machine
doesn't work" is a real symptom that appears when a node returns to the registry but not to its
network; we've been chasing it for months. That can't happen here, because there is no per-node
state that survives a scan.

The price is that scanning costs: one BFS over up to `max-nodes` blocks every
`scan-interval-ticks`. It's a deliberate trade-off — you pay predictable, bounded work in exchange
for no state that can be corrupted.

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

---