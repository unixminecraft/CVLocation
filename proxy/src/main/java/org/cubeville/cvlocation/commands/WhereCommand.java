package org.cubeville.cvlocation.commands;

import java.util.UUID;

import org.cubeville.cvchat.playerdata.PlayerDataManager;
import org.cubeville.cvipc.CVIPC;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class WhereCommand extends Command {
    
    private CVIPC ipc;

    public WhereCommand(CVIPC ipcIn) {
        
        super("where", null, "whereami");
        this.ipc = ipcIn;
    }
    
    @Override
    public void execute(CommandSender commandSender, String[] args) {
        
        if(!(commandSender instanceof ProxiedPlayer)) {
            
            TextComponent errorMessage = new TextComponent();
            errorMessage.setText("ERROR: Command can only be used by a player.");
            return;
        }
        
        ProxiedPlayer proxiedPlayer = (ProxiedPlayer) commandSender;
        UUID playerId = proxiedPlayer.getUniqueId();
        
        if(args.length == 0) {
            
            String serverName = proxiedPlayer.getServer().getInfo().getName();
            String ipcMessage = "locationquery|" + playerId.toString() + "|" + playerId.toString();
            
            this.ipc.sendMessage(serverName, ipcMessage);
            return;
        }
        
        boolean unlimited = proxiedPlayer.hasPermission("cvlocation.unlimited");
        boolean other = proxiedPlayer.hasPermission("cvlocation.other");
        
        if(args.length == 1) {
            
            String queriedPlayerName = args[0];
            
            if(proxiedPlayer.getName().equalsIgnoreCase(queriedPlayerName)) {
                
                String serverName = proxiedPlayer.getServer().getInfo().getName();
                String ipcMessage = "locationquery|" + playerId.toString() + "|" + playerId.toString();
                
                this.ipc.sendMessage(serverName, ipcMessage);
                return;
            }
            
            if((!unlimited) && (!other)) {
                
                TextComponent errorMessage = new TextComponent();
                errorMessage.setText("No permission.");
                errorMessage.setColor(ChatColor.RED);
                proxiedPlayer.sendMessage(errorMessage);
                return;
            }
            
            ProxiedPlayer queriedProxiedPlayer = ProxyServer.getInstance().getPlayer(queriedPlayerName);
            
            if(queriedProxiedPlayer == null) {
                
                TextComponent error1 = new TextComponent();
                TextComponent error2 = new TextComponent();
                TextComponent error3 = new TextComponent();
      
                error1.setText("Player ");
                error2.setText(queriedPlayerName);
                error3.setText(" not found.");
      
                error1.setColor(ChatColor.RED);
                error2.setColor(ChatColor.GOLD);
                error3.setColor(ChatColor.RED);
      
                proxiedPlayer.sendMessage(error1, error2, error3);
                return;
            }
            
            UUID queriedPlayerId = queriedProxiedPlayer.getUniqueId();
            
            if((!unlimited) && (!PlayerDataManager.getInstance().outranks(playerId, queriedPlayerId))) {
                
                TextComponent errorMessage = new TextComponent();
                errorMessage.setText("No permission.");
                errorMessage.setColor(ChatColor.RED);
                proxiedPlayer.sendMessage(errorMessage);
                return;
            }
            
            String serverName = queriedProxiedPlayer.getServer().getInfo().getName();
            String ipcMessage = "locationquery|" + playerId.toString() + "|" + queriedPlayerId.toString();
            
            this.ipc.sendMessage(serverName, ipcMessage);
            return;
        }
        
        if(args.length >= 2) {
            
            TextComponent errorMessage = new TextComponent();
            
            if((unlimited) || (other)) {
                errorMessage.setText("Syntax: /where [player]");
            }
            else {
                errorMessage.setText("Syntax: /where");
            }
            
            errorMessage.setColor(ChatColor.RED);
            proxiedPlayer.sendMessage(errorMessage);
        }
    }
}