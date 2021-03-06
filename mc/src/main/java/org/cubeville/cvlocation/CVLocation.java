package org.cubeville.cvlocation;

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
            if(index == -1) { return; }
            String senderId = message.substring(0, index);
            UUID playerId = UUID.fromString(message.substring(index + 1));
            if(playerId == null) { return; }
            Player player = getServer().getPlayer(playerId);
            if(player == null) { return; }
            if(!player.isOnline()) { return; }
            Location location = player.getLocation();
            String locationWorld = location.getWorld().getName();
            String locationX = Double.toString((int) location.getX());
            String locationY = Double.toString((int) location.getY());
            String locationZ = Double.toString((int) location.getZ());
            String locationYaw = Float.toString(location.getYaw());
            String ipcMessage = "locationresponse|" + senderId + "|" + player.getName() + "|" + locationWorld + "|" + locationX + "|" + locationY + "|" + locationZ + "|" + locationYaw;
            ipc.sendMessage(ipcMessage);
        }
    }
}