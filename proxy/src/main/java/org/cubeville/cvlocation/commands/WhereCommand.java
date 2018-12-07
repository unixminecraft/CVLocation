package org.cubeville.cvlocation.commands;

import java.util.UUID;

import org.cubeville.cvchat.playerdata.PlayerDataManager;
import org.cubeville.cvipc.CVIPC;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class WhereCommand extends Command {

    private CVIPC ipc;
    
    public WhereCommand(CVIPC ipcIn) {
        super("where");
        ipc = ipcIn;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if(!(commandSender instanceof ProxiedPlayer)) { return; }
        ProxiedPlayer sender = (ProxiedPlayer) commandSender;
        UUID senderID = sender.getUniqueId();
        boolean adminOverride = sender.hasPermission("cvlocation.limited");
        boolean saOverride = sender.hasPermission("cvlocation.unlimited");
        if(args.length == 0) {
            String serverName = sender.getServer().getInfo().getName();
            String ipcMessage = "locationquery|" + senderID.toString();
            ipc.sendMessage(serverName, ipcMessage);
        }
        else if(args.length == 1) {
            String playerName = args[0];
            if(sender.getName().equalsIgnoreCase(playerName)) {
                String serverName = sender.getServer().getInfo().getName();
                String ipcMessage = "locationquery|" + senderID.toString();
                ipc.sendMessage(serverName, ipcMessage);
            }
            else if(adminOverride || saOverride) {
                ProxiedPlayer player = null;
                for(ProxiedPlayer p: ProxyServer.getInstance().getPlayers()) {
                    if(p.getName().equalsIgnoreCase(playerName)) {
                        player = p;
                        break;
                    }
                }
                if(player == null) {
                    sender.sendMessage("§cPlayer not online!");
                    return;
                }
                UUID playerID = player.getUniqueId();
                if(playerID == null) {
                    sender.sendMessage("§cInternal error, please try again later.");
                    return;
                }
                if(!saOverride && !PlayerDataManager.getInstance().outranks(senderID, playerID)) {
                    sender.sendMessage("§cNo permission.");
                    return;
                }
                String serverName = player.getServer().getInfo().getName();
                String ipcMessage = "locationquery|" + senderID.toString() + "|" + playerID.toString();
                ipc.sendMessage(serverName, ipcMessage);
            }
            else {
                sender.sendMessage("§cNo permission.");
            }
        }
        else {
            if(adminOverride || saOverride) {
                sender.sendMessage("§cSyntax: /where [player]");
            }
            else {
                sender.sendMessage("§cSyntax: /where");
            }
        }
    }
}
