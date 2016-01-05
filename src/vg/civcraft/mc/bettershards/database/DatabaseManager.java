package vg.civcraft.mc.bettershards.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.misc.InventoryIdentifier;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.bettershards.portal.PortalType;
import vg.civcraft.mc.bettershards.portal.portals.CuboidPortal;
import vg.civcraft.mc.civmodcore.Config;
import vg.civcraft.mc.civmodcore.annotations.CivConfig;
import vg.civcraft.mc.civmodcore.annotations.CivConfigType;
import vg.civcraft.mc.civmodcore.annotations.CivConfigs;

public class DatabaseManager{

	private BetterShardsPlugin plugin = BetterShardsPlugin.getInstance();
	private Config config;
	private Database db;
	
	private Map<UUID, ByteArrayInputStream> invCache = new ConcurrentHashMap<UUID, ByteArrayInputStream>();
	
	private String addPlayerData, getPlayerData, removePlayerData;
	private String addPortalLoc, getPortalLocByWorld, getPortalLoc, removePortalLoc;
	private String addPortalData, getPortalData, removePortalData, updatePortalData;
	private String addExclude, getAllExclude, removeExclude;
	private String addBedLocation, getAllBedLocation, removeBedLocation;
	
	public DatabaseManager(){
		config = plugin.GetConfig();
		if (!isValidConnection())
			return;
		executeDatabaseStatements();
		loadPreparedStatements();
	}
	
	@CivConfigs({
		@CivConfig(name = "mysql.host", def = "localhost", type = CivConfigType.String),
		@CivConfig(name = "mysql.port", def = "3306", type = CivConfigType.Int),
		@CivConfig(name = "mysql.username", type = CivConfigType.String),
		@CivConfig(name = "mysql.password", type = CivConfigType.String),
		@CivConfig(name = "mysql.dbname", def = "BetterShardsDB", type = CivConfigType.String)
	})
	public boolean isValidConnection(){
		String username = config.get("mysql.username").getString();
		String host = config.get("mysql.host").getString();
		int port = config.get("mysql.port").getInt();
		String password = config.get("mysql.password").getString();
		String dbname = config.get("mysql.dbname").getString();
		db = new Database(host, port, dbname, username, password, plugin.getLogger());
		return db.connect();
	}
	
	private void executeDatabaseStatements() {
		db.execute("create table if not exists createPlayerData("
				+ "uuid varchar(36) not null,"
				+ "entity blob,"
				+ "server int not null,"
				+ "primary key (uuid, server));");
		db.execute("create table if not exists createPortalDataTable("
				+ "id varchar(255) not null,"
				+ "server_name varchar(255) not null,"
				+ "portal_type int not null,"
				+ "partner_id varchar(255),"
				+ "primary key(id));");
		db.execute("create table if not exists createPortalLocData("
				+ "rangex int not null,"
				+ "rangey int not null,"
				+ "rangez int not null,"
				+ "x int not null,"
				+ "y int not null,"
				+ "z int not null,"
				+ "world varchar(255) not null,"
				+ "id varchar(255) not null,"
				+ "primary key loc_id (x, y, z, world, id));");
		db.execute("create table if not exists excludedServers("
				+ "name varchar(20) not null,"
				+ "primary key name_id(name));");
		db.execute("create table if not exists player_beds("
				+ "uuid varchar(36) not null,"
				+ "server varchar(36) not null,"
				+ "world_uuid varchar(36) not null,"
				+ "x int not null,"
				+ "y int not null,"
				+ "z int not null,"
				+ "primary key bed_id(uuid));");
	}
	
	public boolean isConnected() {
		return db.isConnected();
	}
	
	private void loadPreparedStatements(){
		addPlayerData = "insert into createPlayerData(uuid, entity, server) values(?,?,?);";
		getPlayerData = "select * from createPlayerData where uuid = ? and server = ?;";
		removePlayerData = "delete from createPlayerData where uuid = ? and server = ?;";
		
		addPortalLoc = "insert into createPortalLocData(rangex, rangey, rangez, x, y, z, world, id)"
				+ "values (?,?,?,?,?,?,?,?);";
		getPortalLocByWorld = "select * from createPortalLocData where world = ?;";
		getPortalLoc = "select * from createPortalLocData where id = ?;";
		removePortalLoc = "delete from createPortalDataTable where id = ?;";
		
		addPortalData = "insert into createPortalDataTable(id, server_name, portal_type, partner_id)"
				+ "values(?,?,?,?);";
		getPortalData = "select * from createPortalDataTable where id = ?;";
		removePortalData = "delete from createPortalLocData where id = ?;";
		updatePortalData = "update createPortalDataTable set partner_id = ? where id = ?;";
		
		addExclude = "insert ignore into excludedServers(name) values(?);";
		removeExclude = "delete from excludedServers where name = ?;";
		getAllExclude = "select * from excludedServers;";
		
		addBedLocation = "insert into player_beds (uuid, server, world_uuid, "
				+ "x, y, z) values (?,?,?,?,?,?)";
		getAllBedLocation = "select * from player_beds;";
		removeBedLocation = "delete from player_beds where uuid = ?;";
	}
	
	/**
	 * Adds a portal instance to the database.  Should be called only when
	 * initially creating a Portal Object.
	 */
	public void addPortal(Portal portal){
		PreparedStatement addPortalLoc = db.prepareStatement(this.addPortalLoc);
		try {
			if (portal instanceof CuboidPortal){
				CuboidPortal p = (CuboidPortal) portal;
				addPortalLoc.setInt(1, p.getXRange());
				addPortalLoc.setInt(2, p.getYRange());
				addPortalLoc.setInt(3, p.getZRange());
				addPortalLoc.setInt(4, p.getCornerBlockLocation().getBlockX());
				addPortalLoc.setInt(5, p.getCornerBlockLocation().getBlockY());
				addPortalLoc.setInt(6, p.getCornerBlockLocation().getBlockZ());
				addPortalLoc.setString(7, p.getCornerBlockLocation().getWorld().getName());
				addPortalLoc.setString(8, p.getName());
				addPortalLoc.execute();
			}
		} catch (SQLException e) {
		e.printStackTrace();
		}
	}
	
	/**
	 * This method is called internally to remove the player from the cache.
	 * Minecraft executes the code to save the player so nothing needs to be
	 * done from this plugin's point of view.
	 * @param uuid The uuid of the player
	 */
	public void playerQuitServer(UUID uuid) {
		invCache.remove(uuid);
	}
	
	private String serverName = plugin.getCurrentServerName();
	public void addPortalData(Portal portal, Portal connection){
		PreparedStatement addPortalData = db.prepareStatement(this.addPortalData);
		try {
			addPortalData.setString(1, portal.getName());
			addPortalData.setString(2, serverName);
			addPortalData.setInt(3, portal.getType().ordinal());
			String name = null;
			if (connection != null)
				name = connection.getName();
			addPortalData.setString(4, name);
			addPortalData.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void savePlayerData(UUID uuid, ByteArrayOutputStream output, InventoryIdentifier id) {
		invCache.remove(uuid); // So if it is loaded again it is recaught.
		PreparedStatement addPlayerData = db.prepareStatement(this.addPlayerData);
		removePlayerData(uuid, id); // So player data won't throw mysql error.
		try {
			addPlayerData.setString(1, uuid.toString());
			addPlayerData.setBytes(2, output.toByteArray());
			addPlayerData.setInt(3, id.ordinal());
			addPlayerData.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ByteArrayInputStream loadPlayerData(UUID uuid, InventoryIdentifier id){
		// Here we had it caches before hand so no need to load it again.
		if (invCache.containsKey(uuid))
			return invCache.get(uuid);
		PreparedStatement getPlayerData = db.prepareStatement(this.getPlayerData);
		try {
			getPlayerData.setString(1, uuid.toString());
			getPlayerData.setInt(2, id.ordinal());
			ResultSet set = getPlayerData.executeQuery();
			if (!set.next())
				return new ByteArrayInputStream(new byte[0];
			return new ByteArrayInputStream(set.getBytes("entity"));			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ByteArrayInputStream(null);
	}
	
	/**
	 * Can only be from worlds that are valid on this server.
	 * This is to prevent possible NullPointExceptions from trying to 
	 * get portals from Worlds that are not present on this server.
	 */
	public List<Portal> getAllPortalsByWorld(World[] worlds){
		List<Portal> portals = new ArrayList<Portal>();
		for (World w: worlds){
			String world = w.getName();
			PreparedStatement getPortalLocation = db.prepareStatement(getPortalLocByWorld);
			try {
				getPortalLocation.setString(1, world);
				ResultSet set = getPortalLocation.executeQuery();
				while (set.next()) {
					int rangex = set.getInt("rangex");
					int rangey = set.getInt("rangey");
					int rangez = set.getInt("rangez");
					int x = set.getInt("x");
					int y = set.getInt("y");
					int z = set.getInt("z");
					String id = set.getString("id");

					Location loc = new Location(w, x, y, z);
					Portal p = getPortalData(id, loc, rangex, rangey, rangez);
					portals.add(p);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return portals;
	}
	
	public Portal getPortal(String name) {
		PreparedStatement getPortalData = db.prepareStatement(this.getPortalLoc);
		try {
			getPortalData.setString(1, name);
			ResultSet set = getPortalData.executeQuery();
			if (!set.next())
				return null;
			int rangex = set.getInt("rangex");
			int rangey = set.getInt("rangey");
			int rangez = set.getInt("rangez");
			int x = set.getInt("x");
			int y = set.getInt("y");
			int z = set.getInt("z");
			String world = set.getString("world");
			World w = Bukkit.getWorld(world);
			Location corner = null;
			
			// If the World object does not equal null then we know that it is on this server.
			if (w != null)
				corner = new Location(w, x, y, z);
			
			return getPortalData(name, corner, rangex, rangey, rangez);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private Portal getPortalData(String name, Location corner, int xrange, int yrange, int zrange){
		PreparedStatement getPortalData = db.prepareStatement(this.getPortalData);
		try {
			getPortalData.setString(1, name);
			ResultSet set = getPortalData.executeQuery();
			if (!set.next())
				return null;
			PortalType type = PortalType.fromOrdeal(set.getInt("portal_type"));
			String serverName = set.getString("server_name");
			String partner = set.getString("partner_id");
			boolean currentServer = corner != null;
			switch (type) {
			case CUBOID:
				CuboidPortal p = new CuboidPortal(name, corner, xrange, yrange, zrange, partner, currentServer);
				p.setServerName(serverName);
				return p;
			default:
				return null;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void removePlayerData(UUID uuid, InventoryIdentifier id) {
		PreparedStatement removePlayerData = db.prepareStatement(this.removePlayerData);
		try {
			removePlayerData.setString(1, uuid.toString());
			removePlayerData.setInt(2, id.ordinal());
			removePlayerData.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void removePortalLoc(Portal p) {
		PreparedStatement removePortalLoc = db.prepareStatement(this.removePortalLoc);
		try {
			removePortalLoc.setString(1, p.getName());
			removePortalLoc.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void removePortalData(Portal p) {
		PreparedStatement removePortalData = db.prepareStatement(this.removePortalData);
		try {
			removePortalData.setString(1, p.getName());
			removePortalData.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void updatePortalData(Portal p) {
		PreparedStatement updatePortalData = db.prepareStatement(this.updatePortalData);
		try {
			String partner = null;
			if (p.getPartnerPortal() != null)
				partner = p.getPartnerPortal().getName();
			updatePortalData.setString(1, partner);
			updatePortalData.setString(2, p.getName());
			updatePortalData.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addExclude(String server) {
		PreparedStatement addExclude = db.prepareStatement(this.addExclude);
		try {
			addExclude.setString(1, server);
			addExclude.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getAllExclude() {
		PreparedStatement getAllExclude = db.prepareStatement(this.getAllExclude);
		StringBuilder builder = new StringBuilder();
		try {
			ResultSet set = getAllExclude.executeQuery();
			while (set.next())
				builder.append(set.getString("name") + " ");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return builder.toString();
	}
	
	public void removeExclude(String server) {
		PreparedStatement removeExclude = db.prepareStatement(this.removeExclude);
		try {
			removeExclude.setString(1, server);
			removeExclude.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addBedLocation(BedLocation bed) {
		PreparedStatement addBedLocation = db.prepareStatement(this.addBedLocation);
		try {
			addBedLocation.setString(1, bed.getUUID().toString());
			addBedLocation.setString(2, bed.getServer());
			String[] locs = bed.getLocation().split(" ");
			addBedLocation.setString(3, locs[0]);
			addBedLocation.setInt(4, Integer.parseInt(locs[1]));
			addBedLocation.setInt(5, Integer.parseInt(locs[2]));
			addBedLocation.setInt(6, Integer.parseInt(locs[3]));
			addBedLocation.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<BedLocation> getAllBedLocations() {
		List<BedLocation> beds = new ArrayList<BedLocation>();
		PreparedStatement getAllBedLocation = db.prepareStatement(this.getAllBedLocation);
		try {
			ResultSet set = getAllBedLocation.executeQuery();
			while (set.next()) {
				UUID uuid = UUID.fromString(set.getString("uuid"));
				String server = set.getString("server");
				UUID world_uuid = UUID.fromString(set.getString("world_uuid"));
				int x = set.getInt("x");
				int y = set.getInt("y");
				int z = set.getInt("z");
				String loc = world_uuid.toString() + " " + x + " " + y + " " + z;
				BedLocation bed = new BedLocation(uuid, loc, server);
				beds.add(bed);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return beds;
	}
	
	public void removeBed(UUID uuid) {
		PreparedStatement removeBedLocation = db.prepareStatement(this.removeBedLocation);
		try {
			removeBedLocation.setString(1, uuid.toString());
			removeBedLocation.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
