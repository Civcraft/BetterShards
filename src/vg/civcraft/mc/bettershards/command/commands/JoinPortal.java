package vg.civcraft.mc.bettershards.command.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;

public class JoinPortal extends PlayerCommand {

	private PortalsManager pm = BetterShardsAPI.getPortalsManager();
	
	public JoinPortal(String name) {
		super(name);
		setIdentifier("bsj");
		setDescription("Joins the main Portal to the secondary Portal.");
		setUsage("/bsj <main portal> <connection>");
		setArguments(2,2);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Must be a player to execute this command.");
			return true;
		}
		Player p = (Player) sender;
		Portal one = pm.getPortal(args[0]);
		Portal two = pm.getPortal(args[1]);
		if (one == null)
			return sendPlayerMessage(p, ChatColor.RED + "The first portal does not exist.", true);
		if (two == null)
			return sendPlayerMessage(p, ChatColor.RED + "The second portal does not exist.", true);
		one.setPartnerPortal(two);
		String m = "%s has been set as Portal %s partner.";
		sender.sendMessage(ChatColor.GREEN + String.format(m, two.getName(), one.getName()));
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}

}
