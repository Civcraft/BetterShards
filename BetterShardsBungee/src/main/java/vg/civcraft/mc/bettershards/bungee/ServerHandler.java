package vg.civcraft.mc.bettershards.bungee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import vg.civcraft.mc.mercury.MercuryAPI;

public class ServerHandler {

	private static Map<String, TreeSet<UUID>> serverDownList; // Server mapping to players.
	private static List<String> servers; // Servers that are currently down.
	private static List<String> excluded; // Servers that are excluded.
	private Map<String, TreeSet<UUID>> toBeRemoved; // List where in the next round if scanning.
	private static Object lockingObject = new Object();
	
	private static boolean initialized = false;
	
	private ServerHandler() {
		serverDownList = new HashMap<String, TreeSet<UUID>>();
		toBeRemoved = new HashMap<String, TreeSet<UUID>>();
		excluded = new ArrayList<String>();
		servers = new ArrayList<String>();
		ProxyServer.getInstance().getScheduler().schedule(BetterShardsBungee.getInstance(), new Runnable() {

			@Override
			public void run() {
				synchronized(lockingObject) {
					servers.clear();
					for (String x: MercuryAPI.getAllConnectedServers()) {
						if (excluded.contains(x)) {
							continue;
						}
						servers.add(x);
					}
					Iterator<String> i = toBeRemoved.keySet().iterator();
					while (i.hasNext()) {
						// iterating through all the servers.
						String server = i.next();
						Iterator<UUID> uuids = toBeRemoved.get(server).iterator();
						while (uuids.hasNext()) {
							UUID uuid = uuids.next();
							ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
							if (p == null || !p.getServer().getInfo().getName().equals(
									BetterShardsBungee.getInstance().getConfig().
									getString("lobby-server", ""))) {
								// So the player is no longer in the lobby and can be
								// safely removed.
								uuids.remove();
								if (serverDownList.containsKey(server)) {
									serverDownList.get(server).remove(uuid);
								}
							}
							// If not then they will get caught the next round.
						}
						if (toBeRemoved.get(server).isEmpty()) {
							// Remove the record if empty.
							i.remove();
						}
					}
					for (String server: servers) {
						if (serverDownList.containsKey(server)) {
							TextComponent message = new TextComponent("The server is back up! Reconnecting now...");
							message.setColor(ChatColor.GREEN);
							if (!toBeRemoved.containsKey(server)) {
								toBeRemoved.put(server, new TreeSet<UUID>());
							}
							for (UUID uuid: serverDownList.get(server)) {
								ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
								if (p == null) // If they aren't here then we don't need to worry.
									continue; // Once they log on they will correctly be redirected.
								p.sendMessage(message);
								p.connect(ProxyServer.getInstance().getServerInfo(server));
								toBeRemoved.get(server).add(uuid);
							}
						}
					}
				}
			}
			
		}, 10, 10, TimeUnit.SECONDS);
	}
	
	public static void initialize() {
		if (initialized)
			return;
		new ServerHandler();
	}
	
	public static void addPlayer(UUID uuid, String server) {
		synchronized(lockingObject) {
			if (!serverDownList.containsKey(server))
				serverDownList.put(server, new TreeSet<UUID>());
			serverDownList.get(server).add(uuid);
		}
	}
	
	public static List<String> getAllServers() {
		synchronized(lockingObject) {
			return servers;
		}
	}
	
	public static void setExcluded(List<String> s) {
		synchronized(lockingObject) {
			excluded.clear();
			excluded.addAll(s);
			servers.removeAll(s);
		}
	}
	
	public static boolean isExclusded(String server) {
		synchronized(lockingObject) {
			return excluded.contains(server);
		}
	}
}
