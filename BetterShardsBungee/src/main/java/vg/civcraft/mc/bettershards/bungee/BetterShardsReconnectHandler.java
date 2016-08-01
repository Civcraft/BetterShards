package vg.civcraft.mc.bettershards.bungee;

import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BetterShardsReconnectHandler implements ReconnectHandler{
    
    private BungeeDatabaseHandler db;

    public BetterShardsReconnectHandler() {
    	db = BetterShardsBungee.getDBHandler();
    }

	@Override
	public void close() {
		// We do nothing here.
	}

	@Override
	public ServerInfo getServer(ProxiedPlayer player) {
		return db.getServer(player);
	}

	@Override
	public void save() {
		// Nothing to do here.
	}

	@Override
	public void setServer(ProxiedPlayer player) {
		ServerInfo serverToSet = (player.getReconnectServer() != null) ? player.getReconnectServer() : player.getServer().getInfo();
		if (serverToSet.getName().equalsIgnoreCase(BetterShardsBungee.getInstance().getConfig().getString("lobby-server", ""))) {
			return;
		}
		BetterShardsBungee.getInstance().getLogger().info("Setting the server of " + player.getName() + " to " + serverToSet.getName());
		db.setServer(player, serverToSet);
	}

}
