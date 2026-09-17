# 📜 Recipes and functions of MultiverseNets items

Below is every item available in the plugin, its **crafting recipe** shown as you would see it on the
crafting table (3×3 grid), and a brief **description** of its role within the network.

> In the grids, `·` marks an empty slot.

---

## Controller
- **Recipe (3×3)**:

  ```
  I I I
  I N I
  I I I
  ```

  > I = **Iron Block** · N = **Nether Star**

- **Result**: 1× Controller
- **Function**: Core of the network. Indexes every node via BFS through the cables and keeps the network topology.

---

## Cable (×16)
- **Recipe (3×3)**:

  ```
  G G G
  G R G
  G G G
  ```

  > G = **Glass** · R = **Redstone**

- **Result**: 16× Cable
- **Function**: Carries the signal between nodes. Required to connect any device to the network.

---

## Terminal
- **Recipe (3×3)**:

  ```
  G E G
  E B E
  G E G
  ```

  > G = **Glass** · E = **Ender Pearl** · B = **Beacon**

- **Result**: 1× Terminal
- **Function**: Main interface for players to interact with the network (open the Grid, withdraw/insert items).

---

## Wireless Terminal
- **Recipe (3×3)**:

  ```
  · P ·
  P N P
  · C ·
  ```

  > P = **Ender Pearl** · N = **Nether Star** · C = **Compass**

- **Result**: 1× Wireless Terminal
- **Function**: Item that, when shift+clicked on a controller, lets you open that network's terminal from a distance (right-click in the air).

---

## Cell T1 – T6
- **T1 recipe (3×3)**:

  ```
  G G G
  G D G
  G G G
  ```

  > G = **Glass** · D = **Diamond**

  - **Tn+1 recipe (n≥1)**: place the previous cell whole in the center and surround it with diamonds.

  ```
  D D D
  D P D
  D D D
  ```

  > D = **Diamond** · P = **Previous cell** (exact item)

- **Result**: 1× cell of the next tier
- **Function**: Stores a single item type with growing capacity. Default capacities in `cells.capacities`:

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
- **Recipe (3×3)**:

  ```
  G H G
  H S H
  G H G
  ```

  > G = **Gold Ingot** · H = **Hopper** · S = **Slime Block**

- **Result**: 1× Greedy Cell
- **Function**: Smart buffer (default capacity 262,144, configurable in `greedy.capacity`). It claims its filtered item from the network and feeds it to adjacent containers, ideal for continuous transport lines.

---

## Infinity Barrel
- **Recipe (3×3)**:

  ```
  N D N
  D B D
  N D N
  ```

  > N = **Netherite Ingot** · D = **Diamond Block** · B = **Barrel**

- **Result**: 1× Infinity Barrel
- **Function**: Individual store with a capacity of **2 × 10⁹** items of a single type. It joins the network and can deposit/withdraw items like any other storage node.

---

## Importer (Grabber)
- **Recipe (3×3)**:

  ```
  I O I
  O R O
  I O I
  ```

  > I = **Iron Ingot** · O = **Observer** · R = **Redstone Block**

- **Result**: 1× Importer
- **Function**: Extracts items from adjacent containers into the network. Supports a whitelist filter.

---

## Advanced Grabber (Grabber HT)
- **Recipe (3×3)**:

  ```
  O P O
  ```

  > O = **Observer** · P = **Sticky Piston**

- **Result**: 1× Advanced Grabber
- **Function**: High-throughput variant (×8 by default, configurable in `transfer.ht-multiplier`) of the simple grabber. It is directional: it faces the container it pulls from.

---

## Exporter (Pusher)
- **Recipe (3×3)**:

  ```
  I D I
  D R D
  I D I
  ```

  > I = **Iron Ingot** · D = **Dropper** · R = **Redstone Block**

- **Result**: 1× Exporter
- **Function**: Inserts items from the network into adjacent containers. Also supports a whitelist filter.

---

## Advanced Pusher (Pusher HT)
- **Recipe (3×3)**:

  ```
  D P D
  ```

  > D = **Dropper** · P = **Piston**

- **Result**: 1× Advanced Pusher
- **Function**: High-throughput variant (×8 by default, configurable in `transfer.ht-multiplier`) of the simple pusher. It is directional: it faces the container it pushes into.

---

## Vacuum
- **Recipe (3×3)**:

  ```
  S R S
  R H R
  S R S
  ```

  > S = **String** · R = **Redstone** · H = **Hopper**

- **Result**: 1× Vacuum
- **Function**: Picks up ground items within a configurable radius (4.0 by default, in `vacuum.radius`). Optionally supports a whitelist filter.

---

## Network Purger
- **Recipe (3×3)**:

  ```
  I L I
  L H L
  I L I
  ```

  > I = **Iron Ingot** · L = **Magma Block** · H = **Hopper**

- **Result**: 1× Network Purger
- **Function**: Discards from the network whatever matches its filter (whitelist/blacklist), preventing waste from jamming the network. **Without a configured filter it removes nothing**, on purpose.

---

## Network Probe
- **Recipe (3×3)**:

  ```
  · A ·
  A S A
  · A ·
  ```

  > A = **Amethyst Shard** · S = **Spyglass**

- **Result**: 1× Network Probe
- **Function**: Right-click on a block (whether it is a node or not) and it shows which network it belongs to, how many nodes it has, and where its controller is.

---

## Auto-Crafter
- **Recipe (3×3)**:

  ```
  R C R
  I T I
  R C R
  ```

  > R = **Redstone** · C = **Crafting Table** · I = **Iron Ingot** · T = **Target**

- **Result**: 1× Auto-Crafter
- **Function**: Executes recipes defined through **Blueprints** (3×3 grid) or by result (legacy mode). Each cycle it attempts a single craft atomically: either there are ingredients for everything or nothing is touched.

---

## Recipe Encoder
- **Recipe (3×3)**:

  ```
  K P K
  P S P
  K P K
  ```

  > K = **Ink Sac** · P = **Paper** · S = **Smithing Table**

- **Result**: 1× Recipe Encoder
- **Function**: Lets you build and save a recipe in a persistent 3×3 template grid. It generates a **Blueprint** that you then install in the Auto-Crafter.

---

## Blank Blueprint (×4)
- **Recipe (3×3)**:

  ```
  P P P
  P B P
  P P P
  ```

  > P = **Paper** · B = **Blue Dye**

- **Result**: 4× Blank Blueprint
- **Function**: Empty template that, once encoded with the **Recipe Encoder**, becomes a Blueprint with the desired recipe.

---

## Crafting Grid
- **Recipe (3×3)**:

  ```
  C R C
  R G R
  C R C
  ```

  > C = **Crafting Table** · R = **Redstone** · G = **Cartography Table**

- **Result**: 1× Crafting Grid
- **Function**: Lets players use the network as a regular crafting table, consuming items directly from the network in a transactional way.

---

## Quantum Workbench (Advanced)
- **Recipe (3×3)**:

  ```
  D D D
  D C D
  D D D
  ```

  > D = **Diamond** · C = **Crafting Table**

- **Result**: 1× Quantum Workbench
- **Function**: Specialized crafting station that lets you **upgrade quantum cells** (T1 → T2 → … → T6). Place a T1–T5 cell in the center, surround it with 8 diamonds, and press *Entangle & Upgrade*: the stored cargo is preserved without loss.

---

## Network Monitor
- **Recipe (3×3)**:

  ```
  G G G
  G C G
  G G G
  ```

  > G = **Glass Pane** · C = **Comparator**

- **Result**: 1× Network Monitor
- **Function**: Diagnostic panel showing the number of nodes, total storage, and network status in real time.

---

## Wireless Transmitter
- **Recipe (3×3)**:

  ```
  I R I
  R C R
  I R I
  ```

  > I = **Iron Ingot** · R = **Redstone Block** · C = **Conduit**

- **Result**: 1× Wireless Transmitter
- **Function**: Binds a **Wireless Receiver** (shift+click with the receiver item on the transmitter) to create a long-distance network connection. The receiver opens the terminal of the transmitter's network.

---

## Wireless Receiver
- **Recipe (3×3)**:

  ```
  I P I
  P L P
  I P I
  ```

  > I = **Iron Ingot** · P = **Ender Pearl** · L = **Redstone Lamp**

- **Result**: 1× Wireless Receiver
- **Function**: Receives the transmitter's signal and gives remote access to the network. It can use filters to **bridge items** between both networks (without a filter it crosses nothing).

---

## Configuration Wrench
- **Recipe (3×3)**:

  ```
  I · I
  · C ·
  · I ·
  ```

  > I = **Iron Ingot** · C = **Comparator**

- **Result**: 1× Configuration Wrench
- **Function**: Quick configuration tool. *Shift+click* copies the configuration of a device with a filter; normal click pastes it onto another.

---

## Network Rake
- **Recipe (3×3)**:

  ```
  D · D
  · S ·
  · S ·
  ```

  > D = **Dead Bush** · S = **Stick**

- **Result**: 1× Network Rake
- **Function**: Removes network nodes instantly (250 uses by default, configurable in `rake.uses`). Does not affect controllers or loaded cells.

---

## Network Crayon
- **Recipe (3×3)**:

  ```
  C
  S
  ```

  > C = **Cyan Dye** · S = **Stick**

- **Result**: 1× Network Crayon
- **Function**: Marks the controller; the network shows particles around active blocks, making the topology easy to visualize.

---

This documentation is meant as a quick reference for both players and developers who want to understand what each item does and how to craft it. The capacities and speeds quoted are the `config.yml` defaults and can be tuned in that file.