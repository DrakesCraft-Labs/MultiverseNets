package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Intercepts player chat input to handle asynchronous text prompts (e.g. searching, configuration).
 *
 * Intercepta la entrada del chat del jugador para procesar solicitudes de texto asíncronas (ej. búsqueda, configuración).
 */
public class ChatPrompts implements Listener {

    private static final Map<UUID, Consumer<String>> PENDING = new ConcurrentHashMap<>();

    public ChatPrompts(MultiverseNets plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Registers a pending chat prompt callback for a specific player.
 *
     * Registra un callback de chat pendiente para un jugador específico.
     *
     * @param player Target player / Jugador objetivo
     * @param prompt Prompt message to display / Mensaje de solicitud a mostrar
     * @param callback Consumer to execute on chat input / Consumer a ejecutar tras el mensaje
     */
    public static void ask(Player player, String prompt, Consumer<String> callback) {
        PENDING.put(player.getUniqueId(), callback);
        player.sendMessage(com.chagui68.multiversenets.util.Text.msg(prompt, net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        player.sendMessage(com.chagui68.multiversenets.util.Text.msg("Type 'cancel' to abort.", net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }

    /**
     * Checks if a player has an active pending chat prompt.
 *
     * Comprueba si un jugador tiene una solicitud de chat pendiente.
     *
     * @param player Target player / Jugador objetivo
     * @return true if pending / true si está pendiente
     */
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PENDING.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Clears any pending chat prompt for the specified player.
     *
     * Cancela y elimina cualquier solicitud de chat pendiente para el jugador especificado.
     *
     * @param player Target player / Jugador objetivo
     */
    public static void clear(Player player) {
        PENDING.remove(player.getUniqueId());
    }
}
