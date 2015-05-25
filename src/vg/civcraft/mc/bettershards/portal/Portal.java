package vg.civcraft.mc.bettershards.portal;

import java.util.List;

import org.bukkit.Location;

public class Portal {
	
	protected List<Location> locs;
	protected Portal connection;
	protected String serverName;
	protected String name;
	
	public Portal(String name, List<Location> locs, Portal connection){
		this.locs = locs;
		this.connection = connection;
		this.name = name;
	}
	
	public boolean isWithinLoc(Location loc){
		return locs.contains(loc);
	}
	
	public Portal getPartnerPortal(){
		return connection;
	}
	
	public String getName(){
		return name;
	}
}
