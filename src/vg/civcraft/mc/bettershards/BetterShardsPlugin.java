package vg.civcraft.mc.bettershards;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.listeners.BetterShardsListener;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.config.NameConfigListener;
import vg.civcraft.mc.namelayer.config.NameConfigManager;
import vg.civcraft.mc.namelayer.config.annotations.NameConfig;
import vg.civcraft.mc.namelayer.config.annotations.NameConfigType;

public class BetterShardsPlugin extends JavaPlugin implements NameConfigListener{

	private static BetterShardsPlugin plugin;
	private PortalsManager pm;
	private DatabaseManager db;
	private static NameConfigManager config;
	
	private List<UUID> transit = new ArrayList<UUID>();
	
	@Override
	public void onEnable(){
		plugin = this;
		db = new DatabaseManager();
		pm = new PortalsManager();
		registerListeners();
		config = NameAPI.getNameConfigManager();
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
	public void teleportPlayerToServer(Player p, String server){
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(server);
		p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
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
		
		// Register with NameLayer to utilize config annotations.
		NameAPI.getNameConfigManager().registerListener(this, db);
		NameAPI.getNameConfigManager().registerListener(this, l);
		NameAPI.getNameConfigManager().registerListener(this, this);
	}
	
	@NameConfig(name = "current_server", def = "", type = NameConfigType.String)
	public static String getCurrentServerName(){
		return config.get(plugin, "current_server").getString();
	}
	
	public PortalsManager getPortalManager(){
		return pm;
	}
}