package vg.civcraft.mc.bettershards.serializers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/** 
 * The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
 * All other rights are reserved.
 */
public class StoredEnchantments implements Serializable{
	private static final long serialVersionUID = -3862785762573072287L;
	private Map<String, Integer> enchantments = new HashMap<String, Integer>(); // enchant name, enchant level
	
	public StoredEnchantments(ItemStack stack){
		if (stack == null) return;
		Map<Enchantment, Integer> enchs = stack.getEnchantments();
		for (Enchantment ench: enchs.keySet()){
			enchantments.put(ench.getName(), enchs.get(ench));
		}
	}
	
	public Map<String, Integer> getEnchantments(){
		return enchantments;
	}
}

