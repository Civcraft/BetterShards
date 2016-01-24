package vg.civcraft.mc.bettershards.portal.portals;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.portal.Portal;

public class CuboidPortal extends Portal {

	protected Location corner; // This should be the location of the first block
	// identified
	protected int xrange, yrange, zrange;

	public CuboidPortal(String name, Location corner, int xrange, int yrange,
			int zrange, Portal connection, boolean isOnCurrentServer) {
		super(name, connection, isOnCurrentServer);
		this.corner = corner;
		this.xrange = xrange;
		this.yrange = yrange;
		this.zrange = zrange;

		/*
		 * Bukkit.getScheduler().scheduleSyncDelayedTask(BetterShardsPlugin.
		 * getInstance(), new Runnable() {
		 * 
		 * @Override public void run() {
		 * setPartnerPortal(BetterShardsPlugin.getInstance
		 * ().getPortalManager().getPortal(con)); }
		 * 
		 * }, 1);
		 */
	}

	public int qtXMax() {
		if (xrange > 0)
			return corner.getBlockX() + xrange;
		return corner.getBlockX();
	}

	public int qtXMid() {
		return (qtXMax() + qtXMin()) / 2;
	}

	public int qtXMin() {
		if (xrange < 0)
			return corner.getBlockX() + xrange;
		return corner.getBlockX();
	}

	public int qtZMax() {
		if (zrange > 0)
			return corner.getBlockZ() + zrange;
		return corner.getBlockZ();
	}

	public int qtZMid() {
		return (qtZMax() + qtZMin()) / 2;
	}

	public int qtZMin() {
		if (zrange < 0)
			return corner.getBlockZ() + zrange;
		return corner.getBlockZ();
	}

	public boolean isValidY(int y) {
		if (yrange < 0)
			return corner.getBlockY() + yrange <= y && corner.getBlockY() >= y;
		return corner.getBlockY() + yrange >= y && corner.getBlockY() <= y;
	}

	public boolean inPortal(Location loc) {
		return rangeCheck(corner.getBlockX(), xrange, loc.getBlockX())
				&& rangeCheck(corner.getBlockY(), yrange, loc.getBlockY())
				&& rangeCheck(corner.getBlockZ(), zrange, loc.getBlockZ());
	}

	private boolean rangeCheck(int center, int range, int checkValue) {
		if (checkValue >= center) {
			return range >= 0 && (center + range) >= checkValue;
		}
		return range < 0 && (center + range) <= checkValue;
	}

	// *------------------------------------------------------------------------------------------------------------*
	// | The following chooseSpawn method contains code made by NuclearW |
	// | based on his SpawnArea plugin: |
	// |
	// http://forums.bukkit.org/threads/tp-spawnarea-v0-1-spawns-targetPlayers-in-a-set-area-randomly-1060.20408/
	// |
	// *------------------------------------------------------------------------------------------------------------*
	public Location findSpawnLocation() {
		double xrand = 0;
		double zrand = 0;
		int tries = 0;
		double y = -1;
		do {

			xrand = qtXMin() + Math.random() * Math.abs(qtXMax() - qtXMin());
			zrand = qtZMin() + Math.random() * Math.abs(qtZMax() - qtZMin());
			y = getValidHighestY(corner.getWorld(), xrand, zrand);
			tries++;

		} while (y == -1 && tries < 100);

		if (y == -1) {
			// still not found
			for (y = 255; y > 0; y--) {
				if (validSpawn(corner.getWorld(), xrand, y, zrand)) {
					break;
				}
			}
		}
		if (y == 0) {
			y = 255;
		}
		Location location = new Location(corner.getWorld(), xrand, y, zrand);

		return location;
	}

	@SuppressWarnings("deprecation")
	private double getValidHighestY(World world, double x, double z) {
		world.getChunkAt(new Location(world, x, 0, z)).load();
		double y = corner.getBlockY() + Math.abs(Math.random() * (yrange));
		y = (double) ((int) y); // round it
		return validSpawn(world, x, y, z) ? y : -1;
	}

	private boolean validSpawn(World world, double x, double y, double z) {
		return world.getBlockAt(new Location(world, x, y - 1, z)).getType()
				.isSolid()
				&& world.getBlockAt(new Location(world, x, y, z)).getType() == Material.AIR
				&& world.getBlockAt(new Location(world, x, y + 1, z)).getType() == Material.AIR;
	}

	public int getXRange() {
		return xrange;
	}

	public int getYRange() {
		return yrange;
	}

	public int getZRange() {
		return zrange;
	}

	public Location getCornerBlockLocation() {
		return corner;
	}
	
	public void teleport(Player p) {
		if (connection == null)
			return;
		if (connection.getServerName().equals(BetterShardsAPI.getServerName())) {
			p.teleport(connection.findSpawnLocation());
			return;
		}
		try {
			BetterShardsAPI.connectPlayer(p, connection,
					PlayerChangeServerReason.PORTAL);
		} catch (PlayerStillDeadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
