package vg.civcraft.mc.bettershards.command.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import vg.civcraft.mc.bettershards.listeners.MercuryListener;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.NameAPI;

public class Teleport extends PlayerCommand {
	
	private MercuryManager mercManager = BetterShardsPlugin.getMercuryManager();

	public Teleport(String name) {
		super(name);
		setIdentifier("tp");
		setDescription("Teleports you to a player or a specific location in the network");
		setUsage("/tp <player>\n"
				+ "/tp <x> <y> <z>\n"
				+ "/tp <x> <y> <z> <world>\n"
				+ "/tp <player> <player>\n"
				+ "/tp <player> <x> <y> <z> <world>");
		setArguments(1, 5);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender.hasPermission("BetterShards.admin") || sender.isOp())) {
			sender.sendMessage(ChatColor.RED + "You must have permission or be an admin to execute this command.");
			return true;
		}
		if (args.length == 1)
			return playerTeleport(sender, args);
		else if (args.length == 2)
			return playerToPlayerTeleport(sender, args);
		else if (args.length == 3)
			return cordsTeleport(sender, args);
		else if (args.length == 4)
			return worldTeleport(sender, args);
		else if (args.length == 5)
			return playerWorldTeleport(sender, args);
		else {
			return false;
		}
	}

	private boolean playerTeleport(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to execute this command. "
					+ "What do you expect, teleporting the console to a Location...");
			return true;
		}
		Player p = (Player)sender;
		
		//Player is on the same server
		Player other = Bukkit.getPlayer(args[0]);
		if (other != null) {
			p.teleport(other);
			return true;
		}
		
		//Player is on a different server
		UUID targetPlayerUUID = NameAPI.getUUID(args[0]);
		if(targetPlayerUUID == null){
			sender.sendMessage(ChatColor.RED + "Player does not exist.");
			return true;
		}
		
		//Get the server of the target player
		String serverName = MercuryAPI.getServerforAccount(targetPlayerUUID).getServerName();
		if(serverName == null){
			sender.sendMessage(ChatColor.RED + "Player is not online.");
			return true;
		}
		
		//Stage teleport on destination server
		mercManager.teleportPlayer(serverName, p.getUniqueId(), targetPlayerUUID);
		
		//Send the player to destination server
		try {
			BetterShardsAPI.connectPlayer(p, serverName, PlayerChangeServerReason.TP_COMMAND);
		} catch (PlayerStillDeadException e) {
			sender.sendMessage(ChatColor.RED + "Player is still dead.");
		}
	
		return true;
	}
	
	private boolean playerToPlayerTeleport(CommandSender sender, String[] args) {
		UUID PlayerUUID = NameAPI.getUUID(args[0]);
		if(PlayerUUID == null){
			sender.sendMessage(ChatColor.RED + "Player does not exist.");
			return true;
		}
		
		UUID targetPlayerUUID = NameAPI.getUUID(args[1]);
		if(targetPlayerUUID == null){
			sender.sendMessage(ChatColor.RED + "Target player does not exist.");
			return true;
		}
		
		//Get the server of the target player
		String serverName = MercuryAPI.getServerforAccount(targetPlayerUUID).getServerName();
		if(serverName == null){
			sender.sendMessage(ChatColor.RED + "Target Player is not online.");
			return true;
		}
		
		//Check if the player is on the current server
		Player player = Bukkit.getPlayer(PlayerUUID);
		if(player != null){
			playerTeleport(player, new String[]{args[1]});
			return true;
		}
		
		//check if the target player is on the current server
		Player targetPlayer = Bukkit.getPlayer(targetPlayerUUID);
		if (targetPlayer != null) {
			//Stage teleport on current server
			MercuryListener.stageTeleport(targetPlayerUUID, targetPlayer.getLocation());		
		} else {
			//Stage teleport on destination server
			mercManager.teleportPlayer(serverName, PlayerUUID, targetPlayerUUID);
		}
		
		//Send the player to destination server
		try {
			BetterShardsAPI.connectPlayer(PlayerUUID, serverName, PlayerChangeServerReason.TP_COMMAND);
		} catch (PlayerStillDeadException e) {
			sender.sendMessage(ChatColor.RED + "Player is still dead.");
		}
	
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length <= 2) {
			List<String> namesToReturn = new ArrayList<String>();
			Set<String> players = MercuryAPI.instance.getAllPlayers();
			if (args.length == 2)
				players.remove(args[0]);
			for (String x : players) {
				if (x.toLowerCase().startsWith(args[0].toLowerCase()))
					namesToReturn.add(x);
			}
			return namesToReturn;
		}
		if(args.length >= 4){
			List<String> worldsToReturn = new ArrayList<String>();
			for(World world : Bukkit.getWorlds()){
				worldsToReturn.add(world.getName());
			}
			return worldsToReturn;
		}
		return null;
	}
	
	// Format for args x, y, z
	private boolean cordsTeleport(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to execute this command. "
					+ "What do you expect, teleporting the console to a Location...");
			return true;
		}
		Player p = (Player)sender;
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
	private boolean worldTeleport(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to execute this command. "
					+ "What do you expect, teleporting the console to a Location...");
			return true;
		}
		
		Player p = (Player)sender;
		World w = Bukkit.getWorld(args[3]);
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
	
	private boolean playerWorldTeleport(CommandSender sender, String[] args) {	
		Player p = Bukkit.getPlayer(args[0]);
		if (p == null) {
			sender.sendMessage(ChatColor.RED + "Player is not online.");
			return true;
		}
		
		World w = Bukkit.getWorld(args[4]);
		if (w == null) {
			p.sendMessage(ChatColor.RED + "That world does not exist.");
			return true;
		}
		
		int x, y, z;
		try {
			x = Integer.parseInt(args[1]);
			y = Integer.parseInt(args[2]);
			z = Integer.parseInt(args[3]);
		} catch(NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Please make sure you entered the cords correctly.");
			return true;
		}
		
		Location newLoc = new Location(w, x, y, z);
		p.teleport(newLoc);
		return true;
	}
	
	// Format for args server, world, x, y, z
/*	private boolean serverTeleport(CommandSender p, String[] args) {
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
	}*/
}
