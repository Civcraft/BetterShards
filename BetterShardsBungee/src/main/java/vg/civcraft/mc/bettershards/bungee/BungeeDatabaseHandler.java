package vg.civcraft.mc.bettershards.bungee;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import vg.civcraft.mc.bettershards.database.Database;

public class BungeeDatabaseHandler {
	public class PriorityInfo {
		private String server;
		private int populationCap;

		public PriorityInfo(String server, int populationCap) {
			this.server = server;
			this.populationCap = populationCap;
		}

		public String getServer() { return this.server; }
		public int getPopulationCap() { return this.populationCap; }
	}

	private Map<String, PriorityInfo> respawnPriorityCache = Collections.emptyMap();
	private Map<String, PriorityInfo> respawnPriorityCacheImmutable = Collections.unmodifiableMap(respawnPriorityCache);
	private long respawnPriorityCacheExpires = 0;
	private final long SPAWN_PRIORITY_TIMEOUT = 5 * 60 * 1000;  // 5 minutes in ms
	
	private Map<UUID, ServerInfo> playerServerCache = new HashMap<UUID, ServerInfo>();

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
	
	private String setServer, getServer;
	private String getAllPriority;
	
	private void setStatements() {
		setServer = "insert into BetterShardsBungeeConnection (uuid, server) values (?, ?) "
				+ "on duplicate key update server = values(server);";
		getServer = "select server from BetterShardsBungeeConnection where uuid = ?;";
		getAllPriority = "select name, cap from priorityServers;";
	}
	
	private void createTables() {
		db.execute("create table if not exists BetterShardsBungeeConnection("
				+ "uuid varchar(36) not null,"
				+ "server varchar(36) not null,"
				+ "primary key uuidKey (uuid));");
	}
	
	public void setServer(ProxiedPlayer p, ServerInfo server) {
		if (!db.isConnected())
			db.connect();
		PreparedStatement setServer = db.prepareStatement(this.setServer);
		try {
			setServer.setString(1, p.getUniqueId().toString());
			setServer.setString(2, server.getName());
			setServer.execute();
			playerServerCache.put(p.getUniqueId(), server);
			BungeeMercuryManager.sendPlayerServerUpdate(p.getUniqueId(), server.getName());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setServer(ProxiedPlayer p, ServerInfo server, boolean saveToDB) {
		if(saveToDB){
			setServer(p, server);
		} else {
			playerServerCache.put(p.getUniqueId(), server);
		}
	}
	
	public ServerInfo getServer(ProxiedPlayer p) {
		return getServer(p.getUniqueId());
	}
	
	public ServerInfo getServer(UUID uuid) {
		if(playerServerCache.containsKey(uuid)){
			return playerServerCache.get(uuid);
		}
		if (!db.isConnected())
			db.connect();
		PreparedStatement getServer = db.prepareStatement(this.getServer);
		try {
			getServer.setString(1, uuid.toString());
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

	public Map<String, PriorityInfo> getPriorityServers() {
		return getPriorityServers(false);
	}

	public Map<String, PriorityInfo> getPriorityServers(boolean forceRefresh) {
		long currentTime = System.currentTimeMillis();
		if (forceRefresh || respawnPriorityCacheExpires <= currentTime) {
			respawnPriorityCacheExpires = currentTime + SPAWN_PRIORITY_TIMEOUT;
			respawnPriorityCache = retrieveAllPriorityFromDb();
			respawnPriorityCacheImmutable = Collections.unmodifiableMap(respawnPriorityCache);
		}
		return respawnPriorityCacheImmutable;
	}

	public Map<String, PriorityInfo> retrieveAllPriorityFromDb() {
		if (!db.isConnected())
			db.connect();
		PreparedStatement getAllPriority = db.prepareStatement(this.getAllPriority);
		Map<String, PriorityInfo> result = new HashMap<>();
		try {
			ResultSet set = getAllPriority.executeQuery();
			while (set.next()) {
				String server = set.getString("name");
				result.put(server, new PriorityInfo(server, set.getInt("cap")));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				getAllPriority.close();
			} catch (Exception ex) {}
		}
		return result;
	}
}
