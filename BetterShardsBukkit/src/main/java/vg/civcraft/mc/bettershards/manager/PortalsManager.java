package vg.civcraft.mc.bettershards.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.portal.Portal;

public class PortalsManager {

	private DatabaseManager db = BetterShardsPlugin.getDatabaseManager();
	private Map<String, Portal> portals;
	private Map<UUID, Long> arrivedPlayers = new ConcurrentHashMap<UUID,Long>();
	private final long portalCoolDown = 10000L;  //10 seconds
	
	public PortalsManager() {
		super();
		portals = new HashMap<String, Portal>();
		registerParticleRunnable();
	}
	
	public void loadPortalsManager() {
		loadPortalsFromServer();
		removeTeleportedPlayers();
		autoSaveTimer();
	}
	
	public void createPortal(Portal portal){
		portals.put(portal.getName(), portal);
		db.addPortal(portal);
		db.addPortalData(portal, null); // At this point it won't have a connection
	}
	
	public void deletePortal(Portal portal) {
		deletePortalLocally(portal);
		db.removePortalData(portal);
		db.removePortalLoc(portal);
		MercuryManager.sendPortalDelete(portal.getName());
	}
	
	public void deletePortalLocally(Portal portal) {
		List <Portal> toRemove = new LinkedList<Portal>();
		for(Entry <String, Portal> entry : portals.entrySet()) {
			if (entry.getValue().getPartnerPortal() == portal) {
				toRemove.add(entry.getValue());
			}
		}
		for(Portal p : toRemove) {
			p.setPartnerPortal(null);
		}
		portals.remove(portal.getName());
	}
	
	/*
	 * It is possible there may be one or more portals in an area but that would
	 * be stupid of admin to do as each player can only go to one server.
	 * So instead if it just finds one valid location what ever it may be
	 * that is the portal you will get.
	 */
	public Portal getPortal(Location loc){
		for(Portal p : portals.values()) {
			if (p.isOnCurrentServer() && p.inPortal(loc)) {
				return p;
			}
		}
		return null; // Like the evil that is nothingness.
	}
	
	public Portal getPortal(String name){
		Portal p = portals.get(name);
		if (p == null)
			p = db.getPortal(name);
		if (p == null)
			return null;
		portals.put(name, p);
		return p;
	}
	
	/** 
	 * Only loads portals from this server
	 */
	public void loadPortalsFromServer(){
		List<World> worlds = Bukkit.getWorlds();
		List<Portal> portals = db.getAllPortalsByWorld(worlds.toArray(new World[worlds.size()]));
		for (Portal p: portals) {
			this.portals.put(p.getName(), p);
		}
	}
	
	private void removeTeleportedPlayers(){
		Bukkit.getScheduler().scheduleSyncRepeatingTask(BetterShardsPlugin.getInstance(), 
				new Runnable() {

					@Override
					public void run() {
						List<UUID> toRemove = new LinkedList<UUID>();
						long currTime = System.currentTimeMillis();
						for (Entry <UUID, Long> entry: arrivedPlayers.entrySet()){
							Player p = Bukkit.getPlayer(entry.getKey());
							if (p == null) {
								continue;
							}
							if (getPortal(p.getLocation()) != null) {
								//the player is still in a portal
								continue;
							}
							if ((currTime - entry.getValue()) > portalCoolDown) {
								toRemove.add(entry.getKey());
							}
						}
						for(UUID rem : toRemove) {
							arrivedPlayers.remove(rem);
						}
					}	
		}, 5, 5);
	}
	
	public boolean canTransferPlayer(Player p) {
		if (BetterShardsPlugin.getCombatTagManager().isInCombatTag(p)){
			return false;
		} else{
			return !arrivedPlayers.containsKey(p.getUniqueId());
		}
	}
	
	public void addArrivedPlayer(Player p) {
		arrivedPlayers.put(p.getUniqueId(), System.currentTimeMillis());
	}
	
	// We want it sync incase a mercury message comes through we don't want it to override the db before 
	// mercury gets a chance to update the portal.
	private void autoSaveTimer() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(BetterShardsPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				Collection<Portal> ports = portals.values();
				for (Portal p: ports) {
					if (p.isDirty()) {
						db.updatePortalData(p);
						p.setDirty(false);
					}
				}
			}
			
		}, 500, 1000);
	}
	
	private void registerParticleRunnable() {
	    Bukkit.getScheduler().scheduleSyncRepeatingTask(BetterShardsPlugin.getInstance(), new Runnable() {
	        
	        @Override
	        public void run() {
	    	for(Portal portal : portals.values()) {
	    	    if (!portal.isOnCurrentServer() || portal.getPartnerPortal() == null) {
	    	    	continue;
	    	    }
	    	    for(Player p : Bukkit.getOnlinePlayers()) {
	    	    	portal.showParticles(p);
	    	    }
	    	}
	        }
	    }, 4L, 4L);
	}
}
