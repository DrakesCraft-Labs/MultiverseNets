package com.chagui68.multiversenets.command;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkStorage;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Root command executor and tab completer for /mvnets administration and diagnostics.
 *
 * Ejecutor de comando principal y autocompletado para administración y diagnóstico de /mvnets.
 */
public class MvnetsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            List.of("help", "guide", "info", "reload", "give", "devices", "doctor", "stats", "inspect", "repair", "recipes");

    private final MultiverseNets plugin;

    public MvnetsCommand(MultiverseNets plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> sendHelp(sender);
            case "guide" -> guide(sender);
            case "info" -> sendInfo(sender);
            case "reload" -> reload(sender);
            case "give" -> give(sender, args);
            case "devices" -> sendDevices(sender);
            case "doctor" -> doctor(sender);
            case "stats" -> stats(sender);
            case "inspect" -> inspect(sender);
            case "repair" -> repair(sender);
            case "recipes" -> recipes(sender);
            default -> sender.sendMessage(Text.msg("Unknown subcommand. Use /" + label + " help.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("multiversenets.admin")) {
            sender.sendMessage(Text.msg("You don't have permission.", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== MultiverseNets ===", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/mvnets guide", NamedTextColor.YELLOW)
                .append(Component.text(" - Receive the official MultiverseNets Guide Book.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets devices", NamedTextColor.YELLOW)
                .append(Component.text(" - List of devices.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets give <id> [n]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get a device.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets doctor", NamedTextColor.YELLOW)
                .append(Component.text(" - Network diagnostics.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets stats", NamedTextColor.YELLOW)
                .append(Component.text(" - Global statistics.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets inspect", NamedTextColor.YELLOW)
                .append(Component.text(" - Inspect the block you are looking at.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets repair", NamedTextColor.YELLOW)
                .append(Component.text(" - Force rescan of the network you are looking at.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload configuration and crafting recipes.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets recipes", NamedTextColor.YELLOW)
                .append(Component.text(" - Synchronize and inspect all crafting recipes.", NamedTextColor.GRAY)));
    }

    private void guide(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.msg("Only players can receive the guide book.", NamedTextColor.RED));
            return;
        }

        ItemStack book = new ItemStack(org.bukkit.Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle("MultiverseNets Guide");
            meta.setAuthor("Chagui68");

            // Page 1: Welcome & Overview
            meta.addPage("§1§lMultiverseNets§r\n"
                    + "§8Digital Logistics Guide§r\n\n"
                    + "Welcome to MultiverseNets!\n\n"
                    + "High-performance digital item & fluid networking, deep quantum storage, and recipe automation.\n\n"
                    + "Everything is crafted in a standard Crafting Table.\n\n"
                    + "§8Flip pages for all crafting recipes!§0");

            // Page 2: Core Components
            meta.addPage("§1§lCore System§r\n\n"
                    + "§9Controller:§0 Brain of the network. Connect cables to it. Accepts CPU Caches.\n\n"
                    + "§9Cables:§0 Connects all network devices together.\n\n"
                    + "§9Terminal:§0 Search & manage items. Includes the §1Network Fluids Storage§0 button for liquid tanks!");

            // Page 3: Controller Recipe
            meta.addPage("§1§lController§r\n"
                    + "§8Central Network Brain§r\n\n"
                    + "§9Recipe:§0\n"
                    + "[IronBlk] [IronBlk] [IronBlk]\n"
                    + "[IronBlk] [Nether*] [IronBlk]\n"
                    + "[IronBlk] [IronBlk] [IronBlk]\n\n"
                    + "§8• 8x Iron Block\n"
                    + "• 1x Nether Star§0");

            // Page 4: Cables & Terminal Recipe
            meta.addPage("§1§lCables & Terminal§r\n\n"
                    + "§9Cable (x16):§0\n"
                    + "[Glass] [Glass] [Glass]\n"
                    + "[Glass] [Redst] [Glass]\n"
                    + "[Glass] [Glass] [Glass]\n\n"
                    + "§9Terminal:§0\n"
                    + "[Glass] [E-Pearl] [Glass]\n"
                    + "[E-Pearl] [Beacon] [E-Pearl]\n"
                    + "[Glass] [E-Pearl] [Glass]");

            // Page 5: Quantum Cells Recipe
            meta.addPage("§1§lQuantum Cells§r\n\n"
                    + "§9Cell Tier 1 (65K):§0\n"
                    + "[Glass] [Glass] [Glass]\n"
                    + "[Glass] [Diamd] [Glass]\n"
                    + "[Glass] [Glass] [Glass]\n\n"
                    + "§9Tiers 2 to 6:§0\n"
                    + "Surround the previous tier cell with 8 Diamonds in the Crafting Table!");

            // Page 6: Greedy Cell & Infinity Barrel
            meta.addPage("§1§lSpecial Storage§r\n\n"
                    + "§9Greedy Cell:§0\n"
                    + "[Gold] [Hopper] [Gold]\n"
                    + "[Hopper] [Slime] [Hopper]\n"
                    + "[Gold] [Hopper] [Gold]\n\n"
                    + "§9Infinity Barrel:§0\n"
                    + "[Netherite] [DiaBlk] [Netherite]\n"
                    + "[DiaBlk] [Barrel] [DiaBlk]\n"
                    + "[Netherite] [DiaBlk] [Netherite]");

            // Page 7: Fluids System
            meta.addPage("§1§lFluids System§r\n\n"
                    + "§9Quantum Fluid Cell:§0\n"
                    + "[Glass] [Bucket] [Glass]\n"
                    + "[Glass] [LapisBlk] [Glass]\n"
                    + "[Glass] [Glass] [Glass]\n\n"
                    + "§9Liquid Pump:§0\n"
                    + "[Air] [BlueGlass] [Air]\n"
                    + "[Piston] [Bucket] [Piston]\n"
                    + "[Air] [Redstone] [Air]\n"
                    + "§8Pumps Water/Lava directly below it into the net!§0");

            // Page 8: Grabbers
            meta.addPage("§1§lImport Grabbers§r\n\n"
                    + "§9Standard Grabber:§0\n"
                    + "[Iron] [Observ] [Iron]\n"
                    + "[Observ] [RedBlk] [Observ]\n"
                    + "[Iron] [Observ] [Iron]\n\n"
                    + "§9Grabber HT:§0\n"
                    + "[Observ] [StickyPist] [Observ]\n"
                    + "§8Single row recipe. Moves up to 128 items!§0");

            // Page 9: Pushers
            meta.addPage("§1§lExport Pushers§r\n\n"
                    + "§9Standard Pusher:§0\n"
                    + "[Iron] [Droppr] [Iron]\n"
                    + "[Droppr] [RedBlk] [Droppr]\n"
                    + "[Iron] [Droppr] [Iron]\n\n"
                    + "§9Pusher HT:§0\n"
                    + "[Droppr] [Piston] [Droppr]\n"
                    + "§8Single row recipe. Exports up to 128 items!§0");

            // Page 10: Vacuum & Purger
            meta.addPage("§1§lCleanup Devices§r\n\n"
                    + "§9Vacuum:§0\n"
                    + "[String] [Redst] [String]\n"
                    + "[Redst] [Hopper] [Redst]\n"
                    + "[String] [Redst] [String]\n\n"
                    + "§9Purger (Incinerator):§0\n"
                    + "[Iron] [Magma] [Iron]\n"
                    + "[Magma] [Hopper] [Magma]\n"
                    + "[Iron] [Magma] [Iron]");

            // Page 11: Limiter & Router
            meta.addPage("§1§lLogistics Control§r\n\n"
                    + "§9Quota Limiter:§0\n"
                    + "[Redst] [Compar] [Redst]\n"
                    + "[Compar] [Target] [Compar]\n"
                    + "[Redst] [Compar] [Redst]\n\n"
                    + "§9Subnet Router:§0\n"
                    + "[Air] [LightningRod] [Air]\n"
                    + "[Air] [Cable] [Air]\n"
                    + "[Air] [RedstoneBlock] [Air]");

            // Page 12: Crafters
            meta.addPage("§1§lCrafters§r\n\n"
                    + "§9Auto-Crafter:§0\n"
                    + "[Redst] [CraftTbl] [Redst]\n"
                    + "[Iron] [Target] [Iron]\n"
                    + "[Redst] [CraftTbl] [Redst]\n\n"
                    + "§9Request Crafter:§0\n"
                    + "[Redst] [CraftTbl] [Redst]\n"
                    + "[Iron] [Lectern] [Iron]\n"
                    + "[Redst] [CraftTbl] [Redst]\n"
                    + "§8Only crafts when ordered!§0");

            // Page 13: Request Terminal & Blueprint
            meta.addPage("§1§lJob Ordering§r\n\n"
                    + "§9Request Terminal:§0\n"
                    + "[Glass] [Lectern] [Glass]\n"
                    + "[Redst] [CraftTbl] [Redst]\n"
                    + "[Glass] [Glass] [Glass]\n\n"
                    + "§9Blank Blueprint (x4):§0\n"
                    + "[Paper] [Paper] [Paper]\n"
                    + "[Paper] [BlueDye] [Paper]\n"
                    + "[Paper] [Paper] [Paper]");

            // Page 14: Recipe Encoders
            meta.addPage("§1§lRecipe Encoders§r\n\n"
                    + "§9Vanilla Encoder:§0\n"
                    + "[InkSac] [Paper] [InkSac]\n"
                    + "[Paper] [Smithing] [Paper]\n"
                    + "[InkSac] [Paper] [InkSac]\n\n"
                    + "§9Slimefun Encoder:§0\n"
                    + "[E-Pearl] [Paper] [E-Pearl]\n"
                    + "[Paper] [EnchantT] [Paper]\n"
                    + "[E-Pearl] [Paper] [E-Pearl]");

            // Page 15: Workbenches
            meta.addPage("§1§lWorkbenches§r\n\n"
                    + "§9Crafting Grid:§0\n"
                    + "[CraftTbl] [Redst] [CraftTbl]\n"
                    + "[Redst] [Cartography] [Redst]\n"
                    + "[CraftTbl] [Redst] [CraftTbl]\n\n"
                    + "§9Quantum Workbench:§0\n"
                    + "[Diamd] [Diamd] [Diamd]\n"
                    + "[Diamd] [CraftTbl] [Diamd]\n"
                    + "[Diamd] [Diamd] [Diamd]");

            // Page 16: Monitor & Wireless Terminal
            meta.addPage("§1§lMonitor & Wireless§r\n\n"
                    + "§9Network Monitor:§0\n"
                    + "Surround 1x Comparator with 8x Glass Panes.\n\n"
                    + "§9Wireless Terminal:§0\n"
                    + "[Air] [E-Pearl] [Air]\n"
                    + "[E-Pearl] [Nether*] [E-Pearl]\n"
                    + "[Air] [Compass] [Air]\n"
                    + "§8Shift+Right click Controller to bind!§0");

            // Page 17: Transmitter & Receiver
            meta.addPage("§1§lCross-Chunk Links§r\n\n"
                    + "§9Transmitter:§0\n"
                    + "[Iron] [RedBlk] [Iron]\n"
                    + "[RedBlk] [Conduit] [RedBlk]\n"
                    + "[Iron] [RedBlk] [Iron]\n\n"
                    + "§9Receiver:§0\n"
                    + "[Iron] [E-Pearl] [Iron]\n"
                    + "[E-Pearl] [RedLamp] [E-Pearl]\n"
                    + "[Iron] [E-Pearl] [Iron]");

            // Page 18: CPU Caches
            meta.addPage("§1§lCPU Cache Modules§r\n\n"
                    + "§9Cache L1:§0 Copper & Redstone.\n\n"
                    + "§9Cache L2:§0 Gold & Lapis around L1.\n\n"
                    + "§9Cache L3:§0 Diamond & Amethyst around L2.\n\n"
                    + "§9Cache DRAM:§0 Netherite & Eye of Ender around L3.\n\n"
                    + "§9Quantum Cache:§0 Netherite Block & Nether Star around DRAM.\n\n"
                    + "§8Right-click Controller to install!§0");

            // Page 19: Tools
            meta.addPage("§1§lNetwork Tools§r\n\n"
                    + "§9Network Probe:§0\n"
                    + "Amethyst shards surrounding Spyglass.\n\n"
                    + "§9Network Rake:§0\n"
                    + "[DeadBush] [Air] [DeadBush]\n"
                    + "[Air] [Stick] [Air]\n"
                    + "[Air] [Stick] [Air]\n\n"
                    + "§9Configurator:§0\n"
                    + "Iron Ingots & Comparator.");

            // Page 20: Controls & Tips
            meta.addPage("§1§lTips & Interactions§r\n\n"
                    + "§9Shift + Right Click:§0\n"
                    + "Access containers and machines through any network node (supports Vanilla & Slimefun)!\n\n"
                    + "§9Network Fluids:§0\n"
                    + "In Terminal, click Network Fluids Storage to inspect and withdraw liquids using buckets/bottles.\n\n"
                    + "§9Request Terminal:§0\n"
                    + "Shift+Right Click to type custom amount in chat!");

            book.setItemMeta(meta);
        }

        var leftovers = player.getInventory().addItem(book);
        for (ItemStack rem : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rem);
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
        player.sendMessage(Text.msg("You received the MultiverseNets Guide Book!", NamedTextColor.GREEN));
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(Text.msg("MultiverseNets v" + plugin.getPluginMeta().getVersion()
                + " by Chagui68. Standalone digital logistics, no Slimefun.", NamedTextColor.AQUA));
    }

    private void reload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        plugin.reloadConfig();
        com.chagui68.multiversenets.util.Settings.refresh(plugin);
        Items.registerRecipes(plugin);
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            Items.discoverRecipes(p);
        }
        sender.sendMessage(Text.msg("Configuration and " + Items.recipeCount() + " recipes reloaded.", NamedTextColor.GREEN));
    }

    private void recipes(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        Items.registerRecipes(plugin);
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            Items.discoverRecipes(p);
        }
        int active = 0;
        for (var key : Items.recipeKeys()) {
            if (org.bukkit.Bukkit.getRecipe(key) != null) {
                active++;
            }
        }
        sender.sendMessage(Text.msg("Recipes synchronized: " + active + "/" + Items.recipeCount() + " active in Bukkit.", NamedTextColor.GREEN));
    }

    private void sendDevices(CommandSender sender) {
        StringBuilder ids = new StringBuilder();
        for (DeviceType type : DeviceType.values()) {
            if (!ids.isEmpty()) {
                ids.append(", ");
            }
            ids.append(type.id());
        }
        sender.sendMessage(Text.msg(ids.toString(), NamedTextColor.AQUA));
    }

    private void give(CommandSender sender, String[] args) {
        if (!requireAdmin(sender) || !(sender instanceof Player player)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Text.msg("Usage: /mvnets give <id> [amount]", NamedTextColor.YELLOW));
            return;
        }
        DeviceType type = DeviceType.parse(args[1]);
        if (type == null && args[1].equalsIgnoreCase("wireless")) {
            type = DeviceType.MVN_WIRELESS_TERMINAL;
        }
        if (type == null) {
            sender.sendMessage(Text.msg("Unknown device. Use /mvnets devices.", NamedTextColor.RED));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            } catch (NumberFormatException ignored) {
            }
        }
        ItemStack stack = type == DeviceType.MVN_RAKE ? Items.rake() : Items.create(type);
        stack.setAmount(type == DeviceType.MVN_WIRELESS_TERMINAL ? 1 : amount);
        player.getInventory().addItem(stack);
        player.sendMessage(Text.msg("Received: " + type.display(), NamedTextColor.GREEN));
    }

    private void doctor(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        var manager = plugin.networks();
        manager.rescanAll();

        boolean sf = com.chagui68.multiversenets.compat.SlimefunBridge.isAvailable();
        sender.sendMessage(Text.msg("Slimefun: " + (sf ? "connected (grabbers & pushers "
                + "can interact with machines)" : "not available (vanilla containers only)"),
                sf ? NamedTextColor.GREEN : NamedTextColor.YELLOW));

        sender.sendMessage(Text.msg("Diagnosing " + manager.all().size() + " network(s):", NamedTextColor.AQUA));
        for (Network net : manager.all()) {
            long cells = net.nodes().values().stream().filter(DeviceType::isCell).count();
            String status = net.error == null || net.error.isBlank() ? "OK" : net.error;
            sender.sendMessage(Component.text("[MVN] ", NamedTextColor.AQUA)
                    .append(Component.text("@" + coord(net) + " | nodes: " + net.size()
                            + " | cells: " + cells + " | " + status, NamedTextColor.GRAY)));
        }
        int activeRecipes = 0;
        for (var key : Items.recipeKeys()) {
            if (org.bukkit.Bukkit.getRecipe(key) != null) {
                activeRecipes++;
            }
        }
        if (activeRecipes < Items.recipeCount()) {
            Items.registerRecipes(plugin);
            sender.sendMessage(Text.msg("Recipes: Restored (" + activeRecipes + " -> " + Items.recipeCount() + " active in Bukkit)", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Text.msg("Recipes: " + activeRecipes + "/" + Items.recipeCount() + " active in Bukkit", NamedTextColor.GREEN));
        }
    }

    private void stats(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        var manager = plugin.networks();
        long items = 0;
        for (Network net : manager.all()) {
            items += net.storage().view().stream().mapToLong(NetworkStorage.View::amount).sum();
        }
        sender.sendMessage(Text.msg("Networks: " + manager.all().size()
                + " | Nodes: " + manager.totalNodes()
                + " | Stored items: " + Items.formatAmount(items), NamedTextColor.AQUA));
    }

    private String coord(Network net) {
        return com.chagui68.multiversenets.util.PosUtil.unpackX(net.controllerPos()) + ","
                + com.chagui68.multiversenets.util.PosUtil.unpackY(net.controllerPos()) + ","
                + com.chagui68.multiversenets.util.PosUtil.unpackZ(net.controllerPos());
    }

    /**
     * /mvnets inspect: como el 'inspect' de NetworksV6, cuenta lo que tiene delante (tipo,
     * red, contenido si es celda/greedy).
     */
    private void inspect(CommandSender sender) {
        if (!requireAdmin(sender) || !(sender instanceof Player player)) {
            return;
        }
        org.bukkit.block.Block target = player.getTargetBlockExact(8);
        if (target == null) {
            sender.sendMessage(Text.msg("Look at a block within 8 blocks.", NamedTextColor.RED));
            return;
        }
        var blob = com.chagui68.multiversenets.persist.NodeStore.get(target);
        if (blob == null) {
            sender.sendMessage(Text.msg("That block is not a network device.", NamedTextColor.GRAY));
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        sender.sendMessage(Text.msg("Device: " + (type == null ? blob.typeName : type.display()),
                NamedTextColor.AQUA));
        Network net = plugin.networks().networkAt(target);
        sender.sendMessage(Text.msg(net == null
                        ? "Network: none (check cables to a controller)"
                        : "Network: " + net.size() + " nodes, controller at " + coord(net),
                net == null ? NamedTextColor.RED : NamedTextColor.GRAY));
        if (blob.cellSample != null && blob.cellAmount > 0) {
            sender.sendMessage(Text.msg("Stored: " + Items.formatAmount(blob.cellAmount)
                    + " x " + blob.cellSample.getType().name(), NamedTextColor.GRAY));
        }
        if (!blob.filterMaterials.isEmpty()) {
            sender.sendMessage(Text.msg("Filter (" + (blob.filterBlacklist ? "blacklist" : "whitelist")
                    + "): " + String.join(", ", blob.filterMaterials), NamedTextColor.GRAY));
        }
    }

    /**
     * /mvnets repair: fuerza el reescaneo de la red del bloque mirado (el "repair" de
     * NetworksV6 que reconstruye la topologia de un controlador).
     */
    private void repair(CommandSender sender) {
        if (!requireAdmin(sender) || !(sender instanceof Player player)) {
            return;
        }
        org.bukkit.block.Block target = player.getTargetBlockExact(8);
        if (target == null) {
            sender.sendMessage(Text.msg("Look at a network block within 8 blocks.", NamedTextColor.RED));
            return;
        }
        Network net = plugin.networks().networkAt(target);
        if (net == null) {
            sender.sendMessage(Text.msg("That block does not belong to any network.", NamedTextColor.RED));
            return;
        }
        net.scan();
        sender.sendMessage(Text.msg("Network rescanned: " + net.size() + " node(s).", NamedTextColor.GREEN));
        if (net.error != null && !net.error.isBlank()) {
            sender.sendMessage(Text.msg("Warning: " + net.error, NamedTextColor.YELLOW));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String @NotNull [] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> out = new ArrayList<>();
            for (DeviceType type : DeviceType.values()) {
                if (type.id().startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(type.id());
                }
            }
            return out;
        }
        return List.of();
    }
}
