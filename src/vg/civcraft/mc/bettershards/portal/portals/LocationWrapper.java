package vg.civcraft.mc.bettershards.portal.portals;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationWrapper {

	private Location loc;
	private String world;
	
	public LocationWrapper(String world, int x, int y, int z) {
		loc = new Location(Bukkit.getWorlds().get(0),x,y,z);
		this.world = world;
	}
	
	public LocationWrapper(Location loc) {
		this.world = loc.getWorld().getName();
		this.loc = loc;
	}
	
	public String getActualWorld() {
		return world;
	}
	
	public Location getFakeLocation() {
		return loc;
	}
}
