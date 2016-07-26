package vg.civcraft.mc.bettershards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import vg.civcraft.mc.bettershards.misc.ChunkLookUp;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.bettershards.portal.portals.WorldBorderPortal;
import vg.civcraft.mc.civmodcore.particles.ParticleEffect;

public class ParticlesManager {
	
	private static boolean initialized = false;
	private static Map<Portal, ParticleEffect> effects = new HashMap<Portal, ParticleEffect>();
	private static Map<Portal, List<ChunkLookUp>> locations = new HashMap<Portal, List<ChunkLookUp>>();
	
	private ParticlesManager() {
		initialized = true;
		Bukkit.getScheduler().runTaskTimer(BetterShardsPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				// We now want to run through the effects and play them if the chunks with the location are loaded.
				for (Portal p: locations.keySet()) {
					playEffect(p);
				}
			}
			
		}, 10, 20);
	}

	public static boolean initialize() {
		if (initialized)
			return false;
		new ParticlesManager();
		return initialized;
	}
	
	/**
	 * This method is used to play an effect for the specified portal.
	 * @param p
	 */
	public static void playEffect(Portal p) {
		// We need to do individualized code for each type cause they work differently.
		if (p instanceof WorldBorderPortal) {
			ParticleEffect e = effects.get(p);
			// All of this is way to much. The amount of locations is around 1 million + and needs to be written better.
			// We can be guaranteed a correct x and z location but how to determine where the y should be displayed needs to be done.
			
			for (ChunkLookUp list: locations.get(p)) {
				for (Location l : list.getLocations()) {
					for (int y = 1; y < 256; y++) {
						l.getWorld().spigot().playEffect(new Location(l.getWorld(), l.getX(), y,
								l.getZ()), e.getEffect(), e.getId(), e.getData(), e.getOffsetX(), e.getOffsetY(), 
								e.getOffsetZ(), e.getSpeed(), e.getParticleCount(), e.getViewDistance());
					}
				}
			}
		}
	}
	
	public static void addPortal(Portal p) {
		ParticleEffect e = new ParticleEffect(Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, .5f, 1, 64);
		effects.put(p, e);
		locations.put(p, new ArrayList<ChunkLookUp>());
	}
	
	public static void passChunkLoadEvent(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		for (Portal p: locations.keySet()) {
			List<Location> locs = p.getLocationsInPortal(chunk);
			if (locs != null)
				System.out.println("fdsfsdfsdfsdfsdF");
			if (locs == null)
				continue;
			locations.get(p).add(new ChunkLookUp(chunk, locs));
		}
	}
	
	public static void passChunkUnLoadEvent(ChunkUnloadEvent event) {
		Chunk chunk = event.getChunk();
		for (Portal p: locations.keySet()) {
			locations.get(p).remove(new ChunkLookUp(chunk, null));
		}
	}
}
