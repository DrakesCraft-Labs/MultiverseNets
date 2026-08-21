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
* **Transmisor / Receptor Inalámbrico**: vincula un receptor (shift+click sobre el transmisor con el ítem en mano) y colócalo en otra base o dimensión para abrir la terminal de esa red remotamente.

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
* **Auto-Crafteador**: registra recetas vanilla (shaped/shapeless/cocción) y las fabrica automáticamente si hay ingredientes en la red.
* **Recipe Encoder**: genera Blueprints (planos) a partir del resultado en mano.
* **Blueprints**: planos reutilizables que se instalan en un Auto-Crafteador con un click.
* **Crafting Grid**: crafteo manual usando ingredientes de la red directamente.

### 🛡️ Fiabilidad
* Protección contra pistones y explosiones sobre nodos.
* Al romper una celda se devuelven los contenidos (hasta 8 stacks) y se avisa de pérdidas.
* `/mvnets doctor` reescanea y diagnostica todas las redes.

## 🍳 Recetas

| Dispositivo | Receta |
|---|---|
| Controlador | 8 bloques de hierro + estrella del Nether |
| Cable x16 | 8 lana blanca + redstone |
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

## ⌨️ Comandos

| Comando | Descripción | Permiso |
|---|---|---|
| `/mvnets devices` | Lista los IDs de dispositivos | `multiversenets.use` |
| `/mvnets give <id> [n]` | Da un dispositivo | `multiversenets.admin` |
| `/mvnets doctor` | Reescanea y diagnostica redes | `multiversenets.admin` |
| `/mvnets stats` | Estadísticas globales | `multiversenets.admin` |
| `/mvnets reload` | Recarga la configuración | `multiversenets.admin` |

Alias: `/mvn`

## 🎮 Uso rápido

1. Coloca un **Controlador**, rodea el área con **Cables** y conecta **Celdas**, **Grabbers/Pushers**, etc.
2. Click derecho en el controlador o en una **Terminal** para abrir la Grid.
3. En la terminal: click izquierdo retira un stack, deposita clicando tus objetos abajo, botón hopper deposita todo, brújula busca.
4. Shift+click con **Terminal Inalámbrico** sobre el controlador para vincularlo.
5. **Encoder**: click con el resultado en mano → Blueprint → click sobre un Auto-Crafteador para instalarlo.
6. **Receptor**: shift+click con el ítem del receptor sobre un Transmisor, colócalo en otra base y ábrelo.

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
