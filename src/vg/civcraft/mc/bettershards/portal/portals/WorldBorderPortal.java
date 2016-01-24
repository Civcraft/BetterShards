package vg.civcraft.mc.bettershards.portal.portals;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.portal.Portal;

public class WorldBorderPortal extends Portal {

	private double wbRange;
	private Location mapCenter;
	private Location first;
	private Location second;

	public WorldBorderPortal(String name, Portal connection,
			boolean isOnCurrentServer, Location first, Location second) {
		super(name, connection, isOnCurrentServer);
		this.first = first;
		this.second = second;
	}

	public boolean inPortal(Location loc) {
		return getXZDistance(loc) > wbRange;
	}

	private double getXZDistance(Location loc) {
		double x = Math.abs(loc.getX() - mapCenter.getX());
		double z = Math.abs(loc.getZ() - mapCenter.getZ());
		return Math.sqrt(x * x + z * z);
	}
	
	public Location findSpawnLocation() {
		return null; //TODO
	}
	
	public double getAngle(Location loc) {
		//TODO
		
		return 0.0;
	}
	
	public Location calculateSpawnLocation(double angle) {
		return null; //TODO
	}
	
	public void teleport(Player p) {
		if (connection == null)
			return;
		Double angle = getAngle(p.getLocation());
		if (connection.getServerName().equals(BetterShardsAPI.getServerName())) {
			p.teleport(((WorldBorderPortal)connection).calculateSpawnLocation(angle));
			return;
		}
		try {
			BetterShardsAPI.connectPlayer(p, connection,
					PlayerChangeServerReason.PORTAL, angle);
		} catch (PlayerStillDeadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
