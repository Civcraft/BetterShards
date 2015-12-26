package vg.civcraft.mc.bettershards;

import java.util.UUID;

import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.portal.Portal;

public class BetterShardsAPI {

	private static BetterShardsPlugin plugin;
	private static MercuryManager mercManager;
	private static DatabaseManager db;
	
	public BetterShardsAPI() {
		plugin = BetterShardsPlugin.getInstance();
		db = plugin.getDatabaseManager();
		mercManager = BetterShardsPlugin.getMercuryManager();
	}
	
	public static void connectPlayer(Player p, String serverName, PlayerChangeServerReason reason) {
		plugin.teleportPlayerToServer(p, serverName, reason);
	}
	
	public static void connectPlayer(Player p, Portal portal, PlayerChangeServerReason reason) {
		if (plugin.teleportPlayerToServer(p, portal.getServerName(), reason))
			mercManager.teleportPlayer(p.getUniqueId(), portal); // We want to do this after because we don't know if a player was teleported yet.
	}

	public static String getServerName() {
		return BetterShardsPlugin.getCurrentServerName();
	}
	
	public static PortalsManager getPortalsManager() {
		return BetterShardsPlugin.getInstance().getPortalManager();
	}
	
	/**
	 * Checks if a player has a bed.
	 * @param uuid
	 * @return Returns true if player has one, false otherwise.
	 */
	public static boolean hasBed(UUID uuid) {
		return plugin.getBed(uuid) != null;
	}
}
