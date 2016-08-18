package vg.civcraft.mc.bettershards.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.events.PlayerEnsuredToTransitEvent;
import vg.civcraft.mc.bettershards.events.PlayerFailedToTransitEvent;

public class TransitManager {

	private Map<UUID, String> exitTransit;
	private Map<UUID, String> arrivalTransit;

	public TransitManager() {
		exitTransit = new ConcurrentHashMap<UUID, String>();
		arrivalTransit = new ConcurrentHashMap<UUID, String>();
	}

	/**
	 * Checks if a player is in any transit.
	 */
	public boolean isPlayerInTransit(UUID uuid) {
		return isPlayerInExitTransit(uuid) || isPlayerInArrivalTransit(uuid);
	}
	
	public boolean isPlayerInExitTransit(UUID uuid) {
		return exitTransit.containsKey(uuid);
	}
	
	public boolean isPlayerInArrivalTransit(UUID uuid) {
		return arrivalTransit.containsKey(uuid);
	}

	/**
	 * This adds a player to a list that can be checked to see if a player is in
	 * transit.
	 */
	public void addPlayerToExitTransit(final UUID uuid, final String server) {
		exitTransit.put(uuid, server);
		Bukkit.getScheduler().runTaskLater(BetterShardsPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				if (isPlayerInExitTransit(uuid)) {
					BetterShardsPlugin.getInstance().info(
							uuid + " failed to transit to " + server + ", was removed by timeout");
					PlayerFailedToTransitEvent event = new PlayerFailedToTransitEvent(uuid, server);
					Bukkit.getPluginManager().callEvent(event);
				}
			}

		}, 600); // 30 seconds timeout
	}
	
	public void addPlayerToArrivalTransit(final UUID uuid, final String server) {
		arrivalTransit.put(uuid, server);
		Bukkit.getScheduler().runTaskLater(BetterShardsPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				if (isPlayerInArrivalTransit(uuid)) {
					BetterShardsPlugin.getInstance().info(
							uuid + " failed to transit from " + server + ", was removed by timeout");
					PlayerFailedToTransitEvent event = new PlayerFailedToTransitEvent(uuid, server);
					Bukkit.getPluginManager().callEvent(event);
				}
			}

		}, 600); // 30 seconds timeout
	}

	/**
	 * Called by the MercuryListener when the target server of a transfer
	 * indicates that the player arrived
	 * 
	 * @param player UUID of the transferred player
	 */
	public void notifySuccessfullExit(UUID player) {
		if (isPlayerInExitTransit(player)) {
			PlayerEnsuredToTransitEvent e = new PlayerEnsuredToTransitEvent(player, exitTransit.get(player));
			Bukkit.getPluginManager().callEvent(e);
			exitTransit.remove(player);
		}
	}
	
	/**
	 * Called when a player's data is fully loaded
	 * 
	 * @param player UUID of the transferred player
	 */
	public void notifySuccessfullArrival(UUID player) {
		if (isPlayerInArrivalTransit(player)) {
			//TODO Add appropriate event here
			arrivalTransit.remove(player);
		}
	}
	
	
}
