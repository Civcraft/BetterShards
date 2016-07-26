package vg.civcraft.mc.bettershards.portal.portals;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.portal.Portal;

public class CircularPortal extends Portal {
	private Location first, second, center;
	private double range;

	public CircularPortal(String name, final String con,
			boolean isOnCurrentServer, Location first,
			Location second) {
		super(name, con, isOnCurrentServer, 2);
		this.first = first;
		this.second = second;
		if (isOnCurrentServer) {
			center = new Location(first.getWorld(),
					(first.getX() + second.getX()) / 2,
					(first.getY() + second.getY()) / 2,
					(first.getZ() + second.getZ()) / 2);
			range = getXZDistance(first);
		}
	}

	public boolean inPortal(Location loc) {
		double y1 = first.getY();
		double y2 = second.getY();
		return getXZDistance(loc) < range && ((loc.getY() >= y1 && loc.getY() <= y2) || (loc.getY() <= y1 && loc.getY() >= y2));
	}
	
	public Location getFirst() {
		return first;
	}
	
	public Location getSecond() {
		return second;
	}

	private double getXZDistance(Location loc) {
		double x = loc.getX() - center.getX();
		double z = loc.getZ() - center.getZ();
		return Math.sqrt(x * x + z * z);
	}
	
	public Location findSpawnLocation() {
		double xScale = Math.random();
		double zScale = Math.random();
		Location loc = new Location(first.getWorld(), xScale * range + center.getX(), center.getY(), zScale * range + center.getZ());
		if (!inPortal(loc)) {
			//could be in the edges outside the circle
			return findSpawnLocation();
		}
		return loc;
	}
	
	public Location calculateLocation(double xScale, double zScale) {
		double x = (xScale * range) + center.getX();
		double z = (zScale * range) + center.getZ();
		return new Location(center.getWorld(), x, center.getY(), z);
	}
	
	public void teleport(Player p) {
		Location loc = p.getLocation();
		Double xScale = (loc.getX() - center.getX()) / range;
		Double zScale = (loc.getZ() - center.getZ()) / range;
		try {
			BetterShardsAPI.connectPlayer(p, connection,
					PlayerChangeServerReason.PORTAL, xScale, zScale);
		} catch (PlayerStillDeadException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<Location> getLocationsInPortal(Chunk chunk) {
		if (!isOnCurrentServer())
			return null;
		List<Location> locs = new ArrayList<Location>();
		int chx = chunk.getX();
		int chz = chunk.getZ();
		
		int fchx = first.getChunk().getX();
		int fchz = first.getChunk().getZ();
		
		int schx = second.getChunk().getX();
		int schz = second.getChunk().getZ();
		
		if (!((fchx >= chx && chx <= schx) || (schx >= chx && chx <= fchx)))
			return null; // No x similarities.
		if (!((fchz >= chz && chz <= schz) || (schz >= chz && chz <= fchz)))
			return null; // No z similarities.

		double y1 = first.getY();
		double y2 = second.getY();
		for (int x = center.getBlockX() - (int)range; x < center.getBlockX() + (int)range; x++) {
			for (int y = center.getBlockY() - (int)range; 
					(y >= y1 && y <= y2) || (y <= y1 && y >= y2); y++) {
				for (int z = center.getBlockZ() - (int)range; z < center.getBlockZ() + (int)range; z++) {
					locs.add(new Location(center.getWorld(), x, y, z));
				}
			}
		}
		return locs.size() == 0 ? null : locs;
	}
}
