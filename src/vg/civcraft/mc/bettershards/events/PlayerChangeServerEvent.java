package vg.civcraft.mc.bettershards.events;

import java.util.UUID;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerChangeServerEvent extends Event implements Cancellable{

	private static HandlerList handler = new HandlerList();
	private boolean cancelled = false;
	private PlayerChangeServerReason reason;
	private UUID uuid;
	private String toServer;
	
	public PlayerChangeServerEvent(PlayerChangeServerReason reason, UUID uuid, String toServer){
		this.reason = reason;
		this.uuid = uuid;
		this.toServer = toServer;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean value) {
		cancelled = value;
	}

	@Override
	public HandlerList getHandlers() {
		return handler;
	}
	
	public static HandlerList getHandler(){
		return handler;
	}
	
	public PlayerChangeServerReason getReason(){
		return reason;
	}
	
	public UUID getPlayerUUID(){
		return uuid;
	}
	
	public String getServerTravelingTo(){
		return toServer;
	}
}
