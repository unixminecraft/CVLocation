/* 
 * This file is part of the BungeeLocation plugins for Bukkit servers and
 * BungeeCord proxies for Minecraft.
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

package org.cubeville.location.bungeecord.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import org.cubeville.location.bungeecord.BungeeLocationPlugin;
import org.bspfsystems.bungeelocation.core.IPCConstants;
import org.cubeville.cvipc.CVIPC;
import org.cubeville.cvplayerdata.CVPlayerData;
import org.cubeville.cvplayerdata.playerdata.PlayerDataManager;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a {@link Command} that allows a {@link ProxiedPlayer} to check
 * their location, or the location of another {@link ProxiedPlayer}, if they
 * have permission.
 */
public final class WhereCommand extends Command {
    
    private final ProxyServer proxy;
    private final CVIPC ipcPlugin;
    private final PlayerDataManager playerDataManager;
    
    /**
     * Constructs a new {@code /where} {@link Command}.
     * 
     * @param proxy The BungeeCord {@link ProxyServer}.
     * @param ipcPlugin The {@link CVIPC} {@link Plugin}, used to send the
     *                  location request to the Bukkit server.
     * @param playerDataPlugin The {@link CVPlayerData} {@link Plugin}, used to
     *                         verify that {@link ProxiedPlayer ProxiedPlayers}
     *                         have the ability to check the location of other
     *                         {@link ProxiedPlayer ProxiedPlayers}.
     */
    public WhereCommand(@NotNull final ProxyServer proxy, @NotNull final CVIPC ipcPlugin, @NotNull final CVPlayerData playerDataPlugin) {
        super("where", null, "whereami");
        
        this.proxy = proxy;
        this.ipcPlugin = ipcPlugin;
        this.playerDataManager = playerDataPlugin.getPlayerDataManager();
    }
    
    /**
     * Executes this {@code /where} {@link Command}.
     * 
     * @param sender The {@link CommandSender} executing this {@link Command}.
     * @param args The arguments supplied with this {@link Command}.
     */
    @Override
    public void execute(@NotNull final CommandSender sender, @NotNull final String[] args) {
        
        final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        if (sender instanceof ProxiedPlayer) {
            this.executePlayer((ProxiedPlayer) sender, argsList);
        } else {
            this.executeConsole(sender, argsList);
        }
    }
    
    /**
     * Executes this {@code /where} {@link Command} as a {@link ProxiedPlayer}.
     * 
     * @param sender The {@link ProxiedPlayer} executing this {@link Command}.
     * @param args A {@link List} of supplied {@link Command} arguments.
     */
    private void executePlayer(@NotNull final ProxiedPlayer sender, @NotNull final List<String> args) {
        
        final UUID senderId = sender.getUniqueId();
        if (args.isEmpty()) {
            this.queryLocation(sender.getServer().getInfo().getName(), senderId, senderId, false);
            return;
        }
        
        final boolean unlimited = sender.hasPermission(BungeeLocationPlugin.PERMISSION_UNLIMITED);
        final boolean limited = sender.hasPermission(BungeeLocationPlugin.PERMISSION_LIMITED);
        if (!unlimited && !limited) {
            this.sendSyntax(sender);
            return;
        }
        
        final String targetName = args.remove(0);
        if (targetName.equalsIgnoreCase(this.proxy.getConsole().getName())) {
            sender.sendMessage(new ComponentBuilder("The Console is omnipresent...").color(ChatColor.GOLD).italic(true).create());
            return;
        }
        
        final UUID targetId = this.playerDataManager.getPlayerByVisibleName(targetName);
        if (targetId == null) {
            sender.sendMessage(new ComponentBuilder(targetName).color(ChatColor.GOLD).append(" is not online.").color(ChatColor.RED).create());
            return;
        }
        
        final String displayName = this.playerDataManager.getPlayerVisibleName(targetId);
        final ProxiedPlayer target = this.proxy.getPlayer(targetId);
        if (target == null) {
            sender.sendMessage(new ComponentBuilder(displayName).color(ChatColor.GOLD).append(" is not online.").color(ChatColor.RED).create());
            return;
        }
        
        if (!unlimited && !this.playerDataManager.outranks(senderId, targetId)) {
            
            final ComponentBuilder builder = new ComponentBuilder("You do not have permission to check ").color(ChatColor.RED);
            builder.append(displayName + "'s").color(ChatColor.GOLD);
            builder.append(" location.").color(ChatColor.RED);
            sender.sendMessage(builder.create());
            return;
        }
        
        final String serverName = target.getServer().getInfo().getName();
        if (args.isEmpty()) {
            this.queryLocation(serverName, senderId, targetId, false);
            return;
        }
        
        final String regionFlag = args.remove(0);
        if (!args.isEmpty()) {
            this.sendSyntax(sender);
            return;
        }
        
        if (!regionFlag.equalsIgnoreCase("-r") && !regionFlag.equalsIgnoreCase("--regions")) {
            this.sendSyntax(sender);
            return;
        }
        
        this.queryLocation(serverName, senderId, targetId, true);
    }
    
    /**
     * Executes this {@code /where} {@link Command} as the console
     * {@link CommandSender}.
     * 
     * @param sender The console {@link CommandSender} executing this
     *               {@link Command}.
     * @param args A {@link List} of supplied {@link Command} arguments.
     */
    private void executeConsole(@NotNull final CommandSender sender, @NotNull final List<String> args) {
        
        if (args.isEmpty()) {
            this.sendSyntax(sender);
            return;
        }
        
        final String targetName = args.remove(0);
        if (targetName.equalsIgnoreCase(this.proxy.getConsole().getName())) {
            sender.sendMessage(new ComponentBuilder("I would hope you know where you are, as you are the console...").color(ChatColor.GOLD).create());
            return;
        }
        
        final UUID targetId = this.playerDataManager.getPlayerByVisibleName(targetName);
        if (targetId == null) {
            sender.sendMessage(new ComponentBuilder(targetName).color(ChatColor.GOLD).append(" is not online.").color(ChatColor.RED).create());
            return;
        }
        
        final String displayName = this.playerDataManager.getPlayerVisibleName(targetId);
        final ProxiedPlayer target = this.proxy.getPlayer(targetId);
        if (target == null) {
            sender.sendMessage(new ComponentBuilder(displayName).color(ChatColor.GOLD).append(" is not online.").color(ChatColor.RED).create());
            return;
        }
        
        final String serverName = target.getServer().getInfo().getName();
        if (args.isEmpty()) {
            this.queryLocation(serverName, targetId, false);
            return;
        }
        
        final String regionFlag = args.remove(0);
        if (!args.isEmpty()) {
            this.sendSyntax(sender);
            return;
        }
        
        if (!regionFlag.equalsIgnoreCase("-r") && !regionFlag.equalsIgnoreCase("--regions")) {
            this.sendSyntax(sender);
            return;
        }
        
        this.queryLocation(serverName, targetId, true);
    }
    
    /**
     * Sends the {@code /where} {@link Command} syntax to the given
     * {@link CommandSender}, customizing the syntax based on the sender's
     * permissions.
     * 
     * @param sender The {@link CommandSender} to send the syntax to.
     */
    private void sendSyntax(@NotNull final CommandSender sender) {
        
        final ComponentBuilder builder = new ComponentBuilder();
        builder.append("Syntax: ").color(ChatColor.RED);
        builder.append("/where").color(ChatColor.AQUA);
        
        if (!(sender instanceof ProxiedPlayer)) {
            builder.append(" <player> [-r|--regions]").color(ChatColor.GREEN);
        } else if (sender.hasPermission(BungeeLocationPlugin.PERMISSION_UNLIMITED) || sender.hasPermission(BungeeLocationPlugin.PERMISSION_LIMITED)) {
            builder.append(" [player] [-r|--regions]");
        }
        
        sender.sendMessage(builder.create());
    }
    
    /**
     * Queries the location of the {@link ProxiedPlayer} with the given target
     * {@link UUID}, returning the WorldGuard regions the player is in, if
     * requested.
     * <p>
     * This method is called by the Console {@link CommandSender}.
     * 
     * @param serverName The name of the server that the target
     *                   {@link ProxiedPlayer} is connected to.
     * @param targetId The {@link UUID} of the target {@link ProxiedPlayer}.
     * @param getRegions {@code true} if the WorldGuard regions should be
     *                   retrieved, {@code false} otherwise.
     */
    private void queryLocation(@NotNull final String serverName, @NotNull final UUID targetId, final boolean getRegions) {
        this.queryLocation(serverName, this.proxy.getConsole().getName(), targetId.toString(), getRegions);
    }
    
    /**
     * Queries the location of the {@link ProxiedPlayer} with the given target
     * {@link UUID}, returning the WorldGuard regions the player is in, if
     * requested.
     * <p>
     * This method is called by another {@link ProxiedPlayer} sender.
     * 
     * @param serverName The name of the server that the target
     *                   {@link ProxiedPlayer} is connected to.
     * @param senderId The {@link UUID} of the sending {@link ProxiedPlayer}.
     * @param targetId The {@link UUID} of the target {@link ProxiedPlayer}.
     * @param getRegions {@code true} if the WorldGuard regions should be
     *                   retrieved, {@code false} otherwise.
     */
    private void queryLocation(@NotNull final String serverName, @NotNull final UUID senderId, @NotNull final UUID targetId, final boolean getRegions) {
        this.queryLocation(serverName, senderId.toString(), targetId.toString(), getRegions);
    }
    
    /**
     * Queries the location of the {@link ProxiedPlayer} with the given target
     * {@link UUID}, returning the WorldGuard regions the player is in, if
     * 
     * @param serverName The name of the server that the target
     *                   {@link ProxiedPlayer} is connected to.
     * @param senderId The {@link String} value of the {@link CommandSender}.
     * @param targetId The {@link String} value of the {@link UUID} of the
     *                 target {@link ProxiedPlayer}.
     * @param getRegions {@code true} if the WorldGuard regions should be
     *                   retrieved, {@code false} otherwise.
     */
    private void queryLocation(@NotNull final String serverName, @NotNull final String senderId, @NotNull final String targetId, final boolean getRegions) {
        
        final StringBuilder builder = new StringBuilder();
        builder.append(IPCConstants.REQUEST_CHANNEL).append(IPCConstants.SEPARATOR);
        builder.append(senderId).append(IPCConstants.SEPARATOR);
        builder.append(targetId).append(IPCConstants.SEPARATOR);
        builder.append(getRegions);
        
        this.ipcPlugin.sendMessage(serverName, builder.toString());
    }
}
