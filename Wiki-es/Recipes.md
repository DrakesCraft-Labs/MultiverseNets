# 📜 Recetas y funciones de los ítems de MultiverseNets

A continuación se lista cada ítem disponible en el plugin, su **receta de elaboración** mostrada como la
verías en la mesa de crafteo (cuadrícula 3×3) y una breve **descripción** de su función dentro de la red.

> En las cuadrículas, `·` marca el hueco vacío de la mesa.

---

## Controlador
- **Receta (3×3)**:

  ```
  I I I
  I N I
  I I I
  ```

  > I = **Bloque de hierro** · N = **Estrella del Nether**

- **Resultado**: 1× Controlador
- **Función**: Núcleo de la red. Indexa todos los nodos mediante BFS a través de los cables y mantiene la topología de la red.

---

## Cable (×16)
- **Receta (3×3)**:

  ```
  G G G
  G R G
  G G G
  ```

  > G = **Vidrio** · R = **Redstone**

- **Resultado**: 16× Cable
- **Función**: Transmite la señal entre nodos. Necesario para conectar cualquier dispositivo a la red.

---

## Terminal
- **Receta (3×3)**:

  ```
  G E G
  E B E
  G E G
  ```

  > G = **Vidrio** · E = **Perla ender** · B = **Beacon**

- **Resultado**: 1× Terminal
- **Función**: Interfaz principal para que los jugadores interactúen con la red (abrir la Grid, extraer/inyectar ítems).

---

## Terminal Inalámbrico
- **Receta (3×3)**:

  ```
  · P ·
  P N P
  · C ·
  ```

  > P = **Perla ender** · N = **Estrella del Nether** · C = **Brújula**

- **Resultado**: 1× Terminal Inalámbrico
- **Función**: Ítem que, al usarse sobre un controlador (shift+clic), permite abrir la terminal de esa red a distancia con clic derecho al aire.

---

## Celda T1 – T6
- **Receta T1 (3×3)**:

  ```
  G G G
  G D G
  G G G
  ```

  > G = **Vidrio** · D = **Diamante**

  - **Receta Tn+1 (n≥1)**: la celda anterior se coloca entera en el centro y se rodea de diamantes.

  ```
  D D D
  D P D
  D D D
  ```

  > D = **Diamante** · P = **Celda anterior** (ítem exacto)

- **Resultado**: 1× Celda del siguiente nivel
- **Función**: Almacena un único tipo de ítem con capacidad creciente. Capacidades por defecto en `cells.capacities`:

| Nivel | Capacidad |
|---|---|
| T1 | 65 536 (65k) |
| T2 | 262 144 (262k) |
| T3 | 1 048 576 (1M) |
| T4 | 16 777 216 (16M) |
| T5 | 268 435 456 (268M) |
| T6 | 2 000 000 000 (2B) |

---

## Greedy Cell
- **Receta (3×3)**:

  ```
  G H G
  H S H
  G H G
  ```

  > G = **Lingote de oro** · H = **Embudo** · S = **Bloque slime**

- **Resultado**: 1× Greedy Cell
- **Función**: Actúa como búfer inteligente (capacidad por defecto 262 144, configurable en `greedy.capacity`). Reclama ítems filtrados de la red y los entrega a contenedores adyacentes, ideal para líneas continuas de transporte.

---

## Infinity Barrel (Barril infinito)
- **Receta (3×3)**:

  ```
  N D N
  D B D
  N D N
  ```

  > N = **Lingote de netherita** · D = **Bloque de diamante** · B = **Barril**

- **Resultado**: 1× Infinity Barrel
- **Función**: Almacén individual con capacidad de **2 × 10⁹** ítems de un solo tipo. Se integra a la red y puede depositar/extraer ítems como cualquier otro nodo de almacenamiento.

---

## Importador (Grabber)
- **Receta (3×3)**:

  ```
  I O I
  O R O
  I O I
  ```

  > I = **Lingote de hierro** · O = **Observador** · R = **Bloque de redstone**

- **Resultado**: 1× Importador
- **Función**: Extrae ítems de contenedores adyacentes hacia la red. Puede configurarse con filtro whitelist.

---

## Importador HT (Advanced Grabber)
- **Receta (3×3)**:

  ```
  O P O
  ```

  > O = **Observador** · P = **Pistón pegajoso**

- **Resultado**: 1× Importador HT
- **Función**: Variante de alto rendimiento (×8 por defecto, configurable en `transfer.ht-multiplier`) del importador normal. Es direccional: apunta al contenedor del que extrae.

---

## Exportador (Pusher)
- **Receta (3×3)**:

  ```
  I D I
  D R D
  I D I
  ```

  > I = **Lingote de hierro** · D = **Soltador** · R = **Bloque de redstone**

- **Resultado**: 1× Exportador
- **Función**: Inserta ítems desde la red a contenedores adyacentes. También soporta filtro whitelist.

---

## Exportador HT (Advanced Pusher)
- **Receta (3×3)**:

  ```
  D P D
  ```

  > D = **Soltador** · P = **Pistón**

- **Resultado**: 1× Exportador HT
- **Función**: Variante de alto rendimiento (×8 por defecto, configurable en `transfer.ht-multiplier`) del exportador normal. Es direccional: apunta al contenedor al que inserta.

---

## Aspirador (Vacuum)
- **Receta (3×3)**:

  ```
  S R S
  R H R
  S R S
  ```

  > S = **Hilo** · R = **Redstone** · H = **Embudo**

- **Resultado**: 1× Aspirador
- **Función**: Recoge ítems del suelo dentro de un radio configurable (por defecto 4.0, en `vacuum.radius`). Opcionalmente puede usar filtro whitelist.

---

## Network Purger
- **Receta (3×3)**:

  ```
  I L I
  L H L
  I L I
  ```

  > I = **Lingote de hierro** · L = **Bloque de magma** · H = **Embudo**

- **Resultado**: 1× Network Purger
- **Función**: Descarta ítems de la red que coincidan con su filtro (whitelist/blacklist), evitando atascos de residuos. **Sin filtro configurado no borra nada**, a propósito.

---

## Network Probe (Sonda)
- **Receta (3×3)**:

  ```
  · A ·
  A S A
  · A ·
  ```

  > A = **Fragmento de amatista** · S = **Catalejo**

- **Resultado**: 1× Network Probe
- **Función**: Al hacer clic derecho sobre un bloque (sea o no un nodo) muestra la red a la que pertenece, cuántos nodos tiene y dónde está su controlador.

---

## Auto‑Crafteador
- **Receta (3×3)**:

  ```
  R C R
  I T I
  R C R
  ```

  > R = **Redstone** · C = **Mesa de crafteo** · I = **Lingote de hierro** · T = **Target**

- **Resultado**: 1× Auto‑Crafteador
- **Función**: Ejecuta recetas definidas mediante **Blueprints** (matriz 3×3) o por resultado (modo antiguo). Cada ciclo intenta una sola elaboración de forma atómica: o hay ingredientes para todo o no se toca nada.

---

## Recipe Encoder
- **Receta (3×3)**:

  ```
  K P K
  P S P
  K P K
  ```

  > K = **Tinta** · P = **Papel** · S = **Mesa de herrería**

- **Resultado**: 1× Recipe Encoder
- **Función**: Permite crear y guardar una receta en una matriz 3×3 de plantillas persistente. Genera un **Blueprint** que luego se instala en el Auto‑Crafteador.

---

## Blueprint en blanco (×4)
- **Receta (3×3)**:

  ```
  P P P
  P B P
  P P P
  ```

  > P = **Papel** · B = **Tinte azul**

- **Resultado**: 4× Blueprint en blanco
- **Función**: Plantilla vacía que, al codificarse con el **Recipe Encoder**, se transforma en un Blueprint con la receta deseada.

---

## Crafting Grid
- **Receta (3×3)**:

  ```
  C R C
  R G R
  C R C
  ```

  > C = **Mesa de crafteo** · R = **Redstone** · G = **Mesa de cartografía**

- **Resultado**: 1× Crafting Grid
- **Función**: Permite a los jugadores usar la red como una mesa de crafteo tradicional, consumiendo ítems directamente de la red de forma transaccional.

---

## Quantum Workbench (Avanzado)
- **Receta (3×3)**:

  ```
  D D D
  D C D
  D D D
  ```

  > D = **Diamante** · C = **Mesa de crafteo**

- **Resultado**: 1× Quantum Workbench
- **Función**: Mesa de trabajo cuántica que permite **actualizar celdas cuánticas** (T1 → T2 → … → T6). Coloca una celda T1–T5 en el centro, rodéala con 8 diamantes y pulsa *Entangle & Upgrade*: la carga almacenada se preserva sin pérdidas.

---

## Network Monitor
- **Receta (3×3)**:

  ```
  G G G
  G C G
  G G G
  ```

  > G = **Panel de vidrio** · C = **Comparador**

- **Resultado**: 1× Network Monitor
- **Función**: Panel de diagnóstico que muestra el número de nodos, el almacenamiento total y el estado de la red en tiempo real.

---

## Transmisor Inalámbrico
- **Receta (3×3)**:

  ```
  I R I
  R C R
  I R I
  ```

  > I = **Lingote de hierro** · R = **Bloque de redstone** · C = **Conducto**

- **Resultado**: 1× Transmisor Inalámbrico
- **Función**: Vincula un **Receptor Inalámbrico** (shift+clic con el ítem del receptor sobre el transmisor) para crear una conexión de red a distancia. El receptor abre la terminal de la red del transmisor.

---

## Receptor Inalámbrico
- **Receta (3×3)**:

  ```
  I P I
  P L P
  I P I
  ```

  > I = **Lingote de hierro** · P = **Perla ender** · L = **Lámpara de redstone**

- **Resultado**: 1× Receptor Inalámbrico
- **Función**: Recibe la señal del transmisor y permite el acceso remoto a la red. Puede aplicar filtros para **bridgear ítems** entre ambas redes (sin filtro no cruza nada).

---

## Configuration Wrench
- **Receta (3×3)**:

  ```
  I · I
  · C ·
  · I ·
  ```

  > I = **Lingote de hierro** · C = **Comparador**

- **Resultado**: 1× Configuration Wrench
- **Función**: Herramienta de configuración rápida. Con *shift‑clic* copia la configuración de un dispositivo con filtro; con clic normal la pega en otro.

---

## Network Rake
- **Receta (3×3)**:

  ```
  D · D
  · S ·
  · S ·
  ```

  > D = **Dead bush** · S = **Palo**

- **Resultado**: 1× Network Rake
- **Función**: Elimina nodos de la red de forma instantánea (250 usos por defecto, configurable en `rake.uses`). No afecta a controladores ni celdas cargadas.

---

## Network Crayon
- **Receta (3×3)**:

  ```
  C
  S
  ```

  > C = **Tinte cian** · S = **Palo**

- **Resultado**: 1× Network Crayon
- **Función**: Marca el controlador; la red muestra partículas alrededor de los bloques activos, facilitando la visualización de la topología.

---

Esta documentación está pensada para servir como referencia rápida tanto a jugadores como a desarrolladores que quieran entender el funcionamiento de cada ítem y cómo fabricarlos. Las capacidades y velocidades citadas corresponden a los valores por defecto de `config.yml` y pueden ajustarse en ese archivo.