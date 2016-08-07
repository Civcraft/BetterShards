package vg.civcraft.mc.bettershards.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	private boolean disableFirstJoin = false;
	private List <Material> ignoreMaterials;

	public RandomSpawn(Integer spawnRange, String worldName, List <String> ignoreMats) {
		dbm = BetterShardsPlugin.getInstance().getDatabaseManager();
		this.spawnRange = spawnRange;
		this.w = BetterShardsPlugin.getInstance().getServer()
				.getWorld(worldName);
		this.ignoreMaterials = new ArrayList<Material>();
		for(String ign : ignoreMats) {
		    try {
		    	Material m = Material.valueOf(ign);
		    	BetterShardsPlugin.getInstance().info("Ignoring " + m.toString() + " for random spawning");
		    	ignoreMaterials.add(m);
		    } catch (IllegalArgumentException e) {
		    	BetterShardsPlugin.getInstance().warning("The randomspawn ignore material specified as " + ign + " is not valid. It was ignored");
		    }
		}
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
		if (servers.isEmpty()) {
			p.teleport(getLocation());
			return;
		}
		Map<String, DatabaseManager.PriorityInfo> priorityServers = dbm.getPriorityServers();
		int serverIndex = -1;
		if (!priorityServers.isEmpty()) {
			List<String> rndPriorityServers = new ArrayList <String> (priorityServers.keySet());
			Collections.shuffle(rndPriorityServers);
			for (String rndServer : rndPriorityServers) {
				int currentPopulation = MercuryAPI.getAllAccountsByServer(rndServer).size();
				if (rndServer.equals(MercuryAPI.serverName()))
					currentPopulation--;
				if (currentPopulation < priorityServers.get(rndServer).getPopulationCap()) {
					serverIndex = servers.indexOf(rndServer);
					break;
				}
			}
		}
		if (serverIndex < 0) {
			serverIndex = (int) (Math.random() * servers.size());
		}
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
		servers.add(MercuryAPI.serverName());
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
	    return getLocation(0);
	}

	/**
	 * Attempts to find a valid spawn location recursively. This method will not spawn in caves initially, 
	 * but once it has failed to find a valid spawning spot 10 times, it might
	 * 
	 * @param depth How often a valid spawning spot was attempted to be found recursively
	 * @return valid spawning spot
	 */
	private Location getLocation(int depth) {
	    
		int currentDepth = depth + 1;
		int x = (int) (spawnRange * Math.random());
		x = x * (Math.random() > 0.5 ? 1 : -1);
		int z = (int) (spawnRange * Math.random());
		z = z * (Math.random() > 0.5 ? 1 : -1);

		if (Math.sqrt((double) ((x * x) + (z * z))) > spawnRange) {
			// the location is outside the circle, even though x and z are in
			// range, so we try again
			return getLocation(currentDepth);
		}
		int y = 253;

		while (y >= 0) {
			if (w.getBlockAt(x, y, z).getType() != Material.AIR) {
				if (w.getBlockAt(x, y, z).getType().isSolid() && !ignoreMaterials.contains(w.getBlockAt(x, y, z).getType())
						&& w.getBlockAt(x, y + 1, z).getType() == Material.AIR
						&& w.getBlockAt(x, y + 2, z).getType() == Material.AIR) {
					return centerLocation(new Location(w, x, y + 1, z)); //+1 because player position is in lower body half
				}
				else {
				    if (currentDepth <= 10) {
					return getLocation(currentDepth);
				    }
				}
			}
			y--;
		}
		return getLocation(currentDepth);
	}
	
	/**
	 * When using flat block coordinates to spawn players, they will always spawn right on the edge of a block or possibly inside an adjacent block
	 * To fix this, this method will increase x and z by 0.5 to center the spawn point on the chosen block
	 * @param loc Location to adjust
	 * @return New adjusted location
	 */
	public static Location centerLocation(Location loc) {
	    return new Location(loc.getWorld(), loc.getX() + 0.5, loc.getY(), loc.getZ() + 0.5);
	}
	
	public World getWorld() {
	    return w;
	}

}
