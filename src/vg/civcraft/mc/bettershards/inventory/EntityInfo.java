package vg.civcraft.mc.bettershards.inventory;

import java.io.Serializable;
import java.util.Collection;

import org.bukkit.entity.Damageable;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import com.gmail.rourke7501.serializers.SerializeInventoryContents;
import com.gmail.rourke7501.serializers.SerializeVector;
import com.gmail.rourke7501.serializers.SerializedPotionEffects;

/** 
* The redistribution and use this software or source for any hosting services without written permission from the author (Rourke750) is prohibited.
* All other rights are reserved.
*/

/*
 * This class handles all the info about the entity that the player was possibly travelling on.
 */
public class EntityInfo implements Serializable{

	private static final long serialVersionUID = -6747360548691930151L;
	
	private boolean breed;
	private SerializedPotionEffects spe;
	private int age;
	private Horse.Color color;
	private String horseName;
	private int domestic;
	private double health;
	private SerializeInventoryContents sInv;
	private double jump;
	private String styleName;
	private String horseVar;
	private SerializeVector vector;
	private SerializeInventoryContents sContents;
	
	public EntityInfo(Horse horse){
		breed = horse.canBreed();
		Collection<PotionEffect> effects = horse.getActivePotionEffects();
		spe = new SerializedPotionEffects(effects);
		age = horse.getAge();
		color = horse.getColor();
		horseName = horse.getCustomName();
		domestic = horse.getDomestication();
		health = ((Damageable) horse).getHealth();
		HorseInventory inv = horse.getInventory();
		sInv = new SerializeInventoryContents(inv.getContents());
		ItemStack[] extra = new ItemStack[]{
				inv.getArmor(), inv.getSaddle()
		};
		sContents = new SerializeInventoryContents(extra);
		jump = horse.getJumpStrength();
		Horse.Style style = horse.getStyle();
		styleName = style.name();
		Horse.Variant var = horse.getVariant();
		horseVar = var.name();
		Vector vec = horse.getVelocity();
		vector = new SerializeVector(vec);
	}
	
	public void unpackHorse(Horse horse){
		horse.setBreed(breed);
		spe.unpackPlayerEffects(horse);
		horse.setAge(age);
		horse.setColor(color);
		horse.setCustomName(horseName);
		horse.setDomestication(domestic);
		horse.setHealth(health);
		horse.getInventory().setContents(sInv.unPackInv());
		ItemStack[] stacks = sContents.unPackInv();
		horse.getInventory().setArmor(stacks[0]);
		horse.getInventory().setSaddle(stacks[1]);
		horse.setJumpStrength(jump);
		horse.setStyle(Horse.Style.valueOf(styleName));
		horse.setVariant(Horse.Variant.valueOf(horseVar));
		horse.setVelocity(vector.unpackVector());
	}
}

