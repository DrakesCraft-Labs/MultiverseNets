package com.chagui68.multiversenets.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Factory for creating the in-game MultiverseNets Guide Book in English and Spanish.
 * Uses 3x3 grid diagrams with single-letter ingredient keys matching wiki specifications.
 *
 * Fábrica para crear el libro guía de MultiverseNets en inglés y español.
 * Emplea diagramas de cuadrícula 3x3 con letras de ingredientes coincidentes con la wiki.
 */
public final class GuideBook {

    private GuideBook() {}

    /**
     * Creates the complete English MultiverseNets guide book.
     */
    public static ItemStack createEnglishBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return book;
        }

        meta.setTitle("MultiverseNets Guide");
        meta.setAuthor("Chagui68");

        // Page 1: Welcome & Overview
        meta.addPage("§1§lMultiverseNets§r\n"
                + "§8Digital Logistics Guide§r\n\n"
                + "Welcome to MultiverseNets!\n\n"
                + "High-performance digital logistics, deep quantum item & fluid storage, and recipe automation.\n\n"
                + "All devices are crafted on a standard 3×3 Crafting Table.\n\n"
                + "§8Turn pages for all recipes!§0");

        // Page 2: Core System Overview
        meta.addPage("§1§lCore System§r\n\n"
                + "§9Controller:§0 Central brain. Connect cables to it. Accepts CPU Caches.\n\n"
                + "§9Cables:§0 Carries signal & data between network devices.\n\n"
                + "§9Terminal:§0 Browse & search items. Includes the §1Network Fluids§0 button for liquid tanks!");

        // Page 3: Controller
        meta.addPage(recipePageEn("Controller", "Central Network Brain",
                "III",
                "INI",
                "III",
                "I = Iron Block",
                "N = Nether Star"));

        // Page 4: Cable (x16)
        meta.addPage(recipePageEn("Cable (×16)", "Network Wiring",
                "GGG",
                "GRG",
                "GGG",
                "G = Glass",
                "R = Redstone"));

        // Page 5: Terminal
        meta.addPage(recipePageEn("Terminal", "Main Item Access Console",
                "GEG",
                "EBE",
                "GEG",
                "G = Glass",
                "E = Ender Pearl",
                "B = Beacon"));

        // Page 6: Wireless Terminal
        meta.addPage(recipePageEn("Wireless Terminal", "Remote Network Access",
                ".P.",
                "PNP",
                ".C.",
                "P = Ender Pearl",
                "N = Nether Star",
                "C = Compass",
                "Shift+RClick Controller to bind!"));

        // Page 7: Quantum Cell T1
        meta.addPage(recipePageEn("Quantum Cell T1", "Capacity: 65,536 items",
                "GGG",
                "GDG",
                "GGG",
                "G = Glass",
                "D = Diamond"));

        // Page 8: Quantum Cells T2 - T6
        meta.addPage(recipePageEn("Cells T2 to T6", "Higher Capacity Tiers",
                "DDD",
                "DPD",
                "DDD",
                "D = Diamond",
                "P = Previous Tier Cell",
                "T2: 262K · T3: 1M · T4: 16M",
                "T5: 268M · T6: 2 Billion"));

        // Page 9: Greedy Cell
        meta.addPage(recipePageEn("Greedy Cell", "Dedicated Smart Buffer",
                "GHG",
                "HSH",
                "GHG",
                "G = Gold Ingot",
                "H = Hopper",
                "S = Slime Block",
                "Feeds item to adjacent chests!"));

        // Page 10: Infinity Barrel
        meta.addPage(recipePageEn("Infinity Barrel", "Deep Single-Item Storage",
                "NDN",
                "DBD",
                "NDN",
                "N = Netherite Ingot",
                "D = Diamond Block",
                "B = Barrel",
                "Capacity: 2 Billion items"));

        // Page 11: Quantum Fluid Cell
        meta.addPage(recipePageEn("Quantum Fluid Cell", "Capacity: 64,000 mB",
                "GBG",
                "GLG",
                "GGG",
                "G = Glass",
                "B = Bucket",
                "L = Lapis Lazuli Block",
                "Holds Water, Lava, Milk, Honey"));

        // Page 12: Liquid Pump
        meta.addPage(recipePageEn("Liquid Pump", "Automated Fluid Harvester",
                ".G.",
                "PBP",
                ".R.",
                "G = Blue Stained Glass",
                "P = Piston",
                "B = Bucket",
                "R = Redstone",
                "Pumps Water/Lava directly below!"));

        // Page 13: Simple Grabber
        meta.addPage(recipePageEn("Simple Grabber", "Container Item Importer",
                "IOI",
                "ORO",
                "IOI",
                "I = Iron Ingot",
                "O = Observer",
                "R = Redstone Block",
                "Pulls items into network"));

        // Page 14: Advanced Grabber HT
        meta.addPage(recipePageEn("Grabber HT", "High-Throughput Importer",
                "...",
                "OPO",
                "...",
                "O = Observer",
                "P = Sticky Piston",
                "Extracts up to 128 items/tick"));

        // Page 15: Simple Pusher
        meta.addPage(recipePageEn("Simple Pusher", "Container Item Exporter",
                "IDI",
                "DRD",
                "IDI",
                "I = Iron Ingot",
                "D = Dropper",
                "R = Redstone Block",
                "Exports items into containers"));

        // Page 16: Advanced Pusher HT
        meta.addPage(recipePageEn("Pusher HT", "High-Throughput Exporter",
                "...",
                "DPD",
                "...",
                "D = Dropper",
                "P = Piston",
                "Exports up to 128 items/tick"));

        // Page 17: Vacuum
        meta.addPage(recipePageEn("Network Vacuum", "Ground Item Collector",
                "SRS",
                "RHR",
                "SRS",
                "S = String",
                "R = Redstone",
                "H = Hopper",
                "Picks up dropped items"));

        // Page 18: Purger
        meta.addPage(recipePageEn("Network Purger", "Filtered Item Destroyer",
                "ILI",
                "LHL",
                "ILI",
                "I = Iron Ingot",
                "L = Magma Block",
                "H = Hopper",
                "Voids filtered junk items"));

        // Page 19: Quota Limiter
        meta.addPage(recipePageEn("Quota Limiter", "Stock Ceiling Regulator",
                "RCR",
                "CTC",
                "RCR",
                "R = Redstone",
                "C = Comparator",
                "T = Target",
                "Stops importing once cap met"));

        // Page 20: Subnet Router
        meta.addPage(recipePageEn("Network Router", "Broadcasting Antenna",
                ".L.",
                ".C.",
                ".R.",
                "L = Lightning Rod",
                "C = Network Cable",
                "R = Redstone Block",
                "Global wireless range across dims"));

        // Page 21: Auto-Crafter
        meta.addPage(recipePageEn("Auto-Crafter", "Autonomous Batch Crafter",
                "RCR",
                "ITI",
                "RCR",
                "R = Redstone",
                "C = Crafting Table",
                "I = Iron Ingot",
                "T = Target",
                "Executes blueprints periodically"));

        // Page 22: Request Crafter
        meta.addPage(recipePageEn("Request Crafter", "On-Demand Crafting Node",
                "RCR",
                "ILI",
                "RCR",
                "R = Redstone",
                "C = Crafting Table",
                "I = Iron Ingot",
                "L = Lectern",
                "Strictly for Request Terminal"));

        // Page 23: Request Terminal
        meta.addPage(recipePageEn("Request Terminal", "Interactive Order Console",
                "GLG",
                "RCR",
                "GGG",
                "G = Glass",
                "L = Lectern",
                "C = Crafting Table",
                "R = Redstone",
                "Recursive chained crafting!"));

        // Page 24: Blueprint (x4)
        meta.addPage(recipePageEn("Blueprint (×4)", "Reusable Pattern Matrix",
                "PPP",
                "PBP",
                "PPP",
                "P = Paper",
                "B = Blue Dye",
                "Encodes 3x3 crafting recipes"));

        // Page 25: Recipe Encoder
        meta.addPage(recipePageEn("Recipe Encoder", "Blueprint Writing Station",
                "KPK",
                "PSP",
                "KPK",
                "K = Ink Sac",
                "P = Paper",
                "S = Smithing Table",
                "No item consumption!"));

        // Page 26: Slimefun Recipe Encoder
        meta.addPage(recipePageEn("SF Recipe Encoder", "Slimefun Recipe Station",
                "EPE",
                "PBP",
                "EPE",
                "E = Ender Pearl",
                "P = Paper",
                "B = Enchanting Table",
                "Encodes Slimefun recipes"));

        // Page 27: Slimefun Auto-Crafter
        meta.addPage(recipePageEn("SF Auto-Crafter", "Autonomous Slimefun Crafter",
                "RCR",
                "ITI",
                "RCR",
                "R = Ender Pearl",
                "C = Crying Obsidian",
                "I = Iron Ingot",
                "T = Target",
                "Crafts Slimefun blueprints"));

        // Page 28: Slimefun Request Crafter
        meta.addPage(recipePageEn("SF Request Crafter", "Slimefun On-Demand Node",
                "RCR",
                "ILI",
                "RCR",
                "R = Ender Pearl",
                "C = Purpur Pillar",
                "I = Iron Ingot",
                "L = Lectern",
                "For Request Terminal"));

        // Page 29: Crafting Grid
        meta.addPage(recipePageEn("Crafting Grid", "Network-Linked Workbench",
                "CRC",
                "RGR",
                "CRC",
                "C = Crafting Table",
                "R = Redstone",
                "G = Cartography Table",
                "Pulls directly from storage"));

        // Page 28: Quantum Workbench
        meta.addPage(recipePageEn("Quantum Workbench", "Disassembly & Duplication",
                "DDD",
                "DCD",
                "DDD",
                "D = Diamond",
                "C = Crafting Table",
                "Disassembles crafted items"));

        // Page 29: Network Monitor
        meta.addPage(recipePageEn("Network Monitor", "Inventory Signal Emitter",
                "GGG",
                "GCG",
                "GGG",
                "G = Glass Pane",
                "C = Comparator",
                "Live network diagnostics panel"));

        // Page 30: Transmitter & Receiver
        meta.addPage("§1§lWireless Links§r\n\n"
                + "§9Transmitter:§0\n"
                + "§1§l[ I ] [ R ] [ I ]\n"
                + "[ R ] [ C ] [ R ]\n"
                + "[ I ] [ R ] [ I ]§0\n"
                + "§8• I=Iron · R=RedstoneBlk · C=Conduit§0\n\n"
                + "§9Receiver:§0\n"
                + "§1§l[ I ] [ P ] [ I ]\n"
                + "[ P ] [ L ] [ P ]\n"
                + "[ I ] [ P ] [ I ]§0\n"
                + "§8• I=Iron · P=EnderPearl · L=RedstoneLamp§0");

        // Page 31: CPU Cache Modules
        meta.addPage("§1§lCPU Cache Modules§r\n"
                + "§8Internal Controller Memory§r\n\n"
                + "§9Cache L1 (2K):§0\n"
                + "Copper & Redstone dust.\n\n"
                + "§9Cache L2 (8K):§0\n"
                + "Gold & Lapis around L1.\n\n"
                + "§9Cache L3 (32K):§0\n"
                + "Diamond & Amethyst around L2.\n\n"
                + "§9DRAM (131K):§0\n"
                + "Netherite & Eye of Ender around L3.\n\n"
                + "§9Quantum Cache (524K):§0\n"
                + "Netherite Blk & Nether Star on DRAM.");

        // Page 32: Network Tools
        meta.addPage("§1§lNetwork Tools§r\n\n"
                + "§9Probe:§0\n"
                + "Amethyst shards surrounding Spyglass.\n\n"
                + "§9Config Wrench:§0\n"
                + "§1§l[ I ] [ · ] [ I ]\n"
                + "[ · ] [ C ] [ · ]\n"
                + "[ · ] [ I ] [ · ]§0\n"
                + "§8• I = Iron Ingot · C = Comparator§0\n\n"
                + "§9Network Rake:§0\n"
                + "§1§l[ D ] [ · ] [ D ]\n"
                + "[ · ] [ S ] [ · ]\n"
                + "[ · ] [ S ] [ · ]§0\n"
                + "§8• D = Dead Bush · S = Stick§0");

        // Page 33: Tips & Controls
        meta.addPage("§1§lTips & Controls§r\n\n"
                + "§9Shift + Right Click:§0\n"
                + "Access containers and machines through any network node (supports Vanilla & Slimefun)!\n\n"
                + "§9Network Fluids:§0\n"
                + "Click Network Fluids Storage in Terminal to inspect/withdraw liquids using buckets & bottles.\n\n"
                + "§9Request Terminal:§0\n"
                + "Shift+Right Click to type custom amounts in chat!");

        book.setItemMeta(meta);
        return book;
    }

    /**
     * Creates the complete Spanish MultiverseNets guide book.
     */
    public static ItemStack createSpanishBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return book;
        }

        meta.setTitle("Guía MultiverseNets");
        meta.setAuthor("Chagui68");

        // Page 1: Bienvenida e Introducción
        meta.addPage("§1§lMultiverseNets§r\n"
                + "§8Guía de Logística Digital§r\n\n"
                + "¡Bienvenido a MultiverseNets!\n\n"
                + "Logística digital de alto rendimiento, almacenamiento cuántico de ítems y fluidos, y automatización de recetas.\n\n"
                + "Todo se fabrica en una mesa de crafteo 3×3 estándar.\n\n"
                + "§8¡Pasa de página para ver las recetas!§0");

        // Page 2: Sistema Central
        meta.addPage("§1§lSistema Central§r\n\n"
                + "§9Controlador:§0 Cerebro de la red. Conecta cables a él. Admite memorias Caché CPU.\n\n"
                + "§9Cables:§0 Transmiten datos y señal entre los dispositivos.\n\n"
                + "§9Terminal:§0 Explora y gestiona ítems. ¡Incluye el botón de §1Almacén de Fluidos§0 para tanques de líquidos!");

        // Page 3: Controlador
        meta.addPage(recipePageEs("Controlador", "Cerebro Central de la Red",
                "III",
                "INI",
                "III",
                "I = Bloque de hierro",
                "N = Estrella del Nether"));

        // Page 4: Cable (x16)
        meta.addPage(recipePageEs("Cable (×16)", "Cableado de Red",
                "GGG",
                "GRG",
                "GGG",
                "G = Cristal",
                "R = Redstone"));

        // Page 5: Terminal
        meta.addPage(recipePageEs("Terminal", "Consola Principal de Ítems",
                "GEG",
                "EBE",
                "GEG",
                "G = Cristal",
                "E = Perla de ender",
                "B = Faro (Beacon)"));

        // Page 6: Terminal Inalámbrico
        meta.addPage(recipePageEs("Terminal Remoto", "Acceso Inalámbrico a la Red",
                ".P.",
                "PNP",
                ".C.",
                "P = Perla de ender",
                "N = Estrella del Nether",
                "C = Brújula",
                "Shift+Clic Derecho al Controlador!"));

        // Page 7: Celda Cuántica T1
        meta.addPage(recipePageEs("Celda Cuántica T1", "Capacidad: 65,536 ítems",
                "GGG",
                "GDG",
                "GGG",
                "G = Cristal",
                "D = Diamante"));

        // Page 8: Celdas Cuánticas T2 - T6
        meta.addPage(recipePageEs("Celdas T2 a T6", "Niveles de Mayor Capacidad",
                "DDD",
                "DPD",
                "DDD",
                "D = Diamante",
                "P = Celda del nivel anterior",
                "T2: 262K · T3: 1M · T4: 16M",
                "T5: 268M · T6: 2 Mil Millones"));

        // Page 9: Greedy Cell
        meta.addPage(recipePageEs("Greedy Cell", "Búfer Inteligente Dedicado",
                "GHG",
                "HSH",
                "GHG",
                "G = Lingote de oro",
                "H = Embudo",
                "S = Bloque de slime",
                "¡Alimenta cofres adyacentes!"));

        // Page 10: Barril Infinito
        meta.addPage(recipePageEs("Barril Infinito", "Almacén Masivo de 1 Ítem",
                "NDN",
                "DBD",
                "NDN",
                "N = Lingote de netherita",
                "D = Bloque de diamante",
                "B = Barril",
                "Capacidad: 2 Mil Millones"));

        // Page 11: Celda de Fluidos
        meta.addPage(recipePageEs("Celda de Fluidos", "Capacidad: 64,000 mB",
                "GBG",
                "GLG",
                "GGG",
                "G = Cristal",
                "B = Cubo",
                "L = Bloque de lapislázuli",
                "Almacena Agua, Lava, Leche, Miel"));

        // Page 12: Bomba de Líquidos
        meta.addPage(recipePageEs("Bomba de Líquidos", "Extractor Automático de Fluidos",
                ".G.",
                "PBP",
                ".R.",
                "G = Cristal azul oscuro",
                "P = Pistón",
                "B = Cubo",
                "R = Redstone",
                "¡Drena Agua/Lava directamente debajo!"));

        // Page 13: Importador Simple
        meta.addPage(recipePageEs("Importador", "Extractor hacia la Red",
                "IOI",
                "ORO",
                "IOI",
                "I = Lingote de hierro",
                "O = Observador",
                "R = Bloque de redstone",
                "Extrae ítems hacia el almacén"));

        // Page 14: Importador HT
        meta.addPage(recipePageEs("Importador HT", "Extractor de Alta Velocidad",
                "...",
                "OPO",
                "...",
                "O = Observador",
                "P = Pistón pegajoso",
                "Extrae hasta 128 ítems/tick"));

        // Page 15: Exportador Simple
        meta.addPage(recipePageEs("Exportador", "Inserta Ítems a Contenedores",
                "IDI",
                "DRD",
                "IDI",
                "I = Lingote de hierro",
                "D = Soltador",
                "R = Bloque de redstone",
                "Inserta ítems desde la red"));

        // Page 16: Exportador HT
        meta.addPage(recipePageEs("Exportador HT", "Insersor de Alta Velocidad",
                "...",
                "DPD",
                "...",
                "D = Soltador",
                "P = Pistón",
                "Inserta hasta 128 ítems/tick"));

        // Page 17: Aspiradora
        meta.addPage(recipePageEs("Aspiradora", "Recolector de Ítems del Suelo",
                "SRS",
                "RHR",
                "SRS",
                "S = Hilo",
                "R = Redstone",
                "H = Embudo",
                "Recoge ítems caídos en el suelo"));

        // Page 18: Purgador
        meta.addPage(recipePageEs("Purgador", "Incinerador de Ítems",
                "ILI",
                "LHL",
                "ILI",
                "I = Lingote de hierro",
                "L = Bloque de magma",
                "H = Embudo",
                "Destruye basura filtrada"));

        // Page 19: Delimitador de Cuota
        meta.addPage(recipePageEs("Delimitador Cuota", "Control de Límite de Stock",
                "RCR",
                "CTC",
                "RCR",
                "R = Redstone",
                "C = Comparador",
                "T = Diana (Target)",
                "Detiene entrada al llegar al tope"));

        // Page 20: Antena Enrutadora
        meta.addPage(recipePageEs("Antena Router", "Amplificador de Red",
                ".L.",
                ".C.",
                ".R.",
                "L = Pararrayos",
                "C = Cable de red",
                "R = Bloque de redstone",
                "Señal inalámbrica entre dimensiones"));

        // Page 21: Autocrafteador
        meta.addPage(recipePageEs("Autocrafteador", "Fabricación Periódica",
                "RCR",
                "ITI",
                "RCR",
                "R = Redstone",
                "C = Mesa de crafteo",
                "I = Lingote de hierro",
                "T = Diana (Target)",
                "Elabora blueprints periódicamente"));

        // Page 22: Request Crafter
        meta.addPage(recipePageEs("Request Crafter", "Cámara de Crafteo Manual",
                "RCR",
                "ILI",
                "RCR",
                "R = Redstone",
                "C = Mesa de crafteo",
                "I = Lingote de hierro",
                "L = Atril (Lectern)",
                "Exclusivo para Request Terminal"));

        // Page 23: Request Terminal
        meta.addPage(recipePageEs("Request Terminal", "Consola de Órdenes a Pedido",
                "GLG",
                "RCR",
                "GGG",
                "G = Cristal",
                "L = Atril (Lectern)",
                "C = Mesa de crafteo",
                "R = Redstone",
                "¡Crafteo recursivo de dependencias!"));

        // Page 24: Blueprint (x4)
        meta.addPage(recipePageEs("Blueprint (×4)", "Matriz de Patrón Reutilizable",
                "PPP",
                "PBP",
                "PPP",
                "P = Papel",
                "B = Tinte azul",
                "Guarda recetas 3×3 completas"));

        // Page 25: Recipe Encoder
        meta.addPage(recipePageEs("Recipe Encoder", "Estación de Grabado de Planos",
                "KPK",
                "PSP",
                "KPK",
                "K = Saco de tinta",
                "P = Papel",
                "S = Mesa de herrería",
                "¡No consume ningún material!"));

        // Page 26: Slimefun Recipe Encoder
        meta.addPage(recipePageEs("Encoder Slimefun", "Grabador de Recetas Slimefun",
                "EPE",
                "PBP",
                "EPE",
                "E = Perla de ender",
                "P = Papel",
                "B = Mesa de encantamientos",
                "Graba recetas de Slimefun"));

        // Page 27: Slimefun Auto-Crafter
        meta.addPage(recipePageEs("Autocraft Slimefun", "Crafteador Autónomo Slimefun",
                "RCR",
                "ITI",
                "RCR",
                "R = Perla de ender",
                "C = Obsidiana llorosa",
                "I = Lingote de hierro",
                "T = Diana (Target)",
                "Elabora planos Slimefun"));

        // Page 28: Slimefun Request Crafter
        meta.addPage(recipePageEs("Req Craft Slimefun", "Cámara a Pedido Slimefun",
                "RCR",
                "ILI",
                "RCR",
                "R = Perla de ender",
                "C = Pilar de púrpura",
                "I = Lingote de hierro",
                "L = Atril (Lectern)",
                "Exclusivo Request Terminal"));

        // Page 29: Parrilla de Crafteo
        meta.addPage(recipePageEs("Parrilla Crafteo", "Mesa Integrada a la Red",
                "CRC",
                "RGR",
                "CRC",
                "C = Mesa de crafteo",
                "R = Redstone",
                "G = Mesa de cartografía",
                "Usa materiales de la red al instante"));

        // Page 28: Quantum Workbench
        meta.addPage(recipePageEs("Quantum Workbench", "Desensamblaje y Duplicación",
                "DDD",
                "DCD",
                "DDD",
                "D = Diamante",
                "C = Mesa de crafteo",
                "Desensambla ítems crafteados"));

        // Page 29: Monitor de Red
        meta.addPage(recipePageEs("Monitor de Red", "Emisor de Señal por Stock",
                "GGG",
                "GCG",
                "GGG",
                "G = Panel de cristal",
                "C = Comparador",
                "Panel de diagnóstico en vivo"));

        // Page 30: Enlaces Inalámbricos
        meta.addPage("§1§lEnlaces de Red§r\n\n"
                + "§9Transmisor:§0\n"
                + "§1§l[ I ] [ R ] [ I ]\n"
                + "[ R ] [ C ] [ R ]\n"
                + "[ I ] [ R ] [ I ]§0\n"
                + "§8• I=Hierro · R=BloqueRedstone · C=Conducto§0\n\n"
                + "§9Receptor:§0\n"
                + "§1§l[ I ] [ P ] [ I ]\n"
                + "[ P ] [ L ] [ P ]\n"
                + "[ I ] [ P ] [ I ]§0\n"
                + "§8• I=Hierro · P=PerlaEnder · L=LámparaRedstone§0");

        // Page 31: Módulos de Caché CPU
        meta.addPage("§1§lMódulos Caché CPU§r\n"
                + "§8Memoria Interna del Controlador§r\n\n"
                + "§9Caché L1 (2K):§0\n"
                + "Cobre y Redstone.\n\n"
                + "§9Caché L2 (8K):§0\n"
                + "Oro y Lapislázuli sobre L1.\n\n"
                + "§9Caché L3 (32K):§0\n"
                + "Diamante y Amatista sobre L2.\n\n"
                + "§9DRAM (131K):§0\n"
                + "Netherita y Ojo de ender sobre L3.\n\n"
                + "§9Caché Cuántico (524K):§0\n"
                + "Bloque netherita y Estrella nether sobre DRAM.");

        // Page 32: Herramientas de Red
        meta.addPage("§1§lHerramientas§r\n\n"
                + "§9Sonda (Probe):§0\n"
                + "Amatista rodeando Catalejo.\n\n"
                + "§9Llave Configuración:§0\n"
                + "§1§l[ I ] [ · ] [ I ]\n"
                + "[ · ] [ C ] [ · ]\n"
                + "[ · ] [ I ] [ · ]§0\n"
                + "§8• I = Lingote hierro · C = Comparador§0\n\n"
                + "§9Rastrillo de Red:§0\n"
                + "§1§l[ D ] [ · ] [ D ]\n"
                + "[ · ] [ S ] [ · ]\n"
                + "[ · ] [ S ] [ · ]§0\n"
                + "§8• D = Arbusto seco · S = Palo§0");

        // Page 33: Consejos y Controles
        meta.addPage("§1§lConsejos y Controles§r\n\n"
                + "§9Shift + Clic Derecho:§0\n"
                + "¡Accede a cofres y máquinas a través de nodos de red (Vanilla y Slimefun)!\n\n"
                + "§9Fluidos de Red:§0\n"
                + "Usa el botón de Almacén de Fluidos en la Terminal para retirar con cubos/botellas.\n\n"
                + "§9Request Terminal:§0\n"
                + "¡Shift+Clic Derecho para escribir la cantidad en el chat!");

        book.setItemMeta(meta);
        return book;
    }

    private static String recipePageEn(String title, String subtitle, String r1, String r2, String r3, String... legend) {
        return buildRecipePage(title, subtitle, "Recipe (3×3):", r1, r2, r3, legend);
    }

    private static String recipePageEs(String title, String subtitle, String r1, String r2, String r3, String... legend) {
        return buildRecipePage(title, subtitle, "Receta (3×3):", r1, r2, r3, legend);
    }

    private static String buildRecipePage(String title, String subtitle, String recipeLabel, String r1, String r2, String r3, String... legend) {
        StringBuilder sb = new StringBuilder();
        sb.append("§1§l").append(title).append("§r\n");
        if (subtitle != null && !subtitle.isBlank()) {
            sb.append("§8").append(subtitle).append("§r\n\n");
        } else {
            sb.append("\n");
        }
        sb.append("§9").append(recipeLabel).append("§0\n");
        sb.append("§1§l").append(formatRow(r1)).append("\n");
        sb.append(formatRow(r2)).append("\n");
        sb.append(formatRow(r3)).append("§0\n\n");
        for (String l : legend) {
            sb.append("§8• ").append(l).append("\n");
        }
        return sb.toString().trim();
    }

    private static String formatRow(String input) {
        String clean = input.replace(" ", "");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (i > 0) {
                out.append(" ");
            }
            if (c == '.' || c == '·' || c == '-') {
                out.append("[ · ]");
            } else {
                out.append("[ ").append(c).append(" ]");
            }
        }
        return out.toString();
    }
}
