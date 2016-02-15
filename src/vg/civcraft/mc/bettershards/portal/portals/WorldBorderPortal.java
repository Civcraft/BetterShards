package vg.civcraft.mc.bettershards.portal.portals;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.portal.Portal;

public class WorldBorderPortal extends Portal {

	private Location mapCenter;
	private LocationWrapper first;
	private LocationWrapper second;
	private double wbRange;
	private double fAngle;
	private double sAngle;
	private double arcLength;

	/**
	 * So without complication everything needlessly, note that all border begin/ends
	 * should be listed in clockwise order. If you fail to adhere to this, the border
	 * will instead be everything you meant to be outside the border.
	 */
	public WorldBorderPortal(String name, String connection,
			boolean isOnCurrentServer,
			LocationWrapper first, LocationWrapper second) {
		super(name, connection, isOnCurrentServer, 1);
		this.mapCenter = new Location(first.getFakeLocation().getWorld(), 0, 0, 0);
		this.first = first;
		this.second = second;

		double fRadius = getXZDistance(first.getFakeLocation());
		double sRadius = getXZDistance(second.getFakeLocation());
		this.wbRange = Math.min(fRadius, sRadius);

		this.fAngle = getAdjustedAngle(first.getFakeLocation());
		this.sAngle = getAdjustedAngle(second.getFakeLocation());

		this.arcLength = (fAngle == sAngle) ? 2 * Math.PI : 
				(fAngle > sAngle) ? 2 * Math.PI - fAngle + sAngle :
				sAngle - fAngle;
	}
	
	public LocationWrapper getFirst() {
		return first;
	}
	
	public LocationWrapper getSecond() {
		return second;
	}

	public boolean inPortal(Location loc) {
		double x1 = first.getFakeLocation().getX();
		double x2 = second.getFakeLocation().getX();
		double z1 = first.getFakeLocation().getZ();
		double z2 = second.getFakeLocation().getZ();
		return getXZDistance(loc) > wbRange && 
				((loc.getX() >= x1 && loc.getX() <= x2) || (loc.getX() <= x1 && loc.getX() >= x2)) && 
				((loc.getZ() >= z1 && loc.getZ() <= z2) || (loc.getZ() <= z1 && loc.getZ() >= z2));
	}

	private double getXZDistance(Location loc) {
		double x = loc.getX() - mapCenter.getX();
		double z = loc.getZ() - mapCenter.getZ();
		return Math.sqrt(x * x + z * z);
	}

	/**
	 * Strictly returns an angle in radians between -PI and PI.
	 */
	private double getAdjustedAngle(Location loc) {
		double x = loc.getX() - mapCenter.getX();
		double z = loc.getZ() - mapCenter.getZ();
		return Math.atan2(z,x);
	}
	
	/**
	 * Just returns a spawn location somewhere along the WB arc
	 */
	public Location findSpawnLocation() {
		return calculateSpawnLocation(Math.random());
	}
	
	/**
	 * For a given location, gives a value 0..1 if the location is within the arc
	 * described by this world border portal. Otherwise returns -1.0
	 */
	public double getArcPosition(Location loc) {
		double locAngle = getAdjustedAngle(loc);
		if (fAngle == sAngle) {
			return (Math.PI + locAngle) / arcLength;
		} else if ((fAngle > sAngle && (locAngle >= fAngle || locAngle <= sAngle)) || 
				(fAngle < sAngle && locAngle >= fAngle && locAngle <= sAngle)) {
			if (fAngle > sAngle && locAngle <= sAngle) {
				locAngle += 2.0*Math.PI;
			}
			return (locAngle - fAngle) / arcLength;
		}
			
		return -1.0;
	}
	
	/**
	 * Takes an arc position %, and computes a Location that falls within the arc that
	 * percentage along the arc.
	 * If a value is received > 1.0 or < 0.0, null is returned.
	 */
	public Location calculateSpawnLocation(double arcPosition) {
		if (arcPosition > 1.0 || arcPosition < 0.0) return null;
		double theta = fAngle + arcLength * arcPosition;
		double x = (wbRange-2.0) * Math.cos(theta);
		double z = (wbRange-2.0) * Math.sin(theta);
		// TODO strengthen this
		Block y = mapCenter.getWorld().getHighestBlockAt((int) x, (int) z);
		Block eyes = y.getRelative(0,1,0);
		return eyes.getLocation();
	}
	
	public void teleport(Player p) {
		if (connection == null)
			return;
		Double relativeArcPosition = getArcPosition(p.getLocation());
		if (connection.getServerName().equals(BetterShardsAPI.getServerName())) {
			p.teleport(((WorldBorderPortal)connection).calculateSpawnLocation(relativeArcPosition));
			return;
		}
		try {
			BetterShardsAPI.connectPlayer(p, connection,
					PlayerChangeServerReason.PORTAL, relativeArcPosition);
		} catch (PlayerStillDeadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
