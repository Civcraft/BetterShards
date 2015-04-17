package vg.civcraft.mc.bettershards.serializers;

import java.io.Serializable;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** 
 * The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
 * All other rights are reserved.
 */
public class SerializePotion implements Serializable{

	private static final long serialVersionUID = -3193193525292079449L;
	
	private int level;
	private int dur;
	private String type;
	
	public SerializePotion(PotionEffect effect){
		level = effect.getAmplifier();
		dur = effect.getDuration();
		type = effect.getType().getName();
	}
	
	public void unpackPotionEffectPlayer(LivingEntity p){
		p.addPotionEffect(new PotionEffect(PotionEffectType.getByName(type), dur, level));
	}
}

