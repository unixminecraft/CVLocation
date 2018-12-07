package org.cubeville.cvlocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvipc.CVIPC;
import org.cubeville.cvipc.IPCInterface;

public class CVLocation extends JavaPlugin implements IPCInterface {

    private CVIPC ipc;
    
    @Override
    public void onEnable() {
        PluginManager pluginManager = getServer().getPluginManager();
        ipc = (CVIPC) pluginManager.getPlugin("CVIPC");
        ipc.registerInterface("locationquery", this);
    }
    
    @Override
    public void onDisable() {
        ipc.deregisterInterface("locationquery");
        ipc = null;
    }
    
    @Override
    public void process(String channel, String message) {
        if(channel.equals("locationquery")) {
            int index = message.indexOf("|");
            if(index == -1) {
                UUID senderID = UUID.fromString(message);
                Player sender = getServer().getPlayer(senderID);
                if(sender == null) { return; }
                List<String> location = processLocation(sender.getLocation());
                String line1 = "§ePlayer: " + sender.getName() + " (That's you!)§r\n";
                String line2 = "§eServer: " + getServer().getName() + "§r\n";
                String line3 = "§eWorld: " + location.get(0) + "§r\n";
                String line4 = "§eLocation: (" + location.get(1) + ", " + location.get(2) + ", " + location.get(3) + ")§r\n";
                String line5 = "§eDirection: " + location.get(4) + "§r\n";
                boolean adminOverride = sender.hasPermission("cvlocation.limited") || sender.hasPermission("cvlocation.unlimited");
                if(adminOverride) {
                    sender.sendMessage(line1 + line2 + line3 + line4 + line5);
                }
                else {
                    sender.sendMessage(line1 + line4 + line5);
                }
                
            }
            else {
                String senderIDString = message.substring(0, index);
                String playerIDString = message.substring(index + 1);
                UUID playerID = UUID.fromString(playerIDString);
                Player player = getServer().getPlayer(playerID);
                if(player == null) { return; }
                if(!player.isOnline()) { return; }
                Location location = player.getLocation();
                String locationWorld = location.getWorld().getName();
                String locationX = Double.toString(location.getX());
                String locationY = Double.toString(location.getY());
                String locationZ = Double.toString(location.getZ());
                String locationYaw = Float.toString(location.getYaw());
                String ipcMessage = "locationresponse|" + senderIDString + "|" + player.getName() + "|" + locationWorld + "|" + locationX + "|" + locationY + "|" + locationZ + "|" + locationYaw;
                ipc.sendMessage(ipcMessage);
            }
        }
    }
    
    private List<String> processLocation(Location location) {
        List<String> ret = new ArrayList<String>();
        ret.add(location.getWorld().getName());
        String x = Double.valueOf(location.getX()).toString();
        String y = Double.valueOf(location.getY()).toString();
        String z = Double.valueOf(location.getZ()).toString();
        String pos = x;
        if(pos.indexOf(".") == -1) {
            ret.add(pos + ".0");
        }
        else {
            ret.add(pos.substring(0, pos.indexOf(".") + 2));
        }
        pos = y;
        if(pos.indexOf(".") == -1) {
            ret.add(pos + ".0");
        }
        else {
            ret.add(pos.substring(0, pos.indexOf(".") + 2));
        }
        pos = z;
        if(pos.indexOf(".") == -1) {
            ret.add(pos + ".0");
        }
        else {
            ret.add(pos.substring(0, pos.indexOf(".") + 2));
        }
        float directionF = location.getYaw();
        String directionS = "";
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
        ret.add(directionS);
        return ret;
    }
}
