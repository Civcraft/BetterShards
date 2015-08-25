package vg.civcraft.mc.bettershards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerEvent;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.listeners.BetterShardsListener;
import vg.civcraft.mc.bettershards.misc.Grid;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.Config;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.MercuryPlugin;
import vg.civcraft.mc.mercury.config.MercuryConfigManager;

public class BetterShardsPlugin extends ACivMod{

	private static BetterShardsPlugin plugin;
	private PortalsManager pm;
	private DatabaseManager db;
	private static Config config;
	private static String servName;

	private Map<Player, Grid> grids = new HashMap<Player, Grid>();
	
	private List<UUID> transit = new ArrayList<UUID>();
	
	@Override
	public void onEnable(){
		plugin = this;
		config = GetConfig();
		servName = MercuryConfigManager.getServerName();
		db = new DatabaseManager();
		pm = new PortalsManager();
		pm.loadPortalsManager();
		registerListeners();
		registerMercuryChannels();
	}
	
	@Override
	public void onDisable(){
		
	}
	/**
	 * @return Returns the instance of the JavaPlugin.
	 */
	public static BetterShardsPlugin getInstance(){
		return plugin;
	}
	/**
	 * This adds a player to a list that can be checked to see if a player is in transit.
	 */
	public void addPlayerToTransit(final UUID uuid){
		transit.add(uuid);
		Bukkit.getScheduler().runTaskLater(this, new Runnable(){

			@Override
			public void run() {
				transit.remove(uuid);
			}
			
		}, 20);
	}
	/**
	 * Checks if a player is in transit.
	 */
	public boolean isPlayerInTransit(UUID uuid){
		return transit.contains(uuid);
	}
	/**
	 * Teleports a player to a specific server.
	 * @param p- The Player to teleport.
	 * @param server- The server to teleport the player to.
	 */
	public boolean teleportPlayerToServer(Player p, String server, PlayerChangeServerReason reason){
		PlayerChangeServerEvent event = new PlayerChangeServerEvent(reason, p.getUniqueId(), server);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return false;
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(server);
		p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
		return true;
	}
	
	public DatabaseManager getDatabaseManager(){
		return db;
	}
	
	private void registerListeners(){
		// Register bukkit channel
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		// Register Bukkit Event Listener.
		BetterShardsListener l = new BetterShardsListener();
		getServer().getPluginManager().registerEvents(l, this);
	}
	
	public static String getCurrentServerName(){
		return servName;
	}
	
	public PortalsManager getPortalManager(){
		return pm;
	}

	@Override
	protected String getPluginName() {
		return "BetterShardsPlugin";
	}
	/*
	 * This will always return a grid object for the player.
	 */
	public Grid getPlayerGrid(Player p) {
		Grid g = grids.get(p);
		if (g == null) {
			g = new Grid(p, null, null);
			grids.put(p, g);
		}
		return g;
	}
	
	public void sendPortalDelete(String name) {
		MercuryAPI.instance.sendMessage("all", "delete " + name, "BetterShards");
	}
	
	private void registerMercuryChannels() {
		MercuryAPI.instance.registerPluginMessageChannel("BetterShardsPlugin");
	}
}