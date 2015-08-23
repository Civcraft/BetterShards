package vg.civcraft.mc.bettershards.command.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;

public class DeletePortal extends PlayerCommand{

	private PortalsManager pm = BetterShardsAPI.getPortalsManager();
	
	public DeletePortal(String name) {
		super(name);
		setIdentifier("bsd");
		setDescription("Deletes a portal.");
		setUsage("/bsd <Portal>");
		setArguments(1,1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Must be a player to execute this command.");
			return true;
		}
		Player p = (Player) sender;
		Portal portal = pm.getPortal(args[0]);
		if (portal == null) 
			return sendPlayerMessage(p, ChatColor.RED + "That portal does not exist.", true);
		pm.deletePortal(portal);
		return sendPlayerMessage(p, ChatColor.GREEN + "Portal was successfully deleted.", true);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}

}
