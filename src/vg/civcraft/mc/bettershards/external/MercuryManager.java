package vg.civcraft.mc.bettershards.external;

import java.util.UUID;

import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.mercury.MercuryAPI;

public class MercuryManager {

	public MercuryManager() {
		registerMercuryChannels();
	}

	public void sendPortalDelete(String name) {
		MercuryAPI.sendGlobalMessage("delete " + name, "BetterShards");
	}

	/**
	 * Used to tell mercury that a player is teleporting. 
	 * This is useful to the player arrives at the right portal.
	 * @param uuid
	 * @param p
	 */
	public void teleportPlayer(UUID uuid, Portal p) {
		MercuryAPI.sendGlobalMessage("teleport portal " + uuid.toString() + " " + p.getName(), "BetterShards");
	}

	/**
	 * Sends the info to servers that a player needs to be teleported.
	 * @param info- Use the format 'uuid server world x y z'
	 * world can be either the world name or world uuid.
	 */
	public void teleportPlayer(String info) {
		MercuryAPI.sendGlobalMessage("teleport command " + info , "BetterShards");
	}

	private void registerMercuryChannels() {
		MercuryAPI.addChannels("BetterShards");
	}

	public void sendBedLocation(BedLocation bed) {
		String info = bed.getUUID().toString() + " " + bed.getServer() + " " + bed.getLocation();
		MercuryAPI.sendGlobalMessage("bed add " + info, "BetterShards");
	}

	public void removeBedLocation(BedLocation bed) {
		MercuryAPI.sendGlobalMessage("bed remove " + bed.getUUID().toString(), "BetterShards");
	}

	public void sendBungeeUpdateMessage(String allExclude) {
		MercuryAPI.sendGlobalMessage("removeServer " + allExclude, "BetterShards");
	}
}
