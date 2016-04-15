package vg.civcraft.mc.bettershards.command.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

public class TeleportServer extends PlayerCommand{
	private MercuryManager mercManager = BetterShardsPlugin.getMercuryManager();
	private BetterShardsPlugin plugin = BetterShardsPlugin.getInstance();

	public TeleportServer(String name) {
		super(name);
		setIdentifier("ts");
		setDescription("Teleports you to another server");
		setUsage("/ts <server>\n"
				+ "/ts <server> <x> <y> <z>\n"
				+ "/ts <server> <x> <y> <z> <world>");
		setArguments(1, 5);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to execute this command. "
					+ "What do you expect, teleporting the console to a server...");
			return true;
		}
		Player p = (Player)sender;
		
		if(args.length == 1){
			serverTeleport(p, args[0]);
		} else if(args.length == 4 || args.length == 5){
			serverTeleport(p, args);
		} else { 
			return false;
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 1) {
			List<String> serversToReturn = new ArrayList<String>();
			Set<String> servers = MercuryAPI.getAllConnectedServers();
			for (String x : servers) {
				if (x.toLowerCase().startsWith(args[0].toLowerCase()))
					serversToReturn.add(x);
			}
			return serversToReturn;
		}
		return null;
	}
	
	private boolean serverTeleport(Player p, String serverName){
		for(String server : MercuryAPI.getAllConnectedServers()){
			if(server.equalsIgnoreCase(serverName)){
				try {
					BetterShardsAPI.connectPlayer(p, server, PlayerChangeServerReason.TP_COMMAND);
				} catch (PlayerStillDeadException e) {
					p.sendMessage(ChatColor.RED + "You can not switch a server when you are dead");
					plugin.info("Teleported " + p.getName() + " to server " + server);
				}
				return true;
			}
		}
		p.sendMessage(ChatColor.RED + "Sorry that server is not connected to the network.");
		return true;
	}
	
	// Format for args server, x, y, z, world
	private boolean serverTeleport(Player p, String[] args) {
		try {
			Integer.parseInt(args[1]);
			Integer.parseInt(args[2]);
			Integer.parseInt(args[3]);
		} catch(NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Please make sure you entered the cords correctly.");
			return true;
		}
		
		String serverName = args[0];
		for(String server : MercuryAPI.getAllConnectedServers()){
			if(server.equalsIgnoreCase(serverName)){
				if(args.length == 4){
					//default world
					mercManager.teleportPlayer(server ,p.getUniqueId(), args[1], args[2], args[3]);
				} else if(args.length == 5){
					mercManager.teleportPlayer(server ,p.getUniqueId(), args[1], args[2], args[3], args[4]);
				}
				
				try {
					BetterShardsAPI.connectPlayer(p, server, PlayerChangeServerReason.TP_COMMAND);
					plugin.info("Teleported " + p.getName() + " to server " + server + " at " + args[1] + "," + args[2] + "," + args[3]);
				} catch (PlayerStillDeadException e) {
					p.sendMessage(ChatColor.RED + "You can not switch a server when you are dead");
				}
			}
		}
		p.sendMessage(ChatColor.RED + "Sorry that server is not connected to the network.");
		return true;
	}
}
