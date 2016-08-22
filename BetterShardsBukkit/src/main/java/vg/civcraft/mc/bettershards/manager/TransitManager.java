package vg.civcraft.mc.bettershards.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.events.PlayerArrivedChangeServerEvent;
import vg.civcraft.mc.bettershards.events.PlayerEnsuredToTransitEvent;
import vg.civcraft.mc.bettershards.events.PlayerFailedToTransitEvent;

public class TransitManager {

	private Map<UUID, String> exitTransit;
	private Map<UUID, String> arrivalTransit;
	private Map<UUID, Long> lastTransit;

	public TransitManager() {
		exitTransit = new ConcurrentHashMap<UUID, String>();
		arrivalTransit = new ConcurrentHashMap<UUID, String>();
		lastTransit = new ConcurrentHashMap<UUID, Long>();
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
		lastTransit.put(uuid, System.currentTimeMillis());
		Bukkit.getScheduler().runTaskLater(BetterShardsPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				if (isPlayerInExitTransit(uuid)) {
					BetterShardsPlugin.getInstance().info(
							uuid + " failed to transit to " + server + ", was removed by timeout");
					PlayerFailedToTransitEvent event = new PlayerFailedToTransitEvent(uuid, server);
					Bukkit.getPluginManager().callEvent(event);
					exitTransit.remove(uuid);
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
					arrivalTransit.remove(uuid);
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
			PlayerArrivedChangeServerEvent event = new PlayerArrivedChangeServerEvent(Bukkit.getPlayer(player), arrivalTransit.get(player));
			Bukkit.getPluginManager().callEvent(event);
			arrivalTransit.remove(player);
		}
	}
	
	/**
	 * Gets a unix timestamp of when the player last left this shard to connect to another. This is reset
	 * on server restart and it might be null if the player hasnt exited from this shard during the current
	 * runtime yet
	 * 
	 * @param uuid Player to check for
	 * @return Unix timestamp of last shard switch from this shard or null if none was found
	 */
	public Long getLastTransit(UUID uuid) {
		return lastTransit.get(uuid);
	}
	
	/**
	 * Checks whether the given player has transferred to another shard from this one within the given timeframe
	 * @param player Player to check for
	 * @param milliSeconds How long ago the transfer might have been maximum (in milliseconds)
	 * @return True if the player did transfer in that timeframe, false if not
	 */
	public boolean hasTransferredRecently(UUID player, long milliSeconds) {
		Long last = lastTransit.get(player);
		if (last == null) {
			return false;
		}
		if ((System.currentTimeMillis() - last) <= milliSeconds) {
			return true;
		}
		return false;
	}	
}
