package vg.civcraft.mc.bettershards.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import vg.civcraft.mc.bettershards.misc.LocationWrapper;
import vg.civcraft.mc.bettershards.portal.Portal;

/**
 * This event is used in order to allow other plugins to hook into the load event for 
 * portals and add any data they might want to add to the portal that is being loaded.
 * @author rourke750
 *
 */
public class PortalLoadEvent extends Event{

	private static HandlerList handler = new HandlerList();

	private String name;
	private int id;
	private String serverName;
	private String partner;
	private LocationWrapper first, second;
	
	private Portal p;
	
	public PortalLoadEvent(String name, int id, String serverName, String partner,
			LocationWrapper first, LocationWrapper second) {
		this.name = name;
		this.id = id;
		this.serverName = serverName;
		this.partner = partner;
		this.first = first;
		this.second = second;
	}
	
	/**
	 * This method is what sets the portal that will then be loaded to BetterShards's 
	 * PortalManager.
	 * @param p The portal that this data represents.
	 */
	public void setPortal(Portal p) {
		this.p = p;
	}
	
	public Portal getPortal() {
		return p;
	}
	
	public String getPortalName() {
		return name;
	}
	
	/**
	 * This method represents the id that is associated with in the BetterShards Database.
	 * This is not to be confused with the plugin specific id but rather this is the
	 * int that you could get when you would call 
	 * {@link vg.civcraft.mc.bettershards.database.DatabaseManager#getPortalID(String, int)}
	 * using the specific plugin id.
	 * @return
	 */
	public int getId() {
		return id;
	}
	
	public String getServerName() {
		return serverName;
	}
	
	public String getPartnerName() {
		return partner;
	}
	
	public LocationWrapper getFirst() {
		return first;
	}
	
	public LocationWrapper getSecond() {
		return second;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handler;
	}
	
	public static HandlerList getHandlerList(){
		return handler;
	}
}
