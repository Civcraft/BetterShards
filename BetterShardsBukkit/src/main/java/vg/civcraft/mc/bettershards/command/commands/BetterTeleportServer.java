package vg.civcraft.mc.bettershards.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.mercury.MercuryAPI;

public class BetterTeleportServer extends PlayerCommand {
	
	private BetterShardsPlugin plugin = BetterShardsPlugin.getInstance();

	public BetterTeleportServer(String name) {
		super(name);
		setIdentifier("bts");
		setDescription("Teleports someone to another, better, server");
		setUsage("/bts <player> <server> <x> <y> <z> [<world>]");
		setArguments(5, 6);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (args.length < 5) {
			return false;
		}

		Player p = null;
		final String nameOrAcct = args[0];

		try {
			UUID acctId = UUID.fromString(nameOrAcct);
			if (acctId != null) {
				p = Bukkit.getPlayer(acctId);
			}
		} catch (Exception ex) {}

		if (p == null) {
			p = Bukkit.getPlayer(nameOrAcct);
		}

		if (p == null) {
			sender.sendMessage("Player not found");
			return true;
		}

		String[] tpArgs = Arrays.copyOfRange(args, 1, args.length);
		return serverTeleport(sender, p, tpArgs);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 2) {
			List<String> serversToReturn = new ArrayList<String>();
			Set<String> servers = MercuryAPI.getAllConnectedServers();
			for (String x : servers) {
				if (x.toLowerCase().startsWith(args[1].toLowerCase()))
					serversToReturn.add(x);
			}
			return serversToReturn;
		}
		return null;
	}

	// Format for args server, x, y, z, world
	private boolean serverTeleport(CommandSender sender, Player p, String[] args) {
		try {
			Integer.parseInt(args[1]);
			Integer.parseInt(args[2]);
			Integer.parseInt(args[3]);
		} catch(NumberFormatException e) {
			sender.sendMessage(ChatColor.RED + "Please make sure you entered the cords correctly.");
			return true;
		}
		String serverName = args[0];
		for(String server : MercuryAPI.getAllConnectedServers()){
			if(server.equalsIgnoreCase(serverName)){
				if(args.length == 4){
					//default world
					MercuryManager.teleportPlayer(server ,p.getUniqueId(), args[1], args[2], args[3]);
				} else if(args.length == 5){
					MercuryManager.teleportPlayer(server ,p.getUniqueId(), args[1], args[2], args[3], args[4]);
				}
				try {
					BetterShardsAPI.connectPlayer(p, server, PlayerChangeServerReason.TP_COMMAND);
					plugin.info("Teleported " + p.getName() + " to server " + server + " at " + args[1] + "," + args[2] + "," + args[3]);
				} catch (PlayerStillDeadException e) {
					sender.sendMessage(ChatColor.RED + "You can not switch a server when you are dead");
					p.sendMessage(ChatColor.RED + "You can not switch a server when you are dead");
				}
				return true;
			}
		}
		sender.sendMessage(ChatColor.RED + "Sorry that server is not connected to the network.");
		p.sendMessage(ChatColor.RED + "Sorry that server is not connected to the network.");
		return true;
	}
}
