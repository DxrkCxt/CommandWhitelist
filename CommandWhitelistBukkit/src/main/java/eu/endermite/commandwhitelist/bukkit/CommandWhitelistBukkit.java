package eu.endermite.commandwhitelist.bukkit;

import eu.endermite.commandwhitelist.bukkit.command.BukkitCommandExecutor;
import eu.endermite.commandwhitelist.bukkit.listeners.AsyncTabCompleteBlockerListener;
import eu.endermite.commandwhitelist.bukkit.listeners.PlayerCommandPreProcessListener;
import eu.endermite.commandwhitelist.bukkit.listeners.PlayerCommandSendListener;
import eu.endermite.commandwhitelist.bukkit.listeners.TabCompleteBlockerListener;
import eu.endermite.commandwhitelist.common.CWGroup;
import eu.endermite.commandwhitelist.common.CommandUtil;
import eu.endermite.commandwhitelist.common.ConfigCache;
import eu.endermite.commandwhitelist.common.commands.CWCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CommandWhitelistBukkit extends JavaPlugin {

    private static CommandWhitelistBukkit commandWhitelist;
    private static ConfigCache configCache;
    private static BukkitAudiences audiences;

    @Override
    public void onEnable() {

        commandWhitelist = this;
        audiences = BukkitAudiences.create(this);

        reloadPluginConfig();

        getServer().getPluginManager().registerEvents(new PlayerCommandPreProcessListener(), this);
        try {
            // Use paper's async tab completions if possible
            Class.forName("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent");
            getServer().getPluginManager().registerEvents(new AsyncTabCompleteBlockerListener(), this);
        } catch (ClassNotFoundException ignored) {
        }
        getServer().getPluginManager().registerEvents(new TabCompleteBlockerListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandSendListener(), this);

        PluginCommand command = getCommand("commandwhitelist");
        if (command != null) {
            BukkitCommandExecutor executor = new BukkitCommandExecutor();
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        new Metrics(this, 8705);
    }

    private void reloadPluginConfig() {
        File configFile = new File("plugins/CommandWhitelist/config.yml");
        if (configCache == null) {
            try {
                configCache = new ConfigCache(configFile, getSLF4JLogger());
            } catch (NoSuchMethodError e) {
                configCache = new ConfigCache(configFile, null);
            }
            return;
        }
        configCache.reloadConfig();
    }

    public void reloadPluginConfig(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            reloadPluginConfig();
            try {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.updateCommands();
                }
            } catch (Exception ignored) {}
            audiences.sender(sender).sendMessage(CWCommand.miniMessage.deserialize(configCache.prefix + configCache.config_reloaded));
        });
    }

    public static CommandWhitelistBukkit getPlugin() {
        return commandWhitelist;
    }

    public static ConfigCache getConfigCache() {
        return configCache;
    }

    public static BukkitAudiences getAudiences() {
        return audiences;
    }

    /**
     * @param player Bukkit Player
     * @return commands available to the player
     */
    public static HashSet<String> getCommands(org.bukkit.entity.Player player) {
        HashSet<String> commandList = new HashSet<>();
        HashMap<String, CWGroup> groups = configCache.getGroupList();
        for (Map.Entry<String, CWGroup> s : groups.entrySet()) {
            if (s.getKey().equalsIgnoreCase("default"))
                commandList.addAll(s.getValue().getCommands());
            else if (player.hasPermission(s.getValue().getPermission()))
                commandList.addAll(s.getValue().getCommands());
        }
        return commandList;
    }

    /**
     * @param player Bukkit Player
     * @return subcommands unavailable for the player
     */
    public static HashSet<String> getSuggestions(Player player) {
        HashSet<String> suggestionList = new HashSet<>();
        HashMap<String, CWGroup> groups = configCache.getGroupList();
        for (Map.Entry<String, CWGroup> s : groups.entrySet()) {
            if (s.getKey().equalsIgnoreCase("default"))
                suggestionList.addAll(s.getValue().getSubCommands());
            if (!player.hasPermission(s.getValue().getPermission())) continue;
            suggestionList.addAll(s.getValue().getSubCommands());
        }
        return suggestionList;
    }

    /**
     * Allocation-free check whether the player may use the given command label.
     *
     * @param player Bukkit Player
     * @param label  lowercase command label
     * @return true if any group available to the player whitelists the command
     */
    public static boolean isCommandAllowed(Player player, String label) {
        return CommandUtil.isCommandAllowed(configCache.getGroupList(), label, player::hasPermission);
    }

    /**
     * Allocation-free check whether the message starts with a subcommand blocked for the player.
     *
     * @param player  Bukkit Player
     * @param message command message without the leading slash
     * @return true if a blocked subcommand matches
     */
    public static boolean isSubCommandBlocked(Player player, String message) {
        return CommandUtil.isSubCommandBlocked(configCache.getGroupList(), message, player::hasPermission);
    }

    /**
     * @return Command denied message. Will use custom if command exists in any group.
     */
    public static String getCommandDeniedMessage(String command) {
        String commandDeniedMessage = configCache.command_denied;
        HashMap<String, CWGroup> groups = configCache.getGroupList();
        for (CWGroup group : groups.values()) {
            if (group.getCommands().contains(command)) {
                if (group.getCommandDeniedMessage() == null || group.getCommandDeniedMessage().isEmpty()) continue;
                commandDeniedMessage = group.getCommandDeniedMessage();
                break; // get first message we find
            }
        }
        return commandDeniedMessage;
    }

    public static ArrayList<String> getServerCommands() {
        try {
            return new ArrayList<>(Bukkit.getCommandMap().getKnownCommands().keySet());
        } catch (NoSuchMethodError error) {
            HashSet<String> commands = new HashSet<>();
            for (HelpTopic topic : Bukkit.getHelpMap().getHelpTopics()) {
                String cmd = topic.getName();
                if (Character.isUpperCase(cmd.charAt(0))) continue;
                commands.add(topic.getName());
            }
            return new ArrayList<>(commands);
        }
    }
}
