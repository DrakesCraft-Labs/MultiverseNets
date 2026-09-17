<div align="center">

<img src="docs/banner.svg" alt="MultiverseNets" width="100%"/>

# 🌌 MultiverseNets

**Redes de logística digital y almacenamiento masivo standalone para Paper — sin Slimefun.**

<img src="https://img.shields.io/badge/Paper-1.21.11-38BDF8?style=for-the-badge&logo=minecraft&logoColor=white" alt="Paper 1.21.11"/>
<img src="https://img.shields.io/badge/Java-21-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
<img src="https://img.shields.io/badge/Autor-Chagui68-22C55E?style=for-the-badge" alt="Chagui68"/>

</div>

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
* Persistencia atómica por chunk (region data de Paper), anti-dupe en todos los flujos.

### 🔄 Transporte
* **Importador (Grabber)**: extrae de contenedores adyacentes hacia la red, con filtro whitelist.
* **Exportador (Pusher)**: inserta desde la red hacia contenedores adyacentes, con filtro.
* **Variantes HT (High-Throughput)**: versiones rápidas x8 (configurable) para factorías masivas.
* **Aspirador (Vacuum)**: recoge ítems del suelo en radio configurable, ahora con filtro whitelist opcional.

### 🛠️ Autocrafteo
* **Auto-Crafteador**: acepta **Blueprints** (matriz 3×3 real) y recetas por resultado (modo antiguo). Cada blueprints se intenta una vez por ciclo con **extracción atómica**: o hay ingredientes para todo o no se toca nada.
* **Recipe Encoder**: monta la receta en una matriz 3×3 de plantillas persistente (clic para fijar huecos, sin gastar ítems) y codifica un Blueprint en blanco con un clic.
* **Blueprints**: planos reutilizables que llevan la receta completa (matriz + resultado) en su PDC; se instalan en un Auto-Crafteador con un click y no se consumen.
* **Crafting Grid**: crafteo manual tirando de la red: la matriz de plantillas se guarda en el bloque, y cada craft retira ingredientes de la red de forma transaccional.

### 🧰 Herramientas (traídas de NetworksV6)
* **Configuration Wrench**: shift+clic sobre un dispositivo con filtro **copia** su configuración; clic normal la **pega** en otro.
* **Network Rake**: retira nodos al instante (250 usos por defecto, `rake.uses`); no toca controladores ni celdas cargadas.
* **Network Crayon**: marca el controlador y la red enseña partículas cuando sus máquinas trabajan.
* Filtros con **modo whitelist/blacklist** en cualquier dispositivo con filtro (grabbers, pushers, vacuum, purgador, greedy cell, receptor).

### 🛡️ Fiabilidad
* Protección contra pistones y explosiones sobre nodos.
* Al romper un nodo su estado viaja dentro del ítem (como en Networks): carga de la celda, filtros, blueprints, matriz de la parrilla y enlace del receptor. Al recolocarlo, sigue como estaba.
* Guardias anti-dupe de Networks en todos los menús (sin double-click, sin drags sobre huecos pintados, sin shift+clic derecho al vacío) y **recuperación de lo dejado en los huecos reales al cerrar**.
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
| Blueprint en blanco x4 | 8 papeles + tinte azul |
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

## 🎮 Uso rápido

1. Coloca un **Controlador**, rodea el área con **Cables** y conecta **Celdas**, **Grabbers/Pushers**, etc.
2. Click derecho en el controlador o en una **Terminal** para abrir la Grid.
3. En la terminal (mismas convenciones que la grilla de Networks): **izquierdo** saca 1 al cursor, **derecho** un stack, **shift+clic** manda al inventario; **shift+izquierdo** sobre tus items los inserta en la red, o déjalos en el **hueco de entrada** (esquina derecha) y la red los absorbe. Lupa/etiqueta busca (clic derecho limpia), botón azul cambia el orden, flechas paganinan.
4. Shift+click con **Terminal Inalámbrico** sobre el controlador para vincularlo (luego clic derecho al aire para abrir la red a distancia).
5. **Encoder**: monta la receta en la matriz de plantillas, mete un **Blueprint en blanco** en el hueco azul y pulsa *Encode*. Ese Blueprint se instala en un Auto-Crafteador con un clic en su lista.
6. **Receptor**: shift+click con el ítem del receptor sobre un Transmisor, colócalo en otra base y ábrelo; ponle filtro y además **traerá ítems** de la red del transmisor.

## 🤝 Convivencia con Networks

**Los dos plugins pueden estar instalados a la vez.** No se pisan en nada:

| | MultiverseNets | NetworksV6-Drake |
|---|---|---|
| Nombre del plugin | `MultiverseNets` | `NetworksV6-Drake` |
| Clase principal | `com.chagui68.multiversenets.…` | `io.github.sefiraat.networks.…` |
| Comando | `/mvnets` (alias `/mvn`) | `/networks` |
| Permisos | `multiversenets.*` | `networks.*` |
| Ítems | propios, por PDC, con recetas de vanilla | de Slimefun (`NTW_*`) |

Networks no registra ninguna receta de vanilla —las suyas van por la mesa de Slimefun— así que
las 20 de aquí tampoco chocan.

Hay **cinco pruebas** que fijan esto (`ConvivenciaConNetworksTest`). No están por gusto: lo que
rompe la convivencia no es el código sino los identificadores, y esos se cambian sin querer al
renombrar algo.

**Una interacción a tener en cuenta.** Con la integración de Slimefun activa, un Grabber de
MultiverseNets puede sacar de un bloque de Networks, porque son ítems de Slimefun con su propio
menú. Es interoperabilidad, no un fallo, pero si prefieres que cada red se ocupe solo de lo suyo:

```yaml
compat:
  slimefun: false
```

## 🧹 Traído de Networks

Lo que las cuatro variantes de Networks tenían y aquí faltaba, escogido por utilidad real y no
por completar la lista:

* **Network Purger** — descarta de la red lo que case con su filtro. Sin algo así una red se
  atasca sola: cualquier máquina que genere un residuo (grava del cuarzo, semillas de una
  cosechadora) acaba llenando las celdas y bloqueando lo que sí interesa. Networks lo repartía
  entre `TRASH` y `PURGER`; aquí basta uno porque el filtro ya decide qué se va.
  **Sin filtro configurado no borra nada**, a propósito: un purgador que por defecto se lo comiera
  todo sería una trituradora de inventarios esperando a que alguien lo coloque sin mirar.
* **Network Probe** — clic derecho sobre un bloque y te dice a qué red pertenece, cuántos nodos
  tiene y dónde está su controlador. `/mvnets doctor` resume la salud de todas las redes; la sonda
  responde la pregunta concreta que uno se hace de pie delante de una máquina parada: *¿esto está
  conectado a algo?*. Funciona también sobre bloques que **no** son nodos, que es justo cuando más
  falta hace.

## 🔗 Integración con Slimefun (opcional)

MultiverseNets **no depende de Slimefun** y funciona entero sin él. Pero si está instalado, lo
detecta al arrancar y los **Grabbers, Pushers y Auto-Crafteadores pueden trabajar con máquinas de
Slimefun** igual que con un cofre: sacar el producto de una fundidora eléctrica, alimentar un
horno de arco, vaciar un cosechador.

Detalles que importan:

* **No añade dependencia.** Todo se resuelve por reflexión al arrancar. Sin Slimefun, el puente
  queda inerte y el resto del plugin no se entera.
* **Sirve para ambos Slimefun.** Reconoce el fork repaquetado de DrakesCraft y el original de
  thebusybiscuit, así que el mismo jar vale en los dos.
* **Respeta el diseño de cada máquina.** Se usan sólo los huecos que la propia máquina declara
  para entrada y salida, no todos los del menú. Meter carbón en la ranura de salida de una
  fundidora la atasca, y sacar de la entrada le roba lo que estaba procesando.

Para comprobar si la integración está activa: `/mvnets doctor` lo dice en la primera línea.

## 🔍 En qué se diferencia de Networks

MultiverseNets no es un recorte de Networks: resuelve el mismo problema con otra arquitectura, y
esa decisión tiene consecuencias concretas.

| | Networks (addon Slimefun) | MultiverseNets |
|---|---|---|
| Dependencias | Slimefun + su cadena | Ninguna, solo Paper API |
| Pertenencia a la red | Cada nodo guarda su raíz | Se recalcula por BFS desde el controlador |
| Nodos huérfanos | Posibles: un nodo puede quedar apuntando a una raíz que su controlador ya sustituyó | **Estructuralmente imposibles**: cada escaneo rehace la topología entera |
| Diagnóstico | Añadido después (`/networks doctor`) | `/mvnets doctor` desde el primer día |
| Ticker | Depende del ciclo de Slimefun | Propio, con intervalos por operación en el config |

La diferencia de fondo está en la tercera fila. En Networks, «lo tengo todo conectado y la máquina
no trabaja» es un síntoma real que aparece cuando un nodo vuelve al registro pero no a su red;
llevamos meses persiguiéndolo. Aquí no puede ocurrir, porque no existe estado por nodo que
sobreviva a un escaneo.

El precio es que el escaneo cuesta: un BFS sobre hasta `max-nodes` bloques cada
`scan-interval-ticks`. Es un intercambio deliberado — se paga trabajo predecible y acotado a
cambio de que no haya estado que se pueda corromper.

## 🛠️ Compilación

```bash
mvn clean package
```

El jar se genera en `target/MultiverseNets-v<versión>.jar`.

## 📋 Compatibilidad

| Parámetro | Requisito |
|---|---|
| **Servidor** | Paper / Purpur / Folia 1.21.11 |
| **Java** | Java 21 LTS |
| **Dependencias** | Ninguna (standalone) |

---

**Autor:** Chagui68 · Revisión y afinado: Jack · Proyecto de [DrakesCraft Labs](https://github.com/DrakesCraft-Labs)
