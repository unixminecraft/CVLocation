package org.cubeville.cvlocation;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import org.cubeville.cvchat.playerdata.PlayerDataManager;
import org.cubeville.cvipc.CVIPC;
import org.cubeville.cvipc.IPCInterface;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventHandler;

public class CVLocation extends Plugin implements Listener, IPCInterface {
    
    private CVIPC ipc;
    
    @Override
    public void onEnable() {
        PluginManager pluginManager = getProxy().getPluginManager();
        pluginManager.registerListener(this, this);
        this.ipc = (CVIPC) pluginManager.getPlugin("CVIPC");
        this.ipc.registerInterface("locationotherreceive", this);
    }
    
    @Override
    public void onDisable() {
        this.ipc.deregisterInterface("locationotherreceive");
        this.ipc = null;
    }
    
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChatEvent(ChatEvent event) {
        if(!event.isCommand()) { return; }
        StringTokenizer tokenizer = new StringTokenizer(event.getMessage(), " ");
        List<String> parameters = new ArrayList<String>();
        for(int i = 0; i < 3 && tokenizer.hasMoreTokens(); i++) {
            parameters.add(tokenizer.nextToken());
        }
        if(parameters.size() <= 0) { return; }
        if(!parameters.get(0).equals("/where")) { return; }
        Connection sender = event.getSender();
        if(!(sender instanceof ProxiedPlayer)) { return; }
        ProxiedPlayer player = (ProxiedPlayer) sender;
        boolean adminOverride = player.hasPermission("cvlocation.limited");
        boolean saOverride = player.hasPermission("cvlocation.unlimited");
        if(parameters.size() >= 3) {
            if(saOverride || adminOverride) {
                player.sendMessage("§cSyntax: /where [player]");
                return;
            }
            else {
                player.sendMessage("§cSyntax: /where");
                return;
            }
        }
        event.setCancelled(true);
        UUID playerID = player.getUniqueId();
        if(parameters.size() == 1) {
            String serverName = player.getServer().getInfo().getName();
            String ipcMessage = "locationself|" + playerID.toString();
            this.ipc.sendMessage(serverName, ipcMessage);
        }
        else {
            if(!adminOverride && !saOverride) {
                player.sendMessage("§cNo permission.");
                return;
            }
            PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
            String queryPlayerName = parameters.get(1);
            UUID queryPlayerID = playerDataManager.getPlayerId(queryPlayerName);
            if(queryPlayerID == null) {
                player.sendMessage("§cPlayer not found!");
                return;
            }
            if(!saOverride && !playerDataManager.outranks(playerID, queryPlayerID)) {
                player.sendMessage("§cNo permission.");
                return;
            }
            ProxiedPlayer queryPlayer = null;
            for(ProxiedPlayer p: ProxyServer.getInstance().getPlayers()) {
                if(p.getUniqueId().equals(queryPlayerID)) {
                    queryPlayer = p;
                    break;
                }
            }
            if(queryPlayer == null) {
                player.sendMessage("§cPlayer not online!");
                return;
            }
            String serverName = queryPlayer.getServer().getInfo().getName();
            String ipcMessage = "locationothersend|" + playerID.toString() + "|" + queryPlayerID.toString();
            this.ipc.sendMessage(serverName, ipcMessage);
        }
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void process(String serverName, String channel, String message) {
        if(channel.equals("locationotherreceive")) {
            List<String> parameters = getMessageParts(message);
            if(parameters.size() != 7) { return; }
            parameters = processParameters(parameters);
            UUID senderID = UUID.fromString(parameters.get(0));
            ProxiedPlayer sender = ProxyServer.getInstance().getPlayer(senderID);
            String line1 = "§ePlayer: " + parameters.get(1) + "§r\n";
            String line2 = "§eServer: " + serverName + "§r\n";
            String line3 = "§eWorld: " + parameters.get(2) + "§r\n";
            String line4 = "§eLocation: (" + parameters.get(3) + ", " + parameters.get(4) + ", " + parameters.get(5) + ")§r\n";
            String line5 = "§eDirection: " + parameters.get(6) + "§r\n";
            sender.sendMessage(line1 + line2 + line3 + line4 + line5);
        }
    }
    
    private List<String> getMessageParts(String message) {
        List<String> ret = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(message, "|");
        while(tok.hasMoreTokens()) {
            ret.add(tok.nextToken());
        }
        return ret;
    }
    
    private List<String> processParameters(List<String> parameters) {
        List<String> ret = new ArrayList<String>();
        ret.add(parameters.get(0));
        ret.add(parameters.get(1));
        ret.add(parameters.get(2));
        String pos = parameters.get(3);
        if(pos.indexOf(".") == -1) {
            ret.add(pos + ".0");
        }
        else {
            ret.add(pos.substring(0, pos.indexOf(".") + 2));
        }
        pos = parameters.get(4);
        if(pos.indexOf(".") == -1) {
            ret.add(pos + ".0");
        }
        else {
            ret.add(pos.substring(0, pos.indexOf(".") + 2));
        }
        pos = parameters.get(5);
        if(pos.indexOf(".") == -1) {
            ret.add(pos + ".0");
        }
        else {
            ret.add(pos.substring(0, pos.indexOf(".") + 2));
        }
        String directionS = "";
        boolean skipForError = false;;
        float directionF = 0.0F;
        try {
            directionF = Float.parseFloat(parameters.get(6));
        }
        catch(NullPointerException | NumberFormatException e) {
            directionS = "Undetermined";
            skipForError = true;
        }
        if(!skipForError) {
            while(directionF < -180.0F) {
                directionF += 360.0F;
            }
            while(directionF > 180.0F) {
                directionF -= 360.0F;
            }
            if((-180.0F <= directionF && directionF < -157.5F) || (157.5F <= directionF && directionF <= 180.0F)) {
                directionS = "North";
            }
            else if(-157.5F <= directionF && directionF < -112.5F) {
                directionS = "Northeast";
            }
            else if(-112.5F <= directionF && directionF < -67.5F) {
                directionS = "East";
            }
            else if(-67.5F <= directionF && directionF < -22.5F) {
                directionS = "Southeast";
            }
            else if(-22.5F <= directionF && directionF < 22.5F) {
                directionS = "South";
            }
            else if(22.5F <= directionF && directionF < 67.5F) {
                directionS = "Southwest";
            }
            else if(67.5F <= directionF && directionF < 112.5F) {
                directionS = "West";
            }
            else if(112.5F <= directionF && directionF < 157.5F) {
                directionS = "Northwest";
            }
            else {
                directionS = "Undetermined";
            }
        }
        ret.add(directionS);
        return ret;
    }
}
