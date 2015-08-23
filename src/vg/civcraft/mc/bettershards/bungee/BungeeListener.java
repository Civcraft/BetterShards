package vg.civcraft.mc.bettershards.bungee;

import java.util.List;
import java.util.Random;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeListener implements Listener {

	private RedisBungeeAPI redisAPI = RedisBungee.getApi();
	
	@EventHandler()
	public void playerJoinedBungeeNetwork(ServerConnectEvent event) {
		ProxiedPlayer p = event.getPlayer();
		if (redisAPI.getLastOnline(p.getUniqueId()) != -1)
			return;
		List<String> servers = redisAPI.getAllServers();
		Random rand = new Random();
		int random = rand.nextInt(servers.size()-1);
		String server = servers.get(random);
		ServerInfo sInfo = BungeeCord.getInstance().getServerInfo(server);
		p.connect(sInfo);
	}
}
