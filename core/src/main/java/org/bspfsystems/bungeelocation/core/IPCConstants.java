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

package org.bspfsystems.bungeelocation.core;

/**
 * This class provides constants for the IPC communications for the
 * BungeeLocations plugin set.
 */
public final class IPCConstants {
    
    /**
     * The separator to use between data items in an IPC message.
     */
    public static final String SEPARATOR = "|";
    
    /**
     * The IPC channel used for location requests.
     */
    public static final String REQUEST_CHANNEL = "locationrequest";
    
    /**
     * The IPC channel used for location responses.
     */
    public static final String RESPONSE_CHANNEL = "locationresponse";
    
    /**
     * The unknown region list, used if the Bukkit plugin cannot determine what
     * WorldGuard regions the target player is standing in.
     */
    public static final String REGIONS_UNKNOWN = "REGIONS_UNKNOWN";
    
    /**
     * Prevents instantiation of this utility class.
     */
    private IPCConstants() {
        // Do nothing.
    }
}
