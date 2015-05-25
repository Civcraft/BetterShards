package vg.civcraft.mc.bettershards.portal.portals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;

import vg.civcraft.mc.bettershards.portal.Portal;

public class NetherPortal extends Portal{

	public NetherPortal(String name, List<Location> locs, Portal connection) {
		super(name, locs, connection);
	}

	/**
	 * Randomly generates a valid spawn location for a portal, spices it up a little by being random.
	 */
	public Location findValidSpawnLocation(){
		List<Location> spawns = new ArrayList<Location>();
		for (Location loc: locs){
			if (spawns.isEmpty()){
				spawns.add(loc);
			}
			else if (loc.getBlockY() == spawns.get(0).getBlockY()){
				spawns.add(loc);
			}
			else if (loc.getBlockY() < spawns.get(0).getBlockY()){
				spawns.clear();
				spawns.add(loc);
			}
		}
		Random rand = new Random();
		return spawns.get(rand.nextInt(spawns.size()));
	}
}
