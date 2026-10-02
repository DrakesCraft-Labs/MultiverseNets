package com.chagui68.multiversenets.item;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * [EN] Text of the in-game guide (the {@code /mvnets guide} menu), in English and Spanish. Recipes are
 * not written here: the menu reads them from the recipes actually registered, so the guide can never
 * show a recipe the server does not have.
 *
 * [ES] Texto de la guía del juego (el menú de {@code /mvnets guide}), en inglés y español. Las recetas
 * no se escriben aquí: el menú las lee de las recetas registradas de verdad, así la guía nunca muestra
 * una receta que el servidor no tiene.
 */
public final class GuideContent {

    private GuideContent() {
    }

    public enum Category {
        CORE(Material.LODESTONE, "Core & Access", "Núcleo y acceso",
                "Controller, cables, terminals and diagnostics.",
                "Controlador, cables, terminales y diagnóstico."),
        STORAGE(Material.TERRACOTTA, "Item Storage", "Almacenamiento de ítems",
                "Quantum Cells, Infinity Barrel and Greedy Cell.",
                "Celdas Cuánticas, Infinity Barrel y Greedy Cell."),
        MEMORY(Material.WAXED_COPPER_BULB, "Memory (DRAM)", "Memoria (DRAM)",
                "DRAM Bay and the memory modules that carry their stock between networks.",
                "DRAM Bay y los módulos de memoria que llevan su stock entre redes."),
        FLUIDS(Material.PRISMARINE_BRICKS, "Fluids", "Fluidos",
                "Fluid cells and the Liquid Pump.",
                "Celdas de fluidos y la Liquid Pump."),
        TRANSPORT(Material.PISTON, "Item Transport", "Transporte de ítems",
                "Grabbers, Pushers, Vacuum, Purger, Limiter and the wireless bridge.",
                "Grabbers, Pushers, Vacuum, Purger, Limitador y el puente inalámbrico."),
        CRAFTING(Material.CRAFTING_TABLE, "Crafting", "Crafteo",
                "Blueprints, encoders, auto-crafters and request crafting.",
                "Blueprints, codificadores, autocrafters y crafteo bajo pedido."),
        TOOLS(Material.COMPARATOR, "Tools", "Herramientas",
                "Configuration Wrench and Network Rake.",
                "Llave de Configuración y Network Rake."),
        CHICKENS(Material.HAY_BLOCK, "GeneticChickengineering", "GeneticChickengineering",
                "Sorting pocket chickens by their genes.",
                "Clasificar pollos de bolsillo por sus genes.");

        private final Material icon;
        private final String nameEn;
        private final String nameEs;
        private final String blurbEn;
        private final String blurbEs;

        Category(Material icon, String nameEn, String nameEs, String blurbEn, String blurbEs) {
            this.icon = icon;
            this.nameEn = nameEn;
            this.nameEs = nameEs;
            this.blurbEn = blurbEn;
            this.blurbEs = blurbEs;
        }

        public Material icon() {
            return icon;
        }

        public String name(boolean es) {
            return es ? nameEs : nameEn;
        }

        public String blurb(boolean es) {
            return es ? blurbEs : blurbEn;
        }
    }

    /**
     * EN: One device: what it does and how to use it. {@code nameEs} is the Spanish name; the item
     * itself keeps its English name, so the Spanish guide shows both.
     * ES: Un dispositivo: qué hace y cómo se usa. {@code nameEs} es el nombre en español; el ítem
     * conserva su nombre en inglés, así la guía en español muestra los dos.
     */
    public record Entry(DeviceType type, Category category, String nameEs,
                        String whatEn, String whatEs, String useEn, String useEs) {

        public String name(boolean es) {
            return es ? nameEs : type.display();
        }

        public String what(boolean es) {
            return es ? whatEs : whatEn;
        }

        public String use(boolean es) {
            return es ? useEs : useEn;
        }
    }

    /** A general topic of the "How networks work" page. */
    public record Topic(Material icon, String titleEn, String titleEs, String textEn, String textEs) {

        public String title(boolean es) {
            return es ? titleEs : titleEn;
        }

        public String text(boolean es) {
            return es ? textEs : textEn;
        }
    }

    private static final Map<DeviceType, Entry> ENTRIES = new EnumMap<>(DeviceType.class);
    private static final List<Topic> TOPICS = new ArrayList<>();

    public static Entry of(DeviceType type) {
        return ENTRIES.get(type);
    }

    public static List<Entry> in(Category category) {
        List<Entry> list = new ArrayList<>();
        for (Entry entry : ENTRIES.values()) {
            if (entry.category() == category) {
                list.add(entry);
            }
        }
        return list;
    }

    public static List<Topic> topics() {
        return Collections.unmodifiableList(TOPICS);
    }

    private static void add(DeviceType type, Category category, String nameEs,
                            String whatEn, String whatEs, String useEn, String useEs) {
        ENTRIES.put(type, new Entry(type, category, nameEs, whatEn, whatEs, useEn, useEs));
    }

    private static void topic(Material icon, String titleEn, String titleEs, String textEn, String textEs) {
        TOPICS.add(new Topic(icon, titleEn, titleEs, textEn, textEs));
    }

    static {
        // ------------------------------------------------------------------ general topics
        topic(Material.LODESTONE, "What is a network?", "¿Qué es una red?",
                "A network is one Network Controller plus every MultiverseNets block touching it, face to face. "
                        + "Cables are the cheap way to connect, but every device conducts. Whoever places the "
                        + "Controller owns the network. Add at least one storage (cell, barrel or DRAM Bay) or the "
                        + "network has nowhere to keep items.",
                "Una red es un Controlador de Red más todos los bloques de MultiverseNets que lo tocan, cara con "
                        + "cara. Los cables son la forma barata de conectar, pero todos los dispositivos conducen. "
                        + "Quien coloca el Controlador es el dueño de la red. Añade al menos un almacenamiento "
                        + "(celda, barril o DRAM Bay) o la red no tendrá dónde guardar ítems.");
        topic(Material.CHEST, "Where items are stored", "Dónde se guardan los ítems",
                "Every deposit fills in this order: Greedy Cells whose filter matches, memory modules that "
                        + "already hold the item, Slimefun barrels, cells and barrels that hold it, free memory, "
                        + "empty Slimefun barrels, empty cells (they adopt the item) and finally Greedy Cells without "
                        + "a filter. Quota Limiters cap everything. What does not fit goes back; nothing is deleted.",
                "Cada depósito llena en este orden: Greedy Cells cuyo filtro coincide, módulos de memoria que ya "
                        + "tienen el ítem, barriles de Slimefun, celdas y barriles que lo tienen, memoria libre, "
                        + "barriles de Slimefun vacíos, celdas vacías (adoptan el ítem) y por último Greedy Cells sin "
                        + "filtro. Los Quota Limiters ponen tope a todo. Lo que no cabe vuelve; nada se borra.");
        topic(Material.HOPPER, "Filters", "Filtros",
                "Filters decide WHICH items move, never where to. Whitelist: only the listed items. Blacklist: "
                        + "everything except the listed items (they stay in the network). An empty whitelist on a "
                        + "Pusher or bridge does nothing, so a fresh device never empties a network. Put the exact "
                        + "item in the filter: Slimefun items are matched by their id.",
                "Los filtros deciden QUÉ ítems se mueven, nunca a dónde. Whitelist: solo lo listado. Blacklist: "
                        + "todo menos lo listado (eso se queda en la red). Una whitelist vacía en un Pusher o en el "
                        + "puente no hace nada, así un dispositivo nuevo nunca vacía una red. Pon el ítem exacto en "
                        + "el filtro: los ítems de Slimefun se comparan por su id.");
        topic(Material.BLAST_FURNACE, "Feeding machines", "Alimentar máquinas",
                "Use an Advanced Pusher and select the machine's face. A whitelist of several ingredients gives "
                        + "each one its share of the machine's slots, served in turn, so one ingredient cannot fill "
                        + "it. Never more than one stack per slot. Furnaces: fuel goes to the fuel slot, the rest to "
                        + "the input. For machines that return the product in the same block (a Smeltery without "
                        + "output chest) add an output chest and point the Advanced Grabber at it.",
                "Usa un Advanced Pusher y elige la cara de la máquina. Una whitelist de varios ingredientes da a "
                        + "cada uno su parte de las ranuras de la máquina, por turnos, así un ingrediente no la "
                        + "llena. Nunca más de un stack por ranura. Hornos: el combustible a su ranura, lo demás a "
                        + "la entrada. Para máquinas que devuelven el producto en el mismo bloque (una Fundición sin "
                        + "cofre de salida) pon un cofre de salida y apunta el Advanced Grabber a él.");
        topic(Material.ENDER_EYE, "Between networks", "Entre redes",
                "Networks never merge through the air. Items only cross with a Transmitter/Receiver link, and "
                        + "the end that holds the link moves items through its own filter. A memory module taken out "
                        + "of a DRAM Bay also carries its whole stock to another network.",
                "Las redes nunca se fusionan por el aire. Los ítems solo cruzan con un enlace Transmisor/Receptor, "
                        + "y mueve el extremo que guarda el enlace, con su propio filtro. Un módulo de memoria "
                        + "sacado de un DRAM Bay también lleva todo su stock a otra red.");
        topic(Material.SHIELD, "Safety rules", "Reglas de seguridad",
                "Breaking a device keeps its state in the item (cargo, filters, Blueprints...). Hoppers never "
                        + "interact with network blocks. Devices are immune to pistons and explosions. On claimed "
                        + "land a network only works inside its owner's land, and players cannot open devices on "
                        + "land they cannot use.",
                "Romper un dispositivo guarda su estado en el ítem (carga, filtros, Blueprints...). Las tolvas "
                        + "nunca interactúan con bloques de la red. Los dispositivos son inmunes a pistones y "
                        + "explosiones. En terreno protegido una red solo trabaja dentro del terreno de su dueño, y "
                        + "los jugadores no pueden abrir dispositivos en terreno que no pueden usar.");
        topic(Material.SPYGLASS, "Diagnostics & commands", "Diagnóstico y comandos",
                "Right-click a cable to see if it reaches a controller. The Network Probe and /mvnets doctor "
                        + "explain scan errors. /mvnets devices lists every device id; admins can use /mvnets give.",
                "Clic derecho en un cable muestra si llega a un controlador. La Network Probe y /mvnets doctor "
                        + "explican los errores del escaneo. /mvnets devices lista los ids; los admins pueden usar "
                        + "/mvnets give.");

        // ------------------------------------------------------------------ core
        add(DeviceType.MVN_CONTROLLER, Category.CORE, "Controlador de Red",
                "The root of the network: every scan starts here and walks through the connected blocks. "
                        + "Records its owner (who placed it) and shows a hologram with status, nodes and stored "
                        + "totals. Only one controller per network.",
                "La raíz de la red: cada escaneo empieza aquí y recorre los bloques conectados. Guarda a su "
                        + "dueño (quien lo colocó) y muestra un holograma con estado, nodos y totales. Un solo "
                        + "controlador por red.",
                "Place it first, then connect cables and devices. Right-click: status, router and a button to "
                        + "open the terminal. It does not take memory modules: they go in a DRAM Bay. A module that "
                        + "was inside an old controller waits in the Terminal as a temporary item, with its items.",
                "Colócalo primero y conecta cables y dispositivos. Clic derecho: estado, router y un botón que "
                        + "abre el terminal. No acepta módulos de memoria: van en un DRAM Bay. Un módulo que estaba "
                        + "dentro de un controlador antiguo espera en el Terminal como ítem temporal, con sus ítems.");
        add(DeviceType.MVN_CABLE, Category.CORE, "Cable de Red",
                "Connects devices. It has no logic of its own: it only carries the network.",
                "Conecta dispositivos. No tiene lógica propia: solo lleva la red.",
                "Right-click a cable to see whether it reaches a controller and how many nodes the network has. "
                        + "Holding a block while right-clicking places the block.",
                "Clic derecho en un cable para ver si llega a un controlador y cuántos nodos tiene la red. Con un "
                        + "bloque en la mano, el clic derecho coloca el bloque.");
        add(DeviceType.MVN_TERMINAL, Category.CORE, "Terminal de Red",
                "The storage grid: every item in the network (memory, cells, barrels, greedy cells, Slimefun "
                        + "barrels) plus a page for fluids.",
                "La cuadrícula del almacenamiento: todos los ítems de la red (memoria, celdas, barriles, greedy "
                        + "cells, barriles de Slimefun) y una página de fluidos.",
                "Left-click takes 1, right-click a stack, shift-click to your inventory. Shift-click your items "
                        + "(or drop them in the input slot) to store them. Search, sort and pages. Buckets and honey "
                        + "bottles go to the fluid storage. Each item shows its total in the network and how much of it "
                        + "is in DRAM, cells, barrels and Greedy Buffers. Recovered modules from an old controller are shown first: click to take them.",
                "Clic izquierdo saca 1, clic derecho un stack, shift+clic al inventario. Shift+clic en tus ítems "
                        + "(o déjalos en la ranura de entrada) para guardarlos. Búsqueda, orden y páginas. Cubos y "
                        + "botellas de miel van al almacenamiento de fluidos. Cada ítem muestra su total en la red y "
                        + "cuánto hay en DRAM, celdas, barriles y Greedy Buffers. Los módulos recuperados de un controlador antiguo salen primero: clic "
                        + "para recogerlos.");
        add(DeviceType.MVN_WIRELESS_TERMINAL, Category.CORE, "Terminal Inalámbrico",
                "Opens the Network Terminal from a distance.",
                "Abre el Terminal de Red a distancia.",
                "Shift+right-click a Controller or a Terminal to bind it, then right-click in the air. Without a "
                        + "Network Router it only works in the same world and nearby. Locked for a few seconds after "
                        + "combat.",
                "Shift+clic derecho en un Controlador o Terminal para vincularlo, luego clic derecho al aire. Sin "
                        + "Network Router solo funciona en el mismo mundo y cerca. Se bloquea unos segundos tras un "
                        + "combate.");
        add(DeviceType.MVN_ROUTER, Category.CORE, "Router de Red",
                "Removes the Wireless Terminal limits for its network: any distance, any world.",
                "Quita los límites del Terminal Inalámbrico para su red: cualquier distancia y mundo.",
                "Connect it anywhere in the network.",
                "Conéctalo en cualquier punto de la red.");
        add(DeviceType.MVN_MONITOR, Category.CORE, "Monitor de Red",
                "Live diagnostic panel: nodes by type, storage usage and errors.",
                "Panel de diagnóstico en vivo: nodos por tipo, uso del almacenamiento y errores.",
                "Right-click it; it refreshes while open.",
                "Clic derecho; se actualiza mientras está abierto.");
        add(DeviceType.MVN_PROBE, Category.CORE, "Sonda de Red",
                "Answers \"is this connected?\": network size, controller position, scan errors and land "
                        + "protection cuts for the block you click.",
                "Responde \"¿esto está conectado?\": tamaño de la red, posición del controlador, errores del "
                        + "escaneo y cortes por protección del bloque que pulsas.",
                "Right-click any block with it.",
                "Clic derecho en cualquier bloque.");

        // ------------------------------------------------------------------ storage
        String cellWhatEn = "Stores ONE item type in large amounts. An empty cell adopts the first item type that "
                + "has nowhere else to go. Breaking it keeps the cargo inside the item.";
        String cellWhatEs = "Guarda UN tipo de ítem en grandes cantidades. Una celda vacía adopta el primer tipo que "
                + "no tenga otro sitio. Al romperla la carga queda dentro del ítem.";
        String cellUseEn = "Connect it to the network. Right-click to see or manage its contents. Upgrade it one tier "
                + "(cell + 8 diamonds) in a crafting table or in the Quantum Workbench: the cargo is kept.";
        String cellUseEs = "Conéctala a la red. Clic derecho para ver o gestionar su contenido. Súbela un nivel "
                + "(celda + 8 diamantes) en la mesa de crafteo o en la Quantum Workbench: la carga se conserva.";
        DeviceType[] cells = {DeviceType.MVN_CELL_T1, DeviceType.MVN_CELL_T2, DeviceType.MVN_CELL_T3,
                DeviceType.MVN_CELL_T4, DeviceType.MVN_CELL_T5, DeviceType.MVN_CELL_T6};
        for (int i = 0; i < cells.length; i++) {
            add(cells[i], Category.STORAGE, "Celda Cuántica T" + (i + 1), cellWhatEn, cellWhatEs, cellUseEn, cellUseEs);
        }
        add(DeviceType.MVN_INFINITY_BARREL, Category.STORAGE, "Barril Infinito",
                "Like a cell (one item type) with a huge capacity, but it STAYS registered to its item when it "
                        + "empties. Items from Slimefun and other plugins are recognised by their id, so whatever "
                        + "you take out always goes back in.",
                "Como una celda (un tipo de ítem) con una capacidad enorme, pero SIGUE registrado a su ítem al "
                        + "vaciarse. Los ítems de Slimefun y otros plugins se reconocen por su id, así lo que sacas "
                        + "siempre vuelve a entrar.",
                "Right-click: click Set Item with an item on the cursor to register it; right-click Set Item with "
                        + "an empty cursor clears it (only when empty). Quick deposit / take out buttons. Hoppers do "
                        + "not interact with it.",
                "Clic derecho: clic en Set Item con un ítem en el cursor para registrarlo; clic derecho en Set Item "
                        + "con el cursor vacío lo borra (solo vacío). Botones de depósito y retirada rápida. Las "
                        + "tolvas no interactúan con él.");
        add(DeviceType.MVN_GREEDY_CELL, Category.STORAGE, "Celda Codiciosa",
                "With a filter: a priority sink. Matching items go to it before any other storage, it pulls more "
                        + "from the network every cycle and pushes them into the containers next to it (not into "
                        + "network blocks). Holds several item types. Without a filter: overflow storage.",
                "Con filtro: un sumidero prioritario. Los ítems que coinciden van a ella antes que a cualquier "
                        + "otro almacenamiento, saca más de la red cada ciclo y los empuja a los contenedores de al "
                        + "lado (no a bloques de la red). Guarda varios tipos. Sin filtro: almacenamiento de "
                        + "desbordamiento.",
                "Right-click to set the filter and see its buffer. It is an internal filter: an item defined in it "
                        + "always keeps 1 unit inside, even when Pushers take the rest. A Pusher with that item in its "
                        + "whitelist releases it: the Greedy Cell moves all of it to the cells and stops taking it.",
                "Clic derecho para poner el filtro y ver su búfer. Es un filtro interno: un ítem definido en él "
                        + "siempre deja 1 unidad dentro, aunque los Pushers saquen el resto. Un Pusher con ese ítem en "
                        + "su whitelist lo libera: la Greedy Cell pasa todo a las celdas y deja de tomarlo.");
        add(DeviceType.MVN_QUANTUM_WORKBENCH, Category.STORAGE, "Mesa Cuántica",
                "Upgrades a Quantum Cell T1-T5 to the next tier keeping its cargo.",
                "Sube una Celda Cuántica T1-T5 al siguiente nivel conservando su carga.",
                "Cell in the centre, 8 diamonds around it, press Entangle & Upgrade and take the result. "
                        + "Ingredients left inside are returned when you close it.",
                "Celda en el centro, 8 diamantes alrededor, pulsa Entangle & Upgrade y recoge el resultado. Lo que "
                        + "quede dentro se devuelve al cerrar.");

        // ------------------------------------------------------------------ memory
        add(DeviceType.MVN_DRAM_BAY, Category.MEMORY, "Bahía DRAM",
                "Network block that holds ONE memory module. The module is the storage: while installed, its "
                        + "stock is part of the network. Place as many bays as you like.",
                "Bloque de red que aloja UN módulo de memoria. El módulo es el almacenamiento: mientras está "
                        + "instalado, su stock es parte de la red. Pon tantos bays como quieras.",
                "Right-click an empty bay holding a module to install it, or right-click to open its menu: stock, "
                        + "install from the cursor or with shift-click, and Eject Module. An ejected module keeps "
                        + "its whole stock: install it in another network's bay and the stock appears there and "
                        + "leaves the first one. Breaking the bay drops it and the module with its stock.",
                "Clic derecho a un bay vacío con un módulo en la mano para instalarlo, o clic derecho para abrir su "
                        + "menú: stock, instalar desde el cursor o con shift+clic, y Eject Module. Un módulo "
                        + "expulsado conserva todo su stock: instálalo en el bay de otra red y el stock aparece allí "
                        + "y sale de la primera. Romper el bay suelta el bay y el módulo con su stock.");
        String moduleWhatEn = "Item memory module for a DRAM Bay: stores any mix of item types up to its capacity. "
                + "It keeps its items when taken out of the bay.";
        String moduleWhatEs = "Módulo de memoria de ítems para un DRAM Bay: guarda cualquier mezcla de tipos hasta su "
                + "capacidad. Conserva sus ítems al sacarlo del bay.";
        String moduleUseEn = "Install it in a DRAM Bay. Each tier is crafted from the previous module; upgrading a "
                + "module that holds items keeps them.";
        String moduleUseEs = "Instálalo en un DRAM Bay. Cada nivel se craftea con el módulo anterior; mejorar un "
                + "módulo con ítems los conserva.";
        add(DeviceType.MVN_CACHE_L1, Category.MEMORY, "Módulo de Caché L1", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_CACHE_L2, Category.MEMORY, "Módulo de Caché L2", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_CACHE_L3, Category.MEMORY, "Módulo de Caché L3", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_CACHE_DRAM, Category.MEMORY, "Módulo DRAM", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_CACHE_QUANTUM, Category.MEMORY, "Matriz de Caché Cuántica", moduleWhatEn, moduleWhatEs, moduleUseEn, moduleUseEs);
        add(DeviceType.MVN_FLUID_DRAM, Category.MEMORY, "Módulo DRAM de Fluidos",
                "Fluid-only memory module: holds several fluids at once up to its total capacity, and adds it to "
                        + "the network's fluid storage.",
                "Módulo de memoria exclusivo para fluidos: guarda varios fluidos a la vez hasta su capacidad total "
                        + "y la suma al almacenamiento de fluidos de la red.",
                "Install it in a DRAM Bay. Taking it out carries its fluids to whichever network you install it "
                        + "in next. The two fluid cells of the recipe must be empty.",
                "Instálalo en un DRAM Bay. Al sacarlo se lleva sus fluidos a la red donde lo instales. Las dos "
                        + "celdas de fluidos de la receta deben estar vacías.");

        // ------------------------------------------------------------------ fluids
        add(DeviceType.MVN_FLUID_CELL, Category.FLUIDS, "Celda Cuántica de Fluidos",
                "Holds one fluid: Water, Lava, Milk, Powder Snow or Honey. Every fluid cell of a network forms its "
                        + "fluid storage, together with Fluid DRAM Modules.",
                "Guarda un fluido: Agua, Lava, Leche, Nieve polvo o Miel. Todas las celdas de fluidos de una red "
                        + "forman su almacenamiento de fluidos, junto con los Fluid DRAM Modules.",
                "Right-click with a filled bucket or honey bottle to pour, with an empty bucket to fill it. Or use "
                        + "the Terminal's fluid page. A bucket is only used up if the whole bucket fits.",
                "Clic derecho con un cubo lleno o botella de miel para verter, con un cubo vacío para llenarlo. O "
                        + "usa la página de fluidos del Terminal. Un cubo solo se gasta si cabe entero.");
        add(DeviceType.MVN_LIQUID_PUMP, Category.FLUIDS, "Bomba de Líquidos",
                "Each cycle drains one SOURCE block of water or lava directly below it (1,000 mB). The block is "
                        + "only removed if the whole 1,000 mB fit in the network.",
                "Cada ciclo drena un bloque FUENTE de agua o lava justo debajo (1.000 mB). El bloque solo "
                        + "desaparece si los 1.000 mB caben enteros en la red.",
                "Place it on top of the liquid. Right-click to choose ANY / WATER / LAVA.",
                "Colócala encima del líquido. Clic derecho para elegir ANY / WATER / LAVA.");

        // ------------------------------------------------------------------ transport
        add(DeviceType.MVN_GRABBER, Category.TRANSPORT, "Grabber Simple",
                "Imports from adjacent containers and machines into the network through ALL six faces, one item "
                        + "type per cycle. From a furnace it only takes the result. What the network cannot take "
                        + "waits in its buffer.",
                "Importa de los contenedores y máquinas de al lado a la red por LAS SEIS caras, un tipo de ítem "
                        + "por ciclo. De un horno solo saca el resultado. Lo que la red no admite espera en su búfer.",
                "Right-click for the filter (whitelist / blacklist). An empty filter takes everything.",
                "Clic derecho para el filtro (whitelist / blacklist). Un filtro vacío lo toma todo.");
        add(DeviceType.MVN_GRABBER_HT, Category.TRANSPORT, "Grabber Avanzado",
                "Same as the Simple Grabber but 8 times faster, and it can be limited to ONE face.",
                "Igual que el Grabber Simple pero 8 veces más rápido, y se puede limitar a UNA cara.",
                "Filter menu plus face selector. Point it at the machine's OUTPUT (or its output chest).",
                "Menú de filtro más selector de cara. Apúntalo a la SALIDA de la máquina (o a su cofre de salida).");
        add(DeviceType.MVN_PUSHER, Category.TRANSPORT, "Pusher Simple",
                "Exports from the network into adjacent containers and machines through ALL six faces (no face "
                        + "selector). Never into another network block, never more than one stack per slot.",
                "Exporta de la red a los contenedores y máquinas de al lado por LAS SEIS caras (sin selector de "
                        + "cara). Nunca a otro bloque de la red, nunca más de un stack por ranura.",
                "The filter decides what leaves: empty whitelist = idle, blacklist = everything except the listed "
                        + "items. A whitelist of several items shares the target's slots between them.",
                "El filtro decide qué sale: whitelist vacía = inactivo, blacklist = todo menos lo listado. Una "
                        + "whitelist de varios ítems reparte las ranuras del destino entre ellos.");
        add(DeviceType.MVN_PUSHER_HT, Category.TRANSPORT, "Pusher Avanzado",
                "Same as the Simple Pusher but 8 times faster, and with a face selected it delivers ONLY to that "
                        + "side. The right tool to feed one machine with its ingredients.",
                "Igual que el Pusher Simple pero 8 veces más rápido, y con una cara elegida entrega SOLO a ese "
                        + "lado. La herramienta para alimentar una máquina con sus ingredientes.",
                "Filter menu plus face selector: pick the machine's face and whitelist its ingredients.",
                "Menú de filtro más selector de cara: elige la cara de la máquina y pon sus ingredientes en la "
                        + "whitelist.");
        add(DeviceType.MVN_VACUUM, Category.TRANSPORT, "Aspiradora de Red",
                "Picks up dropped items around it into the network.",
                "Recoge al almacenamiento los ítems tirados a su alrededor.",
                "Optional filter. Items that do not fit stay on the ground.",
                "Filtro opcional. Lo que no cabe se queda en el suelo.");
        add(DeviceType.MVN_PURGER, Category.TRANSPORT, "Purgador de Red",
                "Deletes items that match its filter from the network every cycle.",
                "Borra de la red cada ciclo los ítems que pasan su filtro.",
                "Without a filter it deletes nothing, on purpose. Use it for machine waste.",
                "Sin filtro no borra nada, a propósito. Úsalo para los residuos de las máquinas.");
        add(DeviceType.MVN_LIMITER, Category.TRANSPORT, "Limitador de Cuota",
                "Caps how much of one item the network may hold. Every deposit stops at the cap.",
                "Limita cuánto de un ítem puede guardar la red. Todo depósito se detiene en el tope.",
                "Right-click: target item, limit (buttons or chat) and on/off. With several limiters on the same "
                        + "item the lowest wins.",
                "Clic derecho: ítem objetivo, límite (botones o chat) y activar/desactivar. Con varios limitadores "
                        + "sobre el mismo ítem gana el más bajo.");
        add(DeviceType.MVN_TRANSMITTER, Category.TRANSPORT, "Transmisor Inalámbrico",
                "One end of the wireless bridge. If it holds the link to a Receiver it PUSHES the items that pass "
                        + "its filter into the receiver's network, at any distance and across worlds.",
                "Un extremo del puente inalámbrico. Si guarda el enlace a un Receptor, EMPUJA los ítems que pasan "
                        + "su filtro a la red del receptor, a cualquier distancia y entre mundos.",
                "Shift+right-click a placed Receiver holding the Transmitter item to link it. Right-click: filter "
                        + "and a button that opens its own network's terminal.",
                "Shift+clic derecho a un Receptor colocado con el Transmisor en la mano para enlazarlo. Clic "
                        + "derecho: filtro y un botón que abre el terminal de su propia red.");
        add(DeviceType.MVN_RECEIVER, Category.TRANSPORT, "Receptor Inalámbrico",
                "The other end. If it holds the link to a Transmitter it PULLS the items that pass its filter "
                        + "from the transmitter's network into its own. An empty whitelist moves nothing.",
                "El otro extremo. Si guarda el enlace a un Transmisor, TRAE los ítems que pasan su filtro desde la "
                        + "red del transmisor a la suya. Una whitelist vacía no mueve nada.",
                "Shift+right-click a placed Transmitter holding the Receiver item to link it. Its menu can open the "
                        + "REMOTE network's terminal.",
                "Shift+clic derecho a un Transmisor colocado con el Receptor en la mano para enlazarlo. Su menú "
                        + "puede abrir el terminal de la red REMOTA.");

        // ------------------------------------------------------------------ crafting
        add(DeviceType.MVN_BLUEPRINT, Category.CRAFTING, "Plano",
                "A recipe: the 3x3 grid and its result. Crafting never consumes it.",
                "Una receta: la cuadrícula 3x3 y su resultado. Craftear nunca lo gasta.",
                "Craft blank ones, write a recipe on them in a Recipe Encoder and install them in a crafter.",
                "Craftea planos en blanco, escribe una receta en un Recipe Encoder e instálalos en un crafter.");
        add(DeviceType.MVN_ENCODER, Category.CRAFTING, "Codificador de Recetas",
                "Writes a vanilla recipe onto a Blueprint. Clicking the template only marks slots, no items are "
                        + "spent.",
                "Escribe una receta vanilla en un Blueprint. Hacer clic en la plantilla solo marca huecos, no "
                        + "gasta ítems.",
                "Fill the template, put a Blueprint in the blue slot and press Encode. Blueprints left in its "
                        + "slots stay stored in the block. Clicking an encoded Blueprint loads its recipe.",
                "Rellena la plantilla, pon un Blueprint en la ranura azul y pulsa Encode. Los Blueprints que dejes "
                        + "en sus ranuras se guardan en el bloque. Clic en un Blueprint codificado carga su receta.");
        add(DeviceType.MVN_SF_ENCODER, Category.CRAFTING, "Codificador de Slimefun",
                "Same as the Recipe Encoder for Slimefun recipes. Needs Slimefun.",
                "Igual que el Recipe Encoder para recetas de Slimefun. Necesita Slimefun.",
                "Blueprints left in its slots stay stored in the block, so you do not need to bring blank "
                        + "Blueprints every time.",
                "Los Blueprints que dejes en sus ranuras se quedan guardados en el bloque, así no hace falta traer "
                        + "planos en blanco cada vez.");
        add(DeviceType.MVN_CRAFTER, Category.CRAFTING, "Autocrafter",
                "Every second it tries each installed vanilla Blueprint once with the network's stock. "
                        + "All-or-nothing: a missing ingredient touches nothing, and a result that does not fit "
                        + "undoes the craft.",
                "Cada segundo intenta una vez cada Blueprint vanilla instalado con el stock de la red. Todo o "
                        + "nada: si falta un ingrediente no se toca nada, y si el resultado no cabe se deshace.",
                "Right-click and click Blueprints in (up to 18). Slimefun Blueprints are refused.",
                "Clic derecho y clic en los Blueprints para instalarlos (hasta 18). Rechaza los de Slimefun.");
        add(DeviceType.MVN_SF_CRAFTER, Category.CRAFTING, "Autocrafter de Slimefun",
                "Same as the Auto-Crafter and it accepts BOTH Slimefun and vanilla Blueprints in the same machine.",
                "Igual que el Autocrafter y acepta Blueprints de Slimefun Y vanilla en la misma máquina.",
                "Install Blueprints like in the Auto-Crafter.",
                "Instala Blueprints igual que en el Autocrafter.");
        add(DeviceType.MVN_REQUEST_CRAFTER, Category.CRAFTING, "Crafter bajo Pedido",
                "Holds vanilla Blueprints that are ONLY crafted on demand from a Request Terminal.",
                "Guarda Blueprints vanilla que SOLO se craftean bajo pedido desde un Request Terminal.",
                "Install Blueprints like in an Auto-Crafter; order from the Request Terminal.",
                "Instala Blueprints igual que en un Autocrafter; pide desde el Request Terminal.");
        add(DeviceType.MVN_SF_REQUEST_CRAFTER, Category.CRAFTING, "Crafter bajo Pedido de Slimefun",
                "Same as the Request Crafter, with Slimefun and vanilla Blueprints.",
                "Igual que el Crafter bajo Pedido, con Blueprints de Slimefun y vanilla.",
                "Install Blueprints and order from the Request Terminal.",
                "Instala Blueprints y pide desde el Request Terminal.");
        add(DeviceType.MVN_REQUEST_TERMINAL, Category.CRAFTING, "Terminal de Pedidos",
                "Lists everything the network's Request Crafters can make and crafts it on demand, solving chains "
                        + "(logs, planks, crafting table) with the network's stock.",
                "Lista todo lo que pueden fabricar los Request Crafters de la red y lo craftea bajo pedido, "
                        + "resolviendo cadenas (troncos, tablones, mesa) con el stock de la red.",
                "Left-click 1 batch, shift-left 10, right-click 64, shift-right asks for an amount in chat. Toggle "
                        + "delivery to your inventory or to the network.",
                "Clic izquierdo 1 lote, shift+izquierdo 10, clic derecho 64, shift+derecho pide una cantidad por "
                        + "chat. Alterna la entrega a tu inventario o a la red.");
        add(DeviceType.MVN_CRAFTING_GRID, Category.CRAFTING, "Mesa de Crafteo de Red",
                "A crafting table that takes the ingredients from the network. Its template is saved in the block.",
                "Una mesa de crafteo que saca los ingredientes de la red. Su plantilla queda guardada en el bloque.",
                "Right-click, set the template and craft one or all.",
                "Clic derecho, monta la plantilla y craftea uno o todos.");

        // ------------------------------------------------------------------ tools
        add(DeviceType.MVN_CONFIGURATOR, Category.TOOLS, "Llave de Configuración",
                "Copies a filter (exact templates, materials and whitelist/blacklist mode) from one device to "
                        + "another.",
                "Copia un filtro (plantillas exactas, materiales y modo whitelist/blacklist) de un dispositivo a "
                        + "otro.",
                "Shift+right-click a device to copy its filter, right-click another one to paste it.",
                "Shift+clic derecho en un dispositivo para copiar su filtro, clic derecho en otro para pegarlo.");
        add(DeviceType.MVN_RAKE, Category.TOOLS, "Rastrillo de Red",
                "Dismantles a device instantly and gives it back with its state (filter, Blueprints, link). A "
                        + "DRAM Bay comes back together with its module.",
                "Desmonta un dispositivo al instante y te lo devuelve con su estado (filtro, Blueprints, enlace). "
                        + "Un DRAM Bay vuelve junto con su módulo.",
                "Right-click a device. It refuses controllers and cells or barrels that still hold something. "
                        + "Limited uses.",
                "Clic derecho en un dispositivo. Rechaza controladores y celdas o barriles que aún guarden algo. "
                        + "Usos limitados.");

        // ------------------------------------------------------------------ chickens
        add(DeviceType.MVN_CHICKEN_SORTER, Category.CHICKENS, "Clasificador Genético de Pollos",
                "Moves ONLY GeneticChickengineering pocket chickens, chosen by their genes; every other item is "
                        + "ignored. Push: from the network into the block it faces. Pull: from that block into the "
                        + "network. Up to 16 chickens per cycle.",
                "Mueve SOLO los pollos de bolsillo de GeneticChickengineering, elegidos por sus genes; cualquier "
                        + "otro ítem se ignora. Push: de la red al bloque al que mira. Pull: de ese bloque a la red. "
                        + "Hasta 16 pollos por ciclo.",
                "Right-click. Rules (all must pass): products (click with a chicken on the cursor; empty = any), "
                        + "min/max tier, min DNA strength, pure genes only, known/unknown DNA, adult/baby. Choose "
                        + "the side. It starts STOPPED: turn it on when it is set up.",
                "Clic derecho. Reglas (todas deben cumplirse): productos (clic con un pollo en el cursor; vacío = "
                        + "cualquiera), nivel mín/máx, fuerza de ADN mínima, solo genes puros, ADN conocido/"
                        + "desconocido, adulto/bebé. Elige el lado. Empieza PARADO: actívalo cuando esté listo.");
    }
}
