package vg.civcraft.mc.bettershards.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a player has fully arrived on a shard after leaving a different one and his player data has been fully loaded.
 * This event will not be called if the player directly logged into the shard (because he was logged out there)
 *
 */
public class PlayerArrivedChangeServerEvent extends Event{

	private static HandlerList handler = new HandlerList();
	private final Player p;
	private final String originServer;
	
	public PlayerArrivedChangeServerEvent(Player p, String originServer) {
		this.p = p;
		this.originServer = originServer;
	}
	
	public Player getPlayer() {
		return p;
	}
	
	public String getOriginServer() {
		return originServer;
	}
	
	
	@Override
	public HandlerList getHandlers() {
		return handler;
	}
	
	public static HandlerList getHandlerList(){
		return handler;
	}
}
