package vg.civcraft.mc.bettershards.serializers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** 
 * The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
 * All other rights are reserved.
 */
public class SerializeLore implements Serializable{
	List<String> lore = new ArrayList<String>();
	String display ="";
	/**
	 * 
	 */
	private static final long serialVersionUID = 4444354610614193435L;
	
	public SerializeLore (ItemStack stack){
		if (stack.getItemMeta() == null) return;
		if (stack.getItemMeta().getDisplayName() != null)
			display = stack.getItemMeta().getDisplayName();
		if (stack.getItemMeta().getLore() != null)
			lore.addAll(stack.getItemMeta().getLore());
	}
	
	public ItemStack getLore(ItemStack stack){
		ItemMeta meta = stack.getItemMeta();
		if (!display.equals(""))
			meta.setDisplayName(display);
		if (lore.size() > 0){
			meta.setLore(lore);
		}
		stack.setItemMeta(meta);
		return stack;
	}
}

