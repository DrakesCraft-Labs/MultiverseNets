# 🌌 MultiverseNets (Español)

**Redes de logística digital y almacenamiento masivo standalone para Paper — sin Slimefun.**

---

## 📖 ¿Qué es MultiverseNets?

**MultiverseNets** implementa la lógica completa de una red de logística estilo Networks, pero **100% standalone**: sin Slimefun ni ninguna dependencia. Todo funciona con la API nativa de Paper mediante ítems personalizados (PDC), bloques etiquetados por chunk y almacenamiento virtual persistente.

## ⚙️ Sistemas implementados

### 🖥️ Red
* **Controlador de Red**: corazón de la red; indexa nodos vía BFS a través de cables.
* **Cable de Red**: transmite la señal entre nodos.
* **Terminal de Red** (bloque) y **Terminal Inalámbrico** (ítem vinculable con shift+click al controlador).
* **Network Monitor**: panel de diagnóstico con desglose de nodos, almacenamiento y estado.
* **Transmisor / Receptor Inalámbrico**: vincula un receptor (shift+click sobre el transmisor con el ítem en mano) y colócalo en otra base o dimensión. El receptor **abre la terminal de la red remota** y, si le pones filtro, **puentea ítems** desde la red del transmisor a la suya cada ciclo (sin filtro no cruza nada, a propósito).

### 📦 Almacenamiento cuántico
* **Celdas T1–T6**: cada celda guarda un solo tipo de ítem hasta su capacidad (65k → 2.000M configurable).
* **Greedy Cell**: búfer inteligente que reclama su ítem filtrado desde la red y lo alimenta a contenedores adyacentes (ideal para líneas continuas).
* El almacenamiento de la red es el agregado de todas las celdas conectadas.
* Persistencia atómica por chunk (region data de Paper), anti‑dupe en todos los flujos.

### 🔄 Transporte
* **Importador (Grabber)**: extrae de contenedores adyacentes hacia la red, con filtro whitelist.
* **Exportador (Pusher)**: inserta desde la red hacia contenedores adyacentes, con filtro.
* **Variantes HT (High‑Throughput)**: versiones rápidas x8 (configurable) para factorías masivas.
* **Aspirador (Vacuum)**: recoge ítems del suelo en radio configurable, ahora con filtro whitelist opcional.

### 🛠️ Autocrafteo
* **Auto‑Crafteador**: acepta **Blueprints** (matriz 3×3 real) y recetas por resultado (modo antiguo). Cada blueprint se intenta una vez por ciclo con **extracción atómica**: o hay ingredientes para todo o no se toca nada.
* **Recipe Encoder**: monta la receta en una matriz 3×3 de plantillas persistente (clic para fijar huecos, sin gastar ítems) y codifica un Blueprint en blanco con un clic.
* **Blueprints**: planos reutilizables que llevan la receta completa (matriz + resultado) en su PDC; se instalan en un Auto‑Crafteador con un click y no se consumen.
* **Crafting Grid**: crafteo manual tirando de la red: la matriz de plantillas se guarda en el bloque, y cada craft retira ingredientes de la red de forma transaccional.

### 🧰 Herramientas (traídas de NetworksV6)
* **Configuration Wrench**: shift+clic sobre un dispositivo con filtro **copia** su configuración; clic normal la **pega** en otro.
* **Network Rake**: retira nodos al instante (250 usos por defecto, `rake.uses`); no toca controladores ni celdas cargadas.
* **Network Crayon**: marca el controlador y la red enseña partículas cuando sus máquinas trabajan.
* Filtros con **modo whitelist/blacklist** en cualquier dispositivo con filtro (grabbers, pushers, vacuum, purgador, greedy cell, receptor).

### 🛡️ Fiabilidad
* Protección contra pistones y explosiones sobre nodos.
* Al romper un nodo su estado viaja dentro del ítem (como en Networks): carga de la celda, filtros, blueprints, matriz de la parrilla y enlace del receptor. Al recolocarlo, sigue como estaba.
* Guardias anti‑dupe de Networks en todos los menús (sin double‑click, sin drags sobre huecos pintados, sin shift+clic derecho al vacío) y **recuperación de lo dejado en los huecos reales al cerrar**.
* `/mvnets doctor` reescanea y diagnostica todas las redes; `/mvnets inspect` y `/mvnets repair` inspeccionan y reescanean el bloque mirado.

## 🍳 Recetas

| Dispositivo | Receta |
|---|---|
| Controlador | 8 bloques de hierro + estrella del Nether |
| Cable x16 | 8 vidrios + redstone |
| Terminal | beacon + perlas ender + vidrio |
| Celda T1 | 8 vidrios + diamante |
| Celda Tn+1 | 8 diamantes + celda anterior |
| Importador | 4 observadores + 4 hierros + bloque de redstone |
| Exportador | 4 soltados + 4 hierros + bloque de redstone |
| Aspirador | esponja + embudo + hilo + redstone |
| Autocrafteador | mesa de crafteo + target + hierro + redstone |
| Terminal inalámbrico | estrella del Nether + perla + brújula |
| Network Monitor | 8 vidrio + comparador |
| Transmisor | conducto + 4 bloques redstone + 4 hierros |
| Receptor | lámpara redstone + 4 perlas + 4 hierros |
| Greedy Cell | bloque slime + 2 embudos + 4 oro |
| Grabber HT | observador + pistón pegajoso + observador |
| Pusher HT | soltador + pistón + soltador |
| Recipe Encoder | mesa herrería + papel + tinta |
| Crafting Grid | cartografía + mesas crafteo + redstone |
| Blueprint en blanco x4 | 8 papeles + tinta azul |
| Configuration Wrench | 4 hierros + comparador |
| Network Rake | 2 dead bushes + 2 palos |
| Network Crayon | 2 tintes cian + palo |

## ⌨️ Comandos

| Comando | Descripción | Permiso |
|---|---|---|
| `/mvnets devices` | Lista los IDs de dispositivos | `multiversenets.use` |
| `/mvnets give <id> [n]` | Da un dispositivo | `multiversenets.admin` |
| `/mvnets doctor` | Reescanea y diagnostica redes | `multiversenets.admin` |
| `/mvnets stats` | Estadísticas globales | `multiversenets.admin` |
| `/mvnets inspect` | Inspecciona el bloque mirado (tipo, red, contenido, filtro) | `multiversenets.admin` |
| `/mvnets repair` | Fuerza el reescaneo de la red del bloque mirado | `multiversenets.admin` |
| `/mvnets reload` | Recarga la configuración | `multiversenets.admin` |

Alias: `/mvn`
