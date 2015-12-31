package vg.civcraft.mc.bettershards.listeners;

import java.lang.reflect.Field;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.FoodMetaData;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minelink.ctplus.compat.api.NpcIdentity;
import net.minelink.ctplus.compat.v1_8_R3.NpcPlayer;
import net.minelink.ctplus.event.NpcDespawnEvent;
import net.minelink.ctplus.event.NpcDespawnReason;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import vg.civcraft.mc.bettershards.misc.CustomWorldNBTStorage;
import vg.civcraft.mc.bettershards.misc.InventoryIdentifier;

/**
 * This class is basically copied from 
 * https://github.com/Civcraft/CombatTagPlus
 * as the original programmers did a different way of saving the player data so it doesn't trigger MC.
 * We need to properly save the dead player.
 */
public class CombatTagListener implements Listener{
	
	private CustomWorldNBTStorage storage = CustomWorldNBTStorage.getWorldNBTStorage();
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void combatTagPlusEntityDespawn(NpcDespawnEvent event) {
		if (!event.getDespawnReason().equals(NpcDespawnReason.DEATH))
			return;
		Player player = event.getNpc().getEntity();
		EntityPlayer entity = ((CraftPlayer) player).getHandle();

        if (!(entity instanceof NpcPlayer)) {
            throw new IllegalArgumentException();
        }

        NpcPlayer npcPlayer = (NpcPlayer) entity;
        NpcIdentity identity = npcPlayer.getNpcIdentity();
        Player p = Bukkit.getPlayer(identity.getId());
        if (p != null && p.isOnline()) return;
        
        NBTTagCompound playerNbt = storage.getPlayerData(identity.getId().toString());
     // foodTickTimer is now private in 1.8.3
        Field foodTickTimerField;
        int foodTickTimer;

        try {
            foodTickTimerField = FoodMetaData.class.getDeclaredField("foodTickTimer");
            foodTickTimerField.setAccessible(true);
            foodTickTimer = foodTickTimerField.getInt(entity.getFoodData());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        playerNbt.setShort("Air", (short) entity.getAirTicks());
        playerNbt.setFloat("HealF", entity.getHealth());
        playerNbt.setShort("Health", (short) ((int) Math.ceil((double) entity.getHealth())));
        playerNbt.setFloat("AbsorptionAmount", entity.getAbsorptionHearts());
        playerNbt.setInt("XpTotal", entity.expTotal);
        playerNbt.setInt("foodLevel", entity.getFoodData().foodLevel);
        playerNbt.setInt("foodTickTimer", foodTickTimer);
        playerNbt.setFloat("foodSaturationLevel", entity.getFoodData().saturationLevel);
        playerNbt.setFloat("foodExhaustionLevel", entity.getFoodData().exhaustionLevel);
        playerNbt.setShort("Fire", (short) entity.fireTicks);
        playerNbt.set("Inventory", npcPlayer.inventory.a(new NBTTagList()));
        storage.save(identity.getId(), playerNbt, InventoryIdentifier.MAIN_INV);
	}

}
