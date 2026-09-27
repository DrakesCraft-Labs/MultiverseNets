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
                    + "§8Digital Logistics & Massive Storage§r\n\n"
                    + "Welcome to MultiverseNets!\n\n"
                    + "This plugin delivers high-performance digital item and fluid networking, deep quantum storage, and recipe automation — running natively on Paper with zero dependencies.");

            // Page 2: Core Components
            meta.addPage("§1§lCore Components§r\n\n"
                    + "§9Controller:§0 Central brain of the network. Needs power/cables and accepts CPU Cache modules.\n\n"
                    + "§9Cables:§0 Connects all network devices.\n\n"
                    + "§9Terminal:§0 Central console to search, store, and withdraw items and liquids.");

            // Page 3: Item Storage
            meta.addPage("§1§lItem Storage§r\n\n"
                    + "§9Quantum Cells (T1-T6):§0 Stores a single item type from 65K up to millions.\n\n"
                    + "§9Infinity Barrel:§0 Infinite storage for a single item type.\n\n"
                    + "§9Greedy Cell:§0 Continuously pulls target items from the net into its fast local buffer.");

            // Page 4: Logistics & Movement
            meta.addPage("§1§lLogistics & Flow§r\n\n"
                    + "§9Grabbers:§0 Pulls items from adjacent inventories into the net.\n\n"
                    + "§9Pushers:§0 Exports network items into target chests.\n\n"
                    + "§9Vacuum:§0 Absorbs dropped items on the ground.\n\n"
                    + "§9Quota Limiter:§0 Limits maximum network stock for items.");

            // Page 5: Fluids System
            meta.addPage("§1§lFluids System§r\n\n"
                    + "§9Quantum Fluid Cell:§0 Digital tank storing up to 64,000+ mB of Water, Lava, Milk, Honey, or Powder Snow.\n\n"
                    + "§9Liquid Pump:§0 Placed above Water or Lava to pump liquid sources directly from the block underneath into the net.\n\n"
                    + "§9Terminal (3rd Button):§0 Withdraw network fluids using Buckets or Bottles!");

            // Page 6: Crafting & Automation
            meta.addPage("§1§lCrafting & Jobs§r\n\n"
                    + "§9Auto-Crafter:§0 Automates crafting recipes installed on Blueprints.\n\n"
                    + "§9Recipe Encoders:§0 Writes vanilla or Slimefun recipes onto Blueprints.\n\n"
                    + "§9Request Terminal:§0 Order custom crafting quantities on-demand via chat or buttons directly from network crafters!");

            // Page 7: Tools & Tips
            meta.addPage("§1§lTools & Quick Tips§r\n\n"
                    + "§9Network Probe:§0 Right-click nodes to view throughput, connections, and diagnostic logs.\n\n"
                    + "§9Rake:§0 Instantly dismantles network nodes without damage.\n\n"
                    + "§9Shift + Right Click:§0 Directly access containers through nodes.");

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
