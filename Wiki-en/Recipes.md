# 📜 Recipes and functions of MultiverseNets items

Below is every item available in the plugin, its **crafting recipe** and a brief **description** of
its role within the network.

---

## Controller
- **Recipe**: 8 iron blocks + nether star.
- **Function**: Core of the network. Indexes every node via BFS through the cables and keeps the
  network topology.

---

## Cable (x16)
- **Recipe**: 8 glass + redstone.
- **Function**: Carries the signal between nodes. Required to connect any device to the network.

---

## Terminal
- **Recipe**: beacon + 4 ender pearls + 4 glass.
- **Function**: Main interface for players to interact with the network (open the Grid, withdraw/
  insert items).

---

## Wireless Terminal
- **Recipe**: nether star + 4 ender pearls + compass.
- **Function**: Item that, when shift+clicked on a controller, lets you open that network's
  terminal from a distance (right-click in the air).

---

## Cell T1 – T6
- **Recipe**:
  - **T1**: 8 glass + diamond.
  - **Tn+1** (n≥1): 8 diamonds + previous cell.
- **Function**: Stores a single item type with growing capacity. Default capacities in
  `cells.capacities`:

| Tier | Capacity |
|---|---|
| T1 | 65,536 (65k) |
| T2 | 262,144 (262k) |
| T3 | 1,048,576 (1M) |
| T4 | 16,777,216 (16M) |
| T5 | 268,435,456 (268M) |
| T6 | 2,000,000,000 (2B) |

---

## Greedy Cell
- **Recipe**: slime block + 4 hoppers + 4 gold ingots.
- **Function**: Smart buffer (default capacity 262,144, configurable in `greedy.capacity`). It
  claims its filtered item from the network and feeds it to adjacent containers, ideal for
  continuous transport lines.

---

## Infinity Barrel
- **Recipe**: barrel + 4 netherite ingots + 4 diamond blocks.
- **Function**: Individual store with a capacity of **2 × 10⁹** items of a single type. It joins the
  network and can deposit/withdraw items like any other storage node.

---

## Importer (Grabber)
- **Recipe**: 4 observers + 4 iron ingots + redstone block.
- **Function**: Extracts items from adjacent containers into the network. Supports a whitelist
  filter.

---

## Advanced Grabber (Grabber HT)
- **Recipe**: 2 observers + sticky piston.
- **Function**: High-throughput variant (×8 by default, configurable in `transfer.ht-multiplier`) of
  the simple grabber. It is directional: it faces the container it pulls from.

---

## Exporter (Pusher)
- **Recipe**: 4 droppers + 4 iron ingots + redstone block.
- **Function**: Inserts items from the network into adjacent containers. Also supports a whitelist
  filter.

---

## Advanced Pusher (Pusher HT)
- **Recipe**: 2 droppers + piston.
- **Function**: High-throughput variant (×8 by default, configurable in `transfer.ht-multiplier`) of
  the simple pusher. It is directional: it faces the container it pushes into.

---

## Vacuum
- **Recipe**: hopper + 4 string + 4 redstone.
- **Function**: Picks up ground items within a configurable radius (4.0 by default, in
  `vacuum.radius`). Optionally supports a whitelist filter.

---

## Network Purger
- **Recipe**: hopper + 4 magma blocks + 4 iron ingots.
- **Function**: Discards from the network whatever matches its filter (whitelist/blacklist),
  preventing waste from jamming the network. **Without a configured filter it removes nothing**, on
  purpose.

---

## Network Probe
- **Recipe**: 4 amethyst shards + spyglass.
- **Function**: Right-click on a block (whether it is a node or not) and it shows which network it
  belongs to, how many nodes it has, and where its controller is.

---

## Auto-Crafter
- **Recipe**: 2 crafting tables + target + 2 iron ingots + 4 redstone.
- **Function**: Executes recipes defined through **Blueprints** (3×3 grid) or by result (legacy
  mode). Each cycle it attempts a single craft atomically: either there are ingredients for
  everything or nothing is touched.

---

## Recipe Encoder
- **Recipe**: smithing table + 4 paper + 4 ink.
- **Function**: Lets you build and save a recipe in a persistent 3×3 template grid. It generates a
  **Blueprint** that you then install in the Auto-Crafter.

---

## Blank Blueprint (x4)
- **Recipe**: 8 paper + blue dye.
- **Function**: Empty template that, once encoded with the **Recipe Encoder**, becomes a Blueprint
  with the desired recipe.

---

## Crafting Grid
- **Recipe**: cartography table + 4 crafting tables + 4 redstone.
- **Function**: Lets players use the network as a regular crafting table, consuming items directly
  from the network in a transactional way.

---

## Quantum Workbench (Advanced)
- **Recipe**: 8 diamonds + crafting table.
- **Function**: Specialized crafting station that lets you **upgrade quantum cells**
  (T1 → T2 → … → T6). Place a T1–T5 cell in the center, surround it with 8 diamonds, and press
  *Entangle & Upgrade*: the stored cargo is preserved without loss.

---

## Network Monitor
- **Recipe**: 8 glass panes + comparator.
- **Function**: Diagnostic panel showing the number of nodes, total storage, and network status in
  real time.

---

## Wireless Transmitter
- **Recipe**: conduit + 4 redstone blocks + 4 iron ingots.
- **Function**: Binds a **Wireless Receiver** (shift+click with the receiver item on the
  transmitter) to create a long-distance network connection. The receiver opens the terminal of the
  transmitter's network.

---

## Wireless Receiver
- **Recipe**: redstone lamp + 4 ender pearls + 4 iron ingots.
- **Function**: Receives the transmitter's signal and gives remote access to the network. It can use
  filters to **bridge items** between both networks (without a filter it crosses nothing).

---

## Configuration Wrench
- **Recipe**: 4 iron ingots + comparator.
- **Function**: Quick configuration tool. *Shift+click* copies the configuration of a device with a
  filter; normal click pastes it onto another.

---

## Network Rake
- **Recipe**: 2 dead bushes + 2 sticks.
- **Function**: Removes network nodes instantly (250 uses by default, configurable in `rake.uses`).
  Does not affect controllers or loaded cells.

---

## Network Crayon
- **Recipe**: cyan dye + stick.
- **Function**: Marks the controller; the network shows particles around active blocks, making the
  topology easy to visualize.

---

This documentation is meant as a quick reference for both players and developers who want to
understand what each item does and how to craft it. The capacities and speeds quoted are the
`config.yml` defaults and can be tuned in that file.