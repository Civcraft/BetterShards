package vg.civcraft.mc.bettershards.portal;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;

public abstract class Portal {

	protected Portal connection;
	protected String serverName;
	protected String name;
	private boolean isOnCurrentServer; // Set to false if not on current server
	protected DatabaseManager db;
	private boolean isDirty = false;

	public Portal(String name, Portal connection, boolean isOnCurrentServer) {
		this.connection = connection;
		this.name = name;
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

}