package vg.civcraft.mc.bettershards.misc;

import java.util.ArrayList;
import java.util.List;

public enum InventoryIdentifier {

	IGNORE_INV, // The inv modifier to ignore saving 
	MAIN_INV, // The main inv that this plugin will set for players.
	CUSTOM_INV1,
	CUSTOM_INV2,
	CUSTOM_INV3;
	
	private static List<InventoryIdentifier> idents = new ArrayList<InventoryIdentifier>();
	/**
	 * 
	 * @param iden The InventoryIdentifier you want to make sure no other plugin uses.
	 * @return True if your plugin was successfully able to register the InventoryIdentifier.
	 * False if it was already taken.
	 */
	public static boolean registerIdentifier(InventoryIdentifier iden) {
		if (idents.contains(iden) || iden == InventoryIdentifier.IGNORE_INV || iden == InventoryIdentifier.MAIN_INV)
			return false;
		idents.add(iden);
		return true;
	}
}
