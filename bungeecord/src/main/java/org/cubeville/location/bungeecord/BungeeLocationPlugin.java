/* 
 * This file is part of the CVLocation plugins for Bukkit servers and BungeeCord
 * proxies for Minecraft.
 * 
 * Copyright (C) 2018-2023 Matt Ciolkosz (https://github.com/mciolkosz/)
 * Copyright (C) 2018-2023 Cubeville (https://www.cubeville.org/)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cubeville.location.bungeecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import org.cubeville.location.bungeecord.command.WhereCommand;
import org.bspfsystems.bungeelocation.core.LocationConstants;
import org.cubeville.cvipc.CVIPC;
import org.cubeville.cvipc.IPCInterface;
import org.cubeville.cvplayerdata.CVPlayerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Represents the main entrypoint to the BungeeCord plugin systems for the
 * BungeeLocation BungeeCord plugin.
 * <p>
 * This also represents the {@link IPCInterface} for receiving and processing
 * location response IPC messages.
 */
public final class BungeeLocationPlugin extends Plugin implements IPCInterface {
    
    public static final String PERMISSION_UNLIMITED = "cvlocation.unlimited";
    public static final String PERMISSION_LIMITED = "cvlocation.limited";
    
    public static final BaseComponent[] NO_PERMISSION_MESSAGE = new ComponentBuilder("You do not have permission to execute this command.").color(ChatColor.RED).create();
    
    private ProxyServer proxy;
    private CVIPC ipcPlugin;
    private CVPlayerData playerDataPlugin;
    
    /**
     * Enables the plugin, registering the IPC request channel.
     */
    @Override
    public void onEnable() {
        
        this.proxy = this.getProxy();
        final PluginManager pluginManager = this.proxy.getPluginManager();
        
        final Plugin ipcPlugin = pluginManager.getPlugin("CVIPC");
        if (ipcPlugin == null) {
            throw new RuntimeException("CVIPC plugin not present.");
        }
        if (!(ipcPlugin instanceof CVIPC)) {
            throw new RuntimeException("CVIPC plugin is not the correct type.");
        }
        this.ipcPlugin = (CVIPC) ipcPlugin;
        
        final Plugin playerDataPlugin = pluginManager.getPlugin("CVPlayerData");
        if (playerDataPlugin == null) {
            throw new RuntimeException("CVPlayerData plugin not present.");
        }
        if (!(playerDataPlugin instanceof CVPlayerData)) {
            throw new RuntimeException("CVPlayerData plugin is not the correct type.");
        }
        this.playerDataPlugin = (CVPlayerData) playerDataPlugin;
        
        pluginManager.registerCommand(this, new WhereCommand(this.proxy, this.ipcPlugin, this.playerDataPlugin));
        
        this.ipcPlugin.registerInterface(LocationConstants.RESPONSE_CHANNEL, this);
    }
    
    /**
     * Disables the plugin, unregistering the IPC request channel.
     */
    @Override
    public void onDisable() {
        this.ipcPlugin.deregisterInterface(LocationConstants.RESPONSE_CHANNEL);
    }
    
    /**
     * Processes the incoming IPC message and channel.
     * <p>
     * This only has 1 channel registered: The location response channel.
     * <p>
     * The location response should contain at minimum:
     *  - The original sender's {@link UUID}.
     *  - The target player's name.
     *  - The target player's Location, including:
     *      - World name
     *      - X coordinate
     *      - Y coordinate
     *      - Z coordinate
     *      - Yaw (used to determine cardinal direction)
     * The optional data that the response may contain would include:
     *  - The keyword {@code "UNKNOWN"} if no WorldGuard regions could be
     *    found, OR
     *  - The list of WorldGuard regions that the target player is standing in.
     * 
     * @param channel The channel the IPC message is destined for.
     * @param message The IPC message.
     */
    @Override
    public void process(final String serverName, final String channel, final String message) {
        
        final Logger logger = this.getLogger();
        if (!channel.equalsIgnoreCase(LocationConstants.RESPONSE_CHANNEL)) {
            logger.log(Level.WARNING, "Invalid channel for BungeeCord Location plugin: " + channel + ". Channel should be " + LocationConstants.RESPONSE_CHANNEL);
            return;
        }
        
        final List<String> parts = this.getMessageParts(message);
        if (parts.size() < 7) {
            logger.log(Level.WARNING, "Missing some combination of sender UUID, target name, world name, X, Y, Z, and Yaw.");
            logger.log(Level.WARNING, "Message: " + message);
            return;
        }
        
        final Iterator<String> iterator = parts.iterator();
        final String rawSenderId = iterator.next();
        final String rawTargetId = iterator.next();
        final String worldName = iterator.next();
        final String x = iterator.next();
        final String y = iterator.next();
        final String z = iterator.next();
        final String direction = this.processYaw(iterator.next());
        
        final CommandSender sender;
        final boolean samePlayer;
        
        if (rawSenderId.equalsIgnoreCase(this.proxy.getConsole().getName())) {
            sender = this.proxy.getConsole();
            samePlayer = false;
        } else {
            
            final UUID senderId;
            try {
                senderId = UUID.fromString(rawSenderId);
            } catch (final IllegalArgumentException e) {
                logger.log(Level.WARNING, "Unable to parse sender UUID.", e);
                logger.log(Level.WARNING, "Message: " + message);
                return;
            }
            
            sender = this.proxy.getPlayer(senderId);
            if (sender == null) {
                logger.log(Level.WARNING, "Original Sender is offline when location response was received.");
                logger.log(Level.WARNING, "Sender UUID: " + rawSenderId);
                logger.log(Level.WARNING, "Message: " + message);
                return;
            }
            
            samePlayer = rawSenderId.equalsIgnoreCase(rawTargetId);
        }
        
        final UUID targetId;
        try {
            targetId = UUID.fromString(rawTargetId);
        } catch (final IllegalArgumentException e) {
            logger.log(Level.WARNING, "Unable to parse target UUID.", e);
            logger.log(Level.WARNING, "Message: " + message);
            return;
        }
        
        final String targetName = this.playerDataPlugin.getPlayerDataManager().getPlayerVisibleName(targetId);
        
        final List<String> regions;
        if (!iterator.hasNext()) {
            regions = null;
        } else {
            final String first = iterator.next();
            if (first.equalsIgnoreCase(LocationConstants.REGIONS_UNKNOWN)) {
                regions = Collections.emptyList();
            } else {
                regions = new ArrayList<String>();
                regions.add(first);
                while (iterator.hasNext()) {
                    regions.add(iterator.next());
                }
            }
        }
        
        final boolean unlimited = sender.hasPermission(PERMISSION_UNLIMITED);
        final boolean limited = sender.hasPermission(PERMISSION_LIMITED);
        
        if (samePlayer) {
            if (unlimited || limited) {
                this.sendLocation(sender, targetName + " (That's you!)", serverName, worldName, x, y, z, direction, regions);
            } else {
                this.sendLocation(sender, targetName + " (That's you!)", null, null, x, y, z, direction, null);
            }
        } else if (unlimited || limited) {
            this.sendLocation(sender, targetName, serverName, worldName, x, y, z, direction, regions);
        } else {
            sender.sendMessage(NO_PERMISSION_MESSAGE);
        }
    }
    
    /**
     * Processes the given whole IPC message into a {@link List} of various
     * parts.
     * 
     * @param message The full IPC message.
     * @return The separated parts of the IPC message.
     */
    @NotNull
    @UnmodifiableView
    private List<String> getMessageParts(final String message) {
        
        final List<String> parts = new ArrayList<String>();
        final StringTokenizer tokenizer = new StringTokenizer(message, LocationConstants.SEPARATOR);
        
        while(tokenizer.hasMoreTokens()) {
            parts.add(tokenizer.nextToken());
        }
        
        return Collections.unmodifiableList(parts);
    }
    
    /**
     * Gets the {@link String} compass direction value for the given
     * {@link String} yaw value.
     * 
     * @param yawValue The {@link String} representation of the yaw value.
     * @return The {@link String} representation of the compass direction.
     */
    @NotNull
    private String processYaw(@NotNull final String yawValue) {
        
        float yaw;
        try {
            yaw = Float.parseFloat(yawValue);
        } catch (final NumberFormatException e) {
            return "Undetermined";
        }
        
        while (yaw < -180.0F) {
            yaw += 360.0F;
        }
        while (yaw > 180.0F) {
            yaw -= 360.0F;
        }
        
        if (yaw >= -180.0F && yaw < -157.0F || yaw >= 157.5F) {
            return "North";
        } else if (yaw >= -157.0F && yaw < -112.5F) {
            return "Northeast";
        } else if (yaw >= -112.5F && yaw < -67.5F) {
            return "East";
        } else if (yaw >= -67.5F && yaw < -22.5F) {
            return "Southeast";
        } else if (yaw >= -22.5F && yaw < 22.5F) {
            return "South";
        } else if (yaw >= 22.5F && yaw < 67.5F) {
            return "Southwest";
        } else if (yaw >= 67.5F && yaw < 112.5F) {
            return "West";
        } else if (yaw >= 112.5F) {
            return "Northwest";
        } else {
            return "Undetermined";
        }
    }
    
    /**
     * Sends the given location data to the given {@link CommandSender} sender.
     * 
     * @param sender The {@link CommandSender} to the location data to.
     * @param targetName The name of the target {@link ProxiedPlayer}.
     * @param serverName The server name that the target is connected to, or
     *                   {@code null} if it should not be displayed.
     * @param worldName The world name that the target is connected to, or
     *                  {@code null} if it should not be displayed.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param z The Z coordinate.
     * @param direction The compass direction the target {@link ProxiedPlayer}
     *                  is facing.
     * @param regions The {@link List} of WorldGuard regions that the target
     *                {@link ProxiedPlayer} is standing in. If {@code null}, no
     *                regions are to be listed. If the {@link List} is empty,
     *                then an error occurred while retrieving the regions from
     *                the Bukkit side, and an error message will be displayed.
     */
    private void sendLocation(@NotNull final CommandSender sender, @NotNull final String targetName, @Nullable final String serverName, @Nullable final String worldName, @NotNull final String x, @NotNull final String y, @NotNull final String z, @NotNull final String direction, @Nullable final List<String> regions) {
        
        final BaseComponent[] divide = new ComponentBuilder("--------------------------------").color(ChatColor.DARK_GRAY).create();
        final String location = "Location: (" + x + ", " + y + ", " + z + ")";
        
        sender.sendMessage(divide);
        sender.sendMessage(this.formatText("Player: " + targetName));
        if (serverName != null) {
            sender.sendMessage(this.formatText("Server: " + serverName));
        }
        if (worldName != null) {
            if (worldName.equalsIgnoreCase("null")) {
                sender.sendMessage(new ComponentBuilder("World: ").color(ChatColor.YELLOW).append("UNKNOWN").color(ChatColor.RED).create());
            } else {
                sender.sendMessage(this.formatText("World: " + worldName));
            }
        }
        sender.sendMessage(this.formatText(location));
        sender.sendMessage(this.formatText("Direction: " + direction));
        sender.sendMessage(divide);
        
        if (regions == null) {
            return;
        }
        
        final ComponentBuilder regionsBuilder = new ComponentBuilder("Regions: ").color(ChatColor.YELLOW);
        if (regions.isEmpty()) {
            sender.sendMessage(regionsBuilder.append("UNKNOWN").color(ChatColor.RED).create());
            sender.sendMessage(divide);
            return;
        }
        
        if (regions.size() == 1 && regions.get(0).equalsIgnoreCase(LocationConstants.REGION_GLOBAL)) {
            sender.sendMessage(regionsBuilder.append("GLOBAL REGION").color(ChatColor.AQUA).create());
            sender.sendMessage(divide);
            return;
        }
        
        final Iterator<String> iterator = regions.iterator();
        while (iterator.hasNext()) {
            regionsBuilder.append(iterator.next()).color(ChatColor.GREEN);
            if (iterator.hasNext()) {
                regionsBuilder.append(", ").color(ChatColor.GREEN).append(iterator.next()).color(ChatColor.DARK_GREEN);
                if (iterator.hasNext()) {
                    regionsBuilder.append(", ").color(ChatColor.DARK_GREEN);
                }
            }
        }
        
        sender.sendMessage(regionsBuilder.create());
        sender.sendMessage(divide);
    }
    
    /**
     * Gets the {@link BaseComponent[]} as {@link ChatColor#YELLOW}-formatted
     * text for the given {@link String}.
     * 
     * @param string The {@link String} to format.
     * @return The formatted {@link BaseComponent[]}.
     */
    @NotNull
    private BaseComponent[] formatText(@NotNull final String string) {
        return new ComponentBuilder(string).color(ChatColor.YELLOW).create();
    }
}
