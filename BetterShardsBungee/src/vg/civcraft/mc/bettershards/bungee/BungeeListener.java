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
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.events.EventListener;
import vg.civcraft.mc.mercury.events.EventManager;

public class BungeeListener implements Listener, EventListener {
	
	private BetterShardsBungee plugin = BetterShardsBungee.getInstance();
	private List<String> servers;
	private List<String> excluded;
	private BungeeDatabaseHandler db;
	private String lobbyServer;
	
	public BungeeListener() {
		servers = new ArrayList<String>();
		excluded = new ArrayList<String>();
		db = BetterShardsBungee.getDBHandler();
		scheduleServerScheduler();
		lobbyServer = BetterShardsBungee.getInstance().getConfig().getString("lobby-server", "");
		
		EventManager.registerListener(this);
	}
	
	private void scheduleServerScheduler() {
		plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {

			@Override
			public void run() {
				synchronized(servers) {
					servers.clear();
					for (String x: MercuryAPI.getAllConnectedServers()) {
						if (excluded.contains(x))
							continue;
						servers.add(x);
					}
				}
			}
			
		}, 0, 5, TimeUnit.SECONDS);
	}
	
	@EventHandler()
	public void playerJoinedBungeeNetwork(ServerConnectEvent event) {
		ProxiedPlayer p = event.getPlayer();
		UUID uuid = p.getUniqueId();
		// Here we are going to check if new player.
		if (db.hasPlayerBefore(uuid))
			return;
			
		Random rand = new Random();
		synchronized(servers) {
			int random = rand.nextInt(servers.size());
			String server = servers.get(random);
			ServerInfo sInfo = ProxyServer.getInstance().getServerInfo(server);
			event.setTarget(sInfo);
			BungeeMercuryManager.sendRandomSpawn(uuid, server);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void checkServerFull(ServerConnectEvent event) {
		if (lobbyServer.equals(""))
			return;
		ServerInfo info = event.getTarget();
		int count = BetterShardsBungee.getServerCount(info.getName());
		int current = MercuryAPI.getAllAccountsByServer(info.getName()).size() + QueueHandler.getPlayerOrder(info.getName()).size();
		if (current >= count) {
			// Now we deal with redirecting the player.
			ProxiedPlayer p = event.getPlayer();
			p.setReconnectServer(info);
			// Let's message the player and let them know what is happening.
			TextComponent message = new TextComponent("The server you are trying to connect to is full, you are being trasnfered"
					+ " to the lobby until the server has room. You will automatically be transfered when space is available.");
			message.setColor(ChatColor.GREEN);
			p.sendMessage(message);
			// Now let's send them to the lobby server.
			ServerInfo lobby = ProxyServer.getInstance().getServerInfo(lobbyServer);
			event.setTarget(lobby);
			BetterShardsBungee.getInstance().getLogger().info(String.format("Player %s tried to connect to %s but they "
					+ "are at %d out of %d players so was denied and added to queue.", p.getDisplayName(), info.getName(),
					count, current));
			return;
		}
		// Now let's deal with the player leaving a server and now allowing another player to take its place.
		handlePlayerQueueLeaving(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerDisconnectEvent(PlayerDisconnectEvent event) {
		handlePlayerQueueLeaving(event.getPlayer());
	}
	
	private void handlePlayerQueueLeaving(ProxiedPlayer player) {
		// Need to get the server they were trying to connect to.
		UUID uuid = player.getUniqueId();
		String name = QueueHandler.getServerName(uuid);
		if (name == null) // The player is not in a queue.
			return;
		QueueHandler.removePlayerQueue(uuid, name);
		BungeeMercuryManager.playerRemoveQueue(uuid, name);
		player.setReconnectServer(ProxyServer.getInstance().getServerInfo(name));
	}

	@Override
	public void receiveMessage(String origin, String channel, String message) {
		if (!channel.equals("BetterShards"))
			return;
		String[] content = message.split(" ");
		if (content[0].equals("removeServer")) {
			for (int x = 1; x < content.length; x++) {
				excluded.clear();
				excluded.add(content[x]);
			}
		}
		else if (content[0].equals("count")) {
			BetterShardsBungee.setServerCount(origin, Integer.parseInt(content[1]));
		}
		else if (content[0].equals("queue")) {
			String type = content[1];
			if (type.equals("add")) {
				UUID uuid = UUID.fromString(content[2]);
				String server = content[3];
				int pos = Integer.parseInt(content[4]);
				QueueHandler.addPlayerToQueue(uuid, server, pos);
			}
			else if (type.equals("request") && QueueHandler.isPrimaryBungee()) {
				UUID uuid = UUID.fromString(content[2]);
				String server = content[3];
				QueueHandler.addPlayerToQueue(uuid, server);
			}
			else if (type.equals("remove")) {
				UUID uuid = UUID.fromString(content[2]);
				String server = content[3];
				QueueHandler.removePlayerQueue(uuid, server);
			}
			else if (type.equals("transfer")) {
				String server = content[2];
				UUID uuid = UUID.fromString(content[3]);
				ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
				QueueHandler.removePlayerQueue(uuid, server);
				if (p == null)
					return;
				TextComponent m = new TextComponent("A space is now available, you are being teleported to the server.");
				m.setColor(ChatColor.GREEN);
				p.sendMessage(m);
			}
			else if (type.equals("sync")) {
				String server = content[2];
				ArrayList<UUID> uuids = new ArrayList<UUID>();
				for (int x = 3; x < content.length; x++) {
					UUID uuid = UUID.fromString(content[x]);
					uuids.add(uuid);
				}
				QueueHandler.setServerQueue(server, uuids);
			}
		}
	}
	
	@EventHandler()
	public void playerJoinBungeeServer(LoginEvent event) {
		ServerInfo server = db.getServer(event.getConnection().getUniqueId());
		if (server != null && !servers.contains(server.getName())) {
			event.setCancelled(true);
			event.setCancelReason("Disconnected: server is down.");
		}
	}
}
