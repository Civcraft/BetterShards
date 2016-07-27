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
		db.setServer(player, player.getServer().getInfo().getName());
	}

}
