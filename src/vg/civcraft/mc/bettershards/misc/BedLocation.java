package vg.civcraft.mc.bettershards.misc;

import java.util.UUID;

public class BedLocation {

	private UUID uuid;
	private String location;
	private String server;
	
	/**
	 * Creates an object representing where a player slept.
	 * @param uuid The UUID of the player.
	 * @param location The String representation of the location. EX: world_uuid x y z
	 * @param server The server the bed is on.
	 */
	public BedLocation(UUID uuid, String location, String server) {
		this.uuid = uuid;
		this.location = location;
		this.server = server;
	}
	
	public String getServer() {
		return server;
	}
	
	/** 
	 * @return The location of the player's bed format: world_uuid x y z
	 */
	public String getLocation() {
		return location;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	public void setServer(String server) {
		this.server = server;
	}
}
