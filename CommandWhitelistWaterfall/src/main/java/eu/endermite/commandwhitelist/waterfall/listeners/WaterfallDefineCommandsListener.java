package eu.endermite.commandwhitelist.waterfall.listeners;

import eu.endermite.commandwhitelist.common.CWPermission;
import eu.endermite.commandwhitelist.waterfall.CommandWhitelistWaterfall;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashSet;
import java.util.Locale;

public class WaterfallDefineCommandsListener implements Listener {

    @EventHandler
    public void onProxyDefineCommandsEvent(io.github.waterfallmc.waterfall.event.ProxyDefineCommandsEvent event) {
        if (event.getReceiver() instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) event.getReceiver();
            if (player.hasPermission(CWPermission.BYPASS.permission())) return;
            HashSet<String> allowed = new HashSet<>();
            for (String cmdName : CommandWhitelistWaterfall.getCommands(player))
                allowed.add(cmdName.toLowerCase(Locale.ENGLISH));
            event.getCommands().values().removeIf((cmd) -> !allowed.contains(cmd.getName().toLowerCase(Locale.ENGLISH)));
        }
    }
}
