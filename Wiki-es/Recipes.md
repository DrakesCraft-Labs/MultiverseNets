# 📜 Recetas y funciones de los ítems de MultiverseNets

Cada ítem del plugin, su **receta** tal como se ve en la mesa de crafteo (cuadrícula 3×3) y qué
**hace** en la red. Las cifras son los valores por defecto de `config.yml`; "ciclo" significa un ciclo
de transferencia (`network.op-interval-ticks.transfer`, 5 ticks).

> En las cuadrículas, `·` marca un hueco vacío. Todo ítem se puede dar también con `/mvnets give <id>`.
> Volver al [índice de la wiki](README.md).

> **Dispositivos como ingredientes.** Cuando una receta pide otro dispositivo (celda anterior, módulo
> anterior, cable, celda de fluidos, Advanced Pusher) tiene que ser ese dispositivo: el material simple
> de debajo (terracota, lingote de cobre, vidrio, ladrillos de prismarina, pistón) no vale. Las mejoras
> de celdas y módulos conservan la carga del ingrediente; cualquier otra receta rechaza un dispositivo
> que aún guarde algo. Ningún dispositivo sirve como ingrediente de una receta vanilla.

---

## 🖥️ Núcleo y acceso

### Controlador de Red · `mvn_controller`

```
I I I
I N I
I I I
```

> I = **Bloque de hierro** · N = **Estrella del Nether**

- **Resultado**: 1× Controlador de Red (Magnetita)
- **Función**: Raíz de la red. Cada escaneo empieza aquí y recorre todos los bloques de MultiverseNets
  conectados. Quien lo coloca pasa a ser el **dueño** de la red (lo usa la protección de terrenos).
  Muestra un holograma con estado, número de nodos y totales almacenados. Los módulos de memoria van
  en un **DRAM Bay**, no en el controlador; un controlador que aún tenga un módulo antiguo lo sigue
  usando y su menú puede sacarlo con sus ítems. Clic derecho abre su menú de estado. Un solo
  controlador por red: un segundo cableado a los mismos cables se reporta como `foreign controller`.

---

### Cable de Red (×16) · `mvn_cable`

```
G G G
G R G
G G G
```

> G = **Vidrio** · R = **Redstone**

- **Resultado**: 16× Cable de Red (Vidrio)
- **Función**: Conecta dispositivos. Todos los bloques de MultiverseNets conducen; los cables son la
  forma barata de cubrir distancia. Clic derecho en un cable para ver si llega a un controlador y el
  tamaño de la red.

---

### Terminal de Red · `mvn_terminal`

```
G E G
E B E
G E G
```

> G = **Vidrio** · E = **Perla de ender** · B = **Faro**

- **Resultado**: 1× Terminal de Red (Faro)
- **Función**: La cuadrícula de almacenamiento. Muestra todos los ítems de la red y tiene una segunda
  página para fluidos. Clic izquierdo saca 1, clic derecho un stack, shift+clic al inventario;
  shift+clic izquierdo en tus ítems (o déjalos en la ranura de entrada) para guardarlos. Los cubos y
  botellas de miel van al almacenamiento de fluidos. Botones de búsqueda, orden y páginas.

---

### Terminal Inalámbrico · `mvn_wireless_terminal`

```
· P ·
P N P
· C ·
```

> P = **Perla de ender** · N = **Estrella del Nether** · C = **Brújula**

- **Resultado**: 1× Terminal Inalámbrico (ítem de mano)
- **Función**: Abre el terminal de una red a distancia. Shift+clic derecho en un Controlador o un
  Terminal para vincularlo, luego clic derecho al aire. Sin **Network Router** solo funciona en el
  mismo mundo y a 64 bloques (`wireless.local-range-without-router`). Se bloquea 10 s tras un combate y
  comprueba que puedas acceder al terreno de la red.

---

### Network Router · `mvn_router`

```
· L ·
· C ·
· R ·
```

> L = **Pararrayos** · C = **Cable de Red** · R = **Bloque de redstone**

- **Resultado**: 1× Network Router (Pararrayos)
- **Función**: Conectado en cualquier punto de una red, quita los límites del Terminal Inalámbrico
  para esa red: cualquier distancia y cualquier mundo.

---

### Network Monitor · `mvn_monitor`

```
G G G
G C G
G G G
```

> G = **Panel de vidrio** · C = **Comparador**

- **Resultado**: 1× Network Monitor (Nexo de reaparición)
- **Función**: Panel de diagnóstico en vivo: nodos por tipo, uso de almacenamiento y errores del
  escaneo. Se actualiza mientras está abierto.

---

### Network Probe · `mvn_probe`

```
· A ·
A S A
· A ·
```

> A = **Fragmento de amatista** · S = **Catalejo**

- **Resultado**: 1× Network Probe (ítem de mano)
- **Función**: Clic derecho en cualquier bloque (sea nodo o no) para ver a qué red pertenece, cuántos
  nodos tiene, dónde está su controlador y cualquier aviso del escaneo — incluidos los enlaces
  cortados por protección de terrenos.

---

### DRAM Bay · `mvn_dram_bay`

```
I Q I
R C R
I Q I
```

> I = **Lingote de hierro** · Q = **Cuarzo** · R = **Redstone** · C = **Bloque de cobre**

- **Resultado**: 1× DRAM Bay (Bombilla de cobre encerada)
- **Función**: Bloque de red que aloja **un módulo de memoria** (cualquiera de los módulos de ítems de
  abajo, o el Fluid DRAM). Mientras está instalado, el stock del módulo forma parte de la red. Clic
  derecho a un bay vacío con un módulo en la mano para instalarlo, o clic derecho para abrir su menú
  (stock, instalar desde el cursor o con shift+clic, **Eject Module**). Un módulo expulsado conserva
  todo su stock: instálalo en un DRAM Bay de otra red y el stock aparece allí y desaparece de la
  primera. Al romper el bay suelta el bay y su módulo (con el stock) por separado.

---

### Módulos de memoria · `mvn_cache_l1` … `mvn_cache_quantum`

Módulos de ítems para el **DRAM Bay**: almacenamiento multi-ítem, cualquier mezcla de tipos, con la
capacidad de cada nivel en `virtual-cache.tier-1` … `tier-5`. Un módulo conserva sus ítems al sacarlo
del bay. Cada nivel se craftea con el anterior; mejorar un módulo con ítems los conserva.

#### L1 CPU Cache Module — 2.048 ítems

```
C R C
R C R
C R C
```

> C = **Lingote de cobre** · R = **Redstone**

#### L2 CPU Cache Module — 8.192 ítems

```
G L G
L P L
G L G
```

> G = **Lingote de oro** · L = **Lapislázuli** · P = **L1 CPU Cache Module**

#### L3 CPU Cache Module — 32.768 ítems

```
D A D
A P A
D A D
```

> D = **Diamante** · A = **Fragmento de amatista** · P = **L2 CPU Cache Module**

#### DRAM Memory Module — 131.072 ítems

```
N E N
E P E
N E N
```

> N = **Lingote de netherita** · E = **Ojo de ender** · P = **L3 CPU Cache Module**

#### Quantum Cache Matrix — 524.288 ítems

```
N S N
S P S
N S N
```

> N = **Bloque de netherita** · S = **Estrella del Nether** · P = **DRAM Memory Module**

---

### Fluid DRAM Module · `mvn_fluid_dram`

```
D B D
F E F
D B D
```

> D = **Diamante** · B = **Cubo** · E = **Ojo de ender** · F = **Quantum Fluid Cell** (vacía)

- **Resultado**: 1× Fluid DRAM Module (ítem de mano, Corazón del mar)
- **Función**: Módulo de memoria **exclusivo para fluidos** para el DRAM Bay. Guarda varios fluidos a
  la vez, hasta `fluids.dram-capacity-mb` (512.000 mB = 512 cubos) en total, y lo suma al
  almacenamiento de fluidos de la red. Igual que los módulos de ítems, al sacarlo del bay se lleva sus
  fluidos a la red donde lo instales.

---

## 📦 Almacenamiento de ítems

### Celda Cuántica T1 – T6 · `mvn_cell_t1` … `mvn_cell_t6`

**T1:**

```
G G G
G D G
G G G
```

> G = **Vidrio** · D = **Diamante**

**Tn+1 (n ≥ 1):** la celda anterior en el centro, rodeada de diamantes.

```
D D D
D P D
D D D
```

> D = **Diamante** · P = **Celda anterior** (con o sin carga)

- **Resultado**: 1× celda del siguiente nivel (terracota: normal, naranja, amarilla, lima, cian, morada)
- **Función**: Guarda **un tipo de ítem** cada una. Una celda vacía adopta el primer tipo que no tenga
  otro sitio. Clic derecho para ver o gestionar su contenido; al romperla la carga queda dentro del
  ítem. Una celda **con carga** se mejora conservándola (en la mesa de crafteo o en la Quantum
  Workbench). Capacidades (`cells.capacities`):

| Nivel | Capacidad |
|---|---|
| T1 | 65.536 |
| T2 | 262.144 |
| T3 | 1.048.576 |
| T4 | 16.777.216 |
| T5 | 268.435.456 |
| T6 | 2.000.000.000 |

---

### Infinity Barrel · `mvn_infinity_barrel`

```
N D N
D B D
N D N
```

> N = **Lingote de netherita** · D = **Bloque de diamante** · B = **Barril**

- **Resultado**: 1× Infinity Barrel (Barril)
- **Función**: Como una celda, un tipo de ítem, con `barrel.capacity` (2.000.000.000). **Sigue
  registrado** a su ítem cuando se vacía. En su menú, clic en *Set Item* con un ítem en el cursor para
  registrarlo; clic derecho en *Set Item* con el cursor vacío borra el registro (solo si está vacío).
  Los ítems con id propio (Slimefun y otros plugins) se reconocen por su id y su texto visible, así
  lo que sacas siempre vuelve a entrar. Las tolvas no interactúan con él.

---

### Greedy Cell · `mvn_greedy_cell`

```
G H G
H S H
G H G
```

> G = **Lingote de oro** · H = **Tolva** · S = **Bloque de slime**

- **Resultado**: 1× Greedy Cell (Bloque de slime)
- **Función**: Búfer multi-ítem de hasta `greedy.capacity` (262.144) en total.
  - **Con filtro** es un sumidero prioritario: los ítems que entran en la red y pasan su filtro van
    a ella primero; cada ciclo extrae hasta 512 más de la red y empuja hasta 256 a los contenedores
    vecinos **que no son de la red** (cofres, máquinas de Slimefun). Ideal para alimentar una línea
    de máquinas.
  - **Sin filtro** es almacenamiento de desbordamiento, solo cuando todo lo demás está lleno.
  - **Filtro interno**: Pushers, terminales y crafteo pueden sacar de ella, pero un ítem definido en
    su filtro siempre deja 1 unidad dentro. Un Pusher con ese ítem en su whitelist lo libera: la
    Greedy Cell pasa todo a las celdas y deja de tomarlo. El puente inalámbrico nunca saca de ella.

---

### Quantum Workbench · `mvn_quantum_workbench`

```
D D D
D C D
D D D
```

> D = **Diamante** · C = **Mesa de crafteo**

- **Resultado**: 1× Quantum Workbench (Bloque de coral cerebro)
- **Función**: Sube una Celda Cuántica T1–T5 al siguiente nivel conservando su carga. Pon la celda en
  el centro, 8 diamantes alrededor, pulsa *Entangle & Upgrade* y recoge el resultado. Los ingredientes
  que queden en la cuadrícula se devuelven al cerrar el menú.

---

## 💧 Fluidos

### Quantum Fluid Cell · `mvn_fluid_cell`

```
G B G
G L G
G G G
```

> G = **Vidrio** · B = **Cubo** · L = **Bloque de lapislázuli**

- **Resultado**: 1× Quantum Fluid Cell (Ladrillos de prismarina)
- **Función**: Guarda un fluido — Agua, Lava, Leche, Nieve polvo o Miel — hasta
  `fluids.cell-capacity-mb` (64.000 mB = 64 cubos). Todas las celdas de fluidos de una red forman su
  almacenamiento de fluidos. Clic derecho con un cubo lleno / botella de miel para verter, con un cubo
  vacío para llenarlo (Agua, Lava, Leche, Nieve polvo). Su menú muestra el nivel, extrae un cubo y
  tiene un botón *Void Fluid Tank* (shift+clic derecho para confirmar) que vacía la celda para siempre.

---

### Liquid Pump · `mvn_liquid_pump`

```
· G ·
P B P
· R ·
```

> G = **Vidrio tintado de azul** · P = **Pistón** · B = **Cubo** · R = **Redstone**

- **Resultado**: 1× Liquid Pump (Vidrio tintado de azul)
- **Función**: Cada ciclo drena un bloque **fuente** de agua o lava justo **debajo** (1.000 mB) hacia
  las celdas de fluidos de la red. La fuente solo desaparece si los 1.000 mB caben enteros. Clic
  derecho para elegir ANY / WATER / LAVA.

---

## 🔄 Transporte de ítems

### Simple Grabber (Importador) · `mvn_grabber`

```
I O I
O R O
I O I
```

> I = **Lingote de hierro** · O = **Observador** · R = **Bloque de redstone**

- **Resultado**: 1× Simple Grabber (Observador)
- **Función**: Importa de los contenedores vecinos a la red: hasta 128 ítems de un tipo por ciclo, por
  las seis caras. Filtro whitelist/blacklist; un filtro vacío lo importa todo. Si la red rechaza
  parte, el sobrante va primero a los Pushers que lo acepten, luego vuelve al origen, y solo entonces
  espera en el búfer de tránsito del grabber (se conserva aunque lo rompas).

---

### Advanced Grabber · `mvn_grabber_ht`

```
O P O
```

> O = **Observador** · P = **Pistón pegajoso**

- **Resultado**: 1× Advanced Grabber (Pistón pegajoso)
- **Función**: Igual que el Simple Grabber ×8 (`transfer.ht-multiplier`): 1.024 ítems por ciclo. Su
  menú permite limitarlo a **una cara**.

---

### Simple Pusher (Exportador) · `mvn_pusher`

```
I D I
D R D
I D I
```

> I = **Lingote de hierro** · D = **Soltador** · R = **Bloque de redstone**

- **Resultado**: 1× Simple Pusher (Diana)
- **Función**: Exporta de la red a los contenedores vecinos por **las seis caras** (no tiene selector
  de cara): hasta 128 ítems por ciclo, solo si tiene un contenedor al lado y nunca a otro bloque de la
  red. **Una whitelist vacía no hace nada** (un pusher recién puesto nunca vacía la red); una
  **blacklist exporta todo menos lo listado** (lo listado se queda en la red). Como mucho un stack por
  ranura; una whitelist de varios ítems da a cada uno su parte de las ranuras del destino y los sirve
  por turnos, así un ingrediente no llena la máquina. A los hornos el combustible va a su ranura y lo
  demás a la entrada, nunca al resultado. Lo que no cabe vuelve a la red.

---

### Advanced Pusher · `mvn_pusher_ht`

```
D P D
```

> D = **Soltador** · P = **Pistón**

- **Resultado**: 1× Advanced Pusher (Pistón)
- **Función**: Igual que el Simple Pusher ×8 (1.024 por ciclo). Con una cara elegida entrega
  **solo** a ese lado; úsalo para alimentar una máquina.

---

### Network Vacuum · `mvn_vacuum`

```
S R S
R H R
S R S
```

> S = **Hilo** · R = **Redstone** · H = **Tolva**

- **Resultado**: 1× Network Vacuum (Esponja)
- **Función**: Cada 10 ticks recoge los ítems del suelo dentro de `vacuum.radius` (4 bloques) hacia la
  red. Filtro opcional. Lo que no cabe se queda en el suelo.

---

### Network Purger · `mvn_purger`

```
I L I
L H L
I L I
```

> I = **Lingote de hierro** · L = **Bloque de magma** · H = **Tolva**

- **Resultado**: 1× Network Purger (Bloque de magma)
- **Función**: Borra hasta 128 ítems por ciclo que pasen su filtro, para que los residuos (grava,
  semillas…) nunca atasquen la red. **Sin filtro no borra nada**, a propósito.

---

### Network Quota Limiter · `mvn_limiter`

```
R C R
C T C
R C R
```

> R = **Redstone** · C = **Comparador** · T = **Diana**

- **Resultado**: 1× Network Quota Limiter (Diana)
- **Función**: Limita cuánto de un ítem puede guardar la red. Todo depósito se detiene en el tope:
  grabbers, vacuum, terminal, resultados de crafteo y el puente inalámbrico. En su menú: clic con un
  ítem para fijar el objetivo, botones ±1/10/64/1.000 o un número por chat para el límite, y un
  interruptor de activado/desactivado. Varios limitadores sobre el mismo ítem: gana el más bajo.

---

### Wireless Transmitter · `mvn_transmitter`

```
I R I
R C R
I R I
```

> I = **Lingote de hierro** · R = **Bloque de redstone** · C = **Conducto**

- **Resultado**: 1× Wireless Transmitter (Conducto)
- **Función**: Un extremo del puente inalámbrico entre dos redes. Shift+clic derecho a un Transmisor
  colocado con un Receptor en la mano para que ese **receptor traiga** ítems de esta red. O enlaza al
  revés: shift+clic derecho a un Receptor colocado con un Transmisor en la mano, y este transmisor
  **envía** hasta 128 ítems por ciclo que pasen su filtro a la red del receptor. Su menú es un menú de
  filtro con un botón que abre el terminal de su propia red.

---

### Wireless Receiver · `mvn_receiver`

```
I P I
P L P
I P I
```

> I = **Lingote de hierro** · P = **Perla de ender** · L = **Lámpara de redstone**

- **Resultado**: 1× Wireless Receiver (Lámpara de redstone)
- **Función**: El otro extremo del puente. Enlazado a un Transmisor, **trae** hasta 128 ítems por ciclo
  que pasen **su** filtro desde la red del transmisor a la suya, a cualquier distancia y entre mundos
  mientras el chunk del otro extremo esté cargado. **Una whitelist vacía no mueve nada**, a propósito;
  una blacklist vacía lo mueve todo. Nunca vacía Greedy Cells. Su menú tiene un botón que abre el
  terminal de la red remota (si tienes acceso a ese terreno).

---

## 🛠️ Crafteo

### Blueprint en blanco (×4) · `mvn_blueprint`

```
P P P
P B P
P P P
```

> P = **Papel** · B = **Tinte azul**

- **Resultado**: 4× Blueprint (Libro)
- **Función**: Lleva una receta (cuadrícula 3×3 + resultado) una vez escrita por un Recipe Encoder.
  Craftear nunca lo consume. Instalarlo lo mueve al crafter; quitarlo, reemplazarlo o *Clear All* lo
  devuelve.

---

### Recipe Encoder · `mvn_encoder`

```
K P K
P S P
K P K
```

> K = **Saco de tinta** · P = **Papel** · S = **Mesa de herrería**

- **Resultado**: 1× Recipe Encoder (Mesa de herrería)
- **Función**: Monta la receta en su plantilla 3×3 persistente (hacer clic solo marca huecos, no gasta
  ítems), pon un Blueprint (en blanco o ya codificado) en la ranura azul y pulsa *Encode*. Clic en un
  Blueprint codificado vuelve a cargar su receta en la plantilla. Los Blueprints que dejes en sus
  ranuras se guardan en el bloque; solo un jugador a la vez los ve.

---

### Slimefun Recipe Encoder · `mvn_sf_encoder`

```
E P E
P B P
E P E
```

> E = **Perla de ender** · P = **Papel** · B = **Mesa de encantamientos**

- **Resultado**: 1× Slimefun Recipe Encoder (Mesa de encantamientos)
- **Función**: Igual que el Recipe Encoder para recetas de Slimefun. Los Blueprints que dejes en sus
  ranuras se quedan guardados en el bloque al cerrarlo. Necesita Slimefun. Su receta y su menú solo
  existen con `sf-encoder.enabled: true`.

---

### Auto-Crafter · `mvn_crafter`

```
R C R
I T I
R C R
```

> R = **Redstone** · C = **Mesa de crafteo** · I = **Lingote de hierro** · T = **Diana**

- **Resultado**: 1× Auto-Crafter (Mesa de crafteo)
- **Función**: Guarda hasta 18 Blueprints vanilla (`crafter.max-recipes`) y cada 20 ticks intenta cada
  uno una vez con el stock de la red. Todo o nada: si falta un ingrediente no se toca nada, y si el
  resultado no cabe el crafteo entero se deshace. Rechaza Blueprints de Slimefun (usa el Slimefun
  Auto-Crafter).

---

### Slimefun Auto-Crafter · `mvn_sf_crafter`

```
R C R
I T I
R C R
```

> R = **Perla de ender** · C = **Obsidiana llorosa** · I = **Lingote de hierro** · T = **Diana**

- **Resultado**: 1× Slimefun Auto-Crafter (Obsidiana llorosa)
- **Función**: Igual que el Auto-Crafter y acepta **Blueprints de Slimefun y vanilla**.
  `sf-crafter.enabled: false` quita su receta, su menú y su crafteo.

---

### Request Crafter · `mvn_request_crafter`

```
R C R
I L I
R C R
```

> R = **Redstone** · C = **Mesa de crafteo** · I = **Lingote de hierro** · L = **Atril**

- **Resultado**: 1× Request Crafter (Mesa de flechas)
- **Función**: Guarda Blueprints vanilla que **solo** se craftean bajo demanda desde un Request
  Terminal, nunca automáticamente.

---

### Slimefun Request Crafter · `mvn_sf_request_crafter`

```
R C R
I L I
R C R
```

> R = **Perla de ender** · C = **Pilar de púrpura** · I = **Lingote de hierro** · L = **Atril**

- **Resultado**: 1× Slimefun Request Crafter (Pilar de púrpura)
- **Función**: Igual que el Request Crafter, con Blueprints de Slimefun y vanilla. Depende de
  `sf-crafter.enabled`.

---

### Request Terminal · `mvn_request_terminal`

```
G L G
R C R
G G G
```

> G = **Vidrio** · L = **Atril** · C = **Mesa de crafteo** · R = **Redstone**

- **Resultado**: 1× Request Terminal (Atril)
- **Función**: Lista todo lo que pueden fabricar los Request Crafters de la red y lo craftea bajo
  demanda, resolviendo cadenas con el stock de la red (p. ej. troncos → tablones → mesa de crafteo).
  Clic izquierdo 1 lote, shift+clic izquierdo 10, clic derecho 64, shift+clic derecho pide una
  cantidad exacta por chat (un valor no numérico o ≤ 0 cancela). Un botón alterna la entrega a tu
  inventario o a la red. Los Auto-Crafters no aparecen aquí.

---

### Network Crafting Grid · `mvn_crafting_grid`

```
C R C
R G R
C R C
```

> C = **Mesa de crafteo** · R = **Redstone** · G = **Mesa de cartografía**

- **Resultado**: 1× Network Crafting Grid (Mesa de cartografía)
- **Función**: Una mesa de crafteo que saca los ingredientes de la red de forma transaccional. La
  plantilla se guarda en el bloque; *Craft 1* / *Craft All* te entregan el resultado.

---

## 🐔 GeneticChickengineering

### Genetic Chicken Sorter · `mvn_chicken_sorter`

```
F E F
C P C
F E F
```

> F = **Pluma** · E = **Huevo** · C = **Comparador** · P = **Advanced Pusher**

- **Resultado**: 1× Genetic Chicken Sorter (Bala de heno)
- **Función**: Mueve **solo** los pollos de bolsillo del addon GeneticChickengineering, elegidos por
  sus genes; cualquier otro ítem se ignora. **Push** envía los pollos que cumplen de la red al bloque
  al que mira; **Pull** los trae de ese bloque a la red (hasta 16 por ciclo). Reglas, todas deben
  cumplirse: productos aceptados (añade uno haciendo clic con un pollo en el cursor o con shift+clic a
  uno del inventario; lista vacía = cualquiera), nivel mínimo/máximo, fuerza de ADN mínima, solo genes
  puros, ADN conocido/desconocido, adulto/bebé. Empieza **parado**, así no puede vaciar una red antes
  de configurarlo.

---

## 🧰 Herramientas

### Configuration Wrench · `mvn_configurator`

```
I · I
· C ·
· I ·
```

> I = **Lingote de hierro** · C = **Comparador**

- **Resultado**: 1× Configuration Wrench (ítem de mano)
- **Función**: Shift+clic derecho en un dispositivo con filtro para copiarlo (plantillas exactas,
  materiales y modo whitelist/blacklist); clic derecho en otro para pegarlo, reemplazando su filtro.

---

### Network Rake · `mvn_rake`

```
D · D
· S ·
· S ·
```

> D = **Arbusto seco** · S = **Palo**

- **Resultado**: 1× Network Rake (ítem de mano, 250 usos — `rake.uses`)
- **Función**: Desmonta un nodo al instante y te lo devuelve con su estado (filtro, Blueprints,
  enlace…). Rechaza controladores y almacenamientos que aún tengan ítems o fluido.
