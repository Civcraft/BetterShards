package vg.civcraft.mc.bettershards.bungee;

import java.util.ArrayList;
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
	
	/**
	 * Put the player in the queue position specified.
	 * @param uuid
	 * @param server
	 * @param position
	 */
	public static void addPlayerQueue(UUID uuid, String server, int position) {
		MercuryAPI.sendGlobalMessage(String.format("queue|add|%s|%s|%d",uuid.toString(), server, position), "BetterShards");
	}
	
	/**
	 * Player needs to be put in the queue.
	 * @param uuid
	 * @param server
	 */
	public static void playerRequestQueue(UUID uuid, String server) {
		MercuryAPI.sendGlobalMessage(String.format("queue|request|%s|%s", uuid.toString(), server), "BetterShards");
	}
	
	/**
	 * Player needs to be removed from the queue.
	 * @param uuid
	 * @param server
	 */
	public static void playerRemoveQueue(UUID uuid, String server) {
		MercuryAPI.sendGlobalMessage(String.format("queue|remove|%s|%s", uuid.toString(), server), "BetterShards");
	}
	
	public static void playerTransferQueue(String server, UUID uuid) {
		MercuryAPI.sendGlobalMessage("queue|transfer|" + server + "|" + uuid.toString(), "BetterShards");
	}
	
	public static void syncPlayerList(String server, ArrayList<UUID> uuids) {
		StringBuilder builder = new StringBuilder();
		for (UUID uuid: uuids)
			builder.append("|" + uuid.toString());
		MercuryAPI.sendGlobalMessage("queue|sync|" + server + builder.toString(), "BetterShards");
	}
	
	public static void attemptPrimaryClaim() {
		MercuryAPI.sendGlobalMessage("primary|request", "BetterShards");
	}
	
	public static void denyPrimaryClaim() {
		MercuryAPI.sendGlobalMessage("primary|deny", "BetterShards");
	}
	
	public static void sendPlayerServerUpdate(UUID uuid, String server){
		MercuryAPI.sendGlobalMessage(String.format("server|%s|%s", uuid.toString(), server), "BetterShards");
	}
}
