package vg.civcraft.mc.bettershards.bungee;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

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
	}
	private String hasPlayedBefore;
	
	private void setStatements() {
		hasPlayedBefore = "select count(*) as count from createPortalLocData where id = ?;";
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
}
