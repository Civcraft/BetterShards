package vg.civcraft.mc.bettershards.portal;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public enum PortalType {

	CUBOID;
	
	public static PortalType fromOrdeal(int num) {
		switch(num){
		case 0:
			return PortalType.CUBOID;
		}
		return null;
	}
	
	public static void sendPlayerErrorMessage(Player p) {
		String message = ChatColor.RED + "Please type in the number that refers to the portal type.\n";
		for (PortalType t: PortalType.values())
			message += t.ordinal() + ": " + t.name();
		p.sendMessage(message);
	}
}
