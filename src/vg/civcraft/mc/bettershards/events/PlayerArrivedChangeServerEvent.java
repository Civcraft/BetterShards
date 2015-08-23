package vg.civcraft.mc.bettershards.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import vg.civcraft.mc.bettershards.portal.Portal;

public class PlayerArrivedChangeServerEvent extends Event{

	private static HandlerList handler = new HandlerList();
	private final Player p;
	private Portal portal;
	
	public PlayerArrivedChangeServerEvent(Player p, Portal portal) {
		this.p = p;
		this.portal = portal;
	}
	
	public Player getPlayer() {
		return p;
	}
	
	public Portal getPortal() {
		return portal;
	}
	
	/**
	 * Sets the portal the player will teleport to. If the portal is set to null
	 * then the player will not be teleported anywhere.
	 */
	public void setPortal(Portal portal) {
		this.portal = portal;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handler;
	}
	
	public static HandlerList getHandler(){
		return handler;
	}
}
