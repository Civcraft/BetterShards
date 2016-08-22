package vg.civcraft.mc.bettershards.events;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called after a player left this shard to go to another one and the target shard doesn't confirm within the timeout (30 seconds) 
 * that the player successfully arrived. Assume the worst if this happens.
 *
 */
public class PlayerFailedToTransitEvent extends Event{

	private static HandlerList handler = new HandlerList();
	private UUID uuid;
	private String server;
	
	public PlayerFailedToTransitEvent(UUID uuid, String server) {
		this.uuid = uuid;
		this.server = server;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public String getTargetServer() {
		return server;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handler;
	}
	
	public static HandlerList getHandlerList(){
		return handler;
	}

}
