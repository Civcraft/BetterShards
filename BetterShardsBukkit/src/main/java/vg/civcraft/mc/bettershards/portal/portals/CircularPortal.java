package vg.civcraft.mc.bettershards.portal.portals;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.portal.Portal;

public class CircularPortal extends Portal {
	private Location center;
	private double range;
	
	private Location actualFirst, actualSecond;
	
	private static int id = -1;

	protected CircularPortal() {
		
	}

	public boolean inPortal(Location loc) {
		double y1 = actualFirst.getY();
		double y2 = actualSecond.getY();
		return getXZDistance(loc) < range && ((loc.getY() >= y1 && loc.getY() <= y2) || (loc.getY() <= y1 && loc.getY() >= y2));
	}
	
	public Location getFirst() {
		return actualFirst;
	}
	
	public Location getSecond() {
		return actualSecond;
	}

	private double getXZDistance(Location loc) {
		double x = loc.getX() - center.getX();
		double z = loc.getZ() - center.getZ();
		return Math.sqrt(x * x + z * z);
	}
	
	public Location findSpawnLocation() {
		double xScale = Math.random();
		double zScale = Math.random();
		Location loc = new Location(Bukkit.getWorld(first.getActualWorld()), xScale * range + center.getX(), center.getY(), zScale * range + center.getZ());
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
			BetterShardsAPI.connectPlayer(p, 
					BetterShardsPlugin.getPortalManager().getPortal(connection),
					PlayerChangeServerReason.PORTAL, xScale, zScale);
		} catch (PlayerStillDeadException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void showParticles(Player p) {
	    Location loc = p.getLocation();
	    //- 16 so players see particles even if they are slightly out of range
	    if (getXZDistance(loc) - PARTICLE_SIGHT_RANGE < range) {
		//ensure player is in y range
		int upperBound = Math.max(first.getFakeLocation().getBlockY(), second.getFakeLocation().getBlockY());
		int lowerBound = Math.min(first.getFakeLocation().getBlockY(), second.getFakeLocation().getBlockY());
		if (upperBound + PARTICLE_SIGHT_RANGE  >= loc.getBlockY() && lowerBound - PARTICLE_SIGHT_RANGE <= loc.getBlockY()) {
		    int y;
		    if (loc.getY() >= upperBound) {
			//player is above portal
			y = upperBound;
		    }
		    else {
			if (loc.getY() <= lowerBound) {
			    //player is below
			    y = lowerBound;
			}
			else {
			    //player is inside portal? weird, but lets not worry here
			    y = loc.getBlockY();
			}
		    }
		    Location center = new Location(loc.getWorld(), loc.getBlockX(), y, loc.getBlockZ());
		    for(int x = - PARTICLE_RANGE; x <= PARTICLE_RANGE; x++) {
			for(int z = - PARTICLE_RANGE; z <= PARTICLE_RANGE; z++) {
				p.spigot().playEffect(center, Effect.FLYING_GLYPH, 0, 0, x, 0, z, 1, 3, PARTICLE_SIGHT_RANGE);
			}
		    }
		}
	    }
	}

	@Override
	public String getTypeName() {
		return "Circle";
	}

	@Override
	public void valuesPopulated() {
		if (isOnCurrentServer) {
			center = new Location(Bukkit.getWorld(first.getActualWorld()),
					(first.getFakeLocation().getX() + second.getFakeLocation().getX()) / 2,
					(first.getFakeLocation().getY() + second.getFakeLocation().getY()) / 2,
					(first.getFakeLocation().getZ() + second.getFakeLocation().getZ()) / 2);
			range = getXZDistance(first.getFakeLocation());
			actualFirst = new Location(Bukkit.getWorld(first.getActualWorld()), first.getFakeLocation().getBlockX(),
					first.getFakeLocation().getBlockY(), first.getFakeLocation().getBlockZ());
			actualSecond = new Location(Bukkit.getWorld(second.getActualWorld()), second.getFakeLocation().getBlockX(),
					second.getFakeLocation().getBlockY(), second.getFakeLocation().getBlockZ());
		}
	}

	@Override
	public int getPortalID() {
		if (id == -1) {
			id = BetterShardsPlugin.getDatabaseManager().getPortalID(BetterShardsPlugin.getInstance().getName(), 2);
		}
		return id;
	}
}
