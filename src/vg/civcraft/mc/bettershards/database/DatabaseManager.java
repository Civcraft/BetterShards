package vg.civcraft.mc.bettershards.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.inventory.EntityInfo;
import vg.civcraft.mc.bettershards.inventory.Info;
import vg.civcraft.mc.bettershards.inventory.PlayerInfo;
import vg.civcraft.mc.bettershards.inventory.PortalInfo;
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
	
	private String[] databases = {
		"tables/createPlayerDataTable.sql",
		"tables/createPortalDataTable.sql",
		"tables/createPortalLocData.sql"
	};
	
	private String addPlayerData, getPlayerData, removePlayerData;
	private String addPortalLoc, getPortalLocByWorld, getPortalLoc, removePortalLoc;
	private String addPortalData, getPortalData, removePortalData, updatePortalData;
	
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
	private boolean isValidConnection(){
		String username = config.get("mysql.host").getString();
		String host = config.get("mysql.host").getString();
		int port = config.get("mysql.port").getInt();
		String password = config.get("mysql.password").getString();
		String dbname = config.get("mysql.dbname").getString();
		db = new Database(host, port, dbname, username, password, plugin.getLogger());
		return db.connect();
	}
	
	private void executeDatabaseStatements(){
		for (String mysql: databases){
			String querry = getQuerry(mysql);
			db.execute(querry);
		}
	}
	
	private String getQuerry(String path){
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("rescources/");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line = "";
		StringBuilder builder = new StringBuilder();
		try {
			while ((line = reader.readLine()) != null)
				builder.append(line + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return builder.toString();
	}
	
	private void loadPreparedStatements(){
		addPlayerData = getQuerry("statements/insertPlayerData.sql");
		getPlayerData = getQuerry("statements/getPlayerData.sql");
		removePlayerData = getQuerry("statements/removePlayerData.sql");
		
		addPortalLoc = getQuerry("statements/insertPortalLoc.sql");
		getPortalLocByWorld = getQuerry("statements/getPortalLocbyWorld.sql");
		getPortalLoc = getQuerry("statements/getPortalLoc.sql");
		removePortalLoc = getQuerry("statements/removePortalLoc.sql");
		
		addPortalData = getQuerry("statements/insertPortalData.sql");
		getPortalData = getQuerry("statements/getPortalData.sql");
		removePortalData = getQuerry("statements/removePortalData.sql");
		updatePortalData = getQuerry("statements/updatePortalData.sql");
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
				addPortalLoc.setInt(2,p.getYRange());
				addPortalLoc.setInt(3, p.getZRange());
				addPortalLoc.setInt(4, p.getCornerBlockLocation().getBlockX());
				addPortalLoc.setInt(5, p.getCornerBlockLocation().getBlockY());
				addPortalLoc.setInt(6, p.getCornerBlockLocation().getBlockZ());
				addPortalLoc.setString(7, p.getCornerBlockLocation().getWorld().toString());
				addPortalLoc.setString(8, p.getName());
				addPortalLoc.execute();
			}
		} catch (SQLException e) {
		e.printStackTrace();
		}
	}
	
	private String serverName = plugin.getName();
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
	
	public void addPlayerData(Player p, PlayerInfo pInfo, EntityInfo eInfo, String portal){
		PreparedStatement addPlayerData = db.prepareStatement(this.addPlayerData);
		try {
			addPlayerData.setString(1, p.getUniqueId().toString());
			addPlayerData.setObject(2, pInfo);
			addPlayerData.setObject(3, eInfo);
			addPlayerData.setString(4, portal);
			addPlayerData.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<Info> getPlayerData(UUID uuid){
		PreparedStatement getPlayerData = db.prepareStatement(this.getPlayerData);
		List<Info> infos = new ArrayList<Info>();
		try {
			getPlayerData.setString(1, uuid.toString());
			ResultSet set = getPlayerData.executeQuery();
			if (!set.next())
				return null;
			
			Object obj = set.getObject("object");
			Object ent = set.getObject("entity");
			String portal_id = set.getString("portal_id");
			
			PlayerInfo pInfo = (PlayerInfo) obj;
			EntityInfo eInfo = (EntityInfo) ent;
			PortalInfo portalInfo = new PortalInfo(portal_id);
			
			infos.add(pInfo);
			infos.add(eInfo);
			infos.add(portalInfo);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return infos;
	}
	
	/**
	 * Can only be from worlds that are valid on this server.
	 * This is to prevent possible NullPointExceptions from trying to 
	 * get portals from Worlds that are not present on this server.
	 */
	public List<Portal> getAllPortalsByWorld(World... worlds){
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
			PortalType type = PortalType.valueOf(set.getString("portal_type"));
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
	
	public void removePlayerData(UUID uuid) {
		PreparedStatement removePlayerData = db.prepareStatement(this.removePlayerData);
		try {
			removePlayerData.setString(1, uuid.toString());
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
			updatePortalData.setString(1, p.getPartnerPortal().getName());
			updatePortalData.setString(2, p.getName());
			updatePortalData.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
