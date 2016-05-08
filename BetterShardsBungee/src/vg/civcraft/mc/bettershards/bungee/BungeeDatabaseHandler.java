package vg.civcraft.mc.bettershards.bungee;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import vg.civcraft.mc.bettershards.database.Database;

public class BungeeDatabaseHandler {

	private Database db;
	
	public BungeeDatabaseHandler(String host, int port, String dbname, String username,
			String password) {
		db = new Database(host, port, dbname, username, password, 
				BetterShardsBungee.getInstance().getLogger());
		if (!db.connect()) {
			BetterShardsBungee.getInstance().getLogger().log(Level.SEVERE, "Major error has "
					+ "occured when trying to connect to global BetterShards Database.\n"
					+ "Was the Bukkit/Spigot/Bukkit API utalizing server able to connect"
					+ " to the global database?\n Shutting down as this plugin cannot "
					+ "function correctly.");
			BetterShardsBungee.getInstance().getProxy().stop();
		}
		
		setStatements();
		createTables();
	}
	
	private String hasPlayedBefore;
	private String setServer, getServer;
	
	private void setStatements() {
		hasPlayedBefore = "select count(*) as count from BetterShardsBungeeConnection where id = ?;";
		setServer = "insert into BetterShardsBungeeConnection (uuid, server) values (?, ?) "
				+ "on duplicate key update server = ?;";
		getServer = "select server from BetterShardsBungeeConnection where uuid = ?;";
	}
	
	private void createTables() {
		db.execute("create table if not exists BetterShardsBungeeConnection("
				+ "uuid varchar(36) not null,"
				+ "server varchar(36) not null,"
				+ "primary key uuidKey (uuid));");
	}
	
	public boolean hasPlayerBefore(UUID uuid) {
		if (!db.isConnected())
			db.connect();
		PreparedStatement hasPlayedBefore = db.prepareStatement(this.hasPlayedBefore);
		try {
			hasPlayedBefore.setString(1, uuid.toString());
			ResultSet set = hasPlayedBefore.executeQuery();
			if (!set.next())
				return false;
			return set.getInt("count") > 0;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public void setServer(ProxiedPlayer p, String server) {
		PreparedStatement setServer = db.prepareStatement(this.setServer);
		try {
			setServer.setString(1, p.getUniqueId().toString());
			setServer.setString(2, server);
			setServer.setString(3, server);
			setServer.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ServerInfo getServer(ProxiedPlayer p) {
		PreparedStatement getServer = db.prepareStatement(this.getServer);
		try {
			getServer.setString(1, p.getUniqueId().toString());
			ResultSet set = getServer.executeQuery();
			if (!set.next())
				return null;
			return ProxyServer.getInstance().getServerInfo(set.getString(1));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
