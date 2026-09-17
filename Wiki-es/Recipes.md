# 📜 Recetas y funciones de los ítems de MultiverseNets

A continuación se lista cada ítem disponible en el plugin, su **receta de elaboración** y una breve **descripción** de su función dentro de la red.

---

## Controlador
- **Receta**: 8 bloques de hierro + estrella del Nether.
- **Función**: Núcleo de la red. Indexa todos los nodos mediante BFS a través de los cables y mantiene la topología de la red.

---

## Cable (x16)
- **Receta**: 8 vidrios + redstone.
- **Función**: Transmite la señal entre nodos. Necesario para conectar cualquier dispositivo a la red.

---

## Terminal
- **Receta**: beacon + 4 perlas ender + 4 vidrios.
- **Función**: Interfaz principal para que los jugadores interactúen con la red (abrir la Grid, extraer/inyectar ítems).

---

## Terminal Inalámbrico
- **Receta**: estrella del Nether + 4 perlas ender + brújula.
- **Función**: Ítem que, al usarse sobre un controlador (shift+clic), permite abrir la terminal de esa red a distancia con clic derecho al aire.

---

## Celda T1 – T6
- **Receta**:
  - **T1**: 8 vidrios + diamante.
  - **Tn+1** (n≥1): 8 diamantes + celda anterior.
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
- **Receta**: bloque slime + 4 embudos + 4 lingotes de oro.
- **Función**: Actúa como búfer inteligente (capacidad por defecto 262 144, configurable en `greedy.capacity`). Reclama ítems filtrados de la red y los entrega a contenedores adyacentes, ideal para líneas continuas de transporte.

---

## Infinity Barrel (Barril infinito)
- **Receta**: barril + 4 lingotes de netherita + 4 bloques de diamante.
- **Función**: Almacén individual con capacidad de **2 × 10⁹** ítems de un solo tipo. Se integra a la red y puede depositar/extraer ítems como cualquier otro nodo de almacenamiento.

---

## Importador (Grabber)
- **Receta**: 4 observadores + 4 lingotes de hierro + bloque de redstone.
- **Función**: Extrae ítems de contenedores adyacentes hacia la red. Puede configurarse con filtro whitelist.

---

## Importador HT (Advanced Grabber)
- **Receta**: 2 observadores + pistón pegajoso.
- **Función**: Variante de alto rendimiento (×8 por defecto, configurable en `transfer.ht-multiplier`) del importador normal. Es direccional: apunta al contenedor del que extrae.

---

## Exportador (Pusher)
- **Receta**: 4 soltadores + 4 lingotes de hierro + bloque de redstone.
- **Función**: Inserta ítems desde la red a contenedores adyacentes. También soporta filtro whitelist.

---

## Exportador HT (Advanced Pusher)
- **Receta**: 2 soltadores + pistón.
- **Función**: Variante de alto rendimiento (×8 por defecto, configurable en `transfer.ht-multiplier`) del exportador normal. Es direccional: apunta al contenedor al que inserta.

---

## Aspirador (Vacuum)
- **Receta**: embudo + 4 hilos + 4 redstone.
- **Función**: Recoge ítems del suelo dentro de un radio configurable (por defecto 4.0, en `vacuum.radius`). Opcionalmente puede usar filtro whitelist.

---

## Network Purger
- **Receta**: embudo + 4 bloques de magma + 4 lingotes de hierro.
- **Función**: Descarta ítems de la red que coincidan con su filtro (whitelist/blacklist), evitando atascos de residuos. **Sin filtro configurado no borra nada**, a propósito.

---

## Network Probe (Sonda)
- **Receta**: 4 fragmentos de amatista + catalejo.
- **Función**: Al hacer clic derecho sobre un bloque (sea o no un nodo) muestra la red a la que pertenece, cuántos nodos tiene y dónde está su controlador.

---

## Auto‑Crafteador
- **Receta**: 2 mesas de crafteo + target + 2 lingotes de hierro + 4 redstone.
- **Función**: Ejecuta recetas definidas mediante **Blueprints** (matriz 3×3) o por resultado (modo antiguo). Cada ciclo intenta una sola elaboración de forma atómica: o hay ingredientes para todo o no se toca nada.

---

## Recipe Encoder
- **Receta**: mesa herrería + 4 papeles + 4 tintas.
- **Función**: Permite crear y guardar una receta en una matriz 3×3 de plantillas persistente. Genera un **Blueprint** que luego se instala en el Auto‑Crafteador.

---

## Blueprint en blanco (x4)
- **Receta**: 8 papeles + tinte azul.
- **Función**: Plantilla vacía que, al codificarse con el **Recipe Encoder**, se transforma en un Blueprint con la receta deseada.

---

## Crafting Grid
- **Receta**: mesa de cartografía + 4 mesas de crafteo + 4 redstone.
- **Función**: Permite a los jugadores usar la red como una mesa de crafteo tradicional, consumiendo ítems directamente de la red de forma transaccional.

---

## Quantum Workbench (Avanzado)
- **Receta**: 8 diamantes + mesa de crafteo.
- **Función**: Mesa de trabajo cuántica que permite **actualizar celdas cuánticas** (T1 → T2 → … → T6). Coloca una celda T1–T5 en el centro, rodéala con 8 diamantes y pulsa *Entangle & Upgrade*: la carga almacenada se preserva sin pérdidas.

---

## Network Monitor
- **Receta**: 8 paneles de vidrio + comparador.
- **Función**: Panel de diagnóstico que muestra el número de nodos, el almacenamiento total y el estado de la red en tiempo real.

---

## Transmisor Inalámbrico
- **Receta**: conducto + 4 bloques de redstone + 4 lingotes de hierro.
- **Función**: Vincula un **Receptor Inalámbrico** (shift+clic con el ítem del receptor sobre el transmisor) para crear una conexión de red a distancia. El receptor abre la terminal de la red del transmisor.

---

## Receptor Inalámbrico
- **Receta**: lámpara de redstone + 4 perlas ender + 4 lingotes de hierro.
- **Función**: Recibe la señal del transmisor y permite el acceso remoto a la red. Puede aplicar filtros para **bridgear ítems** entre ambas redes (sin filtro no cruza nada).

---

## Configuration Wrench
- **Receta**: 4 lingotes de hierro + comparador.
- **Función**: Herramienta de configuración rápida. Con *shift‑clic* copia la configuración de un dispositivo con filtro; con clic normal la pega en otro.

---

## Network Rake
- **Receta**: 2 dead bushes + 2 palos.
- **Función**: Elimina nodos de la red de forma instantánea (250 usos por defecto, configurable en `rake.uses`). No afecta a controladores ni celdas cargadas.

---

## Network Crayon
- **Receta**: tinte cian + palo.
- **Función**: Marca el controlador; la red muestra partículas alrededor de los bloques activos, facilitando la visualización de la topología.

---

Esta documentación está pensada para servir como referencia rápida tanto a jugadores como a desarrolladores que quieran entender el funcionamiento de cada ítem y cómo fabricarlos. Las capacidades y velocidades citadas corresponden a los valores por defecto de `config.yml` y pueden ajustarse en ese archivo.