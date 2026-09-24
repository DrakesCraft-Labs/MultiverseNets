package com.chagui68.multiversenets;

import com.chagui68.multiversenets.command.MvnetsCommand;
import com.chagui68.multiversenets.gui.ChatPrompts;
import com.chagui68.multiversenets.gui.GuiListener;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.listen.BlockListener;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.net.NetworkTicker;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.Settings;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MultiverseNets main plugin entry point and lifecycle manager.
 *
 * Punto de entrada principal y gestor del ciclo de vida del plugin MultiverseNets.
 */
public class MultiverseNets extends JavaPlugin {

    private static MultiverseNets instance;

    private NetworkManager networks;
    private NetworkTicker ticker;

    /**
     * @return Global singleton plugin instance / Instancia singleton global del plugin
     */
    public static MultiverseNets instance() {
        return instance;
    }

    /**
     * @return Global network manager / Gestor global de redes
     */
    public NetworkManager networks() {
        return networks;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        Keys.init(this);
        Settings.refresh(this);
        // Slimefun integration initialization: active if present, dormant otherwise.
        com.chagui68.multiversenets.compat.SlimefunBridge.init(getLogger());
        Items.registerRecipes(this);
        NodeStore.init(this);

        networks = new NetworkManager(this);
        networks.load();

        new BlockListener(this, networks);
        new GuiListener(this);
        new ChatPrompts(this);
        new com.chagui68.multiversenets.listen.CraftingListener(this);

        // Safety net: in case datapack/server reload wipes dynamic recipes during startup
        getServer().getScheduler().runTask(this, () -> Items.registerRecipes(this));
        getServer().getScheduler().runTaskLater(this, () -> {
            Items.registerRecipes(this);
            for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
                Items.discoverRecipes(p);
            }
        }, 100L);

        ticker = new NetworkTicker(this, networks);
        ticker.start();

        PluginCommand command = getCommand("mvnets");
        if (command != null) {
            MvnetsCommand executor = new MvnetsCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("MultiverseNets v" + getPluginMeta().getVersion() + " enabled. Author: Chagui68");
    }

    @Override
    public void onDisable() {
        if (ticker != null) {
            ticker.stop();
        }
        if (networks != null) {
            networks.saveAll();
        }
        getLogger().info("MultiverseNets disabled.");
    }
}
