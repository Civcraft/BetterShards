package vg.civcraft.mc.bettershards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.bettershards.portal.portals.CuboidPortal;
import vg.civcraft.mc.civmodcore.locations.QTBox;
import vg.civcraft.mc.civmodcore.locations.SparseQuadTree;

public class PortalsManager extends SparseQuadTree{

	private DatabaseManager db = BetterShardsPlugin.getInstance().getDatabaseManager();
	private Map<String, Portal> portals;
	private List<Player> arrivedPlayers = new ArrayList<Player>();
	
	public PortalsManager() {
		super();
		portals = new HashMap<String, Portal>();
	}
	
	public void loadPortalsManager() {
		loadPortalsFromServer();
		removeTeleportedPlayers();
	}
	
	public void createPortal(Portal portal){
		portals.put(portal.getName(), portal);
		add(portal);
		db.addPortal(portal);
		db.addPortalData(portal, null); // At this point it won't have a connection
	}
	
	public void deletePortal(Portal portal){
		remove(portal);
		portals.remove(portal);
		db.removePortalData(portal);
		db.removePortalLoc(portal);
		BetterShardsPlugin.getInstance().sendPortalDelete(portal.getName());
	}
	
	/*
	 * It is possible there may be one or more portals in an area but that would
	 * be stupid of admin to do as each player can only go to one server.
	 * So instead if it just finds one valid location what ever it may be
	 * that is the portal you will get.
	 */
	public Portal getPortal(Location loc){
		Set<QTBox> portals = find(loc.getBlockX(), loc.getBlockZ());
		for (QTBox box: portals){
			if (!(box instanceof Portal))
				continue;
			CuboidPortal portal = (CuboidPortal) box;
			if (portal.isValidY(loc.getBlockY()))
				return portal;
		}
		return null; // Like the evil that is nothingness.
	}
	
	public Portal getPortal(String name){
		Portal p = portals.get(name);
		if (p == null)
			p = db.getPortal(name);
		portals.put(name, p);
		return p;
	}
	
	/** 
	 * Only loads portals from this server
	 */
	public void loadPortalsFromServer(){
		List<World> worlds = Bukkit.getWorlds();
		List<Portal> portals = db.getAllPortalsByWorld((World[]) worlds.toArray());
		for (Portal p: portals) {
			this.portals.put(p.getName(), p);
			add(p);
		}
	}
	
	private void removeTeleportedPlayers(){
		Bukkit.getScheduler().scheduleSyncRepeatingTask(BetterShardsPlugin.getInstance(), 
				new Runnable() {

					@Override
					public void run() {
						List<Player> toRemove = new ArrayList<Player>();
						for (Player p: arrivedPlayers){
							if (!p.isOnline()) {
								toRemove.add(p);
								continue;
							}
							Location loc = p.getLocation();
							if (getPortal(loc) != null)
								continue;
							toRemove.add(p);
						}
						arrivedPlayers.removeAll(toRemove);
					}
					
		}, 100, 20);
	}
	
	public boolean canTransferPlayer(Player p) {
		return !arrivedPlayers.contains(p);
	}
	
	public void addArrivedPlayer(Player p) {
		arrivedPlayers.add(p);
	}
}
