package vg.civcraft.mc.bettershards.bungee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import vg.civcraft.mc.bettershards.database.Database;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.ServiceManager;
import vg.civcraft.mc.mercury.config.MercuryConfigManager;
import vg.civcraft.mc.mercury.events.EventListener;
import vg.civcraft.mc.mercury.events.EventManager;

public class BungeeListener implements Listener, EventListener {
	
	private BetterShardsBungee plugin = BetterShardsBungee.getInstance();
	private List<String> servers;
	private List<String> excluded;
	private BungeeDatabaseHandler db;
	
	public BungeeListener() {
		servers = new ArrayList<String>();
		excluded = new ArrayList<String>();
		db = BetterShardsBungee.getDBHandler();
		scheduleServerScheduler();
		
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
	}
	
	@EventHandler()
	public void playerJoinBungeeServer(LoginEvent event) {
		ServerInfo server = db.getServer(event.getConnection().getUniqueId());
		if (!servers.contains(server.getName())) {
			event.setCancelled(true);
			event.setCancelReason("Disconnected because that server is down.");
		}
	}
}
