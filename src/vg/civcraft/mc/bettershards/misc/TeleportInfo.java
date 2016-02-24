package vg.civcraft.mc.bettershards.misc;

public class TeleportInfo {

	private String world;
	private String server;
	private int x, y, z;
	
	public TeleportInfo(String world, String server, int x, int y, int z) {
		this.world = world;
		this.server = server;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Return The location of the player's bed format: world_name x y z
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(world);
		builder.append("|");
		builder.append(x);
		builder.append("|");
		builder.append(y);
		builder.append("|");
		builder.append(z);
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final TeleportInfo other = (TeleportInfo) obj;

		return world.equals(other.getWorld()) && server.equalsIgnoreCase(other.getServer()) && x == other.getX()
				&& y == other.getY() && z == other.getZ();
	}

	public String getWorld() {
		return world;
	}
	
	public String getServer() {
		return server;
	}
	
	public void setWorld(String world){
		this.world = world;
	}
	
	public void setServer(String server) {
		this.server = server;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
}
