package org.cubeville.cvlocation.commands;

import org.cubeville.cvipc.CVIPC;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class WhereAmICommand extends Command {

    private CVIPC ipc;
    
    public WhereAmICommand(CVIPC ipcIn) {
        super("whereami");
        ipc = ipcIn;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if(!(commandSender instanceof ProxiedPlayer)) { return; }
        ProxiedPlayer sender = (ProxiedPlayer) commandSender;
        if(args.length == 0) {
            String serverName = sender.getServer().getInfo().getName();
            String ipcMessage = "locationquery|" + sender.getUniqueId().toString();
            ipc.sendMessage(serverName, ipcMessage);
        }
        else {
            sender.sendMessage("§cSyntax: /whereami");
        }
    }
}
