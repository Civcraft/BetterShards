package vg.civcraft.mc.bettershards.portal.portals;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.portal.Portal;

public class CuboidPortal extends Portal {

	protected Location first; // This should be the location of the first block
	protected Location second;

	public CuboidPortal(String name, Location first, Location second, String connection, boolean isOnCurrentServer) {
		super(name, connection, isOnCurrentServer, 0);
		this.first = first;
		this.second = second;
	}
	
	public Location getFirst() {
		return first;
	}
	
	public Location getSecond() {
		return second;
	}

	public boolean inPortal(Location loc) {
		return rangeCheck(first.getBlockX(), second.getBlockX(), loc.getBlockX())
				&& rangeCheck(first.getBlockY(), second.getBlockY(), loc.getBlockY())
				&& rangeCheck(first.getBlockZ(), second.getBlockZ(), loc.getBlockZ());
	}

	private boolean rangeCheck(int firstBound, int secondBound, int checkValue) {
		if (checkValue >= firstBound) {
			return checkValue <= secondBound;
		}
		return secondBound <= checkValue;
	}

	// *------------------------------------------------------------------------------------------------------------*
	// | The following chooseSpawn method contains code made by NuclearW |
	// | based on his SpawnArea plugin: |
	// |
	// http://forums.bukkit.org/threads/tp-spawnarea-v0-1-spawns-targetPlayers-in-a-set-area-randomly-1060.20408/
	// |
	// *------------------------------------------------------------------------------------------------------------*
	public Location findSpawnLocation() {
		int x = 0, y= 0, z = 0;
		int tries = 0;
		do {
			x = (int) (first.getBlockX() + (Math.random() * (second.getBlockX() - first.getBlockX())));
			z = (int) (first.getBlockZ() + (Math.random() * (second.getBlockZ() - first.getBlockZ())));
			y = (int) (first.getBlockY() + (Math.random() * (second.getBlockY() - first.getBlockY())));
			tries++;

		} while (!validSpawn(first.getWorld(), x, y, z) && tries < 100);

		if (tries == 100) {
			// still not found
			for (y = 255; y > 0; y--) {
				if (validSpawn(first.getWorld(), x, y, z)) {
					break;
				}
			}
		}
		if (y == 0) {
			y = 255;
		}
		return new Location(first.getWorld(), x, y, z);
	}

	private boolean validSpawn(World world, double x, double y, double z) {
		return world.getBlockAt(new Location(world, x, y - 1, z)).getType()
				.isSolid()
				&& world.getBlockAt(new Location(world, x, y, z)).getType() == Material.AIR
				&& world.getBlockAt(new Location(world, x, y + 1, z)).getType() == Material.AIR;
	}
	
	public void teleport(Player p) {
		if (connection == null)
			return;
		if (connection.getServerName().equals(BetterShardsAPI.getServerName())) {
			p.teleport(connection.findSpawnLocation());
			return;
		}
		try {
			BetterShardsAPI.connectPlayer(p, connection,
					PlayerChangeServerReason.PORTAL);
		} catch (PlayerStillDeadException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<Location> getLocationsInPortal(Chunk chunk) {
		if (!isOnCurrentServer())
			return null;
		// TODO: this needs work on.
		return null;
	}
}
