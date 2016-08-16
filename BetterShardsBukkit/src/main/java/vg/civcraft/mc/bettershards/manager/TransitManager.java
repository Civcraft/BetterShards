package vg.civcraft.mc.bettershards.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.events.PlayerEnsuredToTransitEvent;
import vg.civcraft.mc.bettershards.events.PlayerFailedToTransitEvent;

public class TransitManager {
	
	
	private Map<UUID, String> exitTransit = new ConcurrentHashMap<UUID, String>();

	/**
	 * Checks if a player is in transit.
	 */
	public boolean isPlayerInTransit(UUID uuid){
		return exitTransit.containsKey(uuid);
	}
	
	/**
	 * This adds a player to a list that can be checked to see if a player is in transit.
	 */
	public void addPlayerToTransit(final UUID uuid, final String server){
		exitTransit.put(uuid, server);
		Bukkit.getScheduler().runTaskLater(BetterShardsPlugin.getInstance(), new Runnable(){

			@Override
			public void run() {
				if (isPlayerInTransit(uuid)) {
					BetterShardsPlugin.getInstance().info(uuid + " failed to transit to " + server + ", was removed by timeout");
					PlayerFailedToTransitEvent event = new PlayerFailedToTransitEvent(uuid, server);
					Bukkit.getPluginManager().callEvent(event);
				}
			}
			
		}, 600); //30 seconds timeout
	}
	
	public void notifySuccessfullTransfer(UUID player) {
		if (isPlayerInTransit(player)) {
			PlayerEnsuredToTransitEvent e = new PlayerEnsuredToTransitEvent(player, exitTransit.get(player));
			Bukkit.getPluginManager().callEvent(e);
			exitTransit.remove(player);
		}
	}
}
