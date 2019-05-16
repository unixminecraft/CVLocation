package org.cubeville.cvlocation;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import org.cubeville.cvipc.CVIPC;
import org.cubeville.cvipc.IPCInterface;
import org.cubeville.cvlocation.commands.WhereCommand;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

public class CVLocation extends Plugin implements IPCInterface {
    
    private CVIPC ipc;
    
    @Override
    public void onEnable() {
        
        PluginManager pluginManager = getProxy().getPluginManager();
        this.ipc = ((CVIPC)pluginManager.getPlugin("CVIPC"));
        this.ipc.registerInterface("locationresponse", this);
        pluginManager.registerCommand(this, new WhereCommand(this.ipc));
    }
    
    @Override
    public void onDisable() {
        this.ipc.deregisterInterface("locationresponse");
        this.ipc = null;
    }
    
    @Override
    public void process(String serverName, String channel, String message) {
        
        if(channel.equals("locationresponse")) {
          
            List<String> parameters = getMessageParts(message);
        
            if(parameters.size() != 7) {
                return;
            }
        
            parameters = processParameters(parameters);
        
            UUID playerId = UUID.fromString((String)parameters.get(0));
            ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(playerId);
        
            boolean samePlayer = proxiedPlayer.getName().equalsIgnoreCase((String)parameters.get(1));
        
            boolean unlimited = proxiedPlayer.hasPermission("cvlocation.unlimited");
            boolean other = proxiedPlayer.hasPermission("cvlocation.other");
            boolean viewServer = proxiedPlayer.hasPermission("cvlocation.viewserver");
            boolean useCommand = proxiedPlayer.hasPermission("cvlocation.server." + serverName);
        
            if((!unlimited) && (!useCommand)) {
                TextComponent errorMessage = new TextComponent();
                errorMessage.setText("No permission.");
              errorMessage.setColor(ChatColor.RED);
              proxiedPlayer.sendMessage(errorMessage);
              return;
            }
        
            if((!unlimited) && (!other) && (!samePlayer)) {
                TextComponent errorMessage = new TextComponent();
                errorMessage.setText("No permission.");
                errorMessage.setColor(ChatColor.RED);
                proxiedPlayer.sendMessage(errorMessage);
                return;
            }
        
            TextComponent colon = new TextComponent();
            TextComponent playerLabel = new TextComponent();
            TextComponent playerValue = new TextComponent();
            TextComponent worldLabel = new TextComponent();
            TextComponent worldValue = new TextComponent();
            TextComponent locationLabel = new TextComponent();
            TextComponent locationValue = new TextComponent();
            TextComponent directionLabel = new TextComponent();
            TextComponent directionValue = new TextComponent();
        
            colon.setText(": ");
            playerLabel.setText("Player");
        
            if(samePlayer) {
                playerValue.setText((String)parameters.get(1) + " (That's you!)");
            }
            else {
                playerValue.setText((String)parameters.get(1));
            }
        
            worldLabel.setText("World");
            worldValue.setText((String)parameters.get(2));
            locationLabel.setText("Location");
            locationValue.setText("(" + (String)parameters.get(3) + ", " + (String)parameters.get(4) + ", " + (String)parameters.get(5) + ")");
            directionLabel.setText("Facing");
            directionValue.setText((String)parameters.get(6));
        
            colon.setColor(ChatColor.YELLOW);
            playerLabel.setColor(ChatColor.YELLOW);
            playerValue.setColor(ChatColor.YELLOW);
            worldLabel.setColor(ChatColor.YELLOW);
            worldValue.setColor(ChatColor.YELLOW);
            locationLabel.setColor(ChatColor.YELLOW);
            locationValue.setColor(ChatColor.YELLOW);
            directionLabel.setColor(ChatColor.YELLOW);
            directionValue.setColor(ChatColor.YELLOW);
        
            proxiedPlayer.sendMessage(playerLabel, colon, playerValue);
        
            if(viewServer) {
            
                TextComponent serverLabel = new TextComponent();
                TextComponent serverValue = new TextComponent();
          
                serverLabel.setText("Server");
                serverValue.setText(serverName);
          
                serverLabel.setColor(ChatColor.YELLOW);
                serverValue.setColor(ChatColor.YELLOW);
          
                proxiedPlayer.sendMessage(serverLabel, colon, serverValue);
            }
        
            proxiedPlayer.sendMessage(worldLabel, colon, worldValue);
            proxiedPlayer.sendMessage(locationLabel, colon, locationValue);
            proxiedPlayer.sendMessage(directionLabel, colon, directionValue);
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
  
        for(int index = 0; index < 6; index++) {
            ret.add(parameters.get(index));
        }
  
        String directionS = "";
        boolean skipForError = false;
        float directionF = 0.0F;
  
        try {
            directionF = Float.parseFloat((String)parameters.get(6));
        }
        catch(NullPointerException|NumberFormatException e) {
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
    
            if(((-180.0F <= directionF) && (directionF < -157.5F)) || ((157.5F <= directionF) && (directionF <= 180.0F))) {
                directionS = "North";
            }
            else if((-157.5F <= directionF) && (directionF < -112.5F)) {
                directionS = "Northeast";
            }
            else if((-112.5F <= directionF) && (directionF < -67.5F)) {
                directionS = "East";
            }
            else if((-67.5F <= directionF) && (directionF < -22.5F)) {
                directionS = "Southeast";
            }
            else if((-22.5F <= directionF) && (directionF < 22.5F)) {
                directionS = "South";
            }
            else if((22.5F <= directionF) && (directionF < 67.5F)) {
                directionS = "Southwest";
            }
            else if((67.5F <= directionF) && (directionF < 112.5F)) {
                directionS = "West";
            }
            else if((112.5F <= directionF) && (directionF < 157.5F)) {
                directionS = "Northwest";
            }
            else{
                directionS = "Undetermined";
            }
        }
  
        ret.add(directionS);
  
        return ret;
    }
}