package vg.civcraft.mc.bettershards.bungee;

import java.util.List;
import java.util.Random;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import vg.civcraft.mc.mercury.ServiceManager;
import vg.civcraft.mc.mercury.config.MercuryConfigManager;
import vg.civcraft.mc.mercury.events.EventListener;
import vg.civcraft.mc.mercury.events.EventManager;

public class BungeeListener implements Listener, EventListener{

	private RedisBungeeAPI redisAPI = RedisBungee.getApi();
	private BetterShardsBungee plugin = BetterShardsBungee.getInstance();
	private List<String> servers;
	
	public BungeeListener() {
		servers = redisAPI.getAllServers();
		MercuryConfigManager.initialize();
		ServiceManager.getService(); // Initialize everything.
		EventManager.registerListener(this);
	}
	
	@EventHandler()
	public void playerJoinedBungeeNetwork(ServerConnectEvent event) {
		ProxiedPlayer p = event.getPlayer();
		if (redisAPI.getLastOnline(p.getUniqueId()) != -1)
			return;
		Random rand = new Random();
		int random = rand.nextInt(servers.size()-1);
		String server = servers.get(random);
		ServerInfo sInfo = ProxyServer.getInstance().getServerInfo(server);
		p.connect(sInfo);
	}

	@Override
	public void receiveMessage(String channel, String message) {
		if (!channel.equals("BetterShards"))
			return;
		String[] content = message.split(" ");
		if (!content[0].equals("removeServer"))
			return;
		for (int x = 1; x < content.length; x++)
			servers.remove(content[x]);
	}
}
