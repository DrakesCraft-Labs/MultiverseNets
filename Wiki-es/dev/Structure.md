# 📁 Estructura del plugin MultiverseNets

Este documento describe la organización de carpetas y archivos del proyecto **MultiverseNets**. Está pensado para desarrolladores que quieran comprender rápidamente dónde encontrar cada elemento del código y cómo está estructurado el proyecto.

> Esta página forma parte de la **zona de desarrollo** de la wiki. Complementos recomendados:
> [Cómo funciona el código](Code.md) · [Los tests del plugin](Tests.md)

## Raíz del proyecto
```
MultiverseNets/
├─ docs/                 # Imágenes y recursos de documentación (banner, icono, etc.)
├─ src/                  # Código fuente del plugin
│   ├─ main/            # Código que se compila en el artefacto final
│   │   ├─ java/        # Paquetes Java del plugin
│   │   │   └─ com/chagui68/multiversenets/   # Package base del plugin
│   │   │       ├─ command/          # Clases de comandos (/mvnets …)
│   │   │       ├─ compat/           # Compatibilidad opcional con Slimefun
│   │   │       ├─ craft/            # Sistema de blueprints y recetas
│   │   │       ├─ gui/              # Menús gráficos (inventarios) y su lógica
│   │   │       ├─ item/             # Definiciones de tipos de dispositivos y registro de ítems
│   │   │       ├─ listen/           # Listeners de eventos del servidor (bloques, GUI, etc.)
│   │   │       ├─ net/              # Núcleo de la red: controlador, gestión de nodos, ticker
│   │   │       ├─ persist/          # Persistencia de datos por chunk (NodeBlob, NodeStore)
│   │   │       └─ util/             # Utilidades auxiliares (claves, posiciones, configuración)
│   │   └─ resources/    # Configuración YAML, idioma y otros recursos estáticos
│   └─ test/            # Tests unitarios y de integración
│       └─ java/        # Tests correspondientes a los paquetes de src/main/java
├─ pom.xml               # Configuración del proyecto Maven (dependencias, versión Java, etc.)
├─ README.md             # Documentación principal del plugin (Inglés)
├─ Wiki-es/              # Wiki en español
│   ├─ README.md         # Vista general del plugin en español
│   ├─ Recipes.md        # Recetas y funciones de cada ítem del plugin
│   └─ dev/              # Zona técnica / documentación para desarrolladores
│       ├─ Structure.md  # Este archivo – estructura del proyecto
│       ├─ Code.md       # Cómo funciona el código internamente
│       └─ Tests.md      # Los tests: ejecución y qué cubre cada uno
└─ Wiki-en/              # Wiki en inglés (espejo de la wiki en español)
    ├─ README.md         # Overview of the plugin in English
    ├─ Recipes.md        # Recipes and functions of each item in English
    └─ dev/              # Developer documentation (mirror in English)
        ├─ Structure.md  # Project structure description in English
        ├─ Code.md       # How the code works internally in English
        └─ Tests.md      # Tests: running them and what each covers in English
```

## Detalles de carpetas clave
- **`src/main/java/com/chagui68/multiversenets/gui/`** – Implementa los menús de inventario (por ejemplo, `FilterMenu`, `CellMenu`, `QuantumWorkbenchMenu`). Cada clase gestiona la interacción del jugador con los bloques del plugin.
- **`src/main/java/com/chagui68/multiversenets/item/`** – Contiene `DeviceType.java` (enumeración de dispositivos) y `Items.java` (registro de ítems y sus recetas).
- **`src/main/java/com/chagui68/multiversenets/net/`** – Núcleo de la lógica de red: `NetworkManager`, `NetworkTicker`, `NetworkStorage`.
- **`src/main/java/com/chagui68/multiversenets/persist/`** – Clases responsables de guardar y cargar la información de cada nodo de la red (por chunk).
- **`src/main/resources/`** – Archivos de configuración (`config.yml`, `plugin.yml`) y demás recursos estáticos.
- **`src/test/java/`** – Tests JUnit que validan la funcionalidad del plugin. Se ejecutan con `mvn test`.

## Documentación por zona
| Zona | Archivo | Cuando la consultas |
| --- | --- | --- |
| Estructura y organización | `Structure.md` | Quieres saber dónde está cada cosa en el repo. |
| Funcionamiento interno | `Code.md` | Quieres entender cómo funciona la red, la persistencia o los menús. |
| Tests | `Tests.md` | Quieres ejecutar los tests o saber qué cubre cada uno. |

Esta estructura sigue el estándar de proyectos Maven, lo que facilita la compilación (`mvn clean package`) y la gestión de dependencias.