# 🌌 MultiverseNets (Español)

<div align="center">

<img src="../docs/banner-es.svg" alt="MultiverseNets" width="100%"/>

</div>

**Redes de logística digital y almacenamiento masivo standalone para Paper — sin Slimefun.**

> Índice de la wiki: [README](README.md) · [Recetas y funciones](Recipes.md) · **Zona de desarrollo:** [Estructura](dev/Structure.md) · [Código](dev/Code.md) · [Tests](dev/Tests.md) · [English](../Wiki-en/README.md)

---

## 📖 ¿Qué es MultiverseNets?

**MultiverseNets** implementa una red de logística estilo Networks **100% standalone**: sin Slimefun
ni ninguna otra dependencia. Cada dispositivo es un ítem personalizado (identificado por PDC), cada
dispositivo colocado es un bloque etiquetado cuyo estado vive en el chunk, y el almacenamiento de la
red es virtual y persistente.

Una red es **un Controlador de Red más todos los bloques de MultiverseNets conectados a él**, cara
con cara. Los cables son la forma barata de conectar, pero todos los dispositivos conducen: un
grabber pegado a una celda pegada al controlador ya es una red.

## 🎮 Inicio rápido

1. Coloca un **Controlador de Red**. Quien lo coloca pasa a ser el dueño de la red.
2. Conéctale **Cables** y dispositivos. Añade al menos una **Celda Cuántica** (o un Infinity Barrel):
   sin almacenamiento la red no tiene dónde guardar ítems.
3. Clic derecho en un **Terminal** (o en el menú del controlador) para ver y usar el almacenamiento.
4. Pon un **Grabber** junto a un cofre para importar y un **Pusher** junto a otro para exportar.
   Configura sus filtros con clic derecho.
5. Usa `/mvnets doctor` o una **Network Probe** si algo no conecta.

Cada id de dispositivo de abajo se puede dar con `/mvnets give <id>`; `/mvnets devices` los lista
todos. Las recetas de todo están en [Recetas y funciones](Recipes.md).

---

## 🧩 Referencia de máquinas

Las cantidades son los valores por defecto de `config.yml`; todas se pueden configurar (ver
[Configuración](#configuracion)). "Ciclo" significa un ciclo de transferencia
(`network.op-interval-ticks.transfer`, 5 ticks por defecto).

### 🖥️ Núcleo y acceso

| Dispositivo | id · bloque | Qué hace | Cómo se usa |
|---|---|---|---|
| **Controlador de Red** | `mvn_controller` · Magnetita | La raíz de la red. Cada escaneo empieza aquí (en anchura por los bloques conectados, hasta `network.max-nodes`). Guarda a su dueño (quien lo colocó) y muestra un holograma con estado, nodos y totales almacenados. Los módulos de memoria ya **no** van en el controlador: van en un **DRAM Bay**. | Clic derecho: estado y estado del router. Un controlador antiguo que aún tenga un módulo dentro lo sigue usando; en su menú, clic en *Legacy Memory Module* lo saca **con sus ítems** para llevarlo a un DRAM Bay. Un solo controlador por red: un segundo cableado a los mismos cables se reporta como `foreign controller` (ver [Buses compartidos](#buses-compartidos)). |
| **Cable de Red** | `mvn_cable` · Vidrio | Conecta dispositivos. Sin lógica propia. | Clic derecho para ver si llega a un controlador (y el tamaño de la red). Con un bloque en la mano, el clic derecho coloca el bloque. |
| **Terminal de Red** | `mvn_terminal` · Faro | La cuadrícula de almacenamiento: todos los ítems de la red (caché, celdas, barriles, greedy cells, barriles de Slimefun) y una segunda página para fluidos. | Clic izquierdo saca 1, clic derecho un stack, shift+clic al inventario. Shift+clic izquierdo en tus ítems (o déjalos en la ranura de entrada) para guardarlos. Botones de búsqueda, orden y páginas. Los cubos y botellas de miel van al almacenamiento de fluidos. |
| **Terminal Inalámbrico** | `mvn_wireless_terminal` · ítem (Estrella del Nether) | Abre el Terminal de Red a distancia. | Shift+clic derecho en un Controlador o Terminal para vincularlo, luego clic derecho al aire. Sin **Network Router** solo funciona en el mismo mundo y a `wireless.local-range-without-router` (64) bloques. Nunca durante 10 s tras un combate, y solo si puedes acceder al terreno de la red. |
| **Network Router** | `mvn_router` · Pararrayos | Quita los límites del Terminal Inalámbrico para su red: cualquier distancia y cualquier mundo. | Conéctalo en cualquier punto de la red. |
| **Network Monitor** | `mvn_monitor` · Nexo de reaparición | Panel de diagnóstico en vivo: nodos por tipo, uso de almacenamiento, errores. | Clic derecho; se actualiza mientras está abierto. |
| **Network Probe** | `mvn_probe` · ítem (Catalejo) | Responde "¿esto está conectado?": tamaño de la red, posición del controlador, errores del escaneo y cortes por protección del bloque que pulses. | Clic derecho en cualquier bloque. |

### 🧠 Memoria: DRAM Bay y módulos

| Dispositivo | id · bloque | Qué hace | Cómo se usa |
|---|---|---|---|
| **DRAM Bay** | `mvn_dram_bay` · Bombilla de cobre encerada | Bloque de red que aloja **un módulo de memoria**. El módulo es el almacenamiento: mientras está instalado, su stock forma parte de la red. Puedes poner tantos bays como quieras. | Clic derecho con un módulo en la mano (bay vacío) para instalarlo, o clic derecho para abrir su menú: ver el stock, instalar desde el cursor o con shift+clic, y **Eject Module**. Romperlo (o el Rake) suelta el bay y el módulo **con todo su stock**. |
| **Módulos de memoria de ítems** | `mvn_cache_l1` · `_l2` · `_l3` · `_dram` · `_quantum` · ítems | Almacenamiento multi-ítem: 2.048 / 8.192 / 32.768 / 131.072 / 524.288 ítems en total (`virtual-cache.tier-1` … `tier-5`), de cualquier mezcla de tipos. | Instálalo en un DRAM Bay. **Al sacarlo se lleva todos sus ítems**: desaparecen de esa red y aparecen en la red del DRAM Bay donde lo instales. Mejorar un módulo cargado en la mesa de crafteo conserva sus ítems. |
| **Fluid DRAM Module** | `mvn_fluid_dram` · ítem (Corazón del mar) | Módulo **exclusivo para fluidos**: guarda varios fluidos a la vez hasta `fluids.dram-capacity-mb` (512.000 mB = 512 cubos) en total. | Igual que los de ítems: va en un DRAM Bay, suma su capacidad al almacenamiento de fluidos de la red y al sacarlo se lleva sus fluidos a otra red. |

### 📦 Almacenamiento de ítems

| Dispositivo | id · bloque | Qué hace | Cómo se usa |
|---|---|---|---|
| **Celda Cuántica T1–T6** | `mvn_cell_t1` … `mvn_cell_t6` · Terracota (normal, naranja, amarilla, lima, cian, morada) | Guarda **un tipo de ítem** cada una: 65.536 / 262.144 / 1.048.576 / 16.777.216 / 268.435.456 / 2.000.000.000 ítems. Una celda vacía adopta el primer tipo que no tenga otro sitio. | Conéctala. Clic derecho para ver o gestionar su contenido. Al romperla la carga queda en el ítem. Se mejora un nivel en la Quantum Workbench o en la mesa de crafteo (celda rodeada de 8 diamantes) conservando la carga. |
| **Infinity Barrel** | `mvn_infinity_barrel` · Barril | Igual que una celda — un tipo de ítem — con `barrel.capacity` (2.000.000.000), pero **sigue registrado** a su ítem cuando se vacía. Los ítems con id propio (Slimefun y otros plugins) se reconocen por su id y su texto visible, así un ítem que sacas siempre vuelve a entrar aunque su nombre o lore se hayan guardado de otra forma. | Clic derecho: clic en *Set Item* con un ítem en el cursor para registrarlo; clic derecho en *Set Item* con el cursor vacío borra el registro (solo si está vacío). Las tolvas **no** interactúan con él (ver abajo). |
| **Greedy Cell** | `mvn_greedy_cell` · Bloque de slime | Con filtro: un **sumidero prioritario**. Los ítems que entran y pasan su filtro van a ella antes que a cualquier otro almacenamiento; cada ciclo extrae hasta 512 más de la red y empuja hasta 256 a los contenedores vecinos **que no son de la red** (cofres, máquinas de Slimefun). Guarda varios tipos, hasta `greedy.capacity` (262.144) en total. Sin filtro: almacenamiento de desbordamiento, solo cuando todo lo demás está lleno. | Clic derecho para poner el filtro y ver su búfer. Su stock está reservado: ni los Pushers ni el puente inalámbrico sacan ítems de una Greedy Cell (terminales y crafteo sí). |
| **Quantum Workbench** | `mvn_quantum_workbench` · Bloque de coral cerebro | Sube una Celda Cuántica T1–T5 al siguiente nivel conservando la carga. | Celda en el centro, 8 diamantes alrededor, pulsa *Entangle & Upgrade* y recoge el resultado. |

### 💧 Fluidos

Los fluidos tienen su propio almacenamiento, separado de los ítems: las Celdas Cuánticas de Fluidos y
los Fluid DRAM Modules instalados en DRAM Bays.

| Dispositivo | id · bloque | Qué hace | Cómo se usa |
|---|---|---|---|
| **Quantum Fluid Cell** | `mvn_fluid_cell` · Ladrillos de prismarina | Guarda un fluido — Agua, Lava, Leche, Nieve polvo o Miel — hasta `fluids.cell-capacity-mb` (64.000 mB = 64 cubos). Todas las celdas de fluidos de una red forman su almacenamiento de fluidos. | Clic derecho con un cubo lleno / botella de miel para verter en esa celda; con un cubo vacío para llenarlo (Agua, Lava, Leche, Nieve polvo). O usa la página de fluidos del Terminal: deposita cubos/botellas y retira con un cubo vacío (la miel con botella de vidrio). |
| **Liquid Pump** | `mvn_liquid_pump` · Vidrio tintado de azul | Cada ciclo drena un bloque **fuente** de agua o lava justo **debajo** (1.000 mB). El bloque solo desaparece si los 1.000 mB caben enteros en la red. | Colócala encima del líquido. Clic derecho para elegir ANY / WATER / LAVA. |

### 🔄 Transporte de ítems

| Dispositivo | id · bloque | Qué hace | Cómo se usa |
|---|---|---|---|
| **Simple Grabber** | `mvn_grabber` · Observador | Importa de los contenedores vecinos a la red: hasta 128 ítems de un tipo por ciclo, por **las seis caras**. De un horno solo saca el resultado. | Clic derecho para el filtro (whitelist/blacklist). Filtro vacío = todo. |
| **Advanced Grabber** | `mvn_grabber_ht` · Pistón pegajoso | Igual, ×8 (`transfer.ht-multiplier`): 1.024 por ciclo, y se puede limitar a **una cara**. | Menú de filtro + selector de cara. |
| **Simple Pusher** | `mvn_pusher` · Diana | Exporta de la red a los contenedores vecinos: hasta 128 ítems por ciclo, por **las seis caras** (no tiene selector de cara: si quieres una sola, usa el Advanced Pusher). Nunca mete nada en otro bloque de la red. | El filtro decide **qué ítems** salen, no a dónde. **Whitelist vacía = inactivo** (un pusher recién puesto nunca vacía la red); **blacklist = todo menos lo listado**: lo que pongas en la blacklist se queda en la red. |
| **Advanced Pusher** | `mvn_pusher_ht` · Pistón | Igual, ×8, y con el selector de cara **solo** entrega a ese lado. | Menú de filtro + selector de cara. Para una máquina, elige la cara de la máquina. |
| **Network Vacuum** | `mvn_vacuum` · Esponja | Cada 10 ticks recoge los ítems del suelo en `vacuum.radius` (4 bloques) hacia la red. | Filtro opcional. Lo que no cabe se queda en el suelo. |
| **Network Purger** | `mvn_purger` · Bloque de magma | Borra hasta 128 ítems por ciclo que pasen su filtro. | **Sin filtro no borra nada**, a propósito. |
| **Network Quota Limiter** | `mvn_limiter` · Diana | Limita cuánto de un ítem puede guardar la red. Todo depósito (grabbers, vacuum, terminal, resultados de crafteo, puente inalámbrico) se detiene en el tope. | Clic derecho: ítem objetivo, límite y activado/desactivado. Varios limitadores sobre el mismo ítem: gana el más bajo. |
| **Wireless Transmitter** | `mvn_transmitter` · Conducto | Un extremo del puente inalámbrico. Si guarda el enlace a un Receptor, **empuja** hasta 128 ítems por ciclo que pasen **su** filtro a la red del receptor. | Clic derecho: menú de filtro con un botón que abre el terminal de su propia red. Enlace: shift+clic derecho a un Transmisor colocado con un Receptor en la mano, o a un Receptor colocado con un Transmisor en la mano. |
| **Wireless Receiver** | `mvn_receiver` · Lámpara de redstone | El otro extremo. Si guarda el enlace a un Transmisor, **trae** hasta 128 ítems por ciclo que pasen **su** filtro desde la red del transmisor a la suya. Funciona a cualquier distancia y entre mundos mientras el chunk del otro extremo esté cargado. | Clic derecho: menú de filtro con un botón que abre el terminal de la red **remota**. **Whitelist vacía = no cruza nada**, a propósito. |

Contenedores con los que trabajan Grabbers, Pushers y Greedy Cells: cofres, cofres trampa, barriles,
tolvas, dispensadores, soltadores, hornos, altos hornos, ahumadores, soportes de pociones, estanterías
cinceladas, cajas de shulker y — con Slimefun instalado — máquinas de Slimefun (solo las ranuras que
la máquina declara de entrada/salida). Nunca otro bloque de MultiverseNets.

**Cómo entrega un Pusher** (pensado para alimentar máquinas como una Fundición de Slimefun):

* Como mucho **un stack por ranura**. Antes un ciclo de 128 o 1.024 ítems podía acabar en una sola
  ranura y lo que pasaba de un stack se perdía: eso es lo que vaciaba redes enteras.
* Con una **whitelist de varios ítems** (p. ej. los tres ingredientes de una aleación), cada ítem
  recibe su parte de las ranuras del destino (9 ranuras / 3 ingredientes = 3 cada uno) y se sirven por
  turnos en el mismo ciclo. Así el primer ingrediente ya no llena la máquina y bloquea la receta.
* En un **horno**, alto horno o ahumador: desde arriba todo va a la entrada; desde un lado el
  combustible va a su ranura y lo demás a la entrada. Nunca a la ranura de resultado.
* Lo que no cabe vuelve a la red; nada se pierde.

**Consejo para máquinas que reciben y devuelven en el mismo bloque** (la Fundición mete el resultado
en el dispensador si no tiene cofre de salida): pon un cofre de salida, apunta el Advanced Grabber a
ese cofre y el Advanced Pusher al dispensador. Si el grabber mira al dispensador, también sacará los
ingredientes que acabas de meter.

**Tolvas:** las tolvas (y vagonetas con tolva) **no interactúan con ningún bloque de MultiverseNets**,
ni para meter ni para sacar. Antes una tolva encima de un Infinity Barrel podía borrar su stack
entero.

### 🛠️ Crafteo

| Dispositivo | id · bloque | Qué hace | Cómo se usa |
|---|---|---|---|
| **Blueprint** | `mvn_blueprint` · ítem (Libro) | Una receta: cuadrícula 3×3 + resultado en su PDC. Craftear nunca lo consume. | Codifícalo en un Recipe Encoder e instálalo en un crafter. Instalarlo lo mueve al crafter; quitarlo, reemplazarlo o *Clear All* lo devuelve. |
| **Recipe Encoder** | `mvn_encoder` · Mesa de herrería | Monta una receta en una plantilla 3×3 (hacer clic solo marca huecos, no gasta ítems) y la escribe en un Blueprint (en blanco o ya codificado). | Rellena la plantilla, pon un Blueprint en la ranura azul y pulsa *Encode*. Clic en un Blueprint codificado carga su receta en la plantilla. Los Blueprints que dejes en sus ranuras se guardan en el bloque (solo un jugador a la vez los ve). |
| **Slimefun Recipe Encoder** | `mvn_sf_encoder` · Mesa de encantamientos | Igual para recetas de Slimefun. También guarda en el bloque los Blueprints que dejes en sus ranuras, así no hay que traer planos en blanco cada vez. | Necesita Slimefun; se desactiva con `sf-encoder.enabled`. |
| **Auto-Crafter** | `mvn_crafter` · Mesa de crafteo | Cada 20 ticks intenta una vez cada Blueprint vanilla instalado (hasta `crafter.max-recipes`, 18). Todo o nada: si falta un ingrediente no se toca nada, y si el resultado no cabe el crafteo entero se deshace. | Clic derecho y clic en los Blueprints para instalarlos. Rechaza Blueprints de Slimefun. |
| **Slimefun Auto-Crafter** | `mvn_sf_crafter` · Obsidiana llorosa | Igual, y acepta **Blueprints de Slimefun y vanilla**: puedes mezclar ambos en la misma máquina. | `sf-crafter.enabled: false` lo desactiva por completo (menú y crafteo). |
| **Request Crafter** | `mvn_request_crafter` · Mesa de flechas | Guarda Blueprints vanilla que **solo** se craftean bajo demanda desde un Request Terminal (nunca automáticamente). | Instala Blueprints igual que en un Auto-Crafter. |
| **Slimefun Request Crafter** | `mvn_sf_request_crafter` · Pilar de púrpura | Igual, con Blueprints de Slimefun y vanilla. | — |
| **Request Terminal** | `mvn_request_terminal` · Atril | Lista todo lo que pueden fabricar los Request Crafters de la red y lo craftea bajo demanda, resolviendo cadenas (troncos → tablones → mesa de crafteo) con el stock de la red. | Clic izquierdo 1 lote, shift+clic izquierdo 10, clic derecho 64, shift+clic derecho pide un número por chat. Alterna la entrega a tu inventario o a la red. |
| **Network Crafting Grid** | `mvn_crafting_grid` · Mesa de cartografía | Una mesa de crafteo que saca los ingredientes de la red; la plantilla queda guardada en el bloque. | Clic derecho, monta la plantilla, craftea. |

### 🧰 Herramientas

| Dispositivo | id · bloque | Qué hace | Cómo se usa |
|---|---|---|---|
| **Configuration Wrench** | `mvn_configurator` · ítem (Comparador) | Copia un filtro (plantillas exactas, materiales y modo whitelist/blacklist) de un dispositivo a otro. | Shift+clic derecho en un dispositivo con filtro para copiar, clic derecho en otro para pegar. |
| **Network Rake** | `mvn_rake` · ítem (Arbusto seco) | Desmonta un nodo al instante y te lo devuelve con su estado (filtro, Blueprints, enlace). 250 usos (`rake.uses`). | Clic derecho en un nodo. Rechaza controladores y almacenamientos que aún tengan ítems o fluido. |

### 🐔 GeneticChickengineering

| Dispositivo | id · bloque | Qué hace | Cómo se usa |
|---|---|---|---|
| **Genetic Chicken Sorter** | `mvn_chicken_sorter` · Bala de heno | Máquina dedicada **solo** a los pollos de bolsillo del addon GeneticChickengineering. Lee sus genes y mueve únicamente los pollos que cumplen **todas** sus reglas; cualquier otro ítem se ignora. Hasta 16 pollos por ciclo. | Clic derecho. **Push**: de la red al bloque al que mira (p. ej. un Roost o una máquina del addon). **Pull**: de ese bloque a la red. Reglas: lista de productos (clic con un pollo en el cursor o shift+clic a uno del inventario; vacía = cualquiera), nivel mínimo/máximo (genes recesivos; especies especiales 7-9), fuerza de ADN mínima (0-6), solo genes puros (sin Aa), ADN conocido/desconocido y edad (adulto/bebé). Selector de cara. **Empieza parado**: actívalo cuando esté configurado. |

Con este addon instalado, cada pollo es un ítem único (lleva su propio ADN). La red ya no fusiona dos
pollos distintos en un solo stack: antes un pollo podía salir con el ADN de otro.

Dispositivos con filtro: Grabbers, Pushers, Vacuum, Purger, Greedy Cell, Transmisor y Receptor.
Todos admiten modo whitelist y blacklist, y en los menús de Grabber/Pusher hacer clic en el botón de
una cara abre el contenedor de esa cara.

---

## 🔀 Cómo se mueven los ítems dentro y entre redes

### 1. Qué pertenece a una red

En cada escaneo (cada `network.scan-interval-ticks`, y justo después de colocar o romper un nodo) el
controlador recorre todos los bloques de MultiverseNets conectados, cara con cara. El escaneo:

* nunca carga chunks — los nodos de chunks sin cargar no forman parte de la red hasta que cargan;
* se detiene en otro controlador (`foreign controller at x,y,z`);
* se detiene en el terreno que el dueño de la red no puede usar (ver [Protección](#proteccion));
* con Slimefun instalado, también atraviesa bloques de Slimefun cuyo id contiene `CABLE` o `BRIDGE`
  y suma al almacenamiento los **barriles de Slimefun** que toque.

No hay estado por nodo del tipo "en qué red estoy": la topología se reconstruye de cero en cada
escaneo, así que un nodo nunca puede apuntar a una red que ya no existe.

### 2. Los almacenamientos internos y su orden

El almacenamiento de ítems de una red es la suma de cinco almacenamientos internos. Todo depósito —
de un grabber, el vacuum, el terminal, un resultado de crafteo o el puente inalámbrico — primero
consulta los **Quota Limiters** y luego llena en este orden:

| # | A dónde van los ítems | Condición |
|---|---|---|
| 1 | **Greedy Cells** | su filtro coincide, o ya guardan ese ítem |
| 2 | **Módulos de memoria** (DRAM Bays) | ya guardan ese tipo de ítem |
| 3 | **Barriles de Slimefun** | ya guardan ese tipo de ítem |
| 4 | **Celdas Cuánticas / Infinity Barrels** | ya guardan ese tipo de ítem |
| 5 | **Módulos de memoria** (DRAM Bays) | espacio libre, tipo nuevo |
| 6 | **Barriles de Slimefun** | vacíos |
| 7 | **Celdas Cuánticas / Infinity Barrels** | vacías (la celda adopta el tipo) |
| 8 | **Greedy Cells sin filtro** | desbordamiento general |

Lo que no cabe vuelve a quien lo depositó (nunca se borra).

Las retiradas leen en este orden: módulos de memoria → Celdas Cuánticas / Infinity Barrels →
barriles de Slimefun → Greedy Cells. Los Pushers, la succión de la propia Greedy Cell y el puente
inalámbrico **se saltan las Greedy Cells**, así el stock reservado para una línea de máquinas nunca
se exporta a otro sitio; terminales, crafteo y la API sí pueden usarlo.

Una retirada devuelve siempre **un solo** tipo de ítem (el primero que coincide). Un pusher con una
whitelist de varios ítems hace una retirada por ítem, por turnos, dentro del mismo ciclo.

### 3. Un ciclo, paso a paso

Cada 5 ticks el ticker ejecuta esto por red (cada familia con su intervalo configurable):

| Intervalo | Dispositivo | Por dispositivo |
|---|---|---|
| transfer (5 t) | Grabber / Advanced Grabber | Hasta 128 / 1.024 de un tipo de la primera cara que dé algo. Si la red rechaza parte: primero se ofrece directamente a los Pushers cuyo filtro lo acepta y, si sigue sobrando, espera en el **búfer de tránsito** del grabber (el grabber se pausa hasta vaciarlo; el búfer sobrevive a romper el bloque). Nunca se devuelve a la máquina: sus ranuras de entrada lo volverían a procesar. |
| transfer | Pusher / Advanced Pusher | Solo con un contenedor al lado: saca hasta 128 / 1.024 ítems (una whitelist de varios ítems, por turnos) y los mete en los contenedores vecinos con las reglas de arriba; lo que no cabe vuelve a la red, o espera en el búfer de tránsito del pusher si la red se llenó entretanto (se reintenta primero en el siguiente ciclo). |
| transfer | Genetic Chicken Sorter | Hasta 16 pollos que cumplan sus reglas, en el sentido elegido (push/pull). |
| transfer | Greedy Cell | Extrae hasta 512 ítems coincidentes, empuja hasta 256 a contenedores vecinos que no son de la red. |
| transfer | Purger | Borra hasta 128 ítems coincidentes (solo con filtro). |
| transfer | Wireless Receiver / Transmitter | Trae / envía hasta 128 ítems coincidentes por el puente (ver abajo). |
| transfer | Liquid Pump | Drena un bloque fuente (1.000 mB), solo si cabe entero. |
| vacuum (10 t) | Vacuum | Recoge los ítems del suelo dentro del radio. |
| craft (20 t) | Auto-Crafter / Slimefun Auto-Crafter | Intenta una vez cada Blueprint instalado. |

Los grabbers y pushers inactivos se relajan: tras no encontrar nada solo comprueban uno de cada tres
ciclos hasta que algo vuelve a moverse, así mil dispositivos inactivos casi no cuestan.

### 4. Entre redes: el puente inalámbrico

Dos redes nunca se fusionan por el aire. La única forma de que los ítems crucen es un enlace
**Transmisor / Receptor**, y mueve ítems el extremo que guarda el enlace:

```
 Red A                                             Red B
 [Controlador]-[Celdas]-[Transmisor]  ~~~~~~~~~~  [Receptor]-[Celdas]-[Controlador]

 Receptor enlazado al Transmisor:  B trae de A lo que pasa el filtro del RECEPTOR
 Transmisor enlazado al Receptor:  A envía a B lo que pasa el filtro del TRANSMISOR
```

* **Enlace**: shift+clic derecho a un Transmisor colocado con un Receptor en la mano (el receptor
  trae), o a un Receptor colocado con un Transmisor en la mano (el transmisor envía). Enlaza ambos
  para un intercambio en los dos sentidos.
* **El filtro está en el extremo que mueve**: whitelist o blacklist, plantillas exactas o materiales.
  **Una whitelist vacía no mueve nada**; una blacklist vacía lo mueve todo.
* Hasta 128 ítems de un tipo por ciclo y por dispositivo enlazado. **Nunca se vacían Greedy Cells.**
* Funciona a cualquier distancia y entre mundos, pero solo mientras el chunk del otro extremo esté
  cargado.
* Ambos extremos pasan la comprobación de protección, cada uno con el dueño de su propia red.
* Si el destino no admite los ítems vuelven al origen; si el origen tampoco los admite caen junto al
  dispositivo que los movió — nunca al vacío.
* El menú del Receptor tiene un botón que abre el terminal de **A**, así puedes usar a mano el
  almacenamiento de A desde la base de B (solo si tienes acceso al terreno de A).

<a id="buses-compartidos"></a>
### 5. Buses compartidos: dos controladores en los mismos cables

Si dos controladores acaban conectados (un cable que toca a ambos), cada uno forma su propia red y
ambas incluyen los cables y dispositivos intermedios. `/mvnets doctor`, la Probe y el holograma lo
reportan como `foreign controller at x,y,z`. Mientras dure:

* **Cada dispositivo compartido trabaja una sola vez por ciclo**: se asigna a la primera de las dos
  redes (orden estable por mundo y posición del controlador). Antes cada grabber, pusher, purgador,
  bomba y crafter compartido trabajaba dos veces por ciclo.
* Las celdas compartidas se ven desde ambos terminales.
* Los DRAM Bays compartidos se ven desde ambas redes, igual que las celdas. El módulo antiguo de un
  controlador solo lo ve su propia red.

Lo limpio es tener un controlador por red, y usar un par Transmisor/Receptor si de verdad quieres que
dos redes intercambien ítems.

### 6. Fluidos

El almacenamiento de fluidos es aparte: lo forman las Quantum Fluid Cells y los Fluid DRAM Modules
(en DRAM Bays), y solo lo mueven la Liquid Pump, la página de fluidos del Terminal, su ranura de
entrada y los clics directos a una celda de fluidos. Un depósito llena primero las celdas que ya
tienen ese fluido, luego los Fluid DRAM y por último las celdas vacías. Los depósitos son **todo o nada**: un cubo, una botella o un bloque fuente solo
se consume si cabe su volumen entero, así una red casi llena nunca se queda con el fluido y el cubo.

### 7. No se pierde nada, no se duplica nada

* Romper un nodo guarda su estado en el ítem (carga de la celda, filtros, cara, búfer de tránsito,
  Blueprints, plantilla, enlace del puente, limitador, fluido, filtro de la bomba, reglas del
  clasificador de pollos) y colocarlo lo restaura. El Rake hace lo mismo. Un DRAM Bay suelta su módulo
  aparte, con todo su stock dentro.
* Los pushers nunca dejan más de un stack en una ranura y nunca meten nada en otro nodo de la red.
* Las tolvas no pueden tocar ningún dispositivo.
* Todos los menús tienen protección anti-dupe y devuelven al cerrar lo que quede en ranuras reales.
* El crafteo es todo o nada, también cuando solo cabe parte del resultado.
* Los nodos son inmunes a pistones y explosiones.

---

<a id="proteccion"></a>
## 🛡️ Protección de terrenos

Compatibles: ProtectionStones, WorldGuard, Lands, Towny, GriefPrevention (más islas de BentoBox para
el acceso de jugadores).

* El **Controlador recuerda quién lo colocó**; ese jugador es el dueño de la red. Los controladores
  anteriores a esto adoptan como dueño al primer jugador que tenga permiso para abrirlos.
* El escaneo no extiende la red por terreno que su dueño no pueda usar, y **se comprueba cada bloque
  que una red lee o escribe** — grabbers, pushers, desvío a pushers, greedy cells, vacuum, bomba y
  ambos extremos del puente inalámbrico, en los dos sentidos.
* Los jugadores no pueden abrir dispositivos en terreno protegido ajeno
  (`multiversenets.protection.bypass` y `multiversenets.admin` se lo saltan). Eso incluye abrir una
  red remota desde un Receptor.
* Salidas: `protection.exempt-worlds`, `protection.exempt-locations` y
  `protection.block-network-linking: false`.

## 🍳 Recetas (mesa de crafteo)

Cada dispositivo se fabrica en una mesa de crafteo 3×3 estándar. `·` marca un hueco vacío. La lista
completa con la función de cada ítem está en [Recetas y funciones](Recipes.md).

Cuando una receta pide **otro dispositivo** (celda anterior, módulo anterior, cable, celda de fluidos,
Advanced Pusher), tiene que ser ese dispositivo: el material simple (terracota, lingote de cobre,
vidrio…) no vale. Las mejoras de celdas y de módulos conservan su carga; cualquier otra receta rechaza
un dispositivo que aún guarde algo, así nada se pierde crafteando. Y ningún dispositivo sirve como
ingrediente de una receta vanilla (un Terminal Inalámbrico ya no se gasta como Estrella del Nether). Los tres dispositivos
de Slimefun (Slimefun Recipe Encoder, Slimefun Auto-Crafter, Slimefun Request Crafter) solo tienen
receta mientras `sf-encoder.enabled` / `sf-crafter.enabled` estén en `true`.

| Dispositivo | Cuadrícula (3×3) | Ingredientes |
|---|---|---|
| Controlador | <pre>I I I<br/>I N I<br/>I I I</pre> | I = Bloque de hierro · N = Estrella del Nether |
| Cable ×16 | <pre>G G G<br/>G R G<br/>G G G</pre> | G = Vidrio · R = Redstone |
| Terminal | <pre>G E G<br/>E B E<br/>G E G</pre> | G = Vidrio · E = Perla de ender · B = Faro |
| Celda T1 | <pre>G G G<br/>G D G<br/>G G G</pre> | G = Vidrio · D = Diamante |
| Celda Tn+1 | <pre>D D D<br/>D P D<br/>D D D</pre> | D = Diamante · P = Celda anterior (con o sin carga; la conserva) |
| Simple Grabber | <pre>I O I<br/>O R O<br/>I O I</pre> | I = Lingote de hierro · O = Observador · R = Bloque de redstone |
| Simple Pusher | <pre>I D I<br/>D R D<br/>I D I</pre> | I = Lingote de hierro · D = Soltador · R = Bloque de redstone |
| Network Vacuum | <pre>S R S<br/>R H R<br/>S R S</pre> | S = Hilo · R = Redstone · H = Tolva |
| Auto-Crafter | <pre>R C R<br/>I T I<br/>R C R</pre> | R = Redstone · C = Mesa de crafteo · I = Lingote de hierro · T = Diana |
| Terminal Inalámbrico | <pre>· P ·<br/>P N P<br/>· C ·</pre> | P = Perla de ender · N = Estrella del Nether · C = Brújula |
| Network Monitor | <pre>G G G<br/>G C G<br/>G G G</pre> | G = Panel de vidrio · C = Comparador |
| Wireless Transmitter | <pre>I R I<br/>R C R<br/>I R I</pre> | I = Lingote de hierro · R = Bloque de redstone · C = Conducto |
| Wireless Receiver | <pre>I P I<br/>P L P<br/>I P I</pre> | I = Lingote de hierro · P = Perla de ender · L = Lámpara de redstone |
| Greedy Cell | <pre>G H G<br/>H S H<br/>G H G</pre> | G = Lingote de oro · H = Tolva · S = Bloque de slime |
| Advanced Grabber | <pre>O P O</pre> | O = Observador · P = Pistón pegajoso |
| Advanced Pusher | <pre>D P D</pre> | D = Soltador · P = Pistón |
| Recipe Encoder | <pre>K P K<br/>P S P<br/>K P K</pre> | K = Saco de tinta · P = Papel · S = Mesa de herrería |
| Network Crafting Grid | <pre>C R C<br/>R G R<br/>C R C</pre> | C = Mesa de crafteo · R = Redstone · G = Mesa de cartografía |
| Blueprint ×4 | <pre>P P P<br/>P B P<br/>P P P</pre> | P = Papel · B = Tinte azul |
| Configuration Wrench | <pre>I · I<br/>· C ·<br/>· I ·</pre> | I = Lingote de hierro · C = Comparador |
| Network Rake | <pre>D · D<br/>· S ·<br/>· S ·</pre> | D = Arbusto seco · S = Palo |
| Network Purger | <pre>I L I<br/>L H L<br/>I L I</pre> | I = Lingote de hierro · L = Bloque de magma · H = Tolva |
| Network Probe | <pre>· A ·<br/>A S A<br/>· A ·</pre> | A = Fragmento de amatista · S = Catalejo |
| Quantum Workbench | <pre>D D D<br/>D C D<br/>D D D</pre> | D = Diamante · C = Mesa de crafteo |
| Infinity Barrel | <pre>N D N<br/>D B D<br/>N D N</pre> | N = Lingote de netherita · D = Bloque de diamante · B = Barril |
| Network Router | <pre>· L ·<br/>· C ·<br/>· R ·</pre> | L = Pararrayos · C = Cable de Red · R = Bloque de redstone |
| L1 CPU Cache | <pre>C R C<br/>R C R<br/>C R C</pre> | C = Lingote de cobre · R = Redstone |
| L2 CPU Cache | <pre>G L G<br/>L P L<br/>G L G</pre> | G = Lingote de oro · L = Lapislázuli · P = L1 CPU Cache (conserva sus ítems) |
| L3 CPU Cache | <pre>D A D<br/>A P A<br/>D A D</pre> | D = Diamante · A = Fragmento de amatista · P = L2 CPU Cache |
| DRAM Module | <pre>N E N<br/>E P E<br/>N E N</pre> | N = Lingote de netherita · E = Ojo de ender · P = L3 CPU Cache |
| Quantum Cache Matrix | <pre>N S N<br/>S P S<br/>N S N</pre> | N = Bloque de netherita · S = Estrella del Nether · P = DRAM Module |
| DRAM Bay | <pre>I Q I<br/>R C R<br/>I Q I</pre> | I = Lingote de hierro · Q = Cuarzo · R = Redstone · C = Bloque de cobre |
| Fluid DRAM Module | <pre>D B D<br/>F E F<br/>D B D</pre> | D = Diamante · B = Cubo · E = Ojo de ender · F = Quantum Fluid Cell (vacía) |
| Genetic Chicken Sorter | <pre>F E F<br/>C P C<br/>F E F</pre> | F = Pluma · E = Huevo · C = Comparador · P = Advanced Pusher |
| Slimefun Recipe Encoder | <pre>E P E<br/>P B P<br/>E P E</pre> | E = Perla de ender · P = Papel · B = Mesa de encantamientos |
| Network Quota Limiter | <pre>R C R<br/>C T C<br/>R C R</pre> | R = Redstone · C = Comparador · T = Diana |
| Quantum Fluid Cell | <pre>G B G<br/>G L G<br/>G G G</pre> | G = Vidrio · B = Cubo · L = Bloque de lapislázuli |
| Liquid Pump | <pre>· G ·<br/>P B P<br/>· R ·</pre> | G = Vidrio tintado de azul · P = Pistón · B = Cubo · R = Redstone |
| Request Terminal | <pre>G L G<br/>R C R<br/>G G G</pre> | G = Vidrio · L = Atril · C = Mesa de crafteo · R = Redstone |
| Request Crafter | <pre>R C R<br/>I L I<br/>R C R</pre> | R = Redstone · C = Mesa de crafteo · I = Lingote de hierro · L = Atril |
| Slimefun Auto-Crafter | <pre>R C R<br/>I T I<br/>R C R</pre> | R = Perla de ender · C = Obsidiana llorosa · I = Lingote de hierro · T = Diana |
| Slimefun Request Crafter | <pre>R C R<br/>I L I<br/>R C R</pre> | R = Perla de ender · C = Pilar de púrpura · I = Lingote de hierro · L = Atril |

## ⌨️ Comandos

| Comando | Descripción | Permiso |
|---|---|---|
| `/mvnets help` | Lista de comandos | `multiversenets.use` |
| `/mvnets info` | Versión | `multiversenets.use` |
| `/mvnets guide [en\|es]` | Abre el menú de la guía: cada dispositivo con su receta, qué hace y cómo se usa, más cómo funcionan las redes. Botón para cambiar entre inglés y español | `multiversenets.use` |
| `/mvnets devices` | Lista los ids de dispositivos | `multiversenets.use` |
| `/mvnets give <id> [n]` | Da un dispositivo | `multiversenets.admin` |
| `/mvnets doctor` | Reescanea y diagnostica todas las redes (y dice si la integración con Slimefun está activa) | `multiversenets.admin` |
| `/mvnets stats` | Estadísticas globales | `multiversenets.admin` |
| `/mvnets inspect` | Inspecciona el bloque que miras (tipo, red, contenido, filtro) | `multiversenets.admin` |
| `/mvnets repair` | Fuerza un reescaneo de la red que miras | `multiversenets.admin` |
| `/mvnets recipes` | Vuelve a registrar y desbloquear todas las recetas | `multiversenets.admin` |
| `/mvnets reload` | Recarga la configuración, los proveedores de protección y las recetas | `multiversenets.admin` |

<a id="configuracion"></a>
## ⚙️ Configuración

Las claves más relevantes de `config.yml` (cada una está documentada en el propio archivo, en inglés y
español):

| Clave | Por defecto | Efecto |
|---|---|---|
| `network.scan-interval-ticks` | 20 | Intervalo de reescaneo de topología |
| `network.max-nodes` | 16384 | Nodos máximos por red |
| `network.max-nodes-per-chunk` | 64 | Dispositivos máximos por chunk |
| `network.op-interval-ticks.transfer` / `vacuum` / `craft` | 5 / 10 / 20 | Intervalos de cada familia de operaciones |
| `transfer.items-per-op` | 128 | Ítems por operación de Grabber/Pusher/Purger/puente |
| `transfer.ht-multiplier` | 8 | Multiplicador de los Grabbers/Pushers avanzados |
| `cells.capacities` | 65.536 … 2.000.000.000 | Capacidad por nivel de celda |
| `virtual-cache.tier-1` … `tier-5` | 2.048 … 524.288 | Capacidad de cada nivel de módulo de memoria de ítems |
| `greedy.capacity` / `barrel.capacity` | 262.144 / 2.000.000.000 | Capacidad de Greedy Cell / Infinity Barrel |
| `fluids.cell-capacity-mb` | 64.000 | Capacidad de la celda de fluidos |
| `fluids.dram-capacity-mb` | 512.000 | Capacidad total de un Fluid DRAM Module |
| `vacuum.radius` | 4.0 | Radio del vacuum |
| `crafter.max-recipes` | 18 | Blueprints por crafter |
| `wireless.local-range-without-router` | 64 | Alcance del Terminal Inalámbrico sin Router |
| `wireless.combat-cooldown-seconds` | 10 | Bloqueo del Terminal Inalámbrico tras combate |
| `compat.slimefun` | true | Integración con máquinas, cables y barriles de Slimefun |
| `sf-encoder.enabled` / `sf-crafter.enabled` | true | Codificador / crafters de Slimefun |
| `blocked-worlds` | [] | Mundos donde no se pueden colocar dispositivos |
| `protection.*` | — | Ver [Protección](#proteccion) |

## 🧹 Traído de Networks

Lo que tenían las cuatro variantes de Networks y faltaba aquí, elegido por utilidad real:

* **Network Purger** — descarta de la red lo que pase su filtro. Sin algo así una red se atasca sola
  con los residuos de las máquinas. **Sin filtro no borra nada**, a propósito.
* **Network Probe** — clic derecho en un bloque (sea nodo o no) y te dice a qué red pertenece, cuántos
  nodos tiene y dónde está su controlador.

## 🔗 Integración con Slimefun (opcional)

MultiverseNets **no depende de Slimefun**. Si está instalado se detecta al arrancar (tanto el fork de
DrakesCraft como el original) y, con `compat.slimefun: true`:

* Grabbers, Pushers, Greedy Cells y crafters trabajan con **máquinas de Slimefun** como con un cofre,
  usando solo las ranuras que la máquina declara de entrada y salida.
* Los bloques de Slimefun cuyo id contiene `CABLE` o `BRIDGE` conducen una red de MultiverseNets, y
  los barriles de Slimefun que la toquen pasan a formar parte de su almacenamiento.
* El codificador y los crafters de Slimefun quedan disponibles.

Sin Slimefun el puente queda inerte y nada más cambia. `/mvnets doctor` dice en su primera línea si la
integración está activa.

## 🤝 Convivencia con Networks

**Ambos plugins pueden estar instalados a la vez.** Sus identificadores nunca chocan:

| | MultiverseNets | NetworksV6-Drake |
|---|---|---|
| Nombre del plugin | `MultiverseNets` | `NetworksV6-Drake` |
| Clase principal | `com.chagui68.multiversenets.…` | `io.github.sefiraat.networks.…` |
| Comando | `/mvnets` | `/networks` |
| Permisos | `multiversenets.*` | `networks.*` |
| Ítems | propios, por PDC, con recetas vanilla | los de Slimefun (`NTW_*`) |

Networks no registra recetas vanilla (las suyas van por la mesa de Slimefun), así que las 46 de aquí
tampoco chocan. Cinco tests en `NetworksCoexistenceTest` fijan los identificadores, porque lo que
rompe la convivencia es un identificador renombrado sin querer, no el código.

**Interacciones a tener en cuenta** con la integración de Slimefun activa: un Grabber de
MultiverseNets puede sacar de un bloque de Networks (es un ítem de Slimefun con su propio menú), y un
bloque de Networks cuyo id contiene `CABLE` o `BRIDGE` (p. ej. `NTW_BRIDGE`) conduce una red de
MultiverseNets. Si prefieres que cada red se quede con lo suyo:

```yaml
compat:
  slimefun: false
```

## 🔍 En qué se diferencia de Networks

| | Networks (addon de Slimefun) | MultiverseNets |
|---|---|---|
| Dependencias | Slimefun + su cadena | Ninguna, solo la API de Paper |
| Pertenencia a la red | Cada nodo guarda su raíz | Se recalcula por BFS desde el controlador |
| Nodos huérfanos | Posibles | **Estructuralmente imposibles**: cada escaneo reconstruye la topología |
| Diagnóstico | Añadido después (`/networks doctor`) | `/mvnets doctor`, Probe, Monitor, holograma |
| Ticker | El ciclo de Slimefun | Propio, con intervalos por operación en la config |

El precio es que escanear cuesta un BFS sobre hasta `max-nodes` bloques cada `scan-interval-ticks`:
trabajo predecible y acotado a cambio de no tener estado que se pueda corromper.

## 🛠️ Compilación

```bash
mvn clean package
```

El jar se genera en `target/MultiverseNets-v<versión>.jar`.

## 📋 Compatibilidad

| Parámetro | Requisito |
|---|---|
| **Servidor** | Paper / Purpur 1.21.11 |
| **Java** | Java 21 LTS |
| **Dependencias** | Ninguna (Slimefun y los plugins de protección son opcionales) |

> **No es compatible con Folia.** El ticker de red, el escaneo de topología y todos los menús corren
> en el planificador global de Paper y asumen que el hilo principal es dueño de los bloques que
> tocan. Instálalo en Paper o Purpur.

## 📜 Licencia

Este proyecto está licenciado bajo los términos de la **GNU General Public License Version 3 (GPL-3.0)**. Consulta el archivo [LICENSE](../LICENSE) para más detalles.

---
