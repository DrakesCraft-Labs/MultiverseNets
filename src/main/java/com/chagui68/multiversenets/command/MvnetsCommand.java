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

public class MvnetsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("help", "info", "reload", "give", "devices", "doctor", "stats");

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
            case "info" -> sendInfo(sender);
            case "reload" -> reload(sender);
            case "give" -> give(sender, args);
            case "devices" -> sendDevices(sender);
            case "doctor" -> doctor(sender);
            case "stats" -> stats(sender);
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
        sender.sendMessage(Component.text("/mvnets devices", NamedTextColor.YELLOW)
                .append(Component.text(" - List of devices.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets give <id> [n]", NamedTextColor.YELLOW)
                .append(Component.text(" - Get a device.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets doctor", NamedTextColor.YELLOW)
                .append(Component.text(" - Network diagnostics.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets stats", NamedTextColor.YELLOW)
                .append(Component.text(" - Global statistics.", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mvnets reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload the configuration.", NamedTextColor.GRAY)));
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
        sender.sendMessage(Text.msg("Configuration reloaded.", NamedTextColor.GREEN));
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
            type = DeviceType.WIRELESS_TERMINAL;
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
        ItemStack stack = Items.create(type);
        stack.setAmount(type == DeviceType.WIRELESS_TERMINAL ? 1 : amount);
        player.getInventory().addItem(stack);
        player.sendMessage(Text.msg("Received: " + type.display(), NamedTextColor.GREEN));
    }

    private void doctor(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        var manager = plugin.networks();
        manager.rescanAll();
        sender.sendMessage(Text.msg("Diagnosing " + manager.all().size() + " network(s):", NamedTextColor.AQUA));
        for (Network net : manager.all()) {
            long cells = net.nodes().values().stream().filter(DeviceType::isCell).count();
            String status = net.error == null || net.error.isBlank() ? "OK" : net.error;
            sender.sendMessage(Component.text("[MVN] ", NamedTextColor.AQUA)
                    .append(Component.text("@" + coord(net) + " | nodes: " + net.size()
                            + " | cells: " + cells + " | " + status, NamedTextColor.GRAY)));
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
