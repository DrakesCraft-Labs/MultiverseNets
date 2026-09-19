# 🧪 Tests del plugin MultiverseNets

Este documento explica cómo funcionan los tests del proyecto y qué cubre cada uno de ellos. Pensado para desarrolladores que quieran ejecutarlos, entenderlos o ampliarlos.

> Zona de desarrollo: [Estructura](Structure.md) · [Cómo funciona el código](Code.md) · **Los tests**

---

## 1. Cómo ejecutarlos

Los tests son **JUnit 5 (Jupiter)** y no necesitan un servidor de Minecraft real. Para ejecutarlos:

```
mvn test
```

O compilación completa con empaquetado (también ejecuta los tests):

```
mvn clean package
```

Requisitos: JDK compatible con el proyecto (Paper 1.21 / `api-version: '1.21'`) y Maven.

## 2. Infraestructura: MockBukkit

La mayoría de tests usan **[MockBukkit](https://github.com/MockBukkit/MockBukkit)**, que simula un servidor Bukkit en memoria. El patrón típico es:

```java
@BeforeEach void setUp() {
    server = MockBukkit.mock();                    // crea el servidor falso
    plugin = MockBukkit.load(MultiverseNets.class); // carga el plugin (onEnable)
    world = server.addSimpleWorld("world");        // mundo falso
    player = server.addPlayer();                   // jugador falso
}

@AfterEach void tearDown() {
    MockBukkit.unmock();                            // limpieza entre tests
}
```

Con esto se puede: colocar bloques, emitir eventos (`server.getPluginManager().callEvent(...)`), simular clics en inventarios, interacciones, explosiones y pistones, y verificar el estado persistido en el PDC de los chunks.

**Tests puramente unitarios** (sin MockBukkit): `PosUtilTest`, `SettingsCellCapacityTest`, `DeviceTypeTest`, `NetworksCoexistenceTest`, `PluginResourcesTest`, `NewDevicesTest`, `ToolsTest` y `SlimefunBridgeTest`.

## 3. Resumen de los 17 tests

| Archivo (en `src/test/java/com/chagui68/multiversenets/`) | Tipo | Qué cubre |
| --- | --- | --- |
| `BlockFlowsTest` | Integración | Flujos de bloques: rotura/colocación, pistones, explosiones e interacción; protección y persistencia de nodos. |
| `CellGuiTest` | Integración | GUI de la Quantum Cell: plantilla de ítem, depósito rápido, extracción y seguridad de capacidad. |
| `CrafterGuiTest` | Integración | GUI del Auto-Crafter: instalar/desinstalar blueprints y limpiar recetas. |
| `DeviceTypeTest` | Unitario | Clasificación y propiedades de `DeviceType` (filtrables, celdas, mano vs. colocable). |
| `FilterGuiTest` | Integración | GUI de filtros: añadir/eliminar ítems, modo whitelist/blacklist, shift-clic y caras direccionales. |
| `GreedyCellTest` | Integración | Greedy Cell: almacenamiento multi-ítem, capacidad compartida, su menú y mejoras en la terminal. |
| `GuiDupeGuardTest` | Integración | Guardas anti-duplicación de los menús frente a clics peligrosos. |
| `GuiFlowsTest` | Integración | Integración de todas las GUIs de red: terminal, codificador, crafter, mesa de crafteo y monitor. |
| `InfinityBarrelTest` | Integración | Barrica Infinita: capacidad 2×10⁹, depósitos/retiros y persistencia al romper/colocar. |
| `NetworksCoexistenceTest` | Unitario | Convivencia con el plugin legado Networks: nombre, clase principal, comandos, permisos y softdepend de Slimefun. |
| `NewDevicesTest` | Unitario | Dispositivos recientes (Purgador, Sonda): colocables, filtrables y nunca celdas. |
| `PluginResourcesTest` | Unitario | Recursos esenciales (`plugin.yml`, `config.yml`) presentes en el classpath. |
| `PosUtilTest` | Unitario | Empaquetado/desempaquetado de coordenadas en un `long` de 64 bits. |
| `QuantumWorkbenchTest` | Integración | Mesa de Trabajo Cuántica: mejora de celdas preservando la carga y devolución de ingredientes. |
| `SettingsCellCapacityTest` | Unitario | Cálculo de capacidades ante configs vacías, ausentes o límite; valores por defecto. |
| `SlimefunBridgeTest` | Unitario | Comportamiento seguro del puente de Slimefun cuando este no está presente. |
| `ToolsTest` | Unitario | Herramientas de mano (Configurador, Rastrillo, Crayón) y filtrado del Receptor. |

## 4. Detalle por test

### `BlockFlowsTest`
Cubre los «flujos de bloque» gobernados por `BlockListener`: eventos `BlockBreakEvent`, `BlockPlaceEvent`, `BlockPistonExtendEvent`, `EntityExplodeEvent` y `PlayerInteractEvent`. Valida que los nodos no se puedan empujar con pistones ni destruir con explosiones, que al romper un dispositivo se suelte el ítem correspondiente (con su estado preservado) y que colocar/romper nodos actualice correctamente la red (registro de controladores, invalidación de vecinos).

### `CellGuiTest`
Prueba la GUI de la Quantum Cell (`CellMenu`, 18 ranuras): fijar la plantilla de ítem, el **depósito rápido** de la mano al inventario del jugador, la extracción de ítems y que la capacidad de la celda nunca se supere.

### `CrafterGuiTest`
Prueba la GUI del Auto-Crafter (`CrafterMenu`, 27 ranuras): **instalar** un blueprint en una ranura libre, **desinstalarlo** y **limpiar** la lista de recetas.

### `DeviceTypeTest`
Unitario sobre el enum `DeviceType`: los dispositivos **filtrables** incluyen los tipos nuevos; la **Greedy Cell no es una celda de almacenamiento** (no cuenta tier); y los ítems de mano (blueprint, terminal inalámbrica) no son colocables.

### `FilterGuiTest`
Prueba `FilterMenu` (27 ranuras): añadir ítems al filtro, eliminar filtros, alternar **whitelist/blacklist**, el **shift-clic** sobre el inventario del jugador y la configuración de **caras direccionales**.

### `GreedyCellTest`
Prueba la Greedy Cell: almacenamiento **multi-ítem** (varias muestras al mismo tiempo) con **capacidad compartida** (`greedy.capacity`), su menú dedicado (`GreedyMenu`, 54 ranuras con 36 de almacenamiento) y las mejoras de purger/greedy sobre el menú de la terminal.

### `GuiDupeGuardTest`
Valida el **`GuiListener`**: los clics peligrosos (doble-clic, botón medio, teclas numéricas, drop, hotbar, colección al cursor, etc.) se cancelan en los menús personalizados para prevenir duplicación o clonado de ítems.

### `GuiFlowsTest`
Integración de todas las GUIs de red:
- **Terminal**: abrirla clicando el bloque, retirar 1 ítem con clic izquierdo o un stack con shift-clic, guardar los ítems de la ranura de entrada al cerrar, depositar con shift-clic desde el inventario del jugador, y retirar **ítems custom** (con ID en el PDC y lore) preservando su metadata.
- **Codificador**: codifica una matriz 3×3 en un ítem Blueprint (`RecipeData` embebido) y persiste la matriz en el bloque.
- **Auto-Crafter**: craftea desde la red **atómicamente** (con ingredientes insuficientes no consume nada; con los suficientes, consume y deposita el resultado).
- **Mesa de crafteo de red**: consume 8 de 9 ingredientes de la red y entrega el resultado al jugador.
- **Monitor**: abre su GUI al estar conectado a una red; grabber/vacuum abren `FilterMenu` y el crafter su `CrafterMenu`.

### `InfinityBarrelTest`
Prueba la Barrica Infinita: **capacidad de 2 000 000 000** de ítems, abrir el menú y hacer depósito rápido con plantilla fijada, integración con el almacenamiento de la red (depósito/retiro masivo) y **persistencia al romper y volver a colocar** (el `CELL_CARGO` embebido conserva cantidad y tipo).

### `NetworksCoexistenceTest`
**Convivencia con el plugin legado Networks** para poder estar ambos en el mismo servidor:
- El nombre del plugin no es `NetworksV6-Drake`.
- La clase principal no es `io.github.sefiraat.networks.Networks`.
- Los comandos y alias del plugin no colisionan con el comando `networks`.
- Los permisos viven bajo el prefijo `multiversenets.` y no invaden `networks.`.
- La dependencia de Slimefun es **softdepend** (el plugin funciona standalone).

Lee estos valores directamente de `plugin.yml` con SnakeYAML.

### `NewDevicesTest`
Prueba los dispositivos utilitarios recientes:
- El **Purgador** es colocable y **filtrable** (para no borrar ítems indiscriminadamente).
- La **Sonda** es de mano y no colocable ni filtrable, y nunca celda.
- Purgador y Sonda **nunca cuentan como celdas** de almacenamiento.
- Todos los `DeviceType.values()` tienen material y nombre visible no vacío.

### `PluginResourcesTest`
Comprueba que `plugin.yml` y `config.yml` existen en el classpath (esenciales para que el plugin arranque) y un sanity check de versionado.

### `PosUtilTest`
Fija el formato binario de `PosUtil.pack/unpack`: coordenadas positivas y negativas/límites del mundo (±30 000 000 en X/Z, Y negativa) hacen roundtrip sin pérdida en un único `long`.

### `QuantumWorkbenchTest`
Prueba `QuantumWorkbenchMenu` (45 ranuras):
- **Mejora de celda**: una Quantum Cell T1 con carga (500 lingotes de hierro) + diamantes a su alrededor → botón craftear → salida **T2 con la carga preservada** en `CELL_CARGO`, y la matriz consumida.
- **Cierre**: los ingredientes no crafteados se **devuelven al jugador**.

### `SettingsCellCapacityTest`
Prueba `Settings` con inyección por reflexión de configuraciones simuladas:
- Usa las capacidades declaradas en `cells.capacities`.
- Lista vacía o clave ausente → **fallback geométrico** sin excepción.
- Tier no declarada → clamp a la **última** capacidad configurada.
- Tier inválida/negativa → capacidad positiva segura.
- Barril: 2 000 000 000 por defecto o valor custom (`barrel.capacity`).
- `crafter.max-recipes` se clamp entre 1 y 18.
- Capacidades `long` de hasta 2×10⁹ sin desbordamiento de 32 bits.
- Con `cfg == null` todos los getters devuelven sus **valores por defecto** (escaneo 20, máx nodos 16 384, transferencia 5, vacío 10, crafteo 20, 64 ítems/op, multiplicador HT 8, greedy 262 144, barril 2×10⁹, blueprints 18, radio 4.0, `compat.slimefun` true, `debug` false, rake 250, celda 65 536).

### `SlimefunBridgeTest`
Sin Slimefun presente: el puente informa `isAvailable()`/`disponible()` = false; consultar bloques/ítems null no lanza (`isMachine`, `getId`, `isSlimefunItem` y sus alias en español); `extract`/`extraer` devuelven null y `insert`/`insertar` devuelven 0 de forma segura.

### `ToolsTest`
Prueba las herramientas de mano: **Configurador, Rastrillo y Crayón** no son colocables, no almacenan y no filtran; el **Receptor es filtrable** (transporte inalámbrico controlado); y los filtros por defecto usan modo **whitelist** (`filterBlacklist = false`).

---

Para entender el funcionamiento que estos tests cubren, consulta [Cómo funciona el código](Code.md).