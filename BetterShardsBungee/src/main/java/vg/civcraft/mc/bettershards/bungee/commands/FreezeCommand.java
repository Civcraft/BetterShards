package vg.civcraft.mc.bettershards.bungee.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import vg.civcraft.mc.bettershards.bungee.net.ConnectionTracker;

public class FreezeCommand extends Command {
	public FreezeCommand() {
		super("freezeplayercon", "bettershards.admin.freeze");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if ( args.length < 1 ) {
			sender.sendMessage( ChatColor.RED + "Please follow this command by a user name" );
			return;
		}
		ProxiedPlayer user = ProxyServer.getInstance().getPlayer( args[0] );
		if (user == null) {
			sender.sendMessage( ChatColor.RED + "That user is not online" );
		} else {
			Boolean isFrozen = ConnectionTracker.toggleFreeze(user);
			sender.sendMessage( ChatColor.BLUE + args[0] + " is " + isFrozen + " frozen." );
		}
	}
}
