package vg.civcraft.mc.bettershards.external;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerListPingEvent;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.misc.TeleportInfo;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.mercury.MercuryAPI;

/**
 * @author Rourke750
 *
 */
public class MercuryManager {

	public MercuryManager() {
		registerMercuryChannels();
		Bukkit.getScheduler().runTaskTimer(BetterShardsPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				InetAddress address = null;
				try {
					address = InetAddress.getByName("127.0.0.1");
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ServerListPingEvent event = new ServerListPingEvent(address, Bukkit.getMotd(), Bukkit.getOnlinePlayers().size(),
						Bukkit.getMaxPlayers());
				Bukkit.getPluginManager().callEvent(event);
			}
			
		}, 100, 1200);
	}

	public void sendPortalDelete(String name) {
		MercuryAPI.sendGlobalMessage("delete|" + name, "BetterShards");
	}

	/**
	 * Used to tell mercury that a player is teleporting. 
	 * This is useful to the player arrives at the right portal.
	 * @param uuid
	 * @param p
	 */
	public void teleportPlayer(UUID uuid, Portal p, Object ... data) {
		StringBuilder sb = new StringBuilder();
		sb.append("teleport|portal|" + uuid.toString() + "|" + p.getName());
		for(Object o:data) {
			sb.append("|");
			sb.append(o.toString());
		}
		MercuryAPI.sendGlobalMessage(sb.toString(), "BetterShards");
	}

	/**
	 * Sends the info to servers that a player needs to be teleported.
	 * @param info Use the TeleportInfo object.
	 */
	public void teleportPlayer(String server, UUID uuid, TeleportInfo info) {
		MercuryAPI.sendMessage(server, "teleport|command|" + uuid.toString() + "|" + info , "BetterShards");
	}
	
	public void teleportPlayer(String server, UUID playerToTeleportUUID, UUID targetPlayerUUID) {
		MercuryAPI.sendMessage(server, "teleport|command|" + playerToTeleportUUID + "|" + targetPlayerUUID , "BetterShards");
	}
	
	public void teleportPlayer(String server, UUID playerToTeleportUUID, String x, String y, String z) {
		MercuryAPI.sendMessage(server, "teleport|command|" + playerToTeleportUUID + "|" + x + "|" + y +"|" + z , "BetterShards");
	}
	
	public void teleportPlayer(String server, UUID playerToTeleportUUID, String x, String y, String z, String world) {
		MercuryAPI.sendMessage(server, "teleport|command|" + playerToTeleportUUID + "|" + world + "|" + x + "|" + y +"|" + z, "BetterShards");
	}

	private void registerMercuryChannels() {
		MercuryAPI.addChannels("BetterShards");
	}

	public void sendBedLocation(BedLocation bed) {
		String info = bed.getUUID().toString() + "|" + bed.getServer() + "|" + bed.getTeleportInfo();
		MercuryAPI.sendGlobalMessage("bed|add|" + info, "BetterShards");
	}
	
	public void notifyRandomSpawn(String server, UUID player) {
		MercuryAPI.sendMessage(server, "teleport|randomspawn|" + player.toString(), "BetterShards");
	}

	public void removeBedLocation(BedLocation bed) {
		MercuryAPI.sendGlobalMessage("bed|remove|" + bed.getUUID().toString(), "BetterShards");
	}

	public void sendBungeeUpdateMessage(String allExclude) {
		MercuryAPI.sendGlobalMessage("removeServer|" + allExclude, "BetterShards");
	}
	
	public void sendPortalJoin(Portal main, Portal con) {
		MercuryAPI.sendGlobalMessage("portal|connect|"+main.getName()+"|"+con.getName(), "BetterShards");
	}
	
	public void removePortalJoin(Portal main) {
		MercuryAPI.sendGlobalMessage("portal|remove|"+main.getName(), "BetterShards");
	}
	
	public void sendPlayerCount(int count) {
		MercuryAPI.sendGlobalMessage("count|" + count, "BetterShards");
	}
}
