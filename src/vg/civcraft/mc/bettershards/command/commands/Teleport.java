package vg.civcraft.mc.bettershards.command.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.mercury.MercuryAPI;

public class Teleport extends PlayerCommand {
	
	private MercuryManager mercManager = BetterShardsPlugin.getMercuryManager();

	public Teleport(String name) {
		super(name);
		setIdentifier("tp");
		setDescription("Teleports you to another server or world.");
		setUsage("/tp <x> <y> <z>\n"
				+ "/tp <world> <x> <y> <z>\n"
				+ "/tp <server> <world> <x> <y> <z>\n"
				+ "/tp <player>");
		setArguments(1, 5);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to execute this command. "
					+ "What do you expect, teleporting the console to a Location...");
		}
		Player p = (Player) sender;
		if (!(p.hasPermission("BetterShards.admin") || p.isOp())) {
			p.sendMessage(ChatColor.RED + "You must have permission or be an admin to execute this command.");
			return true;
		}
		if (args.length == 1)
			return playerTeleport(p, args);
		if (args.length == 3)
			return cordTeleport(p, args);
		else if (args.length == 4)
			return worldTeleport(p, args);
		else if (args.length == 5)
			return serverTeleport(p, args);
		else {
			p.sendMessage(ChatColor.RED + "Sorry you can't have two args.");
			return true;
		}
	}

	private boolean playerTeleport(Player p, String[] args) {
		Player other = Bukkit.getPlayer(args[0]);
		if (other == null) {
			p.sendMessage(ChatColor.RED + "That player does not exist.");
			return true;
		}
		p.teleport(other);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// Format for args x, y, z
	private boolean cordTeleport(Player p, String[] args) {
		World w = p.getLocation().getWorld();
		int x, y, z;
		try {
			x = Integer.parseInt(args[0]);
			y = Integer.parseInt(args[1]);
			z = Integer.parseInt(args[2]);
		} catch(NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Please make sure you entered the cords correctly.");
			return true;
		}
		Location newLocation = new Location(w, x, y, z);
		p.teleport(newLocation);
		return true;
	}
	
	// Format for args world, x, y, x
	private boolean worldTeleport(Player p, String[] args) {
		World w = Bukkit.getWorld(args[0]);
		if (w == null) {
			p.sendMessage(ChatColor.RED + "That world does not exist.");
			return true;
		}
		int x, y, z;
		try {
			x = Integer.parseInt(args[0]);
			y = Integer.parseInt(args[1]);
			z = Integer.parseInt(args[2]);
		} catch(NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Please make sure you entered the cords correctly.");
			return true;
		}
		Location newLoc = new Location(w, x, y, z);
		p.teleport(newLoc);
		return true;
	}
	
	// Format for args server, world, x, y, z
	private boolean serverTeleport(Player p, String[] args) {
		try {
			Integer.parseInt(args[0]);
			Integer.parseInt(args[1]);
			Integer.parseInt(args[2]);
		} catch(NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Please make sure you entered the cords correctly.");
			return true;
		}
		String server = args[0];
		if (!MercuryAPI.getAllConnectedServers().contains(server)) {
			p.sendMessage(ChatColor.RED + "Sorry that server is not connected to the network.");
			return true;
		}
		StringBuilder message = new StringBuilder();
		message.append(p.getUniqueId().toString() + " ");
		for (int x = 1; x < args.length; x++) {
			message.append(args[x] + " ");
		}
		try {
			BetterShardsAPI.connectPlayer(p, server, PlayerChangeServerReason.TP_COMMAND);
		} catch (PlayerStillDeadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mercManager.teleportPlayer(message.toString());
		return true;
	}
}
