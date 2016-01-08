package vg.civcraft.mc.bettershards.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.ServerNBTManager;

public class CustomWorldNBTStorage extends ServerNBTManager {

	private DatabaseManager db = BetterShardsPlugin.getInstance()
			.getDatabaseManager();
	
	private static CustomWorldNBTStorage storage;
	private Map<UUID, InventoryIdentifier> invs = new HashMap<UUID, InventoryIdentifier>(); 

	public CustomWorldNBTStorage(File file, String s, boolean flag) {
		super(file, s, flag);
		storage = this;
	}

	@Override
	public void save(EntityHuman entityhuman) {
		try {
			NBTTagCompound nbttagcompound = new NBTTagCompound();

			entityhuman.e(nbttagcompound);
			logInventory(nbttagcompound);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			NBTCompressedStreamTools.a(nbttagcompound, output);
			// Now to run our custom mysql code
			UUID uuid = entityhuman.getUniqueID();
			//BetterShardsPlugin.getInstance().getLogger()
			//		.log(Level.INFO, "Save for " + uuid);
			db.savePlayerData(uuid, output, getInvIdentifier(uuid));

		} catch (Exception localException) {
			BetterShardsPlugin
					.getInstance()
					.getLogger()
					.log(Level.SEVERE,
							"Failed to save player data for "
									+ entityhuman.getName());
		}
	}

	@Override
	public NBTTagCompound load(EntityHuman entityhuman) {
		NBTTagCompound nbttagcompound = null;
		try {
			UUID uuid = entityhuman.getUniqueID();
			BetterShardsPlugin.getInstance().getLogger()
					.log(Level.INFO, "Load for " + uuid);

			ByteArrayInputStream input = db.loadPlayerData(uuid, getInvIdentifier(uuid));

			nbttagcompound = NBTCompressedStreamTools.a(input);

		} catch (Exception localException) {
			BetterShardsPlugin
					.getInstance()
					.getLogger()
					.log(Level.SEVERE,
							"Failed to load player data for "
									+ entityhuman.getName());
		}
		if (nbttagcompound != null) {
			logInventory(nbttagcompound);
			if ((entityhuman instanceof EntityPlayer)) {
				CraftPlayer player = (CraftPlayer) entityhuman
						.getBukkitEntity();

				long modified = player.getLastPlayed();
				if (modified < player.getFirstPlayed()) {
					player.setFirstPlayed(modified);
				}
			}
			entityhuman.f(nbttagcompound);
		} else {
			BetterShardsPlugin.getInstance().getLogger().log(Level.INFO,
				"No player data found for " + entityhuman.getName());
		}
		return nbttagcompound;
	}

	@Override
	public NBTTagCompound getPlayerData(String s) {
		try {
			UUID uuid = UUID.fromString(s);
			//BetterShardsPlugin.getInstance().getLogger()
			//		.log(Level.INFO, "Get/Load for " + uuid);
			ByteArrayInputStream input = db.loadPlayerData(uuid, getInvIdentifier(uuid));
			NBTTagCompound nbttagcompound = NBTCompressedStreamTools.a(input);
			logInventory(nbttagcompound);
			return nbttagcompound;
		} catch (Exception localException) {
			BetterShardsPlugin
					.getInstance()
					.getLogger()
					.log(Level.SEVERE,
							"Failed to load player data for " + s);
		}
		return null;
	}
	
	public void load(Player p, InventoryIdentifier iden) {
		UUID uuid = p.getUniqueId();
		CraftPlayer cPlayer = (CraftPlayer) p;
		NBTTagCompound nbttagcompound = null;
		ByteArrayInputStream input = db.loadPlayerData(uuid, iden);
		try {
			nbttagcompound = NBTCompressedStreamTools.a(input);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//logInventory(nbttagcompound);
		cPlayer.getHandle().f(nbttagcompound);
		//BetterShardsPlugin.getInstance().getLogger().log(Level.INFO, String.format("Loaded %s (%s) "
		//		+ "inventory from non default way.", p.getName(), p.getUniqueId().toString()));
	}
	
	public void save(Player p, InventoryIdentifier iden) {
		UUID uuid = p.getUniqueId();
		CraftPlayer cPlayer = (CraftPlayer) p;
		NBTTagCompound nbttagcompound = new NBTTagCompound();

		cPlayer.getHandle().e(nbttagcompound);
		//logInventory(nbttagcompound);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			NBTCompressedStreamTools.a(nbttagcompound, output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db.savePlayerData(uuid, output, iden);
		//BetterShardsPlugin.getInstance().getLogger().log(Level.INFO, String.format("Saved %s (%s) "
		//		+ "inventory from non default way.", p.getName(), p.getUniqueId().toString()));
	}
	
	public void save(UUID uuid, NBTTagCompound nbttagcompound, InventoryIdentifier iden) {
		//logInventory(nbttagcompound);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			NBTCompressedStreamTools.a(nbttagcompound, output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db.savePlayerData(uuid, output, iden);
		//BetterShardsPlugin.getInstance().getLogger().log(Level.INFO, String.format("Saved %s "
		//		+ "inventory from non default way.", uuid.toString()));
	}

	public static CustomWorldNBTStorage getWorldNBTStorage() {
		return storage;
	}
	
	public InventoryIdentifier getInvIdentifier(UUID uuid) {
		if (!invs.containsKey(uuid))
			invs.put(uuid, InventoryIdentifier.MAIN_INV);
		return invs.get(uuid);
	}
	
	/**
	 * Sets the InventoryIdentifier of a player while logging in.
	 * It is very important that you call this method during the 
	 * AsyncPreLoginEvent with an event priority below Monitor to ensure
	 * that the inventory you want is loaded.
	 * Otherwise just call load(Player, InventoryIdentifier) to load your inv.
	 * You need to remember to reset InventoryIdentifier back to normal load if
	 * you wish for to be set back to that.  It will not do that on its own.
	 * @param uuid The uuid of the player.
	 * @param iden The Inventory you wan't to load for a player.
	 */
	public void setInventoryIdentifier(UUID uuid, InventoryIdentifier iden) {
		invs.put(uuid, iden);
	}


	private void logInventory(NBTTagCompound nbt) {
		BetterShardsPlugin.getInstance().getLogger().log(Level.INFO, String.format("Data as saved: %s ", nbt.toString()));
	}
}
