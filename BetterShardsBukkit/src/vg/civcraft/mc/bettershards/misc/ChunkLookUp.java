package vg.civcraft.mc.bettershards.misc;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;

public class ChunkLookUp {

	private Chunk c;
	private List<Location> locs;
	
	public ChunkLookUp(Chunk c, List<Location> locs) {
		this.c = c;
		this.locs = locs;
	}
	
	public List<Location> getLocations() {
		return locs;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Chunk))
			return false;
		Chunk chunk = (Chunk) obj;
		return c.equals(chunk);
	}
}
