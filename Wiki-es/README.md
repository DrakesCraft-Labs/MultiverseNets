# 🌌 MultiverseNets (Español)

<div align="center">

<img src="../docs/banner-es.svg" alt="MultiverseNets" width="100%"/>

</div>

**Redes de logística digital y almacenamiento masivo standalone para Paper — sin Slimefun.**

> Índice de la wiki: [README](README.md) · [Recetas y funciones](Recipes.md) · **Zona de desarrollo:** [Estructura](dev/Structure.md) · [Código](dev/Code.md) · [Tests](dev/Tests.md)

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

## 🍳 Recetas (mesa de crafteo)

Cada dispositivo se fabrica en una mesa de crafteo 3×3 estándar. `·` marca el hueco vacío.

| Dispositivo | Cuadrícula (3×3) | Ingredientes |
|---|---|---|
| Controlador | <pre>I I I<br/>I N I<br/>I I I</pre> | I = Bloque de hierro · N = Estrella del Nether |
| Cable ×16 | <pre>G G G<br/>G R G<br/>G G G</pre> | G = Vidrio · R = Redstone |
| Terminal | <pre>G E G<br/>E B E<br/>G E G</pre> | G = Vidrio · E = Perla ender · B = Beacon |
| Celda T1 | <pre>G G G<br/>G D G<br/>G G G</pre> | G = Vidrio · D = Diamante |
| Celda Tn+1 | <pre>D D D<br/>D P D<br/>D D D</pre> | D = Diamante · P = Celda anterior (ítem exacto) |
| Importador | <pre>I O I<br/>O R O<br/>I O I</pre> | I = Lingote de hierro · O = Observador · R = Bloque de redstone |
| Exportador | <pre>I D I<br/>D R D<br/>I D I</pre> | I = Lingote de hierro · D = Soltador · R = Bloque de redstone |
| Aspirador | <pre>S R S<br/>R H R<br/>S R S</pre> | S = Hilo · R = Redstone · H = Embudo |
| Autocrafteador | <pre>R C R<br/>I T I<br/>R C R</pre> | R = Redstone · C = Mesa de crafteo · I = Lingote de hierro · T = Target |
| Terminal inalámbrico | <pre>· P ·<br/>P N P<br/>· C ·</pre> | P = Perla ender · N = Estrella del Nether · C = Brújula |
| Network Monitor | <pre>G G G<br/>G C G<br/>G G G</pre> | G = Panel de vidrio · C = Comparador |
| Transmisor | <pre>I R I<br/>R C R<br/>I R I</pre> | I = Lingote de hierro · R = Bloque de redstone · C = Conducto |
| Receptor | <pre>I P I<br/>P L P<br/>I P I</pre> | I = Lingote de hierro · P = Perla ender · L = Lámpara de redstone |
| Greedy Cell | <pre>G H G<br/>H S H<br/>G H G</pre> | G = Lingote de oro · H = Embudo · S = Bloque slime |
| Grabber HT | <pre>O P O</pre> | O = Observador · P = Pistón pegajoso |
| Pusher HT | <pre>D P D</pre> | D = Soltador · P = Pistón |
| Recipe Encoder | <pre>K P K<br/>P S P<br/>K P K</pre> | K = Tinta · P = Papel · S = Mesa de herrería |
| Crafting Grid | <pre>C R C<br/>R G R<br/>C R C</pre> | C = Mesa de crafteo · R = Redstone · G = Mesa de cartografía |
| Blueprint ×4 | <pre>P P P<br/>P B P<br/>P P P</pre> | P = Papel · B = Tinte azul |
| Configuration Wrench | <pre>I · I<br/>· C ·<br/>· I ·</pre> | I = Lingote de hierro · C = Comparador |
| Network Rake | <pre>D · D<br/>· S ·<br/>· S ·</pre> | D = Dead bush · S = Palo |
| Network Crayon | <pre>C<br/>S</pre> | C = Tinte cian · S = Palo |
| Network Purger | <pre>I L I<br/>L H L<br/>I L I</pre> | I = Lingote de hierro · L = Bloque de magma · H = Embudo |
| Network Probe | <pre>· A ·<br/>A S A<br/>· A ·</pre> | A = Fragmento de amatista · S = Catalejo |
| Quantum Workbench | <pre>D D D<br/>D C D<br/>D D D</pre> | D = Diamante · C = Mesa de crafteo |
| Infinity Barrel | <pre>N D N<br/>D B D<br/>N D N</pre> | N = Lingote de netherita · D = Bloque de diamante · B = Barril |

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

## 📜 Licencia

Este proyecto está bajo la licencia **GNU General Public License Versión 3 (GPL-3.0)**. Consulta el archivo [LICENSE](../LICENSE) para más detalles.

---

**Autor:** Chagui68 · Revisión y afinamiento: Jack · Un proyecto de [DrakesCraft Labs](https://github.com/DrakesCraft-Labs)
