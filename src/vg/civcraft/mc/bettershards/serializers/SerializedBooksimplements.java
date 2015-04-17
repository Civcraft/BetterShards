package vg.civcraft.mc.bettershards.serializers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/** 
 * The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
 * All other rights are reserved.
 */
public class SerializedBooksimplements implements Serializable{
	private Map<String, Integer> enchantments = new HashMap<String, Integer>(); // enchant name, enchant level
	/**
	 * 
	 */
	private static final long serialVersionUID = -7927364919320998830L;
	public SerializedBooksimplements(EnchantmentStorageMeta esm){
		Map<Enchantment, Integer> enc = esm.getStoredEnchants();
		for (Enchantment ench: enc.keySet()){
			enchantments.put(ench.getName(), enc.get(ench));
		}
	}
	
	public Map<String, Integer> getData(){
		return enchantments;
	}
}
