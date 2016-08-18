package vg.civcraft.mc.bettershards.manager;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerEvent;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.external.CombatTagManager;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.misc.CustomWorldNBTStorage;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.mercury.MercuryAPI;

public class ConnectionManager {
	
	private TransitManager transitManager;
	private CombatTagManager combatManager;
	
	public ConnectionManager() {
		this.transitManager = BetterShardsPlugin.getTransitManager();
		this.combatManager = BetterShardsPlugin.getCombatTagManager();
	}
	
	/**
	 * Sends a bungee request to get the player to be sent to current server.
	 * There must be at least one player on the server for this to work.
	 * @param name Name of a player.
	 */
	public void teleportOtherServerPlayer(String name) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("ConnectOther");
		out.writeUTF(name);
		out.writeUTF(MercuryAPI.serverName());
		Bukkit.getOnlinePlayers().iterator().next().sendPluginMessage(BetterShardsPlugin.getInstance(), "BungeeCord", out.toByteArray());
	}
	
	public boolean teleportPlayerToServer(UUID playerUUID, String server, PlayerChangeServerReason reason) throws PlayerStillDeadException {
		//Get the server of the player
		String currentServer = MercuryAPI.getServerforAccount(playerUUID).getServerName();
		if(currentServer == null){
			return false;
		}
		
		if(currentServer.equals(MercuryAPI.serverName())){
			teleportPlayerToServer(Bukkit.getPlayer(playerUUID), server, reason);
			return true;
		}
		
		MercuryAPI.sendMessage(currentServer, "teleport|connect|" + playerUUID + "|" + server + "|" + reason, "BetterShards");
		return true;
	}
	
	/**
	 * Teleports a player to a specific server.
	 * @param p The Player to teleport.
	 * @param server The server to teleport the player to.
	 */
	public boolean teleportPlayerToServer(Player p, String server, PlayerChangeServerReason reason) throws PlayerStillDeadException {
		if (transitManager.isPlayerInExitTransit(p.getUniqueId())) { // Somehow this got triggered twice for one reason or another
				return false; // We dont wan't to continue twice because it could cause issues with the db.
		}
		PlayerChangeServerEvent event = new PlayerChangeServerEvent(reason, p.getUniqueId(), server);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return false;
		}
		if (combatManager.isCombatTagNPC(p)) {
			return false;
		}
		combatManager.unCombatTag(p);
		if (p.isInsideVehicle()) {
			BetterShardsPlugin.getInstance().getLogger().log(Level.INFO, "During BetterShards teleport, removing player {0} from vehicle", p.getUniqueId());
			p.getVehicle().eject();
		}
		if (p.isDead()) {
			throw new PlayerStillDeadException();
		}
		transitManager.addPlayerToExitTransit(p.getUniqueId(), server); // So the player isn't tried to be sent twice.
		MercuryManager.warnOfArrival(p.getUniqueId(), server); //so target server prepares
		CustomWorldNBTStorage st = CustomWorldNBTStorage.getWorldNBTStorage();
		st.save(p, st.getInvIdentifier(p.getUniqueId()), true);
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(server);
		p.sendPluginMessage(BetterShardsPlugin.getInstance(), "BungeeCord", out.toByteArray());
		return true;
	}
}
