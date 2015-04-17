package vg.civcraft.mc.bettershards;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.config.NameConfigListener;
import vg.civcraft.mc.namelayer.config.annotations.NameConfig;
import vg.civcraft.mc.namelayer.config.annotations.NameConfigType;

public class BetterShardsListener implements Listener, NameConfigListener{
	
	private BetterShardsPlugin plugin;
	private DatabaseManager db;
	
	public BetterShardsListener(){
		plugin = BetterShardsPlugin.getInstance();
		db = plugin.getDatabaseManager();
	}

	@NameConfig(name = "lobby", def = "false", type = NameConfigType.Bool)
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerJoinEvent(PlayerJoinEvent event){
		Player p = event.getPlayer();
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerQuitEvent(PlayerQuitEvent event){
		Player p = event.getPlayer();
		UUID uuid = NameAPI.getUUID(p.getName());
		if (plugin.isPlayerInTransit(uuid))
			return;
	}
}
