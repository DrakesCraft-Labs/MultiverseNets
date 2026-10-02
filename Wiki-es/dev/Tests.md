# 🧪 Tests del plugin MultiverseNets

Este documento explica cómo funcionan los tests del proyecto y qué cubre cada uno. Pensado para quien
quiera ejecutarlos, entenderlos o ampliarlos.

> Zona de desarrollo: [Estructura](Structure.md) · [Cómo funciona el código](Code.md) · **Los tests**

---

## 1. Ejecutar los tests

Los tests son **JUnit 5 (Jupiter)** y no necesitan un servidor de Minecraft real:

```
mvn test
```

Un build completo (`mvn clean package`) también los ejecuta. Requisitos: JDK 21 y Maven.

## 2. Infraestructura: MockBukkit

La mayoría de tests usan **[MockBukkit](https://github.com/MockBukkit/MockBukkit)**, un servidor
Bukkit en memoria:

```java
@BeforeEach void setUp() {
    server = MockBukkit.mock();                     // servidor falso
    plugin = MockBukkit.load(MultiverseNets.class); // ejecuta onEnable
    world = server.addSimpleWorld("world");
    player = server.addPlayer();
}

@AfterEach void tearDown() {
    MockBukkit.unmock();
}
```

Así se pueden colocar bloques, lanzar eventos (`server.getPluginManager().callEvent(...)`), simular
clics de inventario, explosiones y pistones, ejecutar el ticker
(`new NetworkTicker(plugin, plugin.networks()).tick()`) y leer el estado guardado en el PDC de los
chunks.

A tener en cuenta:
- Un test que coloca un dispositivo a mano debe guardar también su blob
  (`NodeStore.put(block, NodeBlob.create(type.name()))`); sin él, el bloque no es un nodo.
- MockBukkit no implementa `Display.setBillboard` ni `HumanEntity.openWorkbench`. El ticker captura
  los fallos del holograma, así que los tests del ticker corren con normalidad; el único test que
  llama directamente a la API del holograma (`FluidAndRequesterTest.testHologramRedesignNoFlowOrRouted`)
  aparece como **skipped**. Los tests de crafteo abren un
  `server.createInventory(player, InventoryType.WORKBENCH)` en vez de `openWorkbench`.
- `src/test/java/dev/espi/protectionstones/PSRegion.java` es un stub de la API de ProtectionStones que
  los tests del provider cargan por reflexión.

## 3. Resumen: 35 clases, 262 tests

| Clase (paquete `com.chagui68.multiversenets` salvo que se indique) | Tests | Cubre |
| --- | --- | --- |
| `BlockFlowsTest` | 24 | Romper/colocar con estado embebido, pistones, explosiones, vínculo inalámbrico, rake, llave, corte por agachado, dimensiones, enlace del puente desde el ítem Transmisor, estado del cable. |
| `BlueprintDupeTest` | 3 | Los Blueprints del Encoder nunca se duplican (dos jugadores, romperlo con el menú abierto); instalar un Blueprint lo consume y *Clear All* lo devuelve. |
| `CellGuiTest` | 9 | Menú de la Celda Cuántica: plantilla, depósito rápido, retirada, capacidad, sin duplicación al correr el ticker. |
| `ChickenSorterTest` | 3 | Los genes de GeneticChickengineering se leen como en el addon (producto, nivel, fuerza, pureza, edad, especies especiales); todas las reglas deben cumplirse; el clasificador solo mueve los pollos que cumplen, solo si está activo, y nunca otros ítems. |
| `CrafterGuiTest` | 10 | Menú del crafter: instalar/desinstalar/limpiar; los crafters de Slimefun aceptan Blueprints de Slimefun y vanilla, los estándar rechazan los de Slimefun. |
| `DramBayTest` | 8 | Un módulo en un DRAM Bay guarda ítems; un módulo expulsado lleva su stock a otra red; romper el bay suelta el módulo con su stock; el Fluid DRAM guarda varios fluidos y viaja con ellos; un controlador ya no acepta módulos; un módulo antiguo del controlador espera en el Terminal como ítem temporal y vuelve con sus ítems; el lore del Terminal muestra el total y su desglose por almacenamiento (celdas + Greedy suman, contadas una vez); romper un controlador suelta los módulos no recogidos. |
| `DeviceTypeTest` | 7 | Clasificación de `DeviceType`: dispositivos con filtro, la Greedy Cell no es celda, ítems de mano, dispositivos direccionales, request y crafters de Slimefun. |
| `FilterGuiTest` | 15 | Menú de filtro: añadir/quitar plantillas, whitelist/blacklist, shift+clic, caras, limpiar. |
| `FluidAndRequesterTest` | 13 | Almacenamiento de fluidos e interacción rápida con la celda, Liquid Pump, página de fluidos del terminal, Request Terminal (pedidos, cantidad por chat, cadenas recursivas, ignora Auto-Crafters, Slimefun Request Crafter), Slimefun Auto-Crafter. |
| `GuideMenuTest` | 6 | Cada dispositivo está documentado en los dos idiomas y se encuentra su receta; `/mvnets guide en|es` abre el menú (sin libro); navegar a un dispositivo muestra su receta real; el cambio de idioma conserva la página; los ingredientes que son dispositivos abren su página; no se puede sacar nada. |
| `GrabberQuotaTest` | 8 | `extractMatching`: respeta toda la cuota por ciclo (también HT), junta ranuras del mismo ítem, no toca otros ítems. |
| `GreedyCellTest` | 8 | Greedy Cell: almacenamiento multi-ítem, capacidad compartida, menú e integración con el terminal. |
| `GuiDupeGuardTest` | 3 | `GuiListener` cancela los clics peligrosos (también en los menús de celda y barril); los depósitos con shift+clic nunca duplican. |
| `GuiFlowsTest` | 10 | Flujos de Terminal, Encoder, Auto-Crafter (crafteo atómico), Crafting Grid y Monitor. |
| `InfinityBarrelTest` | 4 | Capacidad del barril, menú, integración con la red y persistencia al romper/colocar. |
| `NetworksCoexistenceTest` | 5 | Nombre, clase principal, comandos y permisos nunca chocan con NetworksV6; Slimefun es dependencia blanda. |
| `NewDevicesTest` | 6 | Propiedades del Purger y la Probe; todo `DeviceType` tiene material y nombre. |
| `PluginResourcesTest` | 3 | `plugin.yml` y `config.yml` en el classpath; comprobación de versión. |
| `PosUtilTest` | 2 | El empaquetado de coordenadas ida y vuelta, incluidos bordes del mundo e Y negativa. |
| `QuantumWorkbenchTest` | 2 | La mejora de celdas conserva la carga; los ingredientes se devuelven al cerrar. |
| `RecipeTest` | 7 | Cada receta registrada una vez, las recetas de cable y celda funcionan, una celda con carga casa con su receta de mejora y la conserva. |
| `RecipeValidationTest` | 5 | Los ingredientes dispositivo se registran por receta; un material simple no sustituye a un dispositivo; las mejoras de celdas y módulos conservan su carga; una celda de fluidos con fluido se rechaza; los dispositivos nunca alimentan recetas vanilla. |
| `ReportedIssuesTest` | 8 | El Advanced Pusher nunca pierde ítems (un stack por ranura); una whitelist de varios ítems deja sitio a cada ingrediente; lo que está en la blacklist se queda en la red; nada entra en el inventario del bloque de un Infinity Barrel; las tolvas nunca tocan un dispositivo; los ítems custom coinciden aunque su nombre se guarde distinto; el barril acepta de vuelta un ítem custom que entregó; el Slimefun Recipe Encoder guarda sus Blueprints. |
| `SettingsCellCapacityTest` | 9 | Valores por defecto y casos límite de `Settings` (capacidades, límites, config null). |
| `SlimefunBridgeTest` | 5 | El puente de Slimefun queda inerte y nunca lanza excepciones sin Slimefun. |
| `ToolsTest` | 3 | Llave y Rake son herramientas de mano; el Receptor tiene filtro; los filtros empiezan en whitelist. |
| `TransmissionFixesTest` | 13 | Transmisión de ítems y fluidos: depósitos de fluido todo o nada, la bomba nunca duplica fluido, puente con filtro solo de plantillas, el puente nunca vacía Greedy Cells, un dispositivo compartido por dos controladores trabaja una vez por ciclo, los resultados de crafteo parciales se deshacen, la llave pega plantillas exactas, el rake devuelve el dispositivo, filtros/cara/búfer de tránsito sobreviven a romper y colocar, los búferes de tránsito de más de 99 unidades se guardan sin fallar. |
| `UpgradedFeaturesTest` | 6 | Módulo de memoria en un DRAM Bay, Router, límite de nodos por chunk, búfer de tránsito del grabber, caché conservada al romper, romper en creativo no suelta nada. |
| `compat.NetworkOwnershipTest` | 9 | Una red funciona dentro del reclamo de su dueño; otras redes y un dueño null son extraños; el terreno público sigue abierto; providers rotos o sin conectar no dan acceso; las respuestas de dueño no se filtran entre redes. |
| `compat.ProtectionStonesProviderTest` | 18 | El provider de ProtectionStones contra la forma real de la API (`PSRegion.fromLocation*`, firmas exactas), certificación de dueño/miembro, comportamiento cerrado ante fallos; la búsqueda de regiones de WorldGuard falla de forma segura. |
| `compat.ProtectionWhitelistTest` | 15 | Valores por defecto de la protección y parseo y geometría de `exempt-locations`; el puente queda inerte sin providers. |
| `listen.SneakingRightClickTest` | 3 | Agachado + clic derecho nunca abre el menú de un dispositivo y sigue permitiendo colocar bloques. |
| `net.ScanCostTest` | 2 | El recorrido de vecinos del BFS no reserva memoria por nodo y un escaneo grande sigue siendo lineal. |
| `persist.NodeStoreCanonicalTest` | 5 | El blob compartido nunca es más viejo que la última escritura. |
| `persist.NodeStoreCorruptionTest` | 5 | Las entradas corruptas del PDC se leen como ausentes, en silencio y barato, y se pueden sobrescribir. |

## 4. Notas sobre algunas suites

### `TransmissionFixesTest` y `BlueprintDupeTest`
Cada test reproduce una pérdida o duplicación que existía en el código y se corrigió. Se comprobó que
**fallan** con el código anterior a la corrección, así que protegen contra regresiones en vez de
repetir la implementación. Ejemplos: un purgador compartido por dos controladores borraba 256 ítems
por ciclo en vez de 128; una red de fluidos casi llena se quedaba con parte del cubo y con el cubo;
dos jugadores abriendo el mismo Recipe Encoder convertían 16 Blueprints guardados en 32.

### `BlockFlowsTest`
Los tres tests de dimensiones (`networkExtractsInsideTheNether/End/Overworld`) montan una red real con
un grabber y un cofre y ejecutan el planificador. Demuestran que el escaneo, el ticker y el
almacenamiento no tienen ninguna comprobación de dimensión propia, así que lo que bloquee una
dimensión en el servidor es el puente de protección.

### `compat.*`
Los plugins de protección no están en el classpath de test. Se prueba todo lo que no los necesita
(valores por defecto, geometría de la lista blanca, puente inerte) más el provider de ProtectionStones
contra un stub de su API; la decisión real de cada provider se verifica en un servidor.

---

Para entender la funcionalidad que cubren estos tests, ver [Cómo funciona el código](Code.md).
