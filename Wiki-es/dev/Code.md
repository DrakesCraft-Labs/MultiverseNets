# ⚙️ Cómo funciona el código de MultiverseNets

Este documento explica el funcionamiento interno del plugin: cómo se representa una red, dónde y cómo
se persiste el estado, cómo fluyen ítems y fluidos, y cómo se organiza el código por capas. Está
pensado para quien quiera leer o modificar el código. Para el comportamiento de cada máquina desde el
punto de vista del jugador, ver el [README de la wiki](../README.md).

> Zona de desarrollo: [Estructura](Structure.md) · **Cómo funciona el código** · [Tests](Tests.md)

---

## 1. Visión general (capas)

```
┌───────────────────────────────┐
│  Capa de presentación (GUI)   │  com.chagui68.multiversenets.gui
├───────────────────────────────┤
│  Capa de eventos (listeners)  │  com.chagui68.multiversenets.listen
├───────────────────────────────┤
│  Capa de servicio (núcleo)    │  com.chagui68.multiversenets.net
│   + crafteo                   │  com.chagui68.multiversenets.craft
├───────────────────────────────┤
│  Capa de persistencia         │  com.chagui68.multiversenets.persist
│   (NodeBlob / NodeStore)      │
├───────────────────────────────┤
│  Base                         │  util / item / command / compat / api
└───────────────────────────────┘
```

Toda la lógica de red corre en el hilo principal del servidor (síncrona), sin carreras con el mundo.
Por eso el plugin **no es compatible con Folia**.

## 2. Ciclo de vida del plugin

Clase principal: `MultiverseNets extends JavaPlugin` (singleton con `MultiverseNets.instance()`;
expone `networks()`, `ticker()` y `blockListener()`).

**`onEnable()`**, en orden:
1. `saveDefaultConfig()`, `Keys.init(this)`, `Settings.refresh(this)`.
2. `SlimefunBridge.registerSerializationAliases()` y `SlimefunBridge.init(...)` — la integración con
   Slimefun solo se activa si Slimefun está instalado.
3. `ProtectionBridge.init(...)` — registra cada provider de protección cuyo plugin esté presente.
4. `Items.registerRecipes(this)` y `NodeStore.init(this)` (registro de controladores).
5. `new NetworkManager(this); networks.load()` — recrea las redes a partir de los controladores
   guardados.
6. Registra `BlockListener`, `GuiListener`, `ChatPrompts` y `CraftingListener`.
7. Vuelve a registrar las recetas un tick después y otra vez a los 100 ticks (para que un recargo de
   datapacks no las borre) y las desbloquea a los jugadores conectados.
8. `NetworkHologramManager.init`, una tarea periódica que vacía la caché de protección cada
   `protection.cache-ticks`, y por último arranca `NetworkTicker` y registra `/mvnets`.

**`onDisable()`**: detiene el ticker y la tarea de protección, borra todos los hologramas y ejecuta
`networks.saveAll()` (registro de controladores).

## 3. Claves persistentes (`util/Keys`)

Registro único de todas las `NamespacedKey` usadas en el `PersistentDataContainer` (PDC) de chunks e
ítems (namespace `multiversenets:`):

| Constante | Clave | Uso |
| --- | --- | --- |
| `DEVICE_TYPE` | `device_type` | Tipo de dispositivo en los ítems (nombre del enum `DeviceType`). |
| `WIRELESS_BIND` | `wireless_bind` | Coordenadas del controlador vinculado a un Terminal Inalámbrico. |
| `RECEIVER_BIND` | `receiver_bind` | Enlace del puente en un ítem Receptor o Transmisor (`mundo;x;y;z` del otro extremo). |
| `BLUEPRINT_RECIPE` | `blueprint_recipe` | Clave de receta antigua de un blueprint viejo. |
| `CHUNK_HAS_NODES` | `chunk_has_nodes` | Marca rápida de "este chunk puede tener nodos". |
| `TERMINAL_DISPLAY` | `terminal_display` | Ajustes de búsqueda/orden del terminal. |
| `CELL_CARGO` | `cell_cargo` | `NodeBlob` serializado (Base64) dentro del ítem de un dispositivo roto. |
| `BLUEPRINT_DATA` | `blueprint_data` | `RecipeData` codificado en Base64 en un Blueprint. |
| `CONFIG_DATA` | `config_data` | Llave: materiales copiados y modo `WL:`/`BL:`. |
| `CONFIG_ITEMS` | `config_items` | Llave: plantillas exactas del filtro (un `NodeBlob` codificado con `filterItems`). |
| `RAKE_USES` | `rake_uses` | Usos restantes del Network Rake. |
| `SF_BLUEPRINT` | `sf_blueprint` | Marca un Blueprint codificado desde una receta de Slimefun. |

## 4. Coordenadas (`util/PosUtil`)

Una posición 3D se empaqueta en un **único `long` de 64 bits**:

- **X → 26 bits** (bits 38–63), máscara `0x3FFFFFF`, rango ±33.554.431.
- **Z → 26 bits** (bits 12–37), mismo rango.
- **Y → 12 bits** (bits 0–11), rango −2048 … +2047.

`pack(x, y, z) = (x & 0x3FFFFFF) << 38 | (z & 0x3FFFFFF) << 12 | (y & 0xFFF)`. Los negativos se
resuelven con desplazamientos aritméticos en `unpack`. Las posiciones empaquetadas son las claves de
`Network.nodes` y sirven para localizar chunks (`PosUtil.unpackX(pos) >> 4`). `PosUtilTest` fija el
formato.

## 5. Persistencia (`persist/NodeBlob` y `persist/NodeStore`)

### 5.1 Estado de un nodo: `NodeBlob`
`Serializable` (UID fijo en `1L`, así los campos nuevos se leen con su valor por defecto en blobs
antiguos) con campos públicos:

| Campo | Tipo | Lo usa |
| --- | --- | --- |
| `typeName` | `String` | Todo nodo (nombre del `DeviceType`). |
| `cellSample` / `cellAmount` | `ItemStack` / `long` | Celdas Cuánticas e Infinity Barrel (el barril conserva `cellSample` al vaciarse). |
| `filterMaterials` / `filterItems` / `filterBlacklist` | `List<String>` / `List<ItemStack>` / `boolean` | Dispositivos con filtro. `filterItems` (plantillas exactas) manda sobre `filterMaterials` cuando no está vacío. |
| `targetFace` | `String` | Cara del Advanced Grabber/Pusher (`NORTH`… o `ALL`). |
| `transitBuffer` / `transitAmount` | `ItemStack` / `long` | Grabbers y Pushers: ítems en espera porque ni la red ni el origen los aceptaron. El `ItemStack` es una muestra de 1 unidad y la cantidad va en `transitAmount`, porque Paper no puede serializar un `ItemStack` de más de 99 (un búfer puede guardar 1.024). Usa siempre `transitStack()` / `setTransit()` / `addTransit()`. |
| `recipes` / `blueprintData` | `List<String>` | Crafters: claves de receta antiguas / `RecipeData` instalados (Base64). |
| `craftingMatrix` | `ItemStack[9]` | Plantilla del Encoder y de la Crafting Grid. |
| `encoderBlank` / `encoderOutput` | `ItemStack` | Blueprints dejados en las ranuras del Recipe Encoder. |
| `txWorld` / `txX` / `txY` / `txZ` | `String` / `int` | Enlace del puente de un Receptor o Transmisor (el otro extremo). |
| `greedySamples` / `greedyAmounts` | `List<ItemStack>` / `List<Long>` | Búfer multi-ítem de la Greedy Cell. |
| `virtualCacheTier` / `virtualSamples` / `virtualAmounts` | `int` / listas | Stock de un módulo de memoria de ítems: un DRAM Bay con módulo de ítems, o la caché antigua de un Controlador. |
| `installedModule` | `String` | DRAM Bay: nombre del `DeviceType` del módulo instalado, null si está vacío. |
| `dramFluids` / `dramFluidAmounts` | `List<String>` / `List<Long>` | DRAM Bay con Fluid DRAM Module: varios fluidos (mB). |
| `chickenActive` / `chickenPull` / `chickenProducts` / `chickenMinTier` / `chickenMaxTier` / `chickenKnown` / `chickenAge` / `chickenMinStrength` / `chickenPureOnly` | varios | Reglas del Genetic Chicken Sorter (ver §19). |
| `quotaSample` / `quotaLimit` / `quotaActive` | `ItemStack` / `long` / `boolean` | Quota Limiter. |
| `fluidType` / `fluidAmount` | `String` / `long` | Quantum Fluid Cell (mB). |
| `pumpFluid` (`pumpMode` heredado) | `String` | Filtro de la Liquid Pump (`WATER`, `LAVA`, null = cualquiera). |
| `ownerUuid` | `String` | Solo el controlador: el dueño de la red. |
| `crayon` | `boolean` | Campo heredado, sin uso. |

### 5.2 Lectura/escritura por chunk: `NodeStore`
El estado vive en el **PDC del chunk**, por bloque:
- `"n" + x + "_" + y + "_" + z` → blob completo (Base64).
- `"t" + x + "_" + y + "_" + z` → solo el nombre del tipo, para que el escaneo clasifique un bloque
  sin deserializar.

Cada `put()` marca además `CHUNK_HAS_NODES` en el chunk.

API: `put` / `get` (copia en cada lectura; null si el chunk no está cargado) / `canonical` /
`getType` / `hasNode` / `remove` / `countNodesInChunk` / `encode` / `decode`.
- **`canonical(Block)`** devuelve una instancia compartida ya decodificada. `NetworkStorage` lee cada
  celda en cada depósito y retirada, y decodificar Base64 + `BukkitObjectInputStream` ahí era el coste
  más alto del plugin. Cada `put` reemplaza la instancia compartida, así nunca es más vieja que la
  última escritura (`NodeStoreCanonicalTest`). La caché se vacía entera al pasar de 8.192 entradas.
- **`decode`** trata una entrada corrupta como ausente, en silencio y barato
  (`NodeStoreCorruptionTest`); también migra blobs antiguos (listas null, Greedy Cells de un ítem).

### 5.3 Registro de controladores (`networks.yml`)
`Map<UUID, List<String>>` (mundo → `"x,y,z"`) persistido en `<dataFolder>/networks.yml`. `save()`
escribe de forma asíncrona si se llama desde el hilo principal. `NetworkManager.load()` reconstruye
las redes a partir de él al arrancar.

## 6. La red (`net/Network` y `net/NetworkManager`)

### 6.1 Topología: `Network`
- Se identifica por la posición empaquetada del controlador. `nodes: Map<Long, DeviceType>` más el
  índice inverso `byType: Map<DeviceType, Set<Long>>`, para que el ticker solo recorra los tipos que
  necesita.
- `scan()` — **BFS** desde el controlador por los 6 vecinos ortogonales. El escaneo:
  - nunca carga chunks (salta vecinos sin cargar; con el controlador sin cargar deja la red como
    estaba);
  - vacía la red si el bloque del controlador ya no tiene blob (`controller missing`);
  - toma el dueño del blob del controlador y no se expande por terreno que ese dueño no pueda usar
    (`ProtectionBridge.mayActorUse`), contando los rechazos (`linksBlockedByProtection()`);
  - se detiene en otro controlador (`foreign controller at x,y,z`) y activa
    `touchesForeignController()`;
  - con Slimefun, trata como cables los bloques de Slimefun cuyo id contiene `CABLE`/`BRIDGE` y anota
    los barriles de Slimefun que toque (`slimefunBarrels()`);
  - respeta `network.max-nodes`; incrementa `version` e invalida la caché del almacenamiento.
- Consultas: `contains`, `typeAt`, `forEach(type, consumer)` (copia defensiva), `count(type)`,
  `block(pos)`, `ownerUuid()`, `error`.

### 6.2 Gestor: `NetworkManager`
- `networksByWorld: Map<UUID, Map<Long, Network>>`.
- `registerController` / `removeController`, `networkAt(Block)` (búsqueda lineal entre las redes del
  mundo), `networkByController(Location)`.
- `invalidateNear(Block)` — reescanea la red del bloque y las de sus 6 vecinos (colocar/romper/rake).
- Filtros: `filterPredicate(blob)` (filtro vacío → acepta todo; si no, whitelist o blacklist),
  `matchesFilter(template, item)` (orden: DeviceType → id de Slimefun → nombre visible → material),
  `extractMatching(Inventory, …)` (un tipo de ítem, juntando todas las ranuras hasta la cuota),
  `insertInto(Inventory, …)` (por trozos de un stack, así una ranura nunca recibe más de lo que el
  ítem admite) e `insertSmart(inv, muestra, cantidad, cara, kinds)` (la inserción del Pusher: un
  stack por ranura, entrada/combustible del horno según la cara y nunca la ranura de resultado, y con
  `kinds > 1` cada entrada de la whitelist ocupa como mucho `ranuras / kinds`). `extractMatching` de
  un horno solo saca el resultado.

## 7. Almacenamiento de ítems: `NetworkStorage`

Una sola "bóveda" sobre todo el almacenamiento de la red: los **módulos de memoria** (uno por DRAM
Bay con módulo de ítems, más un módulo antiguo dentro del Controlador), las **Celdas Cuánticas**, los **Infinity Barrels**, las **Greedy Cells** y los
**barriles de Slimefun**. Todos los métodos son `synchronized`; los blobs se leen con
`NodeStore.canonical` y solo se reescriben los modificados.

- **`deposit(ItemStack)` → sobrante**. Primero los **Quota Limiters** recortan la cantidad (gana el
  límite más bajo), luego 8 pasadas: (1) Greedy Cells cuyo filtro coincide o que ya tienen el ítem →
  (2) módulos de memoria con ese tipo → (3) barriles de Slimefun con ese tipo → (4) celdas/barriles con ese
  tipo → (5) espacio libre de los módulos de memoria → (6) barriles de Slimefun vacíos → (7) celdas/barriles
  vacíos (adoptan el tipo) → (8) Greedy Cells sin filtro. Nunca modifica el argumento.
- **`withdraw(matcher, want, excludePos, includeGreedy)`** — módulos de memoria → celdas y barriles →
  barriles de Slimefun → Greedy Cells (solo si `includeGreedy`). Devuelve un único tipo de ítem. Los
  Pushers, la succión de la Greedy y las dos direcciones del puente pasan `includeGreedy = false`;
  terminales, crafteo y la API usan la forma de 2 argumentos (Greedy incluida). Un Infinity Barrel
  conserva su `cellSample` al llegar a 0; una celda lo olvida.
- `count`, `remainingQuota`, `view()` (caché de 500 ms, agrupado con `StackUtils.itemsMatch`),
  `getPurgedItemsView`, `isItemPurged`, contadores.

## 8. Almacenamiento de fluidos: `NetworkFluidStorage`

Aparte de los ítems: la suma de todas las `MVN_FLUID_CELL` (un fluido por celda,
`fluids.cell-capacity-mb` cada una) y de los DRAM Bays con Fluid DRAM Module (varios fluidos,
`fluids.dram-capacity-mb` en total). Orden de depósito: celdas con ese fluido → Fluid DRAMs → celdas
vacías. `deposit(fluid, mB)` es **todo o nada**: devuelve `0` si cupo
todo y la cantidad completa (sin guardar nada) en caso contrario, porque cada llamante — bomba,
terminal, ranura de entrada — solo consume un cubo, botella o bloque fuente entero cuando recibe `0`.
`withdraw`, `count`, `getFluids`, `totalCapacity`, `totalStored`.

## 9. El latido: `NetworkTicker`

- `runTaskTimer(plugin, run, 20L, 5L)`; cuentas atrás por familia (`scanIn`, `transferIn`,
  `vacuumIn`, `craftIn`) que disparan cada familia en su intervalo configurado.
- En cada ejecución: las redes se ordenan por mundo y posición del controlador; se escanean las sucias
  o a las que les toca; si toca alguna operación, `assignSharedNodes` entrega cada nodo compartido por
  dos redes (`touchesForeignController`) a la primera, así **cada dispositivo trabaja una vez por
  ciclo**; después transferencias, vacuum y crafteo corren con `forEachWorked`; por último se
  actualiza el holograma dentro de un try/catch (un fallo del holograma se registra una vez y nunca
  detiene el bucle).
- **`doTransfers`** (`items-per-op` = 128, HT = ×`ht-multiplier`):
  - **Grabber** (`grabOnce`): reintenta primero su búfer de tránsito; luego la primera cara que dé
    algo — primero las ranuras de salida de máquinas de Slimefun, después contenedores vanilla.
    Sobrante → Pushers que lo acepten (`streamToPushers`) → búfer de tránsito. Nunca vuelve al
    origen: `SlimefunBridge.insert` usa las ranuras de ENTRADA y el producto se volvía a procesar.
    Los grabbers inactivos se relajan (uno de cada tres ciclos tras uno vacío, hasta 30).
  - **Pusher** (`pushOnce`): reintenta primero su búfer de tránsito; whitelist vacía = inactivo; los
    destinos son las caras de `facesFor` con un contenedor o máquina de Slimefun que no sea un nodo de
    la red (`isPushTarget`). Una whitelist de varias entradas da un predicado por entrada, rotando la
    primera en cada ciclo (`pushPredicates`); cualquier otro filtro prueba hasta 4 tipos distintos.
    Cada retirada (sin Greedy) se inserta con `insertToTarget` → `insertSmart` /
    `SlimefunBridge.insert(…, kinds)`; el resto vuelve a la red o al búfer (`returnToNetwork`).
  - **Genetic Chicken Sorter** (`chickenSortOnce`): solo con `chickenActive`; saca o mete hasta 16
    pollos que pasen `ChickenGenetics.matches`.
  - **Greedy Cell** (`greedyTick`): succión hasta `4 × items-per-op`, reparto hasta
    `2 × items-per-op` a contenedores vecinos que no son de la red.
  - **Purger**: solo con filtro no vacío.
  - **Receptor** (`bridgeOnce`) / **Transmisor** (`transmitOnce`): mueve ítems el extremo que guarda
    el enlace. Regla de filtro `bridgeFilterSet`: whitelist no vacía o cualquier blacklist. Ambos
    extremos pasan la protección con el dueño de su propia red; los sobrantes vuelven al origen y, si
    no, caen junto al dispositivo.
  - **Liquid Pump** (`pumpTick`): un bloque fuente debajo, solo si `deposit` devuelve 0.
- **`doVacuum`**: entidades `Item` sin retardo de recogida dentro de `vacuum.radius`.
- **`doCrafting`**: cada Auto-Crafter (y Slimefun Auto-Crafter si `sf-crafter.enabled`) intenta una
  vez cada Blueprint instalado.
- Cada bloque que se toca pasa `denied(net, block)` → `ProtectionBridge.mayActorUse(block, owner)`.

## 10. Dispositivos e ítems (`item/DeviceType`, `item/Items`)

`DeviceType` enumera los **46** dispositivos, módulos y herramientas; cada constante tiene `material`,
`display`, `placeable` y `cellTier`. Propiedades derivadas: `isCell()`, `isBarrel()`, `isFluidCell()`,
`isLiquidPump()`, `isRequestTerminal()`, `isAutoCrafter()`, `isRequestCrafter()`,
`isSlimefunCrafter()`, `filterable()` (grabbers, pushers, vacuum, greedy cell, purger, receptor,
transmisor), `isImporter()`/`isExporter()`, `isDirectional()`, `isRouter()`, `isCacheModule()`,
`isMemoryModule()` (módulos de caché + Fluid DRAM), `cacheTier()`. `parse(name)` acepta `MVN_…`, la forma sin prefijo y `wireless`.

| Constante | Material | Nombre visible | Colocable |
| --- | --- | --- | --- |
| `MVN_CONTROLLER` | LODESTONE | Network Controller | ✔ |
| `MVN_CABLE` | GLASS | Network Cable | ✔ |
| `MVN_TERMINAL` | BEACON | Network Terminal | ✔ |
| `MVN_MONITOR` | RESPAWN_ANCHOR | Network Monitor | ✔ |
| `MVN_ROUTER` | LIGHTNING_ROD | Network Router | ✔ |
| `MVN_CACHE_L1` … `MVN_CACHE_QUANTUM` | COPPER_INGOT, GOLD_INGOT, DIAMOND, NETHERITE_INGOT, NETHER_STAR | Módulos de memoria de ítems | ✘ (mano) |
| `MVN_DRAM_BAY` | WAXED_COPPER_BULB | DRAM Bay | ✔ |
| `MVN_FLUID_DRAM` | HEART_OF_THE_SEA | Fluid DRAM Module | ✘ (mano) |
| `MVN_CELL_T1` … `MVN_CELL_T6` | Terracota por nivel | Quantum Cell T1…T6 | ✔ |
| `MVN_GREEDY_CELL` | SLIME_BLOCK | Greedy Cell | ✔ |
| `MVN_INFINITY_BARREL` | BARREL | Infinity Barrel | ✔ |
| `MVN_GRABBER` / `MVN_GRABBER_HT` | OBSERVER / STICKY_PISTON | Simple / Advanced Grabber | ✔ |
| `MVN_PUSHER` / `MVN_PUSHER_HT` | TARGET / PISTON | Simple / Advanced Pusher | ✔ |
| `MVN_VACUUM` | SPONGE | Network Vacuum | ✔ |
| `MVN_PURGER` | MAGMA_BLOCK | Network Purger | ✔ |
| `MVN_LIMITER` | TARGET | Network Quota Limiter | ✔ |
| `MVN_PROBE` | SPYGLASS | Network Probe | ✘ (mano) |
| `MVN_CRAFTER` / `MVN_SF_CRAFTER` | CRAFTING_TABLE / CRYING_OBSIDIAN | Auto-Crafter / Slimefun Auto-Crafter | ✔ |
| `MVN_REQUEST_CRAFTER` / `MVN_SF_REQUEST_CRAFTER` | FLETCHING_TABLE / PURPUR_PILLAR | Request Crafter / Slimefun Request Crafter | ✔ |
| `MVN_REQUEST_TERMINAL` | LECTERN | Request Terminal | ✔ |
| `MVN_ENCODER` / `MVN_SF_ENCODER` | SMITHING_TABLE / ENCHANTING_TABLE | Recipe Encoder / Slimefun Recipe Encoder | ✔ |
| `MVN_CRAFTING_GRID` | CARTOGRAPHY_TABLE | Network Crafting Grid | ✔ |
| `MVN_QUANTUM_WORKBENCH` | BRAIN_CORAL_BLOCK | Quantum Workbench | ✔ |
| `MVN_TRANSMITTER` / `MVN_RECEIVER` | CONDUIT / REDSTONE_LAMP | Wireless Transmitter / Receiver | ✔ |
| `MVN_WIRELESS_TERMINAL` | NETHER_STAR | Wireless Terminal | ✘ (mano) |
| `MVN_BLUEPRINT` | BOOK | Blueprint | ✘ (mano) |
| `MVN_CONFIGURATOR` | COMPARATOR | Configuration Wrench | ✘ (mano) |
| `MVN_RAKE` | DEAD_BUSH | Network Rake | ✘ (mano) |
| `MVN_FLUID_CELL` | PRISMARINE_BRICKS | Quantum Fluid Cell | ✔ |
| `MVN_LIQUID_PUMP` | BLUE_STAINED_GLASS | Liquid Pump | ✔ |
| `MVN_CHICKEN_SORTER` | HAY_BLOCK | Genetic Chicken Sorter | ✔ |

`Items`:
- `create(type)` construye el ítem (nombre, lore, `DEVICE_TYPE`); `typeOf(item)` lo lee.
- `capacityOf(type)` — celdas, barril, greedy cell y módulos de caché desde `Settings`.
- Herramientas: `rake()`/`rakeUses`/`spendRakeUse`; `saveConfig`/`readConfig` y
  `saveConfigItems`/`readConfigItems` (llave); `linkReceiver`/`readReceiverBind` (enlace del puente,
  usado tanto en ítems Receptor como Transmisor); `bindWireless`/`readWirelessBind`.
- `registerRecipes(plugin)` — las **46** recetas con forma (43 siempre, más el codificador y los dos
  crafters de Slimefun mientras sus `enabled` estén activos). Patrones: [Recipes.md](../Recipes.md).
  Un dispositivo usado como ingrediente se registra con `device(receta, char, tipo)`: un
  `MaterialChoice` del material del dispositivo más una entrada en `deviceIngredients(key)` (tipo →
  cantidad). `isUpgradeRecipe(key)` marca las mejoras de nivel de celdas y módulos.
- `GuideContent` guarda el texto de la guía del juego en inglés y español: una entrada por
  dispositivo (nombre en español, qué hace, cómo se usa), su categoría y los temas generales. Las
  recetas no se escriben ahí: `GuideMenu` las lee de las registradas (`Items.deviceIngredientAt(key,
  char)` indica qué letras de la cuadrícula son dispositivos).

## 11. Crafteo (`craft/Blueprints` y `craft/CraftingSupport`)

- `RecipeData` — `inputs[9]` (cantidad 1, `null` = vacío) y `output`. `Blueprints.encode/decode`
  (serialización Java + Base64), `toItem`, `read`, `isBlueprint`.
- `Blueprints.resolve(matrix, world)` resuelve la matriz contra las recetas vanilla del servidor (con
  caché); `matchesOutput` exige que esa receta siga dando el resultado guardado, así que **si el
  servidor cambia la receta, el blueprint deja de funcionar**.
- `CraftingSupport.tryCraftBlueprint(net, data)` — **todo o nada**:
  1. Resuelve el resultado (receta vanilla; con Slimefun, la receta de Slimefun; el resultado guardado
     solo se acepta tal cual para ítems de Slimefun).
  2. Agrupa las necesidades por ítem, comprueba con `count` y luego hace `withdraw` de cada una; un
     fallo a mitad devuelve todo lo tomado (`returnOrDrop`: lo deposita de nuevo y suelta junto al
     controlador **solo** lo que no cupo).
  3. Deposita el resultado. Si solo cabe en parte, la parte guardada se vuelve a retirar y se
     devuelven los ingredientes: el crafteo ocurre entero o no ocurre.
- `tryCraftOnce` / `tryCraftAll` — variante antigua por clave de receta con el mismo rollback.
- `RequestTerminalMenu` planifica las cadenas con un stock simulado (`planCraft`), ejecuta paso a paso
  con un búfer intermedio y devuelve a la red los intermedios sobrantes.

## 12. Capa de GUI (`gui/`)

### 12.1 Base: `MenuHolder`
`InventoryHolder` abstracto. `open(size, title)` crea el inventario, llama a `draw()` y lo abre;
`refresh()` redibuja conservando el contenido de `vanillaSlots()`. Ayudas: `giveOrDrop`,
`playerInventorySlot(event)`. Cada menú implementa `draw()` y `click(event)`; `onClose` es opcional.

| Menú | Tamaño | Uso / detalles |
| --- | --- | --- |
| `TerminalMenu` | 54 | Terminal (bloque, inalámbrico, botones de transmisor/receptor). Entrada `INPUT_SLOT=8`, vista del purgador `17`, orden `26`, página de fluidos `35`, páginas `44`/`53`; 48 ítems por página. Depósito/retirada de fluidos con cubos y botellas. |
| `ControllerMenu` | 27 | Estado del controlador y del router; la ranura `11` expulsa un módulo de memoria antiguo con sus ítems. |
| `DramBayMenu` | 27 | Estadísticas `11`, módulo `13` (instalar desde el cursor/shift-clic, o expulsar), expulsar `15`. Lee el blob en cada clic, así dos jugadores no pueden expulsar a la vez. |
| `ChickenSorterMenu` | 54 | Productos `0–17`, activo `27`, push/pull `28`, cara `29`, nivel mín/máx `31`/`32`, fuerza `33`, ADN `34`, edad `35`, puros `40`, limpiar `44`, ayuda `49`. |
| `MonitorMenu` | 27 | Diagnóstico en vivo (tarea de refresco mientras está abierto). |
| `FilterMenu` | 27 | Grabbers, pushers, vacuum, purger, greedy cell, receptor y transmisor: hasta 17 plantillas, modo `17`, limpiar `25`, ayuda `26`. Ranura `24`: selector de cara en los avanzados, "abrir bloque adyacente" en los simples, "abrir terminal" en Transmisor/Receptor. |
| `CellMenu` / `BarrelMenu` | 18 | Plantilla `4`, depositar todo `11`, fijar ítem `13`, extraer todo `15`. En el barril, clic derecho en *Set Item* borra el registro si está vacío. |
| `GreedyMenu` | 54 | 36 ranuras de almacenamiento con páginas, filtro `45`, depósito `46`, monitor `49`, dirección `50`, info `53`. |
| `CrafterMenu` | 27 | Auto/Request/Slimefun crafters: hasta 18 Blueprints, estado `24`, limpiar todo `25` (los devuelve), ayuda `26`. Instalar consume el Blueprint; desinstalar o reemplazar lo devuelve. |
| `EncoderMenu` / `SfEncoderMenu` | 45 | Plantilla, ranura de blueprint `19`, codificar `16`, vista previa `25`, salida `34`. Los dos codificadores guardan en el bloque los Blueprints dejados en `19`/`34`; mientras un menú está abierto solo viven en ese menú. |
| `CraftingGridMenu` | 54 | Crafteo con la red: resultado `31`, craftear uno `33`, craftear todo `35`, limpiar `38`, páginas `27`/`29`, info `41`. |
| `RequestTerminalMenu` | 54 | 45 opciones por página, entrega `49`, refrescar `51`, páginas `45`/`53`. |
| `QuotaLimiterMenu` | 36 | Ítem objetivo `13`, activar/desactivar `22`, límite por chat `31`, botones ±1/10/64/1.000. |
| `FluidCellMenu` | 27 | Tanque `13`, interacción con cubo `10`, extraer un cubo `15`, vaciar tanque `16` (shift+clic derecho). |
| `LiquidPumpMenu` | 27 | Filtro de fluido `12` (ANY/WATER/LAVA), fluidos de la red `14`. |
| `QuantumWorkbenchMenu` | 45 | Mejora de celdas: centro `20`, craftear `23`, salida `25`; ingredientes devueltos al cerrar. |
| `GuideMenu` | 54 | `/mvnets guide`: inicio (temas `19`, categorías `21–25`, `30–32`), temas, lista de la categoría, página del dispositivo (receta `10-12/19-21/28-30`, resultado `24`, qué hace `15`, cómo se usa `16`, números `33`, ingredientes `34`, anterior/siguiente `48`/`50`, volver `45`, inicio `49`); el botón de idioma `53` conserva la página. Las páginas se reabren un tick después. |
| `ChatPrompts` | — | Preguntas numéricas por chat (request terminal, limitador). |

### 12.2 Seguridad: `GuiListener` (anti-dupe)
Para todo inventario superior que sea un `MenuHolder`: cancela doble clic, clic central, teclas
numéricas, cambio a la otra mano, soltar, acciones de creativo, `COLLECT_TO_CURSOR`, movimientos de la
barra rápida y `UNKNOWN`; del inventario del jugador solo llegan al menú los shift+clic; las ranuras
superiores no vanilla se cancelan y se envían a `click`; los arrastres sobre ranuras no vanilla se
cancelan; `onClose` se reenvía.

## 13. Eventos (`listen/`)

`BlockListener` lleva el cableado de eventos; `DeviceInteractions` decide qué abre cada dispositivo y
la puerta de acceso del jugador (`canAccessNetwork`, estático: bypass de admin, providers de
protección, pertenencia a la isla de BentoBox), instala módulos de memoria en un DRAM Bay y gestiona la
interacción rápida con la celda de fluidos.

| Evento | Comportamiento |
| --- | --- |
| `BlockPlaceEvent` | Comprueba mundos bloqueados y `max-nodes-per-chunk`; registra el nodo, restaura el estado embebido (`CELL_CARGO`), aplica el enlace del puente desde el ítem (Receptor o Transmisor), guarda el dueño del Controlador y luego registra el controlador o reescanea los vecinos. |
| `BlockBreakEvent` | Suelta los Blueprints guardados del Encoder y el módulo de un DRAM Bay (con su stock, también en creativo), suelta el dispositivo con su estado embebido (nada en creativo), quita el nodo y reescanea. |
| `PlayerInteractEvent` | Clic al aire con Terminal Inalámbrico (bloqueo por combate, alcance/mundo salvo con Router, acceso). Clic en bloque: acceso, luego Probe, Rake (devuelve el dispositivo), Llave, vínculos (inalámbrico en controlador/terminal; ítem Receptor en Transmisor e ítem Transmisor en Receptor), agachado nunca abre menús, mensaje de estado del cable, un módulo sobre un Controlador solo muestra un aviso, instalación de módulos en un DRAM Bay vacío, interacción rápida con la celda de fluidos, menú del dispositivo. |
| `InventoryMoveItemEvent` | Se cancela siempre que el origen o el destino sea un nodo de la red, sin tocar ningún inventario. Las tolvas nunca interactúan con el plugin; el antiguo camino del Infinity Barrel llamaba a `removeItem` mientras Paper había reducido la ranura de la tolva a la cantidad movida, y eso borraba el stack entero. |
| Pistones / explosiones | Los nodos no se pueden mover y se quitan de las listas de bloques de las explosiones. |
| `EntityDamageByEntityEvent` | Anota el momento del combate para el bloqueo del Terminal Inalámbrico. |

`CraftingListener` vuelve a registrar las recetas tras recargas y las desbloquea al entrar. En
`PrepareItemCraftEvent` solo actúa si el servidor casó una receta: en las de este plugin los
dispositivos de la mesa deben ser exactamente `Items.deviceIngredients(key)`; un dispositivo con
`CELL_CARGO` solo lo acepta una receta de mejora, cuyo resultado recibe esa carga; cualquier otra
receta con un dispositivo en la mesa se queda sin resultado.

## 14. Comando `/mvnets` (`command/MvnetsCommand`)

Subcomandos: `help`, `info`, `guide`, `devices` (abiertos) y `give <id> [n]`, `doctor`, `stats`,
`inspect`, `repair`, `recipes`, `reload` (**`multiversenets.admin`**). El autocompletado sugiere
subcomandos, `en|es` para `guide` (abre `GuideMenu`) y los ids de dispositivo para `give` (`type.id()`, p. ej.
`mvn_controller`; `give` también acepta la forma sin prefijo).

## 15. API pública (`api/MultiverseNetsAPI`)

Métodos estáticos y null-safe para otros plugins, todos a partir de cualquier bloque de una red:
`isNetworkBlock(block)`, `extract(block, matcher, amount)`, `insert(block, stack)` (devuelve el
sobrante) y `count(block, matcher)`. Van directos a `NetworkStorage`, así que se aplican las cuotas y
el orden de almacenamiento.

## 16. Integración con Slimefun (`compat/SlimefunBridge`)

- Las máquinas de Slimefun guardan su inventario en un `BlockMenu`, no en un `InventoryHolder`; sin el
  puente parecerían bloques decorativos.
- **Solo por reflexión** — prueba los paquetes `com.github.drakescraft_labs.slimefun4.legacy` e
  `io.github.thebusybiscuit.slimefun4.legacy`. Con `compat.slimefun: false` o sin Slimefun queda
  dormido (`isAvailable()` = false).
- API: `isMachine`, `getId(Block)` / `getId(ItemStack)`, `extract` (ranuras de salida + `WITHDRAW`),
  `insert` (ranuras de entrada + `INSERT`), `isNetworkCable`, `isBarrel` y depósito/retirada de
  barriles, `findSlimefunRecipe`, `openSlimefunMenu`. Se mantienen los alias en español (`disponible`,
  `esMaquina`, `idDe`, `esItemSlimefun`, `extraer`, `insertar`).

## 17. Protección de terrenos (`compat/ProtectionBridge`)

- **Por qué**: una red es un actor anónimo; sin esto, un grabber en terreno público podría leer un
  cofre dentro de la región de otro jugador.
- **Dueño**: el Controlador guarda el UUID de quien lo colocó (`ownerUuid`); `mayActorUse(lugar,
  dueño)` permite a la red operar en el terreno de ese dueño. Un provider certifica la propiedad con el
  método opcional `allowsActor(UUID, Location)` (lo implementa ProtectionStones); los demás devuelven
  `null` y sus reclamos siguen cerrados para toda red. Un provider que lanza una excepción nunca da
  acceso.
- **Controladores antiguos** sin dueño adoptan al primer jugador que `mayPlayerAccess` permite
  (`DeviceInteractions.adoptControllerOwner`).
- **Dónde se aplica**: `Network.scan()` (el BFS se detiene en terreno que el dueño no puede usar y
  cuenta el corte) y `NetworkTicker` antes de cada bloque que toca — grabbers, pushers, desvío a
  pushers, reparto de greedy, vacuum, bomba y ambos extremos del puente en los dos sentidos. Los
  jugadores que abren dispositivos pasan por `canAccessNetwork`, incluido el botón de terminal remoto
  del Receptor.
- **Providers** (un archivo cada uno, solo se registran si su plugin y su API se resuelven):
  `ProtectionStonesProvider` (`PSRegion.fromLocationUnsafe`), `WorldGuardProvider`, `LandsProvider`,
  `TownyProvider`, `GriefPreventionProvider`. Los métodos se enlazan por firma exacta y solo se
  comprueba la pertenencia a una región, nunca flags.
- **Salvedad de ProtectionStones**: su API no distingue un reclamo de una región del servidor, así que
  `protection.allow-claims` no se le aplica.
- **Rendimiento**: las respuestas se memorizan por mundo y posición (y por dueño en `ownsAt`) y se
  descartan cada `protection.cache-ticks`.
- **Salidas**: `protection.exempt-worlds`, `protection.exempt-locations` (`mundo;x;y;z;radio`, radio
  16 por defecto), `protection.block-network-linking: false` y el permiso
  `multiversenets.protection.bypass` para jugadores.

## 18. Configuración (`util/Settings`)

Todas las lecturas pasan por `Settings` sobre `plugin.getConfig()` (se refresca en `onEnable` y con
`/mvnets reload`).

| Método | Clave de `config.yml` | Por defecto | Límites |
| --- | --- | --- | --- |
| `scanIntervalTicks()` | `network.scan-interval-ticks` | 20 | ≥ 5 |
| `maxNodes()` | `network.max-nodes` | 16.384 | ≥ 16 |
| `maxNodesPerChunk()` | `network.max-nodes-per-chunk` | 64 | ≥ 1 |
| `transferIntervalTicks()` | `network.op-interval-ticks.transfer` | 5 | ≥ 1 |
| `vacuumIntervalTicks()` | `network.op-interval-ticks.vacuum` | 10 | ≥ 1 |
| `craftIntervalTicks()` | `network.op-interval-ticks.craft` | 20 | ≥ 1 |
| `itemsPerOp()` | `transfer.items-per-op` | 128 | ≥ 1 |
| `htMultiplier()` | `transfer.ht-multiplier` | 8 | ≥ 1 |
| `cellCapacity(tier)` | `cells.capacities` | lista de abajo | se ajusta al último nivel |
| `virtualCacheCapacity(tier)` | `virtual-cache.tier-1` … `tier-5` | 2.048 … 524.288 | — |
| `greedyCapacity()` | `greedy.capacity` | 262.144 | ≥ 1 |
| `barrelCapacity()` | `barrel.capacity` | 2.000.000.000 | ≥ 1 |
| `fluidCellCapacity()` | `fluids.cell-capacity-mb` | 64.000 | ≥ 1.000 |
| `fluidDramCapacity()` | `fluids.dram-capacity-mb` | 512.000 | ≥ 1.000 |
| `maxBlueprints()` | `crafter.max-recipes` | 18 | 1 – 18 |
| `vacuumRadius()` | `vacuum.radius` | 4.0 | ≥ 1.0 |
| `rakeUses()` | `rake.uses` | 250 | ≥ 1 |
| `wirelessLocalRange()` | `wireless.local-range-without-router` | 64 | ≥ 1 |
| `wirelessCombatCooldownSeconds()` | `wireless.combat-cooldown-seconds` | 10 | ≥ 1 |
| `blockedWorld(world)` | `blocked-worlds` | `[]` | sin distinguir mayúsculas |
| `compatSlimefun()` | `compat.slimefun` | `true` | — |
| `sfEncoderEnabled()` / `sfCrafterEnabled()` | `sf-encoder.enabled` / `sf-crafter.enabled` | `true` | — |
| `protectionEnabled()` | `protection.enabled` | `true` | — |
| `protectionProviderEnabled(id)` | `protection.providers` | lista vacía = todos | sin distinguir mayúsculas |
| `protectionAllowClaims()` | `protection.allow-claims` | `false` | config ausente = `false` |
| `protectionBlocksNetworkLinking()` | `protection.block-network-linking` | `true` | — |
| `protectionBlocksPlayerInteraction()` | `protection.deny-player-interaction` | `true` | — |
| `protectionBypassPermission()` | `protection.bypass-permission` | `multiversenets.protection.bypass` | vacío quita el bypass |
| `protectionCacheTicks()` | `protection.cache-ticks` | 100 | ≥ 20 |
| `protectionExemptWorlds()` / `protectionExemptLocations()` | `protection.exempt-worlds` / `exempt-locations` | `[]` | entradas ilegibles se descartan |
| `debug()` | `debug` | `false` | — |

**Capacidades de celda** (`cells.capacities`, lista de `long`): por defecto `[65536, 262144, 1048576,
16777216, 268435456, 2000000000]`. Clave ausente o vacía → `65536 × 2^(nivel−1)`; un nivel no
declarado usa el último, con un aviso único en consola (`SettingsCellCapacityTest`).

## 19. GeneticChickengineering (`compat/ChickenGenetics`)

Lee los pollos de bolsillo directamente de su PDC, sin depender del addon:
`geneticchickengineering:gce_pocket_chicken_dna` (`int[7]`: seis estados de gen 0 = aa, 1 = Aa,
3 = AA, más "conocido"), `gce_pocket_chicken_adapter` (JSON, `baby`) y `gce_expanded_species` (especies
especiales, nivel 7-9). `read(item)` devuelve producto (`TYPE:<tipo>` o `SPECIES:<id>`), nivel (genes
recesivos), fuerza de ADN (`6 − recesivos − mixtos`), puro, conocido y adulto, igual que el
`PocketChickenData` del addon. `matches(blob, item)` aplica las reglas del clasificador (todas deben
cumplirse; por defecto aceptan cualquier pollo y lo que no es un pollo nunca pasa).

## 20. Comparación de ítems (`util/StackUtils`)

`itemsMatch(a, b)` es una comparación estricta (material, clase de meta, datos por subtipo, custom
model data, PDC, encantamientos, flags, lore, nombre) con una alternativa para ítems con datos de
plugin: dos ítems con el mismo PDC no vacío, encantamientos y daño cuyo nombre y lore se leen igual
como texto plano son el mismo ítem. Eso permite que un Infinity Barrel vuelva a aceptar un ítem de
Slimefun cuyo nombre se guardó con otra estructura de componentes. `pdcMatches` compara todos los
tipos de tag primitivos y de array y los contenedores anidados; un tipo que no sabe comparar cuenta
como distinto (antes contaba como igual, y fundía pollos de bolsillo con distinto ADN).
