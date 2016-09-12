package vg.civcraft.mc.bettershards.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.misc.CustomWorldNBTStorage;
import vg.civcraft.mc.bettershards.misc.InventoryIdentifier;
import vg.civcraft.mc.bettershards.misc.LocationWrapper;
import vg.civcraft.mc.bettershards.misc.TeleportInfo;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.bettershards.portal.portals.CircularPortal;
import vg.civcraft.mc.bettershards.portal.portals.CuboidPortal;
import vg.civcraft.mc.bettershards.portal.portals.WorldBorderPortal;
import vg.civcraft.mc.civmodcore.Config;
import vg.civcraft.mc.civmodcore.annotations.CivConfig;
import vg.civcraft.mc.civmodcore.annotations.CivConfigType;
import vg.civcraft.mc.civmodcore.annotations.CivConfigs;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.mercury.MercuryAPI;

public class DatabaseManager {

	private BetterShardsPlugin plugin = BetterShardsPlugin.getInstance();
	private Config config;
	private ManagedDatasource db;
	
	private Map<UUID, Map<InventoryIdentifier, byte[]>> invCache = new HashMap<UUID, Map<InventoryIdentifier, byte[]>>();
	private Map<UUID, Long> invCacheFreshness = new HashMap<UUID, Long>();
	private long invCacheTimeout = 30000; // how long does a cached inventory stick around?
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	private List<String> respawnExclusionCache = Collections.emptyList();
	private List<String> respawnExclusionCacheImmutable = Collections.unmodifiableList(respawnExclusionCache);
	private long respawnExclusionCacheExpires = 0;
	private final long SPAWN_EXCLUSION_TIMEOUT = 5 * 60 * 1000;  // 5 minutes in ms

	private Map<String, PriorityInfo> respawnPriorityCache = Collections.emptyMap();
	private Map<String, PriorityInfo> respawnPriorityCacheImmutable = Collections.unmodifiableMap(respawnPriorityCache);
	private long respawnPriorityCacheExpires = 0;
	private final long SPAWN_PRIORITY_TIMEOUT = 5 * 60 * 1000;  // 5 minutes in ms

	private static final String insertPlayerData = "INSERT INTO createPlayerData(uuid, server, entity, config_sect) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE entity = VALUES(entity), config_sect = VALUES(config_sect);";
	private static final String getPlayerData = "select * from createPlayerData where uuid = ? and server = ?;";
	private static final String removePlayerData = "delete from createPlayerData where uuid = ? and server = ?;";
	
	private static final String getLock = "INSERT INTO playerDataLock(uuid, inv_id) VALUES (?, ?);";
	private static final String checkLock = "SELECT last_upd FROM playerDataLock WHERE uuid = ? AND inv_id = ?;";
	private static final String releaseLock = "DELETE FROM playerDataLock WHERE uuid = ? AND inv_id = ?;";
	
	private static final String addPortalLoc = "insert into createPortalLocData(x1, y1, z1, x2, y2, z2, world, id) values (?,?,?,?,?,?,?,?);";
	private static final String getPortalLocByWorld = "select * from createPortalLocData where world = ?;";
	private static final String getPortalLoc = "select * from createPortalLocData where id = ?;";
	private static final String removePortalLoc = "delete from createPortalDataTable where id = ?;";
	
	private static final String addPortalData = "insert into createPortalDataTable(id, server_name, portal_type, partner_id) values(?,?,?,?);";
	private static final String getPortalData = "select * from createPortalDataTable where id = ?;";
	private static final String removePortalData = "delete from createPortalLocData where id = ?;";
	private static final String updatePortalData = "update createPortalDataTable set partner_id = ? where id = ?;";
	
	private static final String addExclude = "insert ignore into excludedServers(name) values(?);";
	private static final String removeExclude = "delete from excludedServers where name = ?;";
	private static final String getAllExclude = "select * from excludedServers;";
	
	private static final String addPriority = "insert into priorityServers(name, cap) values(?,?);";
	private static final String removePriority = "delete from priorityServers where name = ?;";
	private static final String getAllPriority = "select name, cap from priorityServers;";

	private static final String addBedLocation = "insert into player_beds (uuid, server, world_name, x, y, z) values (?,?,?,?,?,?)";
	private static final String getAllBedLocation = "select * from player_beds;";
	private static final String removeBedLocation = "delete from player_beds where uuid = ?;";
	
	private String cleanupLocks;

	private BukkitTask lockCleanup;
	
	private Logger logger;

	/**
	 * Does a few key things.
	 * 
	 * Builds a new {@link ManagedDatasource} from the <code>mysql</code> section in the Config. 
	 * Then, sets up migrations into the new format. Note that first time you run while on the old migration path,
	 * the server will shutdown. Restart, and it'll be good (this is the auto-migration to new migration system).
	 * 
	 * Next, optionally sets up cleanup if configured to do so.
	 */
	public DatabaseManager(){
		config = plugin.GetConfig();
		logger = plugin.getLogger();
		setupDatabase();
		try {
			db.getConnection().close();
		} catch (Exception se) {
			logger.log(Level.WARNING, "Could not connect to Database, shutting down!");
			Bukkit.shutdown();
		}

		prepareLocalStatements();
		prepareMigrations();
		
		if (!db.updateDatabase()) {
			logger.log(Level.WARNING, "Failed to migrate to latest Database, shutting down!");
			Bukkit.shutdown();
		}
		
		setupCleanup();
	}
	
	@CivConfigs({
		@CivConfig(name = "mysql.host", def = "localhost", type = CivConfigType.String),
		@CivConfig(name = "mysql.port", def = "3306", type = CivConfigType.Int),
		@CivConfig(name = "mysql.username", type = CivConfigType.String),
		@CivConfig(name = "mysql.password", type = CivConfigType.String),
		@CivConfig(name = "mysql.dbname", def = "BetterShardsDB", type = CivConfigType.String),
		@CivConfig(name = "mysql.poolsize", def = "10", type = CivConfigType.Int),
		@CivConfig(name = "mysql.connection_timeout", def = "10000", type = CivConfigType.Long),
		@CivConfig(name = "mysql.idle_timeout", def = "600000", type = CivConfigType.Long),
		@CivConfig(name = "mysql.max_lifetime", def = "7200000", type = CivConfigType.Long)
	})
	private void setupDatabase(){
		String username = config.get("mysql.username").getString();
		String host = config.get("mysql.host").getString();
		int port = config.get("mysql.port").getInt();
		String password = config.get("mysql.password").getString();
		String dbname = config.get("mysql.dbname").getString();
		int poolsize = config.get("mysql.poolsize").getInt();
		long connectionTimeout = config.get("mysql.connection_timeout").getLong();
		long idleTimeout = config.get("mysql.idle_timeout").getLong();
		long maxLifetime = config.get("mysql.max_lifetime").getLong();
		this.db = new ManagedDatasource(plugin, username, password, host, port, dbname, poolsize, connectionTimeout, idleTimeout, maxLifetime);
	}
	
	private void prepareMigrations() {
		// First migration is conversion from old system to new
		db.registerMigration(-1, false,
				new Callable<Boolean>() {
					@Override
					public Boolean call() {
						return false; // Force a failure. Migrations doesn't check the current migration per step, only at beginning.
						// So, we force a shutdown failure on first run. Then on second run, the Migration table will hold the correct values.
					}
				},
				"INSERT INTO managed_plugin_data (plugin_name, current_migration_number, last_migration)"
						+ " SELECT '" + plugin.getName() + "', max(db_version), timestamp(update_time) FROM bettershards_version LIMIT 1;");

		this.db.registerMigration(0, false,
				"create table if not exists createPlayerData("
						+ "uuid varchar(36) not null,"
						+ "entity blob,"
						+ "server int not null,"
						+ "primary key (uuid, server));",
				"create table if not exists createPortalDataTable("
						+ "id varchar(255) not null,"
						+ "server_name varchar(255) not null,"
						+ "portal_type int not null,"
						+ "partner_id varchar(255),"
						+ "primary key(id));",
				"create table if not exists createPortalLocData("
						+ "x1 int not null,"
						+ "y1 int not null,"
						+ "z1 int not null,"
						+ "x2 int not null,"
						+ "y2 int not null,"
						+ "z2 int not null,"
						+ "world varchar(255) not null,"
						+ "id varchar(255) not null,"
						+ "primary key loc_id (x1, y1, z1, x2, y2, z2, world, id));",
				"create table if not exists excludedServers("
						+ "name varchar(20) not null,"
						+ "primary key name_id(name));",
				"create table if not exists priorityServers("
						+ "name varchar(20) not null,"
						+ "cap int not null,"
						+ "primary key name_id(name));",
				"create table if not exists player_beds("
						+ "uuid varchar(36) not null,"
						+ "server varchar(36) not null,"
						+ "world_name varchar(36) not null,"
						+ "x int not null,"
						+ "y int not null,"
						+ "z int not null,"
						+ "primary key bed_id(uuid));",
				"create table if not exists bettershards_version("
						+ "db_version int not null,"
						+ "update_time varchar(24));");
		this.db.registerMigration(1, false, "alter table createPlayerData add config_sect text;");
		this.db.registerMigration(2, false, "CREATE TABLE IF NOT EXISTS playerDataLock("
					+ "uuid VARCHAR(36) NOT NULL,"
					+ "inv_id INT NOT NULL,"
					+ "last_upd TIMESTAMP NOT NULL DEFAULT NOW(),"
					+ "PRIMARY KEY (uuid, inv_id));");
	}
	
	@CivConfigs({
		@CivConfig(name = "locks.cleanup_minutes", def = "1", type = CivConfigType.Int),
	})
	private void prepareLocalStatements(){
		cleanupLocks = "DELETE FROM playerDataLock WHERE last_upd <= TIMESTAMPADD(MINUTE, -" + config.get("locks.cleanup_minutes").getInt() + ", NOW());";
	}

	@CivConfigs({
		@CivConfig(name = "locks.cleanup", def = "true", type = CivConfigType.Bool),
		@CivConfig(name = "locks.interval", def = "1200", type = CivConfigType.Long),
		@CivConfig(name = "cache.freshness_period", def = "30000", type = CivConfigType.Long)
	})
	private void setupCleanup() {
		this.invCacheTimeout = config.get("cache.freshness_period").getLong();
		if (config.get("locks.cleanup").getBool()) {  // no forever locks
			this.lockCleanup = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {

				@Override
				public void run() {
					try (Connection connection = db.getConnection();
							Statement statement = connection.createStatement();){
						statement.execute(cleanupLocks);
					} catch (SQLException se) {
						logger.log(Level.WARNING, "Failed to clean up outstanding locks", se);
					}
				}
				
			}, 20l, config.get("locks.interval").getLong());
		}
	}

	/**
	 * Closes things up.
	 */
	public void cleanup() {
		if (lockCleanup != null) {
			lockCleanup.cancel();
		}
		try {
			this.db.close();
		} catch (SQLException se) {
			
		}
	}


	/**
	 * Threadsafe accessor for the inventory cache. Respects the cache timeout; old caches are discarded, not returned.
	 *
	 * @param uuid The player to lookup
	 * @param id The inventory to retrieve
	 * @returns a byte array for the raw inventory data or null if none found or cache expired.
	 */
	private byte[] queryCache(UUID uuid, InventoryIdentifier id) {
		return null;
	}

	/**
	 * Threadsafe mutator for the inventory cache. Updates cache timeout.
	 * Be warned that since this doesn't check the cache timeout or clear out old caches, it is best to have some other process
	 * that periodically clears up old caches.
	 *
	 * @param uuid The player to cache data for
	 * @param id The inventory to cache into
	 * @param data The inventory data to cache
	 */
	private void updateCache(UUID uuid, InventoryIdentifier id, byte[] data) {
		synchronized(invCache) {
			Map<InventoryIdentifier, byte[]> playerInvCache = invCache.get(uuid);
			if (playerInvCache == null) {
				playerInvCache = new HashMap<InventoryIdentifier, byte[]>();
				invCache.put(uuid, playerInvCache);
			}
			playerInvCache.put(id, data);
			invCacheFreshness.put(uuid, System.currentTimeMillis());
		}
	}

	/**
	 * Threadsafe cleanup for the inventory cache. Respects freshness; if not fresh removes all inventories
	 * Can be used otherwise to remove a single cached inventory or all inventories.
	 * 
	 * @param uuid The player whose cache to modify
	 * @param id The inventory to remove from cache, null to clear all inventories
	 */
	private void clearCache(UUID uuid, InventoryIdentifier id) {
		synchronized(invCache) {
			Map<InventoryIdentifier, byte[]> playerInvCache = invCache.get(uuid);
			if (playerInvCache == null) {
				return;
			}
			if (id == null) { // clear all
				playerInvCache.clear();
				invCacheFreshness.remove(uuid);
			} else {
				// Check freshness
				Long freshness = invCacheFreshness.get(uuid);
				if (freshness != null && (System.currentTimeMillis() - freshness) > invCacheTimeout) {
					// Not fresh so clear all the cache anyway
					playerInvCache.clear();
					invCacheFreshness.remove(uuid);
				} else {
					// Fresh so just remove this one inventory
					playerInvCache.remove(id);
				}
			}
		}
	}
	
	/**
	 * Adds a portal instance to the database. Should be called only when
	 * initially creating a Portal Object.
	 */
	public void addPortal(Portal portal){
		try (Connection connection = db.getConnection();
				PreparedStatement addPortalLoc = connection.prepareStatement(DatabaseManager.addPortalLoc)){
			if (portal instanceof CuboidPortal){
				CuboidPortal p = (CuboidPortal) portal;
				Location first = p.getFirst();
				Location second = p.getSecond();
				addPortalLoc.setInt(1, first.getBlockX());
				addPortalLoc.setInt(2, first.getBlockY());
				addPortalLoc.setInt(3, first.getBlockZ());
				addPortalLoc.setInt(4, second.getBlockX());
				addPortalLoc.setInt(5, second.getBlockY());
				addPortalLoc.setInt(6, second.getBlockZ());
				addPortalLoc.setString(7, first.getWorld().getName());
				addPortalLoc.setString(8, p.getName());
			}
			else if (portal instanceof WorldBorderPortal) {
				WorldBorderPortal p = (WorldBorderPortal) portal;
				LocationWrapper firstW = p.getFirst();
				LocationWrapper secondW = p.getSecond();
				Location first = firstW.getFakeLocation();
				Location second = secondW.getFakeLocation();
				addPortalLoc.setInt(1, first.getBlockX());
				addPortalLoc.setInt(2, first.getBlockY());
				addPortalLoc.setInt(3, first.getBlockZ());
				addPortalLoc.setInt(4, second.getBlockX());
				addPortalLoc.setInt(5, second.getBlockY());
				addPortalLoc.setInt(6, second.getBlockZ());
				addPortalLoc.setString(7, firstW.getActualWorld());
				addPortalLoc.setString(8, p.getName());
			}
			else if (portal instanceof CircularPortal) {
				CircularPortal p = (CircularPortal) portal;
				Location first = p.getFirst();
				Location second = p.getSecond();
				addPortalLoc.setInt(1, first.getBlockX());
				addPortalLoc.setInt(2, first.getBlockY());
				addPortalLoc.setInt(3, first.getBlockZ());
				addPortalLoc.setInt(4, second.getBlockX());
				addPortalLoc.setInt(5, second.getBlockY());
				addPortalLoc.setInt(6, second.getBlockZ());
				addPortalLoc.setString(7, first.getWorld().getName());
				addPortalLoc.setString(8, p.getName());
			}
			addPortalLoc.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Add Portal DB failure: ", e);
		}
	}
	
	/**
	 * This method is called internally to remove the player from the cache.
	 * Minecraft executes the code to save the player so nothing needs to be
	 * done from this plugin's point of view.
	 * @param uuid The uuid of the player
	 */
	public void playerQuitServer(UUID uuid) {
	    clearCache(uuid, null);
	}
	
	private String serverName = MercuryAPI.serverName();
	public void addPortalData(Portal portal, Portal connection){
		try (Connection connect = db.getConnection();
				PreparedStatement addPortalData = connect.prepareStatement(DatabaseManager.addPortalData);) {
			addPortalData.setString(1, portal.getName());
			addPortalData.setString(2, serverName);
			addPortalData.setInt(3, portal.specialId);
			String name = null;
			if (connection != null)
				name = connection.getName();
			addPortalData.setString(4, name);
			addPortalData.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Add Portal Data DB failure: ", e);
		}
	}
	
	@CivConfigs({
		@CivConfig(name="locks.show_culprit", def="false", type = CivConfigType.Bool)
	})
	public boolean getPlayerLock(UUID uuid, InventoryIdentifier id) {
		try (Connection connection = db.getConnection();
				PreparedStatement getLock = connection.prepareStatement(DatabaseManager.getLock);) {
			getLock.setString(1, uuid.toString());
			getLock.setInt(2, id.ordinal());
			getLock.executeUpdate();
			return true;
		} catch (SQLException nolockforyou) {
			// TODO ideally only return false for known duplicate key failure error; otherwise raise holy hell.
			if (config.get("locks.show_culprit").getBool()) {
				// Who is responsible for this expected travesty
				logger.log(Level.WARNING, "Someone wanted a save-lock but was late to the party: ", nolockforyou);
			}
			return false;
		}
	}
	
	public boolean releasePlayerLock(UUID uuid, InventoryIdentifier id) {
		try (Connection connection = db.getConnection();
				PreparedStatement releaseLock = connection.prepareStatement(DatabaseManager.releaseLock);) {
			releaseLock.setString(1, uuid.toString());
			releaseLock.setInt(2, id.ordinal());
			return releaseLock.executeUpdate() > 0;
		} catch (SQLException nolockforyou) {
			plugin.getLogger().log(Level.INFO, "Unable to release lock for {0}, please investigate", uuid);
			plugin.getLogger().log(Level.INFO, "Unable to release lock, exception: ", nolockforyou);
			return false;
		}
	}
	
	public boolean isPlayerLocked(UUID uuid, InventoryIdentifier id){
		try (Connection connection = db.getConnection();
				PreparedStatement checkLock = connection.prepareStatement(DatabaseManager.checkLock)) {
			checkLock.setString(1, uuid.toString());
			checkLock.setInt(2, id.ordinal());
			ResultSet rs = checkLock.executeQuery();
			return rs.first();
		} catch (SQLException se) {
			plugin.getLogger().log(Level.INFO, "Could not check on lock for {0}, please investigate", uuid);
			plugin.getLogger().log(Level.INFO, "Unable to check lock, exception: ", se);
			return true;
		}
	}
	
	/**
	 * Saves player data synchronously. Use for operations where that is okay, e.g. server shutdown but not for
	 * ordinary operations (scheduled saves, quit saves). Use {@link #savePlayerDataAsync(UUID, ByteArrayOutputStream, InventoryIdentifier, ConfigurationSection)} instead.
	 * 
	 * @param uuid
	 * @param output
	 * @param id
	 * @param section
	 */
	public void savePlayerData(UUID uuid, ByteArrayOutputStream output, InventoryIdentifier id, 
			ConfigurationSection section) {
		logger.log(Level.FINER, "savePlayer Sync player data {0}", uuid);
		
		/*
		 * Some notes. 
		 * 
		 * For code that has a great many race conditions spread across multiple servers, you need something to play traffic cop.
		 * 
		 * Now usually, just change transaction isolation and you're good to go w/ automatic row-locks.
		 * 
		 * In our case, we explicitly want to _prevent_ read of the data if a save is _pending_. So we externalize our lock using
		 * an ultralightweight fast-failure locking mechanism.
		 * 
		 * It also helps up detect and prevent the "usual" badboys of simultaneous saves from multiple shards; a condition
		 * that should never occur but _if_ we externalize the lock, we can find it.
		 */
		if (!getPlayerLock(uuid, id)) { // someone beat us to it?
			logger.log(Level.WARNING, "Unable to grab rowlock for save of {0}, another process or server is saving at the same time as me.", uuid);
			shortTrace();
			return;
		}
		clearCache(uuid, id); // So if it is loaded again it is recaught.

		doSavePlayerData(uuid, output, id, section);
	}
	
	/**
	 * Used by Sync and Async calls.
	 * @param uuid
	 * @param output
	 * @param id
	 * @param section
	 */
	private void doSavePlayerData(UUID uuid, ByteArrayOutputStream output, InventoryIdentifier id, 
			ConfigurationSection section) {
		plugin.getLogger().log(Level.FINER, "doSave player data {0}", uuid);
		try (Connection connection = db.getConnection();
				PreparedStatement insertPlayerDataPS = connection.prepareStatement(DatabaseManager.insertPlayerData);) {
			insertPlayerDataPS.setString(1, uuid.toString());
			insertPlayerDataPS.setInt(2, id.ordinal());

			byte[] outputBytes = null;
			if (output == null) {
				insertPlayerDataPS.setNull(3, Types.BLOB);
			} else {
				outputBytes = output.toByteArray();
				insertPlayerDataPS.setBytes(3, outputBytes);
			}
			YamlConfiguration yaml = (YamlConfiguration) section;
			if (yaml == null) {
				insertPlayerDataPS.setNull(4, Types.VARCHAR);
			} else {
				String ymlStr = yaml.saveToString();
				insertPlayerDataPS.setString(4, ymlStr);
			}
			insertPlayerDataPS.execute();
			updateCache(uuid, id, outputBytes);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to doSavePlayerData for {0}", uuid);
			logger.log(Level.SEVERE, "Failed to doSavePlayerData exception:", e);
		} catch (Exception ididntthinkofthis) {
			logger.log(Level.SEVERE, "Failed to doSavePlayerData for {0}", uuid);
			logger.log(Level.SEVERE, "Failed to doSavePlayerData exception:", ididntthinkofthis);
		} finally {
			if (!releasePlayerLock(uuid, id)) { // Both calling methods require release always, so we'll just do it here.
				plugin.getLogger().log(Level.WARNING, "Unable to release lock for {0}, lock is already released", uuid);
			} else {
				plugin.getLogger().log(Level.FINER, "DONE doSave player data {0}", uuid);
			}
		}
	}

	/**
	 * Defers the actual saving; establishes a software lock prior to save and fails fast if it can't.
	 * 
	 * This method is the preferred route to save!
	 * 
	 * @param uuid
	 * @param output
	 * @param id
	 * @param section
	 */
	public void savePlayerDataAsync(final UUID uuid, final ByteArrayOutputStream output, 
			final InventoryIdentifier id, final ConfigurationSection section) {
		plugin.getLogger().log(Level.FINER, "savePlayer Async player data {0}", uuid);
		
		if (!getPlayerLock(uuid, id)) { // someone beat us to it?
			plugin.getLogger().log(Level.WARNING, "Unable to grab rowlock for save of {0}, some other server or process is saving at the same time as me.", uuid);
			shortTrace();
			return;
		}
		clearCache(uuid, id); // So if it is loaded again it is recaught.

		// So, we get the lock synchronously, then do our save asynch. When done, we end.
		executor.submit( new Runnable() {
			public void run() {
				doSavePlayerData(uuid, output, id, section);
			}
		});
	}
	
	public void clearPrefetch(UUID uuid, InventoryIdentifier id) {
		clearCache(uuid, id);
	}

	/**
	 * This loadPlayerData ignores any data locks. PLEASE USE WITH CARE. Calls to this method might return old data!
	 * <br><br>
	 * This method _reads_ from the cache and _updates_ the cache if nothing previously cached
	 * 
	 * @param uuid
	 * @param id
	 * @return
	 */
	public ByteArrayInputStream loadPlayerData(UUID uuid, InventoryIdentifier id){
		// Here we had it caches before hand so no need to load it again.
		byte[] bais = queryCache(uuid, id);
		if (bais != null) {
			plugin.getLogger().log(Level.FINER, "Getting player data sync from cache for {0}", uuid);
			return new ByteArrayInputStream(bais);
		}
			
		plugin.getLogger().log(Level.FINER, "IGNORING LOCKS: Getting player data sync for {0}", uuid);
		bais = doLoadPlayerData(uuid, id);
		updateCache(uuid, id, bais);
		plugin.getLogger().log(Level.FINER, "IGNORING LOCKS: Done getting player data sync for {0}", uuid);
		return new ByteArrayInputStream(bais);
	}
	
	/**
	 * Internal method does the real loading for both sync and async
	 * 
	 * @param uuid
	 * @param id
	 * @return
	 */
	private byte[] doLoadPlayerData(UUID uuid, InventoryIdentifier id){
		try (Connection connection = db.getConnection();
				PreparedStatement getPlayerData = connection.prepareStatement(DatabaseManager.getPlayerData);) {
			getPlayerData.setString(1, uuid.toString());
			getPlayerData.setInt(2, id.ordinal());
			ResultSet set = getPlayerData.executeQuery();
			if (!set.next()) {
				return new byte[0];
			}
			YamlConfiguration sect = new YamlConfiguration();
			String sectString = set.getString("config_sect");
			if (sectString != null) {
				sect.loadFromString(sectString);
			}
			CustomWorldNBTStorage.getWorldNBTStorage().loadConfigurationSectionForPlayer(uuid, sect);
			return set.getBytes("entity");			
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Error retrieving player data from database for {0}", uuid);
			plugin.getLogger().log(Level.SEVERE, "Error retrieving player data from database exception:", e);
		} catch (InvalidConfigurationException e) {
			plugin.getLogger().log(Level.WARNING, "Configuration is invalid for {0}", uuid);
			plugin.getLogger().log(Level.WARNING, "Configuration is invalid exception:", e);
		}
		return new byte[0];
	}
	
	/**
	 * Asynchronously gets the player data. Gets a little exotic but the basis is simply. Returns a Future object that holds the eventual result of the command.
	 * <br><br>
	 * Use .get on the Future to wait for it to load and return the result. 
	 * <br><br>
	 * EXTERNAL PLUGINS SHOULD USE THIS EXCLUSIVELY
	 * <br><br>
	 * This method both consumes and contributes to the cache. If data is on cache when triggered, it consumes it.
	 * If data is not on cache, it puts it on cache.
	 * <br><br>
	 * So, if you want to preload your data but not _use_ it, call loadPlayerDataAsync in an async method, then
	 * call loadPlayerData in a sync method. If you're lucky and there aren't any race conditions in play to get that
	 * data (multiple consumers) then the sync call will pull from the cache.
	 * 
	 * @param uuid
	 * @param id
	 * @return
	 */
	public Future<ByteArrayInputStream> loadPlayerDataAsync(final UUID uuid, final InventoryIdentifier id) {
		byte[] bais = queryCache(uuid, id);
		if (bais != null) {
			plugin.getLogger().log(Level.FINER, "Getting player data async from cache for {0}", uuid);
			final byte[] baisPIT = bais;
			return new Future<ByteArrayInputStream>() {
				byte[] bais = baisPIT;

				@Override
				public boolean cancel(boolean arg0) {return false;}

				@Override
				public ByteArrayInputStream get() throws InterruptedException, ExecutionException {
					return new ByteArrayInputStream(bais);
				}

				@Override
				public ByteArrayInputStream get(long arg0, TimeUnit arg1) throws InterruptedException,ExecutionException, TimeoutException {
					return new ByteArrayInputStream(bais);
				}

				@Override
				public boolean isCancelled() {return false;}

				@Override
				public boolean isDone() {return true;}
			};
		}
		
		return executor.submit( new Callable<ByteArrayInputStream>(){
				@Override
				public ByteArrayInputStream call() throws Exception {
					plugin.getLogger().log(Level.FINER, "Getting player data async for {0}", uuid);
					long sleepSoFar = (long) (Math.random() * 10.0);
					// basic spinlock.
					while (isPlayerLocked(uuid, id)) {
						Thread.sleep( sleepSoFar );
						if (sleepSoFar > 1000l) { // let's not get crazy!
							sleepSoFar = 1000l;
						}
						sleepSoFar += (long) (Math.random() * 10.0); // but let's keep the random, to prevent synchronization of multiple spinlocks.
					} 
					/* This leaves an opening for race conditions, but with a very small interval size (< 10ms) which is
					 * far superior to previous.
					 */ 
					byte[] bais = doLoadPlayerData(uuid, id);
					updateCache(uuid, id, bais);
					plugin.getLogger().log(Level.FINER, "Done getting player data async for {0}", uuid);
					return new ByteArrayInputStream(bais);
				}
			}
		);
	}

	
	/**
	 * Can only be from worlds that are valid on this server.
	 * This is to prevent possible NullPointExceptions from trying to 
	 * get portals from Worlds that are not present on this server.
	 * 
	 * @param worlds
	 * @return
	 */
	public List<Portal> getAllPortalsByWorld(World[] worlds){
		List<Portal> portals = new ArrayList<Portal>();
		for (World w: worlds){
			String world = w.getName();
			try (Connection connection = db.getConnection();
					PreparedStatement getPortalLocation = connection.prepareStatement(DatabaseManager.getPortalLocByWorld);) {
				getPortalLocation.setString(1, world);
				ResultSet set = getPortalLocation.executeQuery();
				while (set.next()) {
					int x1 = set.getInt("x1");
					int y1 = set.getInt("y1");
					int z1 = set.getInt("z1");
					int x2 = set.getInt("x2");
					int y2 = set.getInt("y2");
					int z2 = set.getInt("z2");
					String id = set.getString("id");

					LocationWrapper first = new LocationWrapper(new Location(w, x1, y1, z1));
					LocationWrapper second = new LocationWrapper(new Location(w, x2, y2, z2));
					Portal p = getPortalData(id, first, second);
					portals.add(p);
				}
			} catch (SQLException e) {
				logger.log(Level.SEVERE, "Failed to getAllPortalsByWorld, exception:", e);
			}
		}
		return portals;
	}

	public Portal getPortal(String name) {
		try (Connection connection = db.getConnection();
				PreparedStatement getPortalData = connection.prepareStatement(DatabaseManager.getPortalLoc);) {
			getPortalData.setString(1, name);
			ResultSet set = getPortalData.executeQuery();
			if (!set.next())
				return null;
			int x1 = set.getInt("x1");
			int y1 = set.getInt("y1");
			int z1 = set.getInt("z1");
			int x2 = set.getInt("x2");
			int y2 = set.getInt("y2");
			int z2 = set.getInt("z2");
			String world = set.getString("world");
			World w = Bukkit.getWorld(world);
			LocationWrapper first = null, second = null;
			
			// If the World object does not equal null then we know that it is on this server.
			if (w != null) {
				first = new LocationWrapper(new Location(w, x1, y1, z1));
				second = new LocationWrapper(new Location(w, x2, y2, z2));
			}
			else {
				first = new LocationWrapper(world, x1, y1, z1);
				second = new LocationWrapper(world, x2, y2, z2);
			}
			
			return getPortalData(name, first, second);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to getPortal for {0}", name);
			logger.log(Level.SEVERE, "Failed to getPortal, exception:", e);
		}
		return null;
	}
	
	private Portal getPortalData(String name, LocationWrapper first, LocationWrapper second) {
		try (Connection connection = db.getConnection();
				PreparedStatement getPortalData = connection.prepareStatement(DatabaseManager.getPortalData);) {
			getPortalData.setString(1, name);
			ResultSet set = getPortalData.executeQuery();
			if (!set.next())
				return null;
			int specialId = set.getInt("portal_type"); // determine the type of portal.
			String serverName = set.getString("server_name");
			String partner = set.getString("partner_id");
			boolean currentServer = serverName.equals(MercuryAPI.serverName());
			switch (specialId) {
			case 0:
				CuboidPortal p = new CuboidPortal(name, first.getFakeLocation(), second.getFakeLocation(), partner, currentServer);
				p.setServerName(serverName);
				return p;
			case 1:
				WorldBorderPortal wb = new WorldBorderPortal(name, partner, currentServer, first, second);
				wb.setServerName(serverName);
				return wb;
			case 2:
				CircularPortal cp = new CircularPortal(name, partner, currentServer, first.getFakeLocation(), second.getFakeLocation());
				cp.setServerName(serverName);
				return cp;
			default:
				return null;
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to getPortalData for {0}", name);
			logger.log(Level.SEVERE, "Failed to getPortalData, exception:", e);
		}
		return null;
	}

	public void removePlayerData(UUID uuid, InventoryIdentifier id) {
		clearCache(uuid, id);
		try (Connection connection = db.getConnection();
				PreparedStatement removePlayerData = connection.prepareStatement(DatabaseManager.removePlayerData);) {
			removePlayerData.setString(1, uuid.toString());
			removePlayerData.setInt(2, id.ordinal());
			removePlayerData.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to removePlayerData, uuid: {0} id: {1}", new Object[]{uuid, id});
			logger.log(Level.SEVERE, "Failed to removePlayerData, exception:", e);
		}
	}

	public void removePortalLoc(Portal p) {
		try (Connection connection = db.getConnection();
				PreparedStatement removePortalLoc = connection.prepareStatement(DatabaseManager.removePortalLoc);) {
			removePortalLoc.setString(1, p.getName());
			removePortalLoc.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to removePortalLoc, exception:", e);
		}
	}

	public void removePortalData(Portal p) {
		try (Connection connection = db.getConnection();
				PreparedStatement removePortalData = connection.prepareStatement(DatabaseManager.removePortalData);) {
			removePortalData.setString(1, p.getName());
			removePortalData.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to removePortalData, exception:", e);
		}
	}

	public void updatePortalData(Portal p) {
		try (Connection connection = db.getConnection();
				PreparedStatement updatePortalData = connection.prepareStatement(DatabaseManager.updatePortalData);) {
			String partner = null;
			if (p.getPartnerPortal() != null)
				partner = p.getPartnerPortal().getName();
			updatePortalData.setString(1, partner);
			updatePortalData.setString(2, p.getName());
			updatePortalData.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to updatePortalData, exception:", e);
		}
	}

	public void addExclude(String server) {
		if (respawnExclusionCache.contains(server)) {
			return;
		}
		respawnExclusionCache.add(server);
		try (Connection connection = db.getConnection();
				PreparedStatement addExclude = connection.prepareStatement(DatabaseManager.addExclude);) {
			addExclude.setString(1, server);
			addExclude.execute();
		} catch (SQLException e) {
	 		logger.log(Level.SEVERE, "Failed to addExclude for {0}", server);
	 		logger.log(Level.SEVERE, "Failed to addExclude, exception:", e);
		}
	}

	public List<String> getAllExclude() {
		return getAllExclude(false);
	}

	public List<String> getAllExclude(boolean forceRefresh) {
		long currentTime = System.currentTimeMillis();
		if (forceRefresh || respawnExclusionCacheExpires <= currentTime) {
			respawnExclusionCacheExpires = currentTime + SPAWN_EXCLUSION_TIMEOUT;
			respawnExclusionCache = retrieveAllExcludeFromDb();
			respawnExclusionCacheImmutable = Collections.unmodifiableList(respawnExclusionCache);
		}
		return respawnExclusionCacheImmutable;
	}

	public List <String> retrieveAllExcludeFromDb() {
		List <String> result = new LinkedList<String>();
		try (Connection connection = db.getConnection();
				PreparedStatement getAllExclude = connection.prepareStatement(DatabaseManager.getAllExclude);) {
			ResultSet set = getAllExclude.executeQuery();
			while (set.next()) {
				result.add(set.getString("name"));
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to retrieveAllExcludeFromDb, exception:", e);
		}
		return result;
	}

	public void removeExclude(String server) {
		if (!respawnExclusionCache.remove(server)) {
			return;
		}
		try (Connection connection = db.getConnection();
				PreparedStatement removeExclude = connection.prepareStatement(DatabaseManager.removeExclude);) {
			removeExclude.setString(1, server);
			removeExclude.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to removeExclude for {0}", server);
			logger.log(Level.SEVERE, "Failed to removeExclude, exception:", e);
		}
	}

	public void addPriorityServer(String server, int populationCap) {
		if (respawnPriorityCache.containsKey(server)) {
			return;
		}
		respawnPriorityCache.put(server, new PriorityInfo(server, populationCap));
		try (Connection connection = db.getConnection();
				PreparedStatement addPriority = connection.prepareStatement(DatabaseManager.addPriority);) {
			addPriority.setString(1, server);
			addPriority.setInt(2, populationCap);
			addPriority.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to addPriorityServer {0}", server);
			logger.log(Level.SEVERE, "Failed to addPriorityServer, exception:", e);
		}
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
				PreparedStatement getAllPriority = connection.prepareStatement(DatabaseManager.getAllPriority);) {
			ResultSet set = getAllPriority.executeQuery();
			while (set.next()) {
				String server = set.getString("name");
				result.put(server, new PriorityInfo(server, set.getInt("cap")));
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to retrieveAllPriorityFromDb, exception:", e);
		}
		return result;
	}

	public void removePriorityServer(String server) {
		if (respawnPriorityCache.remove(server) == null) {
			return;
		}
		try (Connection connection = db.getConnection();
				PreparedStatement removePriority = connection.prepareStatement(DatabaseManager.removePriority);) {
			removePriority.setString(1, server);
			removePriority.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to removePriorityServer {0}", server);
			logger.log(Level.SEVERE, "Failed to removePriorityServer, exception:", e);
		}
	}

	public void addBedLocation(BedLocation bed) {
		TeleportInfo info = bed.getTeleportInfo();
		try (Connection connection = db.getConnection();
				PreparedStatement addBedLocation = connection.prepareStatement(DatabaseManager.addBedLocation);) {
			addBedLocation.setString(1, bed.getUUID().toString());
			addBedLocation.setString(2, bed.getServer());
			addBedLocation.setString(3, info.getWorld());
			addBedLocation.setInt(4, info.getX());
			addBedLocation.setInt(5, info.getY());
			addBedLocation.setInt(6, info.getZ());
			addBedLocation.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to addBedLocation, exception:", e);
		}
	}

	public List<BedLocation> getAllBedLocations() {
		List<BedLocation> beds = new ArrayList<BedLocation>();
		try (Connection connection = db.getConnection();
				PreparedStatement getAllBedLocation = connection.prepareStatement(DatabaseManager.getAllBedLocation);) {
			ResultSet set = getAllBedLocation.executeQuery();
			while (set.next()) {
				UUID uuid = UUID.fromString(set.getString("uuid"));
				String server = set.getString("server");
				String world_name = set.getString("world_name");
				int x = set.getInt("x");
				int y = set.getInt("y");
				int z = set.getInt("z");
				TeleportInfo info = new TeleportInfo(world_name, server, x, y, z);
				BedLocation bed = new BedLocation(uuid, info);
				beds.add(bed);
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to getAllBedLocations, exception:", e);
		}
		return beds;
	}

	public void removeBed(UUID uuid) {
		try (Connection connection = db.getConnection();
				PreparedStatement removeBedLocation = connection.prepareStatement(DatabaseManager.removeBedLocation);) {
			removeBedLocation.setString(1, uuid.toString());
			removeBedLocation.execute();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to removeBed for {0}", uuid);
			logger.log(Level.SEVERE, "Failed to removeBed, exception:", e);
		}
	}

	private void shortTrace() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		if (ste.length < 2) return;

		StringBuffer sb = new StringBuffer();
		sb.append(ste[1].toString());
		for (int i = 2; i < Math.min(5, ste.length); i++) {
			sb.append("\n").append(ste[i].toString());
		}

		logger.log(Level.INFO, "Short Traceback: \n {0}", sb);
	}


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
}
