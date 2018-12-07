package org.cubeville.cvlocation;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import org.cubeville.cvipc.CVIPC;
import org.cubeville.cvipc.IPCInterface;
import org.cubeville.cvlocation.commands.WhereAmICommand;
import org.cubeville.cvlocation.commands.WhereCommand;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

public class CVLocation extends Plugin implements IPCInterface {
    
    private CVIPC ipc;
    
    @Override
    public void onEnable() {
        PluginManager pluginManager = getProxy().getPluginManager();
        ipc = (CVIPC) pluginManager.getPlugin("CVIPC");
        ipc.registerInterface("locationresponse", this);
        pluginManager.registerCommand(this, new WhereCommand(ipc));
        pluginManager.registerCommand(this, new WhereAmICommand(ipc));
    }
    
    @Override
    public void onDisable() {
        ipc.deregisterInterface("locationresponse");
        ipc = null;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void process(String serverName, String channel, String message) {
        if(channel.equals("locationresponse")) {
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
