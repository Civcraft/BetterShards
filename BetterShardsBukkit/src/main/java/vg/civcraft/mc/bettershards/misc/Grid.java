package vg.civcraft.mc.bettershards.misc;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Grid {

	private Player p;
	private Location left;
	private Location right;
	
	public Grid(Player p, Location left, Location right) {
		this.p = p;
		this.left = left;
		this.right = right;
	}
	
	public Player getPlayer() {
		return p;
	}
	
	public void setLeftClickLocation(Location left) {
		this.left = left;
	}
	
	public void setRightClickLocation(Location right) {
		this.right = right;
	}
	
	public Location getLeftClickLocation() {
		return left;
	}
	
	public Location getRightClickLocation() {
		return right;
	}
	
	public Location getFocusedLocation() {
		if (left == null)
			return right;
		return left;
	}
	
	public GridLocation getMissingSelection() {
		if (left == null && right == null)
			return GridLocation.NOSELECTION;
		else if (left == null)
			return GridLocation.LEFTSELECTION;
		else if (right == null)
			return GridLocation.RIGHTSELECTION;
		else return GridLocation.SELECTION;
	}
	
	public int getXRadius() {
		if (right == null || left == null)
			return 0;
		return right.getBlockX() - left.getBlockX();
	}
	
	public int getYRadius() {
		if (right == null || left == null)
			return 0;
		return right.getBlockY() - left.getBlockY();
	}
	
	public int getZRadius() {
		if (right == null || left == null)
			return 0;
		return right.getBlockZ() - left.getBlockZ();
	}
	
	public enum GridLocation {
		LEFTSELECTION,
		RIGHTSELECTION,
		NOSELECTION,
		SELECTION;
	}
}
