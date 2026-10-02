# 📁 MultiverseNets plugin structure

This document describes the folder and file organization of the **MultiverseNets** project. It is
aimed at developers who want to quickly understand where to find each piece of code and how the
project is laid out.

> This page is part of the wiki's **development area**. Recommended companions:
> [How the code works](Code.md) · [Plugin tests](Tests.md)

## Project root
```
MultiverseNets/
├─ .github/              # CI: Modrinth publishing workflow and its helper scripts
├─ docs/                 # Documentation images (banners, icon)
├─ src/
│   ├─ main/
│   │   ├─ java/com/chagui68/multiversenets/
│   │   │   ├─ MultiverseNets.java   # Main class (onEnable/onDisable, singletons)
│   │   │   ├─ api/          # MultiverseNetsAPI: public read/write access to a network's storage
│   │   │   ├─ command/      # /mvnets and its tab completion
│   │   │   ├─ compat/       # Optional integrations: SlimefunBridge and land protection
│   │   │   │                #   (ProtectionBridge + one provider per protection plugin)
│   │   │   ├─ craft/        # Blueprints, RecipeData and atomic crafting (CraftingSupport)
│   │   │   ├─ gui/          # Every inventory menu (MenuHolder base + one class per device)
│   │   │   ├─ item/         # DeviceType (every device), Items (items, lore, recipes), GuideBook
│   │   │   ├─ listen/       # BlockListener (events), DeviceInteractions (what each device opens),
│   │   │   │                #   CraftingListener (recipe book, cell upgrades in the crafting table)
│   │   │   ├─ net/          # Network, NetworkManager, NetworkTicker, NetworkStorage,
│   │   │   │                #   NetworkFluidStorage, hologram and throughput tracker
│   │   │   ├─ persist/      # Per-chunk persistence (NodeBlob, NodeStore)
│   │   │   └─ util/         # Keys, PosUtil, Settings, StackUtils, Text
│   │   └─ resources/        # config.yml and plugin.yml
│   └─ test/java/            # JUnit 5 + MockBukkit tests (same packages as main, plus stubs)
├─ pom.xml                   # Maven build (Java 21, Paper API, MockBukkit)
├─ README.md                 # Main documentation (English)
├─ Wiki-en/                  # Wiki in English
│   ├─ README.md             # Overview, machine reference, item flow, commands, configuration
│   ├─ Recipes.md            # Recipe and function of every item
│   └─ dev/
│       ├─ Structure.md      # This file
│       ├─ Code.md           # How the code works internally
│       └─ Tests.md          # Running the tests and what each one covers
└─ Wiki-es/                  # Wiki in Spanish (same files, translated)
```

## Key folder details
- **`api/`** – `MultiverseNetsAPI`: a thin, null-safe facade other plugins use to `extract`,
  `insert` and `count` items in the network that owns a given block.
- **`compat/`** – `SlimefunBridge` (reflection-only Slimefun integration) and land protection:
  `ProtectionBridge` plus `ProtectionStonesProvider`, `WorldGuardProvider`/`WorldGuardRegions`,
  `LandsProvider`, `TownyProvider` and `GriefPreventionProvider`. Each provider is one file.
- **`gui/`** – One menu per device (`TerminalMenu`, `FilterMenu`, `CellMenu`, `BarrelMenu`,
  `GreedyMenu`, `CrafterMenu`, `EncoderMenu`, `SfEncoderMenu`, `CraftingGridMenu`,
  `RequestTerminalMenu`, `QuotaLimiterMenu`, `FluidCellMenu`, `LiquidPumpMenu`, `MonitorMenu`,
  `ControllerMenu`, `QuantumWorkbenchMenu`), the `MenuHolder` base, the `GuiListener` dupe guard and
  `ChatPrompts`.
- **`item/`** – `DeviceType` (enumeration of the 43 devices, modules and tools), `Items` (item
  creation, lore, PDC helpers and the 43 recipes) and `GuideBook` (the in-game guide book).
- **`net/`** – Network core: topology (`Network`), registry (`NetworkManager`), the heartbeat
  (`NetworkTicker`), item and fluid storage, the controller hologram and throughput tracking.
- **`persist/`** – `NodeBlob` (serializable node state) and `NodeStore` (chunk PDC storage, the
  controller registry and the decode cache).
- **`src/main/resources/`** – `config.yml` (fully commented in English and Spanish) and `plugin.yml`.
- **`src/test/java/`** – JUnit tests; `dev/espi/protectionstones/PSRegion` is a stub of the
  ProtectionStones API used by the provider tests. Run them with `mvn test`.

## Documentation by area
| Area | File | When to consult |
| --- | --- | --- |
| Structure and organization | `Structure.md` | You want to know where each thing lives in the repo. |
| Internal workings | `Code.md` | You want to understand the network, persistence or the menus. |
| Tests | `Tests.md` | You want to run the tests or know what each one covers. |

This structure follows the standard Maven project layout, which makes building (`mvn clean package`)
and dependency management straightforward.
