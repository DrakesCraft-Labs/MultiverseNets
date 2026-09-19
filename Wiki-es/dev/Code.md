# ⚙️ Cómo funciona el código de MultiverseNets

Este documento explica el funcionamiento interno del plugin: cómo se representa una red, dónde y cómo se guarda el estado, cómo fluyen los ítems y cómo se organiza el código por capas. Está pensado para desarrolladores que quieran leer o modificar el código.

> Zona de desarrollo: [Estructura](Structure.md) · **Cómo funciona el código** · [Tests](Tests.md)

---

## 1. Vista de conjunto (capas)

```
┌───────────────────────────────┐
│  Capa de presentación (GUI)   │  com.chagui68.multiversenets.gui
├───────────────────────────────┤
│  Capa de eventos (listeners)  │  com.chagui68.multiversenets.listen
├───────────────────────────────┤
│  Capa de servicio (núcleo)    │  com.chagui68.multiversenets.net
│   + crafteo  (compat/craft)   │
├───────────────────────────────┤
│  Capa de persistencia         │  com.chagui68.multiversenets.persist
│   (NodeBlob / NodeStore)      │
├───────────────────────────────┤
│  Capa base (util + item +     │  util / item / command / compat
│   command + compat)           │
└───────────────────────────────┘
```

Toda la lógica de red vive en el hilo principal del servidor (síncrona), lo que evita condiciones de carrera con el mundo.

## 2. Ciclo de vida del plugin

Clase principal: `MultiverseNets extends JavaPlugin` (singleton accesible mediante `MultiverseNets.instance()`; expone el gestor de redes con `networks()`).

**`onEnable()`**, en orden:
1. `saveDefaultConfig()` — copia `config.yml` si no existe.
2. `Keys.init(this)` — inicializa las `NamespacedKey` persistentes.
3. `Settings.refresh(this)` — carga la configuración.
4. `SlimefunBridge.init(getLogger())` — activa la integración con Slimefun solo si está instalado.
5. `Items.registerRecipes(this)` — registra las recetas de todos los dispositivos.
6. `NodeStore.init(this)` — prepara la persistencia por chunk y carga el registro de controladores.
7. `new NetworkManager(this); networks.load()` — recrea las redes a partir de los controladores guardados.
8. Registra `BlockListener`, `GuiListener` y `ChatPrompts`.
9. Arranca `NetworkTicker` y registra el comando `/mvnets` (alias `/mvn`).

**`onDisable()`**: detiene el `NetworkTicker`, ejecuta `networks.saveAll()` (guarda el registro de controladores) y loguea el apagado.

## 3. Claves persistentes (`util/Keys`)

Registro único de todos los `NamespacedKey` usados en los `PersistentDataContainer` (PDC) de chunks e ítems:

| Constante | Clave (namespace `multiversenets:`) | Uso |
| --- | --- | --- |
| `DEVICE_TYPE` | `device_type` | Tipo de dispositivo en ítems (Nombre del enum `DeviceType`). |
| `WIRELESS_BIND` | `wireless_bind` | Coordenadas del controlador vinculado a la terminal inalámbrica. |
| `RECEIVER_BIND` | `receiver_bind` | Coordenadas del transmisor vinculado a un receptor. |
| `BLUEPRINT_RECIPE` | `blueprint_recipe` | Receta legada guardada en un blueprint antiguo. |
| `CHUNK_HAS_NODES` | `chunk_has_nodes` | Marca rápida «este chunk puede tener nodos». |
| `TERMINAL_DISPLAY` | `terminal_display` | Ajustes de búsqueda/orden de la terminal. |
| `CELL_CARGO` | `cell_cargo` | Estado serializado (Base64) de la carga de una celda/nodo. |
| `BLUEPRINT_DATA` | `blueprint_data` | Receta codificada en Base64 dentro del ítem Blueprint. |
| `CONFIG_DATA` | `config_data` | Configuración de filtros copiada en la llave inglesa. |
| `RAKE_USES` | `rake_uses` | Usos restantes del Network Rake. |

## 4. Coordenadas (`util/PosUtil`)

Una posición 3D de bloque se compacta en **un solo `long`** de 64 bits (clave de mapa muy eficiente):

- **X → 26 bits** (bits 38–63), máscara `0x3FFFFFF`, rango ±33 554 431.
- **Z → 26 bits** (bits 12–37), igual rango.
- **Y → 12 bits** (bits 0–11), rango −2048 … +2047 (toda la altura del mundo).

`pack(x, y, z) = (x & 0x3FFFFFF) << 38 | (z & 0x3FFFFFF) << 12 | (y & 0xFFF)`. Los negativos se manejan con desplazamientos aritméticos (signo) en los `unpack`. Las coordenadas empaquetadas se usan como claves en `Network.nodes`, `NetworkStorage.CellRef` y para localizar chunks (`PosUtil.unpackX(pos) >> 4`).
El test `PosUtilTest` fija este formato (positivos y límites de mundo negativos).

## 5. Persistencia (`persist/NodeBlob` y `persist/NodeStore`)

### 5.1 Estado de un nodo: `NodeBlob`
Clase `Serializable` (UID fijo `1L`) con **campos públicos** que describen el estado persistente de un bloque de la red:

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `typeName` | `String` | Nombre del enum `DeviceType`. |
| `cellSample` | `ItemStack` | Plantilla del ítem guardado en una Quantum Cell / Barrica. |
| `cellAmount` | `long` | Cantidad total almacenada. |
| `filterMaterials` | `List<String>` | Materiales/IDs del filtro. |
| `filterItems` | `List<ItemStack>` | Plantillas exactas del filtro. |
| `filterBlacklist` | `boolean` | `true` = blacklist, `false` = whitelist. |
| `recipes` | `List<String>` | Claves de recetas legadas (Auto-Crafter). |
| `blueprintData` | `List<String>` | `RecipeData` en Base64 instalados en el Auto-Crafter. |
| `craftingMatrix` | `ItemStack[9]` | Plantilla de crafteo 3×3 persistente. |
| `crayon` | `boolean` | Efecto de partículas del controlador. |
| `txWorld` / `txX` / `txY` / `txZ` | `String` / `int` | Vinculación del Receptor al Transmisor. |
| `targetFace` | `String` | Cara direccional (`NORTH`… o `ALL`). |
| `greedySamples` | `List<ItemStack>` | Plantillas multi-ítem de la Greedy Cell. |
| `greedyAmounts` | `List<Long>` | Cantidades por muestra de la Greedy Cell. |

### 5.2 Lectura/escritura por chunk: `NodeStore`
El estado no se guarda en la entidad del bloque, sino en el **PDC del chunk**, con claves por bloque:
- `"n" + x + "_" + y + "_" + z` → **blob** completo (Base64).
- `"t" + x + "_" + y + "_" + z` → solo el **nombre del tipo**, para clasificar un bloque sin deserializar (camino rápido del escáner).

Además cada `put()` sella la marca `CHUNK_HAS_NODES` (byte 1) en el chunk; el escáner la usa para saltar chunks vacíos sin leer PDCs.

API principal:
- `put(Block, NodeBlob)` / `get(Block)` (null si el chunk no está cargado) / `getType(Block)` / `hasNode(Block)` / `remove(Block)`.
- `encode(NodeBlob)` / `decode(String)` — serialización Java vía `BukkitObjectOutputStream`/`BukkitObjectInputStream` + Base64.
- `normalize()` — repara blobs antiguos (listas/null a valores por defecto) y **migra Greedy Cells legadas**: si una Greedy tenía carga de ítem único en `cellSample`/`cellAmount`, se mueve a `greedySamples`/`greedyAmounts`.

### 5.3 Registro de controladores (`networks.yml`)
`NodeStore` mantiene en memoria `Map<UUID, List<String>> CONTROLLERS` (mundo → `"x,y,z"`). Se guarda en `<dataFolder>/networks.yml` bajo `controllers.<world-uuid>`. El `save()` se ejecuta **asíncronamente** si se llama desde el hilo principal (no bloquea el tick). `NetworkManager.load()` recrea las redes desde este registro al arrancar.

## 6. La red (`net/Network` y `net/NetworkManager`)

### 6.1 Topología: `Network`
- Identificada por la posición empaquetada (long) del **controlador** (`controllerPos`).
- `nodes: Map<Long, DeviceType>` = conjunto de miembros; `byType: Map<DeviceType, Set<Long>>` = índice inverso por tipo (para iterar solo grabbers/pushers/etc. con miles de nodos).
- `version` (long, volatile) — se incrementa en cada `scan()`; `NetworkStorage` la usa para detectar topologías obsoletas.
- `scan()` — **BFS de relleno** desde el controlador sobre los 6 vecinos adyacentes (±X/±Y/±Z). Poda por: chunk sin cargar, chunk sin marca `CHUNK_HAS_NODES`, bloque sin tipo, y **segundo controlador** (se rechaza con `error = "foreign controller"`); respeta `network.max-nodes`. Nunca fuerza cargas de chunk. Expone `error` (aviso legible), `markDirty()` y `needsScan(intervalMs)`.
- Queries: `contains(pos)`, `typeAt(pos)`, `forEach(type, consumer)` (copia defensiva para poder romper/poner bloques durante la iteración), `count(type)`, `block(pos)`.

### 6.2 Gestor: `NetworkManager`
- Mapa `networksByWorld: Map<UUID, Map<Long, Network>>` (mundo → controlador → red).
- `registerController(Block)` / `removeController(Block)` — crean/eliminan la red y actualizan `NodeStore`.
- `networkAt(Block)` — búsqueda lineal entre las redes del mundo que contienen el bloque; `networkByController(Location)` — acceso directo por controlador (terminal inalámbrica).
- `invalidateNear(Block)` — reescanea la red del bloque y las de sus **6 vecinos** (para cuando un bloque cambia la conectividad). Se llama desde `BlockListener` al colocar/romper/rastrillar/pintar.
- Helpers de filtros: `filterPredicate(blob)` (whitelist/blacklist), `matchesFilter(template, item)` (ordena: DeviceType → ID Slimefun → nombre mostrado → material), `extractFirst(Inventory,…)`, `insertInto(Inventory,…)`.

## 7. Almacenamiento virtual: `NetworkStorage`

Vista agregada de todos los bloques de almacenamiento de la red (Quantum Cells, Greedy Cells y Barriles Infinitos) como **una sola «bóveda»**. Los GUIs y el ticker interactúan con la red, no con celdas individuales.

- `sync()` — reconstruye la lista de celdas `CellRef(pos, tier, greedy, barrel)` cuando `network.versionSnapshot() != boundVersion`; clasifica cada nodo (celda normal → `cellTier()`, greedy, barril).
- `load()` / `flush()` — por operación descodifica los blobs de celdas cargadas (ignora chunks sin cargar o celdas cuyo tipo real ya no coincide) y reescribe los sucios.
- **`deposit(ItemStack)` → leftover** — en tres pasadas: (1) **Greedy Cells** (sumidero preferido, si el ítem matchea una muestra o pasa el filtro); (2) **celdas con la misma plantilla** (`cellSample`); (3) **celdas vacías** (adoptan el tipo). Nunca muta el stack que recibe.
- **`withdraw(matcher, amount[, excludePos])`** — dos pasadas: normales+barriles primero, **Greedy Cells después** (buffer de salida; `excludePos` evita que una Greedy se retire su propia carga). Resultado de un único tipo de ítem.
- `count(predicate)` — suma total de ítems que cumplen el predicado.
- `view()` — instantánea consolidada para GUIs con **caché de 500 ms**; agrupa por material y mezcla con `StackUtils.itemsMatch` (nunca con `hashCode`).
- Capacidades: celda → `Settings.cellCapacity(tier)`; greedy → `Settings.greedyCapacity()`; barril → `Settings.barrelCapacity()`.
- Utilidades: `getGreedyStoredAmount`, `isItemPurged`, `getPurgedItemsView`, `countActivePurgers`, `countActiveGreedyCells`, `isEmpty`.

## 8. El latido: `NetworkTicker`

- Sincrónico, `runTaskTimer(plugin, run, 20L, 5L)` — primer tick a los 20 ticks, después **cada 5 ticks**.
- Contadores por familia (`scanIn`, `transferIn`, `vacuumIn`, `craftIn`) → cada familia se ejecuta según su intervalo configurado (múltiplos de 5).
- Por red: si `dirty` o toca escaneo → `scan()`; si toca transferencia → `doTransfers`; si toca vacío → `doVacuum`; si toca crafteo → `doCrafting`.
- **`doTransfers`** (batching por operación): `items-per-op` = 64 base; el HT (Grabber/Pusher avanzados) usa `64 × ht-multiplier (8) = 512`. Orden de procesado por tipo:
  1. `GRABBER` (importa 64) → `GRABBER_HT` (512) → `PUSHER` → `PUSHER_HT` → `GREEDY_CELL` → `PURGER` → `RECEIVER`.
  2. **Grabber**: extrae de contenedores vanilla (o máquinas Slimefun si `compat.slimefun`) contra su filtro, deposita en la red; si el destino se llena, el sobrante se devuelve o **se suelta al mundo** (`dropItemNaturally`).
  3. **Pusher**: retira de la red contra su filtro e inserta en contenedores/máquinas; si no caben, el sobrante **vuelve a la red**.
  4. **Purger**: solo actúa si tiene **filtros no vacíos** (a salvo del borrado indiscriminado); retira y descarta.
  5. **Greedy**: `greedyTick` = aspira (retira hasta `4 × items-per-op` con `excludePos` propio) si tiene filtro y espacio, y luego **distribuye** hasta `2 × items-per-op` a contenedores/máquinas adyacentes.
  6. **Receiver** (puente inalámbrico): solo si el blob tiene `txWorld` y **filtro no vacío** (evita fusiones accidentales de redes); retira de la red remota y deposita en la propia.
- **`doVacuum`**: radio `vacuum.radius`; recoge entidades `Item` del suelo (sin `pickupDelay`, y que pasen el filtro) hacia la red.
- **`doCrafting`**: por Auto-Crafter, decodifica cada blueprint instalado (`craft/Blueprints.decode`) e intenta craftear (ver §10).
- Partículas: si `net.crayon()`, se emiten partículas cian en cada operación (función del Network Crayon).

## 9. Ítems y dispositivos (`item/DeviceType` y `item/Items`)

- `DeviceType` — enum de **30 dispositivos**. Cada constante tiene `material`, `display`, `placeable` y `cellTier` (1–6). Propiedades derivadas: `isCell()` (tier > 0), `filterable()` (grabbers, pushers, vacuum, greedy, purger, receiver), `isImporter()`/`isExporter()`, `isDirectional()` (avanzados HT). `parse(name)` acepta `MVN_…`, sin prefijo y `wireless`.

| Constante | Material | Nombre visible | Colocable |
| --- | --- | --- | --- |
| `MVN_CONTROLLER` | LODESTONE | Network Controller | ✔ |
| `MVN_CABLE` | GLASS | Network Cable | ✔ |
| `MVN_TERMINAL` | BEACON | Network Terminal | ✔ |
| `MVN_MONITOR` | RESPAWN_ANCHOR | Network Monitor | ✔ |
| `MVN_CELL_T1`…`T6` | Terracota por nivel | Quantum Cell T1…T6 | ✔ |
| `MVN_GREEDY_CELL` | SLIME_BLOCK | Greedy Cell | ✔ |
| `MVN_INFINITY_BARREL` | BARREL | Infinity Barrel | ✔ |
| `MVN_GRABBER` / `MVN_GRABBER_HT` | OBSERVER / STICKY_PISTON | Simple / Advanced Grabber | ✔ |
| `MVN_PUSHER` / `MVN_PUSHER_HT` | TARGET / PISTON | Simple / Advanced Pusher | ✔ |
| `MVN_VACUUM` | SPONGE | Network Vacuum | ✔ |
| `MVN_PURGER` | MAGMA_BLOCK | Network Purger | ✔ |
| `MVN_PROBE` | SPYGLASS | Network Probe | ✘ (mano) |
| `MVN_CRAFTER` | CRAFTING_TABLE | Auto-Crafter | ✔ |
| `MVN_ENCODER` | SMITHING_TABLE | Recipe Encoder | ✔ |
| `MVN_CRAFTING_GRID` | CARTOGRAPHY_TABLE | Network Crafting Grid | ✔ |
| `MVN_QUANTUM_WORKBENCH` | BRAIN_CORAL_BLOCK | Quantum Workbench | ✔ |
| `MVN_TRANSMITTER` / `MVN_RECEIVER` | CONDUIT / REDSTONE_LAMP | Wireless Transmitter / Receiver | ✔ |
| `MVN_WIRELESS_TERMINAL` | NETHER_STAR | Wireless Terminal | ✘ (mano) |
| `MVN_BLUEPRINT` | BOOK | Blueprint | ✘ (mano) |
| `MVN_CONFIGURATOR` | COMPARATOR | Configuration Wrench | ✘ (mano) |
| `MVN_RAKE` | DEAD_BUSH | Network Rake | ✘ (mano) |
| `MVN_CRAYON` | CYAN_DYE | Network Crayon | ✘ (mano) |

- `Items.create(type)` — construye el `ItemStack` con su nombre mostrado y graba `Keys.DEVICE_TYPE = type.name()` en el PDC. `Items.typeOf(item)` lo recupera (distingue ítems del plugin de items vanilla).
- `Items.capacityOf(type)` — capacidad declarada de un dispositivo (Barril 2 000 000 000; celdas según `Settings`).
- Herramientas: `rake()` (graba `RAKE_USES`), `spendRakeUse` (gasta un uso, rompe al llegar a 0), `saveConfig`/`readConfig` (llave: `CONFIG_DATA` con materiales + `bl`/`wl`), `linkReceiver`/`readReceiverBind` (`RECEIVER_BIND`), `bindWireless`/`readWirelessBind` (`WIRELESS_BIND`), `blueprint`/`isBlueprint`/`readBlueprint` (recetas legadas por `BLUEPRINT_RECIPE`).
- `registerRecipes(plugin)` — registra **todas** las recetas shaped del plugin. Los patrones 3×3 están documentados en [Recipes.md](../Recipes.md).

## 10. Crafteo (`craft/Blueprints` y `craft/CraftingSupport`)

- `RecipeData` — estructura `Serializable` con `inputs[9]` (cantidad 1, `null` = vacío) y `output`.
- `Blueprints.encode/decode` — serialización Java + Base64. Un blueprint es un `ItemStack` BOOK con `Keys.BLUEPRINT_DATA`; `toItem` le pone nombre/lore legibles y `read` lo recupera.
- `Blueprints.resolve(matrix, world)` — normaliza la matriz (quantity-1, aire→null) y la resuelve contra las recetas vanilla del servidor (`Bukkit.getCraftingRecipe`), con caché por matriz. `matchesOutput` exige que la receta actual del servidor siga produciendo la salida grabada (patrón NetworksV6): **si el servidor cambia la receta, el blueprint deja de funcionar**.
- `CraftingSupport.tryCraftBlueprint(net, data)` — crafteo **atómico** desde el almacenamiento de la red:
  1. Guardas: datos válidos y matriz no vacía.
  2. `resolve` + `matchesOutput`.
  3. Agrega necesidades por tipo (`Need(sample, amount)`) usando `StackUtils.itemsMatch`.
  4. Pre-chequeo de disponibilidad (con `count`) → si falta algo, no toca nada.
  5. Consume con `withdraw`; si algo falla a mitad, **reintegra todo lo tomado** (`depositAll`).
  6. Deposita el resultado; si **no cabe entero** en la red, revierte los ingredientes y falla.
- `tryCraftOnce(net, recipe)` — variante para recetas Bukkit (legadas) con el mismo patrón de rollback. `tryCraftAll` itera las recetas legadas del blob. La mesa de crafteo de red (`CraftingGridMenu`) usa la misma resolución contra la red (ver §11).

## 11. Capa GUI (`gui/`)

### 11.1 Base: `MenuHolder`
Clase abstracta que implementa `InventoryHolder`. `open(size, title)` crea el inventario, llama a `draw()` (render de botones/ítems) y lo abre. `refresh()` redibuja conservando las ranuras `vanillaSlots()`. Proporciona `giveOrDrop` (devuelve ítems al jugador o los suelta si el inventario está lleno) y `playerInventorySlot(event)` (resolución robusta de la ranura del jugador para evitar dupes). Cada menú implementa `draw()` y `click(event)`; `onClose(event)` opcional.

Menús e inventarios:

| Menú | Tamaño | Uso / detalles |
| --- | --- | --- |
| `TerminalMenu` | 54 | Terminal (bloque, inalámbrica y receptor). Cajas de extracción por ítem; ranura de entrada `INPUT_SLOT=8`; botones de purger `17`, orden `SORT=26`, filtro `35`, páginas `PREV=44`/`NEXT=53`; 48 ítems por página. En cierre, guarda lo dejado en la entrada en la red. |
| `EncoderMenu` | 45 | Matriz 3×3 (`BLANK_SLOT=19`), botón codificar `ENCODE=16`, salida `OUTPUT=34`, vista previa `PREVIEW=25`. Produce un ítem Blueprint con la receta embebida. |
| `CrafterMenu` | 27 | Auto-Crafter: hasta 18 blueprints instalados (`MAX_BLUEPRINT_SLOTS=18`), estado `STATUS=24`, limpiar `CLEAR=25`, ayuda `HELP=26`. |
| `CraftingGridMenu` | 54 | Mesa de crafteo de red: 3×3 propio + `RESULT=31`, `CRAFT_ONE=33`, `CRAFT_ALL=35`, `CLEAR=38`, páginas `27/29`, info `41`. Consume de la red y entrega al jugador. |
| `FilterMenu` | 27 | Filtros (grabbers, pushers, vacuum, purger, greedy, receiver): hasta 17 ítems, modo whitelist/blacklist `MODE=17`, botón todas las caras `ALL_DIRECTIONS=24`, limpiar `25`, ayuda `26`. |
| `MonitorMenu` | 27 | Monitor de conteo de la red. |
| `CellMenu` | 18 | Quantum Cell individual: plantilla `ITEM=4`, depósito rápido `DEPOSIT_ALL=11`, fijar `SET=13`, extraer todo `EXTRACT_ALL=15`. |
| `GreedyMenu` | 54 | Greedy Cell: 36 ranuras de almacenamiento (0–35), filtro `45`, depósito rápido `46`, monitor `49`, dirección `50`, info `53`. |
| `BarrelMenu` | 18 | Barrica Infinita: plantilla `ITEM=4`, depósito `11`, fijar `13`, extraer todo `15`. |
| `QuantumWorkbenchMenu` | 45 | Mejora de celdas: receta 3×3 (centro `CENTER=20`), craftear `CRAFT=23`, salida `OUTPUT=25` (celda mejorada con carga preservada). |
| `ChatPrompts` | — | Prompts por chat del plugin (estado `isPending` consultado por `GuiListener`). |

### 11.2 Seguridad: `GuiListener` (anti-dupe)
Listener global que filtra TODOS los clics cuando el inventario superior es un `MenuHolder`:
- Cancela clics peligrosos: doble-clic, botón medio, teclas numéricas, intercambio con offhand, drop (simple/control), modo creativo, `COLLECT_TO_CURSOR`, movimientos de hotbar y `UNKNOWN`.
- En inventario del jugador: solo se reenvía el **shift-clic** al menú (salvo si hay un `ChatPrompts` pendiente).
- En inventario superior: si la ranura no está en `vanillaSlots()`, cancela y reenvía a `menu.click(event)`.
- `onDrag`: cancela si el arrastre pisa ranuras no-vanilla del menú. `onClose`: notifica a `menu.onClose(event)`.

## 12. Listener de bloques (`listen/BlockListener`)

Eventos manejados:

| Evento | Comportamiento |
| --- | --- |
| `BlockPlaceEvent` | Si el ítem colocado es un dispositivo: lo registra (`NodeStore.put`), restaura carga embebida (`CELL_CARGO`), vincula un Receptor si procede, y si es controlador lo da de alta (`registerController`) o invalida los vecinos. |
| `BlockBreakEvent` | Si el bloque es un nodo: suelta un ítem del dispositivo **con el estado embebido en `CELL_CARGO`** (salvo controlador/cable/estado vacío), elimina el nodo y reescanea. |
| `PlayerInteractEvent` | Herramientas de mano (Sonda → diagnóstico, Rastrillo → retirar sin romper, Llave → copiar/pegar filtros, Crayón → partículas); vinculaciones (`shift+clic` terminal inalámbrica sobre controlador/terminal; receptor sobre transmisor); y apertura del menú correspondiente según dispositivo (ver tabla de menús). |
| `BlockPistonExtendEvent` / `BlockPistonRetractEvent` | Cancela si algún bloque movido es un nodo (los nodos no se pueden empujar/arrastrar). |
| `EntityExplodeEvent` / `BlockExplodeEvent` | Retira los nodos de la lista de bloques destruidos (inmunes a explosiones). |

Herramientas destacadas:
- **Probe** (`probeNode`): informa del dispositivo, de su red (o «NO NETWORK» si no llega al controlador) y de avisos del último escaneo.
- **Rake** (`useRake`): elimina el nodo al instante (nunca un controlador y nunca almacenamiento con carga), gasta un uso y se rompe al acabarse.
- **Wrench** (`useWrench`): shift+clic copia filtros al ítem; clic normal los pega en otro dispositivo filtrable.
- **Crayon** (`useCrayon`): alterna `blob.crayon` del controlador (partículas de red).

## 13. Comando `/mvnets` (`command/MvnetsCommand`)

Subcomandos: `help`, `info`, `reload`, `give <id> [n]`, `devices`, `doctor`, `stats`, `inspect`, `repair`. Los nocionales usan **`multiversenets.admin`** (`reload`, `give`, `doctor`, `stats`, `inspect`, `repair`); `help`/`info`/`devices` están abiertos. `TabCompleter` completa subcomandos y, para `give`, los IDs de dispositivos (`type.id()`, en minúsculas sin prefijo `mvn_`).

## 14. Integración con Slimefun (`compat/SlimefunBridge`)

- **Por qué existe**: las máquinas de Slimefun guardan su inventario en `BlockMenu`, no como `InventoryHolder`; sin puente serían bloques decorativos para grabbers/pushers.
- **Por reflexión**: el plugin sigue siendo 100 % autónomo. Busca los paquetes `com.github.drakescraft_labs.slimefun4.legacy` y `io.github.thebusybiscuit.slimefun4.legacy`. Si `compat.slimefun=false` o Slimefun no está instalado, queda **inactivo** (`isAvailable()` = false) y solo se usan contenedores vanilla.
- API: `isMachine(Block)` (menú propio), `getId(Block)` (`checkID`) e `getId(ItemStack)` (lee el tag PDC `slimefun_item`), `extract` (saca de los slots de salida respetando `getSlotsAccessedByItemTransport` + `WITHDRAW`), `insert` (empuja a slots de entrada + `INSERT`). Conserva alias legados en español: `disponible`, `esMaquina`, `idDe`, `esItemSlimefun`, `extraer`, `insertar`.

## 15. Configuración (`util/Settings`)

Todas las lecturas pasan por `Settings` sobre `plugin.getConfig()` (se refresca en `onEnable` y `/mvnets reload`). Tabla completa:

| Método | Clave en `config.yml` | Valor por defecto | Límites |
| --- | --- | --- | --- |
| `scanIntervalTicks()` | `network.scan-interval-ticks` | 20 | ≥ 5 |
| `maxNodes()` | `network.max-nodes` | 16 384 | ≥ 16 |
| `transferIntervalTicks()` | `network.op-interval-ticks.transfer` | 5 | ≥ 1 |
| `vacuumIntervalTicks()` | `network.op-interval-ticks.vacuum` | 10 | ≥ 1 |
| `craftIntervalTicks()` | `network.op-interval-ticks.craft` | 20 | ≥ 1 |
| `itemsPerOp()` | `transfer.items-per-op` | 64 | ≥ 1 |
| `htMultiplier()` | `transfer.ht-multiplier` | 8 | ≥ 1 |
| `greedyCapacity()` | `greedy.capacity` | 262 144 | ≥ 1 |
| `barrelCapacity()` | `barrel.capacity` | 2 000 000 000 | ≥ 1 |
| `maxBlueprints()` | `crafter.max-recipes` | 18 | entre 1 y 18 |
| `vacuumRadius()` | `vacuum.radius` | 4.0 | ≥ 1.0 |
| `cellCapacity(tier)` | `cells.capacities` | lista por defecto (ver abajo) | clamping a última tier |
| `compatSlimefun()` | `compat.slimefun` | `true` | — |
| `debug()` | `debug` | `false` | — |
| `rakeUses()` | `rake.uses` | 250 | ≥ 1 |

**Capacidades de celdas por tier** (`cells.capacities`, lista de `long`): por defecto `[65536, 262144, 1048576, 16777216, 268435456, 2000000000]` para T1…T6. Si la clave falta o está vacía, se usa la fórmula geométrica `65536 × 2^(tier−1)` (solo T1 coincide con la lista). Si se pide una tier no declarada, se clamp a la última disponible (con aviso único en consola). `SettingsCellCapacityTest` cubre todos estos casos límite.

---

Siguiente página: [Los tests del plugin](Tests.md).