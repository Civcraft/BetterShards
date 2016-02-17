package vg.civcraft.mc.bettershards.misc;

import java.util.UUID;

public class BedLocation {

	private UUID uuid;
	private TeleportInfo info;
	
	/**
	 * Creates an object representing where a player slept.
	 * @param uuid The UUID of the player.
	 * @param info The TeleportInfo object
	 */
	public BedLocation(UUID uuid, TeleportInfo info) {
		this.uuid = uuid;
	}
	
	public String getServer() {
		return info.getServer();
	}
	
	public TeleportInfo getTeleportInfo() {
		return info;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public void setTeleportInfo(TeleportInfo info) {
		this.info = info;
	}
	
	public void setServer(String server) {
		info.setServer(server);
	}
}