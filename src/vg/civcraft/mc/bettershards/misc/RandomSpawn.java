package vg.civcraft.mc.bettershards.misc;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.mercury.MercuryAPI;

public class RandomSpawn {
	private DatabaseManager dbm;
	private int spawnRange;
	private World w;
	private boolean disableFirstJoin = false;;

	public RandomSpawn(Integer spawnRange, String worldName) {
		dbm = BetterShardsPlugin.getInstance().getDatabaseManager();
		this.spawnRange = spawnRange;
		this.w = BetterShardsPlugin.getInstance().getServer()
				.getWorld(worldName);
	}

	/**
	 * Called whenever a player dies without having a bed to determine on which
	 * server he randomspawns, to get the player to that server and to ensure
	 * that the player is randomspawned on that server
	 * 
	 * @param p Player who just died
	 */
	public void handleDeath(Player p) {
		List<String> servers = getAllowedServers();
		if (servers.size() == 0) {
			 p.teleport(getLocation());
			 return;
		}
		int serverIndex = (int) (Math.random() * servers.size());
		if (servers.get(serverIndex).equalsIgnoreCase(MercuryAPI.serverName())) {
			// same server
			p.teleport(getLocation());
		} else {
			try {
				BetterShardsAPI.connectPlayer(p, servers.get(serverIndex),
						PlayerChangeServerReason.RANDOMSPAWN);
			} catch (PlayerStillDeadException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BetterShardsPlugin.getMercuryManager().notifyRandomSpawn(
					servers.get(serverIndex), p.getUniqueId());
		}
	}
	
	/**
	 * Sets whether or not to disable or enable randomspawn for players who have just joined.
	 * @param disabled Set this to true to disable randomspawn for players who are new
	 * and set to false to random spawn players who are new.
	 */
	public void setFirstJoin(boolean disabled) {
		this.disableFirstJoin = disabled;
	}
	
	/**
	 * Called when it is the first time a player joins.
	 * @param p Player who just joined.
	 */
	public void handleFirstJoin(Player p) {
		if (disableFirstJoin)
			return;
		final Player player = p;
		Bukkit.getScheduler().scheduleSyncDelayedTask(BetterShardsPlugin.getInstance(), new Runnable() {
			@Override
			public void run() {
				handleDeath(player);
			}
		});
	}

	/**
	 * @return names of all the servers players are allowed to randomspawn in
	 */
	public List<String> getAllowedServers() {
		List<String> servers = new LinkedList<String>();
		for (String s : MercuryAPI.getAllConnectedServers()) {
			servers.add(s);
		}
		for (String s : dbm.getAllExclude()) {
			servers.remove(s);
		}
		return servers;
	}

	/**
	 * Gets a random spawn location on this server in the world which was
	 * specified as spawn world in the config
	 * 
	 * @return random spawn location
	 */
	public Location getLocation() {
		int x = (int) (spawnRange * Math.random());
		x = x * (Math.random() > 0.5 ? 1 : -1);
		int z = (int) (spawnRange * Math.random());
		z = z * (Math.random() > 0.5 ? 1 : -1);

		if (Math.sqrt((double) ((x * x) + (z * z))) > spawnRange) {
			// the location is outside the circle, even though x and z are in
			// range, so we try again
			return getLocation();
		}
		int y = 253;

		while (y >= 0) {
			if (w.getBlockAt(x, y, z).getType() != Material.AIR) {
				if (w.getBlockAt(x, y, z).getType().isSolid()
						&& w.getBlockAt(x, y + 1, z).getType() == Material.AIR
						&& w.getBlockAt(x, y + 2, z).getType() == Material.AIR) {
					return new Location(w, x, y + 1, z); //+1 because player position is in lower body half
				}
				else {
					return getLocation();
				}
			}
			y--;
		}
		return getLocation();
	}

}
