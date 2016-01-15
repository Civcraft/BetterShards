package vg.civcraft.mc.bettershards;

import java.util.UUID;

import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
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
	
	/**
	 * Teleports a player to a different shard.
	 * @param p The Player that you wish to connect.
	 * @param serverName The name of the server that you wish the player to be sent to.
	 * @param reason The reason to be identified by the PlayerChangeServerEvent triggered by this method.
	 * @throws PlayerStillDeadException If the player is dead than this exception is thrown. There are issues with trying
	 * to teleport a dead player.  If calling from PlayerRespawnEvent just schedule a sync method to occur after the event.
	 */
	public static void connectPlayer(Player p, String serverName, PlayerChangeServerReason reason) throws PlayerStillDeadException {
		plugin.teleportPlayerToServer(p, serverName, reason);
	}
	
	/**
	 * Teleports the specified player to the current server.
	 * @param uuid The name of said player.
	 */
	public static void requestPlayerTeleport(String name) {
		plugin.teleportOtherServerPlayer(name);
	}
	
	/**
	 * Teleports a player to a different shard.
	 * @param p The Player that you wish to connect.
	 * @param portal The portal that the player should be teleported to.
	 * @param reason The reason to be identified by the PlayerChangeServerEvent triggered by this method.
	 * @throws PlayerStillDeadException If the player is dead than this exception is thrown. There are issues with trying
	 * to teleport a dead player.  If calling from PlayerRespawnEvent just schedule a sync method to occur after the event.
	 */
	public static void connectPlayer(Player p, Portal portal, PlayerChangeServerReason reason) throws PlayerStillDeadException {
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
