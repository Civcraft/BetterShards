package vg.civcraft.mc.bettershards.serializers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;

/** 
 * The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
 * All other rights are reserved.
 */
public class SerializedPotionEffects implements Serializable{

	private static final long serialVersionUID = 4521462593924796935L;
	private List<SerializePotion> potions = new ArrayList<SerializePotion>();
	
	public SerializedPotionEffects(Collection<PotionEffect> effects){
		for (PotionEffect e: effects){
			potions.add(new SerializePotion(e));
		}
	}
	
	public void unpackPlayerEffects(LivingEntity p){
		for (SerializePotion sp: potions)
			sp.unpackPotionEffectPlayer(p);
	}
}
