package vg.civcraft.mc.bettershards;

import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.inventory.PlayerInfo;
import vg.civcraft.mc.bettershards.portal.Portal;

public class BetterShardsAPI {

	private static BetterShardsPlugin plugin;
	private static DatabaseManager db;
	
	public BetterShardsAPI() {
		plugin = BetterShardsPlugin.getInstance();
		db = plugin.getDatabaseManager();
	}
	
	public static void connectPlayer(Player p, String serverName, PlayerChangeServerReason reason) {
		savePlayerInfo(p, null);
		plugin.addPlayerToTransit(p.getUniqueId());
		plugin.teleportPlayerToServer(p, serverName, reason);
	}
	
	public static void connectPlayer(Player p, Portal portal, PlayerChangeServerReason reason) {
		savePlayerInfo(p, portal.getName());
		plugin.addPlayerToTransit(p.getUniqueId());
		plugin.teleportPlayerToServer(p, portal.getServerName(), reason);
	}
	
	private static void savePlayerInfo(Player p, String portal) {
		PlayerInfo pInfo = new PlayerInfo(p);
		// Also when we aren't pressured add entities to the things that can cross.
		db.addPlayerData(p, pInfo, null, portal);
	}

	public static String getServerName() {
		return BetterShardsPlugin.getCurrentServerName();
	}
	
	public static PortalsManager getPortalsManager() {
		return BetterShardsPlugin.getInstance().getPortalManager();
	}
}
