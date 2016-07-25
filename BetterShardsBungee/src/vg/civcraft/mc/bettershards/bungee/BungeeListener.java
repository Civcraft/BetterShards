package vg.civcraft.mc.bettershards.bungee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.events.EventListener;
import vg.civcraft.mc.mercury.events.EventManager;

public class BungeeListener implements Listener, EventListener {
	
	private BetterShardsBungee plugin = BetterShardsBungee.getInstance();
	private BungeeDatabaseHandler db;
	private String lobbyServer;
	private Random rand = new Random();
	
	public BungeeListener() {
		db = BetterShardsBungee.getDBHandler();
		lobbyServer = BetterShardsBungee.getInstance().getConfig().getString("lobby-server", "");
		
		EventManager.registerListener(this);
	}
	
	@EventHandler()
	public void playerJoinedBungeeNetwork(ServerConnectEvent event) {
		ProxiedPlayer p = event.getPlayer();
		UUID uuid = p.getUniqueId();
		// Here we are going to check if new player.
		if (db.hasPlayerBefore(uuid))
			return;
		List<String> servers = ServerHandler.getAllServers();
		Map<String, BungeeDatabaseHandler.PriorityInfo> priorityServers = db.getPriorityServers();
		int random = -1;
		if (!priorityServers.isEmpty()) {
		    	List <String> rndmServer = new ArrayList<String>(priorityServers.keySet());
			Collections.shuffle(rndmServer, rand);
			for (String rndServer : rndmServer) {
				int currentPopulation = MercuryAPI.getAllAccountsByServer(rndServer).size();
				if (currentPopulation < priorityServers.get(rndServer).getPopulationCap()) {
					random = servers.indexOf(rndServer);
					break;
				}
			}
		}
		if (random < 0) {
			if (servers.size() == 0) // No servers are up to do anything.
				return;
			random = rand.nextInt(servers.size());
		}
		String server = servers.get(random);
		ServerInfo sInfo = ProxyServer.getInstance().getServerInfo(server);
		event.setTarget(sInfo);
		BungeeMercuryManager.sendRandomSpawn(uuid, server);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void checkServerFull(ServerConnectEvent event) {
		if (lobbyServer.equals(""))
			return;
		ProxiedPlayer p = event.getPlayer();
		if (p.hasPermission("bettershards.admin") || p.hasPermission("bettershards.bypass"))
			return;
		ServerInfo info = event.getTarget();
		int count = BetterShardsBungee.getServerCount(info.getName());
		int current = MercuryAPI.getAllAccountsByServer(info.getName()).size() + QueueHandler.getPlayerOrder(info.getName()).size();
		if (current >= count && !QueueHandler.isAllowedPassThrough(p.getUniqueId())) {
			// Now we deal with redirecting the player.
			db.setServer(p, info.getName());
			// Let's message the player and let them know what is happening.
			TextComponent message = new TextComponent("The server you are trying to connect to is full, you are being transfered"
					+ " to the lobby until the server has room. You will automatically be transfered when space is available.");
			message.setColor(ChatColor.GREEN);
			p.sendMessage(message);
			// Now let's send them to the lobby server.
			ServerInfo lobby = ProxyServer.getInstance().getServerInfo(lobbyServer);
			event.setTarget(lobby);
			QueueHandler.addPlayerToQueue(p.getUniqueId(), info.getName());
			return;
		}
		else if (QueueHandler.isAllowedPassThrough(p.getUniqueId())){
			QueueHandler.removePassThrough(p.getUniqueId());
		}
		// Now let's deal with the player leaving a server and now allowing another player to take its place.
		UUID uuid = p.getUniqueId();
		String name = QueueHandler.getServerName(uuid);
		if (name == null) // The player is not in a queue.
			return;
		QueueHandler.removePlayerQueue(uuid, name);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void checkServerDown(ServerConnectEvent event) {
		ServerInfo server = event.getTarget();
		if (server != null && !ServerHandler.getAllServers().contains(server.getName()) && 
				!server.getName().equals(lobbyServer) && !ServerHandler.isExclusded(server.getName())) {
			ProxiedPlayer p = event.getPlayer();
			db.setServer(p, server.getName());
			// Let's message the player and let them know what is happening.
			TextComponent message = new TextComponent("The server you are trying to connect to is down, you are being transfered"
					+ " to the lobby until the server comes back online. You will automatically be transfered"
					+ " when the server comes back online.");
			message.setColor(ChatColor.GREEN);
			// Let's add the player to our watch list.
			ServerHandler.addPlayer(p.getUniqueId(), server.getName());
			p.sendMessage(message);
			// Now let's send them to the lobby server.
			ServerInfo lobby = ProxyServer.getInstance().getServerInfo(lobbyServer);
			event.setTarget(lobby);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void checkServerDownLogin(PostLoginEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		String name = db.getServerName(uuid);
		if (name == null)
			return;
		if (!ServerHandler.getAllServers().contains(name) && !name.equals(lobbyServer)) {
			ProxiedPlayer p = event.getPlayer();
			db.setServer(p, name);
			/* So we don't need to do what we did before in checkServerDown
			 * because by setting the player reconnect server it triggers server connect event.
			 * Any duplicate code would be duplicated.
			 * Now let's send them to the lobby server.
			 */
			ServerInfo lobby = ProxyServer.getInstance().getServerInfo(lobbyServer);
			p.setReconnectServer(lobby);
		}
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
		db.setServer(player, name);
	}

	@Override
	public void receiveMessage(String origin, String channel, String message) {
		if (!channel.equals("BetterShards"))
			return;
		String[] content = message.split("\\|");
		if (content[0].equals("removeServer")) {
			List<String> excluded = new ArrayList<String>();
			for (int x = 1; x < content.length; x++) {
				excluded.add(content[x]);
			}
			ServerHandler.setExcluded(excluded);
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
				if (p == null)
					return;
				TextComponent m = new TextComponent("A space is now available, you are being teleported to the server.");
				m.setColor(ChatColor.GREEN);
				p.sendMessage(m);
				ServerInfo info = ProxyServer.getInstance().getServerInfo(server);
				QueueHandler.removePlayerQueue(uuid, server); // This needs to be before the player transfers servers.
				// If it isnt the player may potentially be reput back in queue.
				QueueHandler.addAllowPassThrough(uuid);
				p.connect(info);
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
		else if (content[0].equals("primary")) {
			String dataType = content[1];
			if (dataType.equals("deny")) {
				QueueHandler.denyPrimary();
			}
			else if (dataType.equals("request")) {
				QueueHandler.requestPrimary();
			}
		}
	}
	
	@EventHandler()
	public void playerJoinBungeeServer(LoginEvent event) {
		ServerInfo server = db.getServer(event.getConnection().getUniqueId());
		if (server != null && server.getName().equals(lobbyServer)) {
			BetterShardsBungee.getInstance().getLogger().info(String.format("The player %s (%s) has somehow logged "
					+ "onto the lobby server without being redirected there.", event.getConnection().getName(),
					event.getConnection().getUniqueId().toString()));
		}
	}
}
