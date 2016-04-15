package vg.civcraft.mc.bettershards.bungee;

import java.util.UUID;

import vg.civcraft.mc.mercury.MercuryAPI;

public class BungeeMercuryManager {

	public static void sendRandomSpawn(UUID uuid, String server) {
		MercuryAPI.sendMessage(server, "teleport|randomspawn|" + uuid, "BetterShards");
	}
	
	/** 
	 * Disables player first randomspawn as it will be handled here.
	 */
	public static void disableLocalRandomSpawn() {
		MercuryAPI.sendGlobalMessage("info|randomspawn|disable", "BetterShards");
	}
	
	public static void requestDBInfo() {
		MercuryAPI.sendGlobalMessage("info|db|request", "BetterShards");
	}
}
