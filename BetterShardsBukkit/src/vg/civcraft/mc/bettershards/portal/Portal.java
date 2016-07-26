package vg.civcraft.mc.bettershards.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;

public abstract class Portal {

	protected Portal connection;
	protected String serverName;
	protected String name;
	private boolean isOnCurrentServer; // Set to false if not on current server
	protected DatabaseManager db;
	private boolean isDirty = false;
	public final int specialId;
	protected static final int PARTICLE_RANGE = 4;
	protected static final int PARTICLE_SIGHT_RANGE = 16;

	public Portal(String name, final String con, boolean isOnCurrentServer, int specialId) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(BetterShardsPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				connection = BetterShardsAPI.getPortalsManager().getPortal(con);
			}
			
		});
		this.name = name;
		this.isOnCurrentServer = isOnCurrentServer;
		db = BetterShardsPlugin.getInstance().getDatabaseManager();
		this.specialId = specialId;
	}

	public Portal getPartnerPortal() {
		return connection;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		setDirty(true);
	}

	public void setPartnerPortal(Portal connection) {
		this.connection = connection;
		setDirty(true);
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
		setDirty(true);
	}

	public boolean isOnCurrentServer() {
		return isOnCurrentServer;
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean dirty) {
		isDirty = dirty;
	}

	/**
	 * Finds a random spawn location, this doesn't have to be inside the portal,
	 * but can be anywhere
	 * 
	 * @return Random spawn location
	 */
	public abstract Location findSpawnLocation();

	public abstract boolean inPortal(Location loc);
	
	public abstract void teleport(Player p);
	
	public abstract void showParticles(Player p);

}