package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatPrompts implements Listener {

    private static final Map<UUID, Consumer<String>> PENDING = new ConcurrentHashMap<>();

    public ChatPrompts(MultiverseNets plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void ask(Player player, String prompt, Consumer<String> callback) {
        PENDING.put(player.getUniqueId(), callback);
        player.sendMessage(com.chagui68.multiversenets.util.Text.msg(prompt, net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        player.sendMessage(com.chagui68.multiversenets.util.Text.msg("Type 'cancel' to abort.", net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }

    public static boolean isPending(Player player) {
        return PENDING.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = PENDING.remove(player.getUniqueId());
        if (callback == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Player p = player;
        var plugin = com.chagui68.multiversenets.MultiverseNets.instance();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if ("cancel".equalsIgnoreCase(text)) {
                p.sendMessage(com.chagui68.multiversenets.util.Text.msg("Cancelled.", net.kyori.adventure.text.format.NamedTextColor.GRAY));
                return;
            }
            callback.accept(text);
        });
    }
}
