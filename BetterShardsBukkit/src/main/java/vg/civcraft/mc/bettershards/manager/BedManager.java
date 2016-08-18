package vg.civcraft.mc.bettershards.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.misc.BedLocation;

public class BedManager {

	private DatabaseManager db;
	private Map<UUID, BedLocation> beds;

	public BedManager() {
		db = BetterShardsPlugin.getDatabaseManager();
		beds = new HashMap<UUID, BedLocation>();
		loadAllBeds();
	}

	/**
	 * Loads all bed locations from the database into the cache
	 */
	private void loadAllBeds() {
		List<BedLocation> db_beds = db.getAllBedLocations();
		for (BedLocation bed : db_beds) {
			beds.put(bed.getUUID(), bed);
		}
	}

	/**
	 * Adds the given bed location to the cache
	 * 
	 * @param uuid
	 *            The UUID of the player
	 * @param bed
	 *            The BedLocation object
	 */
	public void addBedLocation(UUID uuid, BedLocation bed) {
		beds.put(uuid, bed);
	}

	/**
	 * Gets the current bed location of the player from the cache
	 * 
	 * @param uuid
	 *            The uuid of the player.
	 * @return BedLocation of the player or null if none exists
	 */
	public BedLocation getBed(UUID uuid) {
		return beds.get(uuid);
	}

	/**
	 * Removes the given players bed location from the cache, if he has one
	 * 
	 * @param uuid
	 *            The UUID of the player.
	 */
	public void removeBed(UUID uuid) {
		beds.remove(uuid);
	}

	/**
	 * @return All bed locations currently held in the cache
	 */
	public Collection<BedLocation> getAllBeds() {
		return beds.values();
	}

}
