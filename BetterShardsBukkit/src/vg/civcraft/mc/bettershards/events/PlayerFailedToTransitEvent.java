package vg.civcraft.mc.bettershards.events;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
	
	public String getServer() {
		return server;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handler;
	}
	
	public static HandlerList getHandler(){
		return handler;
	}

}
