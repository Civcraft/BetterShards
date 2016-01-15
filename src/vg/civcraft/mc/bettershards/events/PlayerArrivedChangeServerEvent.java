package vg.civcraft.mc.bettershards.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import vg.civcraft.mc.bettershards.portal.Portal;

public class PlayerArrivedChangeServerEvent extends Event{

	private static HandlerList handler = new HandlerList();
	private final Player p;
	private Location loc;
	
	public PlayerArrivedChangeServerEvent(Player p, Location loc) {
		this.p = p;
		this.loc = loc;
	}
	
	public Player getPlayer() {
		return p;
	}
	
	public Location getLocation() {
		return loc;
	}
	
	/**
	 * Sets the Location that the player will be teleported to.
	 */
	public void setLocation(Location loc) {
		this.loc = loc;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handler;
	}
	
	public static HandlerList getHandler(){
		return handler;
	}
}
