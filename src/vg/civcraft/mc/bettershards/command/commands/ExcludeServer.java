package vg.civcraft.mc.bettershards.command.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;

public class ExcludeServer extends PlayerCommand{

	private DatabaseManager db = BetterShardsPlugin.getInstance().getDatabaseManager();
	
	public ExcludeServer(String name) {
		super(name);
		setIdentifier("bse");
		setDescription("Used to exclude or remove exclusion of a server on first join.");
		setUsage("/bse <add/remove> <server>");
		setArguments(2,2);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to use this command.");
			return true;
		}
		Player p = (Player) sender;
		String action = args[0];
		String server = args[1];
		String message = ChatColor.RED + "You must type either add or remove to add or remove a portal.";
		if (action.equalsIgnoreCase("add")) {
			db.addExclude(server);
			message = ChatColor.GREEN + "The server has been added to exclude, please wait shortly as "
					+ "updates to this list take a little time.";
		}
		else if (action.equalsIgnoreCase("remove")) {
			db.removeExclude(server);
			message = ChatColor.GREEN + "The server has been removed drom exclude, please wait shortly as "
					+ "updates to this list take a little time.";
		}
		return sendPlayerMessage(p, message, true);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}

}
