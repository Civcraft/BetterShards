package vg.civcraft.mc.bettershards.command.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;

public class RemovePortal extends PlayerCommand {

	private PortalsManager pm = BetterShardsAPI.getPortalsManager();
	
	public RemovePortal(String name) {
		super(name);
		setIdentifier("bsr");
		setDescription("Removes an association portal from a portal.");
		setUsage("/bsr <Portal>");
		setArguments(1,1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to execute the command.");
			return true;
		}
		Player p = (Player) sender;
		Portal portal = pm.getPortal(args[0]);
		if (portal == null)
			return sendPlayerMessage(p, ChatColor.RED + "That portal does not exist.", true);
		if (!portal.isOnCurrentServer())
			return sendPlayerMessage(p, ChatColor.RED + "You are not on the server with that portal.", true);
		portal.setPartnerPortal(null);
		return sendPlayerMessage(p, ChatColor.GREEN + "You have removed the partner portal.", true);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}

}
