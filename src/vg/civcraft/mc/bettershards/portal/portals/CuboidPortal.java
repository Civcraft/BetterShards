package vg.civcraft.mc.bettershards.portal.portals;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.bettershards.portal.PortalType;

public class CuboidPortal extends Portal{
	public CuboidPortal(String name, Location corner, int xrange, int yrange,
			int zrange, Portal connection, boolean isOnCurrentServer) {
		super(name, corner, xrange, yrange, zrange, connection, PortalType.CUBOID, isOnCurrentServer);
	}
	
	/**
	 * This Constructor is used to gather the Portal Object for you if you don't
	 * have it already.
	 */
	public CuboidPortal(String name, Location corner, int xrange, int yrange,
			int zrange, final String con, boolean isOnCurrentServer) {
		super(name, corner, xrange, yrange, zrange, 
				null, PortalType.CUBOID, isOnCurrentServer);
		Bukkit.getScheduler().scheduleSyncDelayedTask(BetterShardsPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				setPartnerPortal(BetterShardsPlugin.getInstance().getPortalManager().getPortal(con));
			}
			
		}, 1);
	}
	
	public CuboidPortal(String name, Location corner, int xrange, int yrange,
			int zrange, boolean isOnCurrentServer) {
		super(name, corner, xrange, yrange, zrange, null, PortalType.CUBOID, isOnCurrentServer);
	}
}