package vg.civcraft.mc.bettershards.bungee;

import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class LobbyHandler {

	public LobbyHandler() {
		ProxyServer.getInstance().getScheduler().schedule(BetterShardsBungee.getInstance(), new Runnable() {
			ProxyServer proxy = BetterShardsBungee.getInstance().getProxy();

			@Override
			public void run() {
				ServerInfo lobby = proxy.getServerInfo(BetterShardsBungee.getInstance().getConfig().getString("lobby-server", ""));
				if (lobby == null) {
					return;
				}
				
				for (ProxiedPlayer player : lobby.getPlayers()) {
					if(QueueHandler.getServerName(player.getUniqueId()) != null){
						continue; //Player is in the queue used for full servers, skipping him.
					}
					ServerInfo server = (player.getReconnectServer() != null) ? player.getReconnectServer() : proxy.getReconnectHandler().getServer(player);
					
					if (server == null) {
						proxy.getLogger().warning("Couldn't find a server to send " + player.getName() + " from the lobby.");
						return;
					}
					
					if (server.getName().equalsIgnoreCase(lobby.getName())) {
						proxy.getLogger().warning("The server saved for " + player.getName()
								+ " is the lobby. Value from getReconnectServer(): " + player.getReconnectServer()
								+ " Value from the ReconnectHandler: " + proxy.getReconnectHandler().getServer(player));
						return;
					}
					
					server.ping(new Callback<ServerPing>() {

						@Override
						public void done(ServerPing result, Throwable error) {
							if (error != null) {
								// Server is still offline.
								return;
							}

							if (server.canAccess(player) && server.getPlayers().size() < BetterShardsBungee.getServerCount(server.getName())) {
								TextComponent message = new TextComponent("The server is back up! Reconnecting now...");
								message.setColor(ChatColor.GREEN);
								player.sendMessage(message);
								
								proxy.getLogger().info("Moving " + player.getName() + " from the lobby to " + server.getName());
								player.connect(server);
							}
						}
					});

				}
			}

		}, 10, 10, TimeUnit.SECONDS);
	}
	
	public void onConnectToLobby(ProxiedPlayer player){
		TextComponent message = new TextComponent("The server you are trying to connect to is down, you are being transfered"
				+ " to the lobby until the server comes back online. You will automatically be transfered"
				+ " when the server comes back online.");
		message.setColor(ChatColor.GREEN);
		player.sendMessage(message);
	}
}
