package vg.civcraft.mc.bettershards.bungee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.mercury.MercuryAPI;

public class QueueHandler {

	private static boolean isPrimaryBungee;
	private static Map<String, ArrayList<UUID>> queue = new HashMap<String, ArrayList<UUID>>();
	private static Map<UUID, String> uuidToServerMap = new HashMap<UUID, String>();
	private static Object lockingObject = new Object();
	private static boolean initialized = false;
	private static List<UUID> allowPassThrough = new ArrayList<UUID>();;
	
	private static ScheduledTask thread;
	
	private QueueHandler() {
		isPrimaryBungee = true;
		ProxyServer.getInstance().getScheduler().schedule(BetterShardsBungee.getInstance(), new Runnable() {

			@Override
			public void run() {
				synchronized(lockingObject) {
					for (String server: queue.keySet()) {
						if (!queue.containsKey(server))
							queue.put(server, new ArrayList<UUID>());
						List<UUID> uuids = queue.get(server);
						int maxSlots = BetterShardsBungee.getServerCount(server);
						int currentSlots = MercuryAPI.getAllAccountsByServer(server).size();
						TextComponent message = new TextComponent("A space is now available, you are being teleported to the server.");
						message.setColor(ChatColor.GREEN);
						for (int x = 0; x < uuids.size() && x < maxSlots - currentSlots; x++) {
							ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuids.get(x));
							UUID uuid = uuids.get(x);
							uuids.remove(x);
							uuidToServerMap.remove(uuid);
							// The four lines above are important in that position.
							// Without it we get an infinite lock situation.
							if (p != null) {
								p.sendMessage(message);
								QueueHandler.addAllowPassThrough(uuid);
								p.connect(ProxyServer.getInstance().getServerInfo(server));
								BungeeMercuryManager.playerRemoveQueue(uuid, server); // We have dealt with the player now.
							} else {
								// may be on other bungee server. Either way lets remove him and get other servers to check.
								BungeeMercuryManager.playerTransferQueue(server, uuid);
							}
							x--;
							currentSlots++;
						}
						for (int x = 0; x < uuids.size(); x++) {
							ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuids.get(x));
							if (p == null)
								continue; // Not on this Mercury server.
							message = new TextComponent("Your current position is " + (x+1) + " in line.");
							message.setColor(ChatColor.GREEN);
							p.sendMessage(message);
						}
					}
				}
			}
			
		}, 10, 10, TimeUnit.SECONDS);
		ProxyServer.getInstance().getScheduler().schedule(BetterShardsBungee.getInstance(), new Runnable() {

			@Override
			public void run() {
				if (isPrimaryBungee) {
					synchronized(lockingObject) {
						for (String server: queue.keySet()) {
							if (!queue.containsKey(server))
								queue.put(server, new ArrayList<UUID>());
							BungeeMercuryManager.syncPlayerList(server, queue.get(server));
						}
					}
				}
			}	
		}, 10, 60, TimeUnit.SECONDS);
		
		attemptPrimary();
	}
	
	public static void initialize() {
		if (initialized)
			return;
		new QueueHandler();
	}
	
	public static ArrayList<UUID> getPlayerOrder(String server) {
		synchronized(lockingObject) {
			if (!queue.containsKey(server))
				queue.put(server, new ArrayList<UUID>());
			return queue.get(server);
		}
	}
	
	/**
	 * This method will determine if this bungee server is the primary one or not.
	 * If it is it will then add the player to the queue and broadcast the position the player should be in.
	 * If not it will request a position and then once received it will be correctly placed.
	 * @param uuid
	 * @param server
	 */
	public static void addPlayerToQueue(UUID uuid, String server) {
		if (isPrimaryBungee) {
			synchronized(lockingObject) {
				if (!queue.containsKey(server))
					queue.put(server, new ArrayList<UUID>());
				if (queue.get(server).contains(uuid)) {
					int pos = queue.get(server).indexOf(uuid);
					BungeeMercuryManager.addPlayerQueue(uuid, server, pos);
				} 
				else {
					queue.get(server).add(uuid);
					int pos = queue.get(server).indexOf(uuid);
					uuidToServerMap.put(uuid, server);
					BungeeMercuryManager.addPlayerQueue(uuid, server, pos);
				}
			}
		}
		else {
			// Need to be assigned a position.
			BungeeMercuryManager.playerRequestQueue(uuid, server);
		}
	}
	
	/**
	 * This should only be called by BungeeMercuryListener
	 * This method should be fired when the main Bungee proxy decides what position the player is in.
	 * @param uuid
	 * @param server
	 * @param pos
	 */
	public static void addPlayerToQueue(UUID uuid, String server, int pos) {
		synchronized(lockingObject) {
			if (!queue.containsKey(server))
				queue.put(server, new ArrayList<UUID>());
			queue.get(server).add(pos, uuid);
			uuidToServerMap.put(uuid, server);
		}
	}
	
	/**
	 * Removes the player from the queue. This method will not broadcast to other bungee servers.
	 * @param uuid
	 * @param server
	 */
	public static void removePlayerQueue(UUID uuid, String server) {
		synchronized(lockingObject) {
			if (!queue.containsKey(server))
				queue.put(server, new ArrayList<UUID>());
			queue.get(server).remove(uuid);
			uuidToServerMap.remove(uuid);
		}
	}
	
	public static String getServerName(UUID uuid) {
		synchronized(lockingObject) {
			return uuidToServerMap.get(uuid);
		}
	}
	
	public static boolean isPrimaryBungee() {
		return isPrimaryBungee;
	}
	
	public static void setServerQueue(String server, ArrayList<UUID> uuids) {
		synchronized(lockingObject) {
			if (!queue.containsKey(server))
				queue.put(server, new ArrayList<UUID>());
			queue.get(server).clear();
			queue.get(server).addAll(uuids);
			for (UUID uuid: uuids)
				uuidToServerMap.put(uuid, server);
		}
	}
	
	/**
	 * This method is when other server's attempt to take control and be a primary server.
	 * This method is fired from BungeeListener to decide whether or not a server can be primary.
	 * If this instance of bungee has determined that it should be a primary it turns primary to false
	 * and sends out a message to other servers to let them know that they need to try negotiate again.
	 */
	public static void requestPrimary() {
		BetterShardsBungee.info("Other servers are checking if we have a valid claim for handling queue.");
		if (thread != null) {
			thread.cancel();
		}
		if (isPrimaryBungee) {
			BetterShardsBungee.info("We do infact have a claim, sending try again.");
			BungeeMercuryManager.denyPrimaryClaim();
			isPrimaryBungee = false;
			attemptPrimary();
		}
		else {
			BetterShardsBungee.info("We do not.");
		}
	}
	
	/**
	 * This method is for when a server deny's other servers from being primary.
	 */
	public static void denyPrimary() {
		BetterShardsBungee.info("Other servers told us to try again.");
		if (thread != null) {
			BetterShardsBungee.info("An attempt to take over was found, cancelling.");
			thread.cancel();
		}
		isPrimaryBungee = false;
		attemptPrimary();
	}
	
	private static void attemptPrimary() {
		BetterShardsBungee.info("Scheduling attempt to take over queue system.");
		Random r = new Random();
		thread = ProxyServer.getInstance().getScheduler().schedule(BetterShardsBungee.getInstance(), new Runnable() {

			@Override
			public void run() {
				BetterShardsBungee.info("Beginning to attempt control over queue system.");
				isPrimaryBungee = true;
				BungeeMercuryManager.attemptPrimaryClaim();
			}
			
		}, r.nextInt(1000), TimeUnit.MILLISECONDS);
	}
	
	public static void addAllowPassThrough(UUID uuid) {
		allowPassThrough.add(uuid);
	}
	
	public static boolean isAllowedPassThrough(UUID uuid) {
		return allowPassThrough.contains(uuid);
	}
	
	public static void removePassThrough(UUID uuid) {
		allowPassThrough.remove(uuid);
	}
}
