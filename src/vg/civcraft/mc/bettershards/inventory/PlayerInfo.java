package vg.civcraft.mc.bettershards.inventory;

import java.io.Serializable;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;

import vg.civcraft.mc.bettershards.serializers.SerializeInventoryContents;
import vg.civcraft.mc.bettershards.serializers.SerializedPotionEffects;

/** 
* The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
* All other rights are reserved.
*/

public class PlayerInfo implements Serializable, Info{

	private static final long serialVersionUID = 5833993531522573373L;
	private SerializeInventoryContents inv;
	private SerializeInventoryContents armor;
	private SerializeInventoryContents ender;
	
	private double health;
	private int foodLevel;
	private int x, y, z;
	private String world;
	private SerializedPotionEffects effects;
	float xp;
	float exhaus;
	float saturation;
	
	public PlayerInfo(Inventory inv){
		this.inv = new SerializeInventoryContents(inv.getContents());
	}
	
	public PlayerInfo(Player p){
		this.inv = new SerializeInventoryContents(p.getInventory().getContents());
		this.armor = new SerializeInventoryContents(p.getInventory().getArmorContents());
		this.ender = new SerializeInventoryContents(p.getEnderChest().getContents());
		this.health = ((Damageable) p).getHealth();
		foodLevel = p.getFoodLevel();
		Location bed = p.getBedSpawnLocation();
		x = bed.getBlockX();
		y = bed.getBlockY();
		z = bed.getBlockZ();
		world = bed.getWorld().getName();
		Collection<PotionEffect> potions = p.getActivePotionEffects();
		effects = new SerializedPotionEffects(potions);
		xp = p.getExp();
		exhaus = p.getExhaustion();
		saturation = p.getSaturation();
	}
	
	public void unpackInventory(Inventory inv){
		inv.setContents(this.inv.unPackInv());
	}
	
	public void unpackInventory(Player p){
		p.getInventory().setContents(this.inv.unPackInv());
		p.getInventory().setArmorContents(this.armor.unPackInv());
		p.getEnderChest().setContents(this.ender.unPackInv());
		p.setHealth(health);
		p.setFoodLevel(foodLevel);
		Location bed = new Location(Bukkit.getWorld(world), x, y, z);
		p.setBedSpawnLocation(bed);
		effects.unpackPlayerEffects(p);
		p.setExp(xp);
		p.setExhaustion(exhaus);
		p.setSaturation(saturation);
	}
}

