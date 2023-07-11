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

package org.cubeville.location.bukkit.old;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bspfsystems.bungeelocation.core.LocationConstants;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvipc.CVIPC;
import org.cubeville.cvipc.IPCInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Represents the main entrypoint to the Bukkit plugin systems for the
 * BungeeLocation Bukkit plugin.
 * <p>
 * This also represents the {@link IPCInterface} for receiving and processing
 * location request IPC messages.
 */
public final class BukkitOldLocationPlugin extends JavaPlugin implements IPCInterface {
    
    private Server server;
    private CVIPC ipcPlugin;
    
    /**
     * Enables the plugin, registering the IPC request channel.
     */
    @Override
    public void onEnable() {
        
        this.server = this.getServer();
        
        final Plugin ipcPlugin = this.server.getPluginManager().getPlugin("CVIPC");
        if (ipcPlugin == null) {
            throw new RuntimeException("CVIPC plugin not present.");
        }
        if (!(ipcPlugin instanceof CVIPC)) {
            throw new RuntimeException("CVIPC plugin is not the correct type.");
        }
        
        this.ipcPlugin = (CVIPC) ipcPlugin;
        
        this.ipcPlugin.registerInterface(LocationConstants.REQUEST_CHANNEL, this);
    }
    
    /**
     * Disables the plugin, unregistering the IPC request channel.
     */
    @Override
    public void onDisable() {
        this.ipcPlugin.deregisterInterface(LocationConstants.REQUEST_CHANNEL);
    }
    
    /**
     * Processes the incoming IPC message and channel.
     * <p>
     * This only has 1 channel registered: The location request channel.
     * <p>
     * The location request will find the player with the given target
     * {@link UUID}, obtain their {@link Location}, and, if requested, also
     * obtain the set of {@link ProtectedRegion} names that the player is
     * located in. It will then compile the data and send it back via the
     * response channel.
     * 
     * @param channel The channel the IPC message is destined for.
     * @param message The IPC message.
     */
    @Override
    public void process(final String channel, final String message) {
        
        final Logger logger = this.getLogger();
        if (!channel.equalsIgnoreCase(LocationConstants.REQUEST_CHANNEL)) {
            logger.log(Level.WARNING, "Invalid channel for Bukkit Location plugin: " + channel + ". Channel should be " + LocationConstants.REQUEST_CHANNEL);
            return;
        }
        
        final List<String> parts = this.getMessageParts(message);
        if (parts.size() < 3) {
            logger.log(Level.WARNING, "Missing some combination of sender UUID, target UUID, and regions flag.");
            logger.log(Level.WARNING, "Message: " + message);
            return;
        }
        if (parts.size() > 3) {
            logger.log(Level.WARNING, "Message contains too much data. Should only contain sender UUID, target UUID, and regions flag.");
            logger.log(Level.WARNING, "Message: " + message);
            return;
        }
        
        final String rawSenderId = parts.get(0);
        final String rawTargetId = parts.get(1);
        final boolean getRegions = parts.get(2).equalsIgnoreCase("true");
        
        final UUID targetId;
        try {
            targetId = UUID.fromString(rawTargetId);
        } catch (final IllegalArgumentException e) {
            logger.log(Level.WARNING, "Unable to parse target UUID. Sender UUID: " + rawSenderId, e);
            return;
        }
        
        final Player target = this.server.getPlayer(targetId);
        if (target == null) {
            return;
        }
        if (!target.isOnline()) {
            return;
        }
        
        final Location location = target.getLocation();
        final World world = location.getWorld();
        final String worldName = world == null ? "null" : world.getName();
        final String x = String.valueOf((int) location.getX());
        final String y = String.valueOf((int) location.getY());
        final String z = String.valueOf((int) location.getZ());
        final String yaw = String.valueOf(location.getYaw());
        
        final StringBuilder builder = new StringBuilder();
        builder.append(LocationConstants.RESPONSE_CHANNEL).append(LocationConstants.SEPARATOR);
        builder.append(rawSenderId).append(LocationConstants.SEPARATOR);
        builder.append(rawTargetId).append(LocationConstants.SEPARATOR);
        builder.append(worldName).append(LocationConstants.SEPARATOR);
        builder.append(x).append(LocationConstants.SEPARATOR);
        builder.append(y).append(LocationConstants.SEPARATOR);
        builder.append(z).append(LocationConstants.SEPARATOR);
        builder.append(yaw);
        
        if (!getRegions) {
            this.ipcPlugin.sendMessage(builder.toString());
            return;
        }
        
        builder.append(LocationConstants.SEPARATOR);
        if (world == null) {
            logger.log(Level.WARNING, "Sender UUID: " + rawSenderId + " / Target UUID: " + rawTargetId + " / Null World.");
            builder.append(LocationConstants.REGIONS_UNKNOWN);
            this.ipcPlugin.sendMessage(builder.toString());
            return;
        }
        
        final RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            logger.log(Level.WARNING, "Sender UUID: " + rawSenderId + " / Target UUID: " + rawTargetId + " / World: " + worldName + " / Null Region Manager.");
            builder.append(LocationConstants.REGIONS_UNKNOWN);
            this.ipcPlugin.sendMessage(builder.toString());
            return;
        }
        
        final ApplicableRegionSet regions = regionManager.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
        if (regions.size() == 0) {
            builder.append(LocationConstants.REGION_GLOBAL);
            this.ipcPlugin.sendMessage(builder.toString());
            return;
        }
        
        final Iterator<ProtectedRegion> iterator = regions.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next().getId());
            if (iterator.hasNext()) {
                builder.append(LocationConstants.SEPARATOR);
            }
        }
        
        this.ipcPlugin.sendMessage(builder.toString());
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
}
