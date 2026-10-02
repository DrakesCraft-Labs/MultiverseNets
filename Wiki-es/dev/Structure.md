# 📁 Estructura del plugin MultiverseNets

Este documento describe la organización de carpetas y archivos del proyecto **MultiverseNets**. Está
pensado para desarrolladores que quieran entender rápido dónde está cada pieza de código y cómo se
organiza el proyecto.

> Esta página forma parte de la **zona de desarrollo** de la wiki. Complementos recomendados:
> [Cómo funciona el código](Code.md) · [Tests del plugin](Tests.md)

## Raíz del proyecto
```
MultiverseNets/
├─ .github/              # CI: verify.yml (tests en 1.21.11, compilación en 26.1/26.2) y publicación en Modrinth
├─ docs/                 # Imágenes de documentación (banners, icono)
├─ src/
│   ├─ main/
│   │   ├─ java/com/chagui68/multiversenets/
│   │   │   ├─ MultiverseNets.java   # Clase principal (onEnable/onDisable, singletons)
│   │   │   ├─ api/          # MultiverseNetsAPI: acceso público de lectura/escritura al almacenamiento
│   │   │   ├─ command/      # /mvnets y su autocompletado
│   │   │   ├─ compat/       # Integraciones opcionales: SlimefunBridge y protección de terrenos
│   │   │   │                #   (ProtectionBridge + un provider por plugin de protección)
│   │   │   ├─ craft/        # Blueprints, RecipeData y crafteo atómico (CraftingSupport)
│   │   │   ├─ gui/          # Todos los menús (base MenuHolder + una clase por dispositivo)
│   │   │   ├─ item/         # DeviceType (todos los dispositivos), Items (ítems, lore, recetas), GuideContent
│   │   │   ├─ listen/       # BlockListener (eventos), DeviceInteractions (qué abre cada dispositivo),
│   │   │   │                #   CraftingListener (libro de recetas, mejora de celdas en la mesa)
│   │   │   ├─ net/          # Network, NetworkManager, NetworkTicker, NetworkStorage,
│   │   │   │                #   NetworkFluidStorage, holograma y medidor de flujo
│   │   │   ├─ persist/      # Persistencia por chunk (NodeBlob, NodeStore)
│   │   │   └─ util/         # Keys, PosUtil, Settings, StackUtils, Text
│   │   └─ resources/        # config.yml y plugin.yml
│   └─ test/java/            # Tests JUnit 5 + MockBukkit (mismos paquetes que main, más stubs)
├─ pom.xml                   # Build de Maven (Java 21, API Paper 1.21.11, MockBukkit; perfiles api-26.1 / api-26.2)
├─ README.md                 # Documentación principal (inglés)
├─ Wiki-en/                  # Wiki en inglés (mismos archivos)
└─ Wiki-es/                  # Wiki en español
    ├─ README.md             # Visión general, referencia de máquinas, flujo de ítems, comandos, config
    ├─ Recipes.md            # Receta y función de cada ítem
    └─ dev/
        ├─ Structure.md      # Este archivo
        ├─ Code.md           # Cómo funciona el código por dentro
        └─ Tests.md          # Cómo ejecutar los tests y qué cubre cada uno
```

## Detalle de las carpetas clave
- **`api/`** – `MultiverseNetsAPI`: fachada fina y null-safe que otros plugins usan para `extract`,
  `insert` y `count` ítems en la red a la que pertenece un bloque.
- **`compat/`** – `SlimefunBridge` (integración con Slimefun solo por reflexión), `ChickenGenetics`
  (lee los pollos de GeneticChickengineering desde su PDC) y protección de
  terrenos: `ProtectionBridge` más `ProtectionStonesProvider`, `WorldGuardProvider`/`WorldGuardRegions`,
  `LandsProvider`, `TownyProvider` y `GriefPreventionProvider`. Cada provider es un archivo.
- **`gui/`** – Un menú por dispositivo (`TerminalMenu`, `FilterMenu`, `CellMenu`, `BarrelMenu`,
  `GreedyMenu`, `CrafterMenu`, `EncoderMenu`, `SfEncoderMenu`, `CraftingGridMenu`,
  `RequestTerminalMenu`, `QuotaLimiterMenu`, `FluidCellMenu`, `LiquidPumpMenu`, `MonitorMenu`,
  `ControllerMenu`, `QuantumWorkbenchMenu`, `DramBayMenu`, `ChickenSorterMenu`, `GuideMenu`), la base `MenuHolder`, la protección anti-dupe
  `GuiListener` y `ChatPrompts`.
- **`item/`** – `DeviceType` (enumeración de los 46 dispositivos, módulos y herramientas), `Items`
  (creación de ítems, lore, ayudantes de PDC y las 46 recetas) y `GuideContent` (el texto de la guía, EN/ES).
- **`net/`** – Núcleo de red: topología (`Network`), registro (`NetworkManager`), el latido
  (`NetworkTicker`), almacenamiento de ítems y fluidos, `MemoryModules` (instalar/expulsar módulos del
  DRAM Bay con su stock), holograma del controlador y medición de flujo.
- **`persist/`** – `NodeBlob` (estado serializable de un nodo) y `NodeStore` (almacenamiento en el PDC
  del chunk, registro de controladores y caché de decodificación).
- **`src/main/resources/`** – `config.yml` (comentado en inglés y español) y `plugin.yml`.
- **`src/test/java/`** – Tests JUnit; `dev/espi/protectionstones/PSRegion` es un stub de la API de
  ProtectionStones que usan los tests del provider. Se ejecutan con `mvn test`.

## Documentación por área
| Área | Archivo | Cuándo consultarlo |
| --- | --- | --- |
| Estructura y organización | `Structure.md` | Quieres saber dónde vive cada cosa en el repo. |
| Funcionamiento interno | `Code.md` | Quieres entender la red, la persistencia o los menús. |
| Tests | `Tests.md` | Quieres ejecutar los tests o saber qué cubre cada uno. |

Esta estructura sigue el layout estándar de Maven, lo que simplifica la compilación
(`mvn clean package`) y la gestión de dependencias.
