package vg.civcraft.mc.bettershards.bungee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.ServiceManager;
import vg.civcraft.mc.mercury.config.MercuryConfigManager;
import vg.civcraft.mc.mercury.events.EventListener;
import vg.civcraft.mc.mercury.events.EventManager;

public class BungeeListener implements Listener, EventListener{

	private RedisBungeeAPI redisAPI = RedisBungee.getApi();
	private BetterShardsBungee plugin = BetterShardsBungee.getInstance();
	private List<String> servers;
	private List<String> excluded;
	private List<UUID> pending = new ArrayList<UUID>();
	
	public BungeeListener() {
		servers = new ArrayList<String>();
		excluded = new ArrayList<String>();
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
	public void playerLoginNetwork(LoginEvent event) {
		if (redisAPI.getLastOnline(event.getConnection().getUniqueId()) != -1)
			return;
		pending.add(event.getConnection().getUniqueId());
	}
	
	@EventHandler()
	public void playerJoinedBungeeNetwork(ServerConnectEvent event) {
		ProxiedPlayer p = event.getPlayer();
		if (!pending.contains(p.getUniqueId()))
				return;
		Random rand = new Random();
		synchronized(servers) {
			int random = rand.nextInt(servers.size());
			String server = servers.get(random);
			ServerInfo sInfo = ProxyServer.getInstance().getServerInfo(server);
			event.setTarget(sInfo);
		}
		pending.remove(p.getUniqueId());
	}

	@Override
	public void receiveMessage(String origin, String channel, String message) {
		if (!channel.equals("BetterShards"))
			return;
		String[] content = message.split(" ");
		if (!content[0].equals("removeServer"))
			return;
		for (int x = 1; x < content.length; x++) {
			excluded.clear();
			excluded.add(content[x]);
		}
	}
}
