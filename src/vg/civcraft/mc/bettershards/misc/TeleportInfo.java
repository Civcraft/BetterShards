package vg.civcraft.mc.bettershards.misc;


import java.util.UUID;

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
	
	public TeleportInfo(UUID world, String server, int x, int y, int z) {
		this.world = world.toString();
		this.server = server;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Return The location of the player's bed format: world_uuid x y z
	 * or the format world_name x y z
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
	
	public String getServer() {
		return server;
	}
	
	public void setWorld(String world) {
		this.world = world;
	}
	
	public String getWorld() {
		return world;
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
