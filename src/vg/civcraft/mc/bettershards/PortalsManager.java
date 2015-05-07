package vg.civcraft.mc.bettershards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.portal.Portal;

public class PortalsManager {

	private DatabaseManager db = BetterShardsPlugin.getInstance().getDatabaseManager();
	private Map<String, Portal> portals;
	
	public PortalsManager(){
		portals = new HashMap<String, Portal>();
	}
	
	public void createPortal(Portal portal){
		
	}
	
	public void deletePortal(Portal portal){
		
	}
	
	public Portal getPortal(String name){
		return portals.get(name);
	}
	
}
