package vg.civcraft.mc.bettershards.portal;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.misc.LocationWrapper;

public abstract class Portal {

	protected LocationWrapper first, second;
	protected String connection;
	protected String serverName;
	protected String name;
	protected boolean isOnCurrentServer; // Set to false if not on current server
	private boolean isDirty = false;
	protected static final int PARTICLE_RANGE = 4;
	protected static final int PARTICLE_SIGHT_RANGE = 16;

	public Portal() {
		
	}

	public Portal getPartnerPortal() {
		return BetterShardsPlugin.getPortalManager().getPortal(connection);
	}

	public String getName() {
		return name;
	}

	public Portal setName(String name) {
		this.name = name;
		setDirty(true);
		return this;
	}

	public Portal setPartnerPortal(String connection) {
		this.connection = connection;
		setDirty(true);
		return this;
	}

	public String getServerName() {
		return serverName;
	}

	public Portal setServerName(String serverName) {
		this.serverName = serverName;
		setDirty(true);
		return this;
	}

	public boolean isOnCurrentServer() {
		return isOnCurrentServer;
	}
	
	public Portal setIsOnCurrentServer(boolean value) {
		isOnCurrentServer = value;
		return this;
	}

	public boolean isDirty() {
		return isDirty;
	}

	public Portal setDirty(boolean dirty) {
		isDirty = dirty;
		return this;
	}
	
	public Portal setFirstLocation(LocationWrapper loc) {
		first = loc;
		return this;
	}
	
	public Portal setSecondLocation(LocationWrapper loc) {
		second = loc;
		return this;
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
	
	public abstract String getTypeName();
	
	/**
	 * This method should be called after a portal is populated with its necessary values.
	 */
	public abstract void valuesPopulated();
	
	/**
	 * Get the id of the portal.
	 * @return
	 */
	public abstract int getPortalID();

}