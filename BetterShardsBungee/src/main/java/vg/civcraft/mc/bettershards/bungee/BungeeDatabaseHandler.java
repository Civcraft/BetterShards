package vg.civcraft.mc.bettershards.bungee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Updated to use HikariCP connection pool (9/12/2016 ProgrammerDan)
 */
public class BungeeDatabaseHandler {
	private Logger logger; 
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
			String password, int poolSize, long connectionTimeout, long idleTimeout, 
			long maxLifetime) {
		logger = BetterShardsBungee.getInstance().getLogger();
		db = new Database(logger,
				username, password, host, port, dbname, poolSize,
				connectionTimeout, idleTimeout, maxLifetime);
		try {
			db.getConnection().close();
		} catch (Exception se) {
			logger.log(Level.SEVERE, "Major error has "
					+ "occured when trying to connect to global BetterShards Database.\n"
					+ "Was the Bukkit/Spigot/Bukkit API utalizing server able to connect"
					+ " to the global database?\n Shutting down as this plugin cannot "
					+ "function correctly.");
			BetterShardsBungee.getInstance().getProxy().stop();
		}
		
		if (!createTables()) {
			BetterShardsBungee.getInstance().getProxy().stop();
		}
	}

	private static final String setServer = "insert into BetterShardsBungeeConnection (uuid, server) values (?, ?) "
			+ "on duplicate key update server = VALUES(server);";
	private static final String getServer = "select server from BetterShardsBungeeConnection where uuid = ?;";
	private static final String getAllPriority = "select name, cap from priorityServers;";
	private static final String getAllExclude = "select * from excludedServers;";
	
	// TODO: We might want a CivModCore for Bungee. Getting tired of 
	// copying in the boilerplate for Hikari...
	private boolean createTables() {
		try (Connection connection = db.getConnection();
				Statement statement = connection.createStatement();) { 
			statement.executeUpdate("create table if not exists BetterShardsBungeeConnection("
				+ "uuid varchar(36) not null,"
				+ "server varchar(36) not null,"
				+ "primary key uuidKey (uuid));");
			return true;
		} catch (SQLException se) {
			logger.log(Level.SEVERE, "Failed to create the BetterShardsBungee tables", se);
			return false;
		}
	}
	
	public boolean hasPlayerBefore(UUID uuid) {
		 ServerInfo test = getServer(uuid);
		 return test != null;
	}
	
	public void setServer(UUID uuid, ServerInfo server) {
		try (Connection connection = db.getConnection();
				PreparedStatement setServer = connection.prepareStatement(BungeeDatabaseHandler.setServer);) {
			setServer.setString(1, uuid.toString());
			setServer.setString(2, server.getName());
			setServer.execute();
			playerServerCache.put(uuid, server);
			BungeeMercuryManager.sendPlayerServerUpdate(uuid, server.getName());
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Failed to set server for a player", e);
		}
	}
	
	public void setServer(UUID uuid, ServerInfo server, boolean saveToDB) {
		if(saveToDB){
			setServer(uuid, server);
		} else {
			playerServerCache.put(uuid, server);
		}
	}
	
	public ServerInfo getServer(ProxiedPlayer p) {
		return getServer(p.getUniqueId());
	}
	
	public ServerInfo getServer(UUID uuid) {
		if(playerServerCache.containsKey(uuid)){
			return playerServerCache.get(uuid);
		}
		try (Connection connection = db.getConnection();
				PreparedStatement getServer = connection.prepareStatement(BungeeDatabaseHandler.getServer);){
			getServer.setString(1, uuid.toString());
			ResultSet set = getServer.executeQuery();
			if (!set.next())
				return null;
			ServerInfo server = ProxyServer.getInstance().getServerInfo(set.getString(1));
			playerServerCache.put(uuid, server);
			return server;
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to get server for a user", e);
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
		Map<String, PriorityInfo> result = new HashMap<>();
		try (Connection connection = db.getConnection();
				PreparedStatement getAllPriority = connection.prepareStatement(BungeeDatabaseHandler.getAllPriority);) {
			ResultSet set = getAllPriority.executeQuery();
			while (set.next()) {
				String server = set.getString("name");
				result.put(server, new PriorityInfo(server, set.getInt("cap")));
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Failed to retrieve all priority from DB", e);
		}
		return result;
	}
	
	public List <String> retrieveAllExcludeFromDb() {
		List <String> result = new LinkedList <String> ();
		try (Connection connection = db.getConnection();
				PreparedStatement getAllExclude = connection.prepareStatement(BungeeDatabaseHandler.getAllExclude);) {
			ResultSet set = getAllExclude.executeQuery();
			while (set.next()) {
				result.add(set.getString("name"));
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "Failed to retrieve all exclude from DB", e);
		}
		return result;
	}
}
