package vg.civcraft.mc.bettershards.events;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerEnsuredToTransitEvent extends Event{

	private static HandlerList handler = new HandlerList();
	private UUID uuid;
	private String server;
	
	public PlayerEnsuredToTransitEvent(UUID uuid, String server) {
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
	
	public static HandlerList getHandlerList(){
		return handler;
	}

}
