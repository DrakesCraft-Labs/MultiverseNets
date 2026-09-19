# 📁 MultiverseNets plugin structure

This document describes the folder and file organization of the **MultiverseNets** project. It is
aimed at developers who want to quickly understand where to find each piece of code and how the
project is laid out.

> This page is part of the wiki's **development area**. Recommended companions:
> [How the code works](Code.md) · [Plugin tests](Tests.md)

## Project root
```
MultiverseNets/
├─ docs/                 # Documentation images and resources (banner, icon, etc.)
├─ src/                  # Plugin source code
│   ├─ main/            # Code compiled into the final artifact
│   │   ├─ java/        # Java packages of the plugin
│   │   │   └─ com/chagui68/multiversenets/   # Base package of the plugin
│   │   │       ├─ command/          # Command classes (/mvnets …)
│   │   │       ├─ compat/           # Optional Slimefun compatibility
│   │   │       ├─ craft/            # Blueprint and recipe system
│   │   │       ├─ gui/              # Graphical menus (inventories) and their logic
│   │   │       ├─ item/             # Device type definitions and item registration
│   │   │       ├─ listen/           # Server event listeners (blocks, GUI, etc.)
│   │   │       ├─ net/              # Network core: controller, node management, ticker
│   │   │       ├─ persist/          # Per-chunk data persistence (NodeBlob, NodeStore)
│   │   │       └─ util/             # Helper utilities (keys, positions, settings)
│   │   └─ resources/    # YAML configuration, plugin metadata and other static resources
│   └─ test/            # Unit and integration tests
│       └─ java/        # Tests mirroring the packages in src/main/java
├─ pom.xml               # Maven project configuration (dependencies, Java version, etc.)
├─ README.md             # Main plugin documentation (English)
├─ Wiki-es/              # Wiki in Spanish
│   ├─ README.md         # Plugin overview in Spanish
│   ├─ Recipes.md        # Recipes and functions of each item in Spanish
│   └─ dev/              # Technical / developer documentation (in Spanish)
│       ├─ Structure.md  # Project structure description in Spanish
│       ├─ Code.md       # How the code works internally in Spanish
│       └─ Tests.md      # Tests: running them and what each covers in Spanish
└─ Wiki-en/              # Wiki in English (mirror of the Spanish wiki)
    ├─ README.md         # Plugin overview in English
    ├─ Recipes.md        # Recipes and functions of each item in English
    └─ dev/              # Technical / developer documentation
        ├─ Structure.md  # This file – project structure description in English
        ├─ Code.md       # How the code works internally in English
        └─ Tests.md      # Tests: running them and what each covers in English
```

## Key folder details
- **`src/main/java/com/chagui68/multiversenets/gui/`** – Implements the inventory menus (for
  example, `FilterMenu`, `CellMenu`, `QuantumWorkbenchMenu`). Each class manages the player's
  interaction with the plugin's blocks.
- **`src/main/java/com/chagui68/multiversenets/item/`** – Contains `DeviceType.java` (device
  enumeration) and `Items.java` (item registration and their recipes).
- **`src/main/java/com/chagui68/multiversenets/net/`** – Core network logic: `NetworkManager`,
  `NetworkTicker`, `NetworkStorage`.
- **`src/main/java/com/chagui68/multiversenets/persist/`** – Classes responsible for saving and
  loading each network node's information (per chunk).
- **`src/main/resources/`** – Configuration files (`config.yml`, `plugin.yml`) and other static
  resources.
- **`src/test/java/`** – JUnit tests that validate the plugin's functionality. Run them with
  `mvn test`.

## Documentation by area
| Area | File | When to consult |
| --- | --- | --- |
| Structure and organization | `Structure.md` | You want to know where each thing lives in the repo. |
| Internal workings | `Code.md` | You want to understand the network, persistence or the menus. |
| Tests | `Tests.md` | You want to run the tests or know what each one covers. |

This structure follows the standard Maven project layout, which makes building (`mvn clean package`)
and dependency management straightforward.