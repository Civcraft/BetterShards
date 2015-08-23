package vg.civcraft.mc.bettershards.portal;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.civmodcore.locations.QTBox;

public class Portal implements QTBox {

	protected Location corner; // This should be the location of the first block
								// identified
	protected int xrange, yrange, zrange;
	protected Portal connection;
	protected String serverName;
	protected String name;
	protected PortalType type;
	private boolean isOnCurrentServer; // Set to false if not on current server
	protected DatabaseManager db;

	public Portal(String name, Location corner, int xrange, int yrange,
			int zrange, Portal connection, PortalType type, boolean isOnCurrentServer) {
		this.corner = corner;
		this.xrange = xrange;
		this.yrange = yrange;
		this.zrange = zrange;
		this.connection = connection;
		this.name = name;
		this.type = type;
		this.isOnCurrentServer = isOnCurrentServer;
		db = BetterShardsPlugin.getInstance().getDatabaseManager();
	}

	public Portal getPartnerPortal() {
		return connection;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
		db.updatePortalData(this);
	}

	public void setPartnerPortal(Portal connection) {
		this.connection = connection;
		db.updatePortalData(this);
	}

	public PortalType getType() {
		return type;
	}

	public String getServerName() {
		return serverName;
	}
	
	public void setServerName(String serverName) {
		this.serverName = serverName;
		db.updatePortalData(this);
	}

	@Override
	public int qtXMax() {
		if (xrange > 0)
			return corner.getBlockX() + xrange;
		return corner.getBlockX();
	}

	@Override
	public int qtXMid() {
		return (qtXMax() + qtXMin()) / 2;
	}

	@Override
	public int qtXMin() {
		if (xrange < 0)
			return corner.getBlockX() - xrange;
		return corner.getBlockX();
	}

	@Override
	public int qtZMax() {
		if (zrange > 0)
			return corner.getBlockZ() + zrange;
		return corner.getBlockZ();
	}

	@Override
	public int qtZMid() {
		return (qtZMax() + qtZMin()) / 2;
	}

	@Override
	public int qtZMin() {
		if (zrange < 0)
			return corner.getBlockZ() - zrange;
		return corner.getBlockZ();
	}

	public boolean isValidY(int y) {
		return corner.getBlockY() + yrange >= y
				&& corner.getBlockY() - yrange <= y;
	}

	// *------------------------------------------------------------------------------------------------------------*
	// | The following chooseSpawn method contains code made by NuclearW |
	// | based on his SpawnArea plugin: |
	// |
	// http://forums.bukkit.org/threads/tp-spawnarea-v0-1-spawns-targetPlayers-in-a-set-area-randomly-1060.20408/
	// |
	// *------------------------------------------------------------------------------------------------------------*
	public Location findRandomSafeLocation() {
		double xrand = 0;
		double zrand = 0;
		double y = -1;
		do {

			xrand = qtXMin() + Math.random() * (qtXMax() - qtXMin() + 1);
			zrand = qtZMin() + Math.random() * (qtZMax() - qtZMin() + 1);

			y = getValidHighestY(corner.getWorld(), xrand, zrand);

		} while (y == -1);

		Location location = new Location(corner.getWorld(), xrand, y, zrand);

		return location;
	}

	private double getValidHighestY(World world, double x, double z) {

		world.getChunkAt(new Location(world, x, 0, z)).load();

		double y = 0;
		int blockid = 0;

		if (world.getEnvironment().equals(Environment.NETHER)) {
			int blockYid = world.getBlockTypeIdAt((int) x, (int) y, (int) z);
			int blockY2id = world.getBlockTypeIdAt((int) x, (int) (y + 1),
					(int) z);
			while (y < 128 && !(blockYid == 0 && blockY2id == 0)) {
				y++;
				blockYid = blockY2id;
				blockY2id = world.getBlockTypeIdAt((int) x, (int) (y + 1),
						(int) z);
			}
			if (y == 127)
				return -1;
		} else {
			y = 257;
			while (y >= 0 && blockid == 0) {
				y--;
				blockid = world.getBlockTypeIdAt((int) x, (int) y, (int) z);
			}
			if (y == 0)
				return -1;
		}

		return y;
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
	
	public boolean isOnCurrentServer() {
		return isOnCurrentServer;
	}
	
	public void teleportPlayer(Player p) {
		if (connection == null)
			return;
		if (connection.getServerName().equals(BetterShardsAPI.getServerName()))
			p.teleport(connection.findRandomSafeLocation());
		BetterShardsAPI.connectPlayer(p, connection, PlayerChangeServerReason.PORTAL);
	}
}