package vg.civcraft.mc.bettershards.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import net.minecraft.server.v1_10_R1.DataConverterManager;
import net.minecraft.server.v1_10_R1.EntityHuman;
import net.minecraft.server.v1_10_R1.EntityPlayer;
import net.minecraft.server.v1_10_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_10_R1.NBTTagCompound;
import net.minecraft.server.v1_10_R1.ServerNBTManager;

public class CustomWorldNBTStorage extends ServerNBTManager {

	private DatabaseManager db = BetterShardsPlugin.getInstance().getDatabaseManager();
	
	private static CustomWorldNBTStorage storage;
	private Map<UUID, InventoryIdentifier> invs = new HashMap<UUID, InventoryIdentifier>(); 
	private Map<UUID, ConfigurationSection> sect = new HashMap<UUID, ConfigurationSection>();
	private Logger logger = BetterShardsPlugin.getInstance().getLogger();

	public CustomWorldNBTStorage(File file, String s, boolean flag) {
		super(file, s, flag, new DataConverterManager(0));
		storage = this;
	}

	@Override
	public void save(EntityHuman entityhuman) {
		try {
			UUID uuid = entityhuman.getUniqueID();
			logger.log(Level.INFO, "EntityHuman]* Save player data for {0}", uuid);
			
			NBTTagCompound nbttagcompound = new NBTTagCompound();

			entityhuman.e(nbttagcompound);
			//logInventory(nbttagcompound);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			NBTCompressedStreamTools.a(nbttagcompound, output);

			// Now to run our custom mysql code
			db.savePlayerData(uuid, output, getInvIdentifier(uuid), sect.get(uuid));
		} catch (Exception localException) {
			logger.log(Level.SEVERE, "EntityHuman]* Failed to save player data for {0}", entityhuman.getName());
		}
	}
	
	public void save(NBTTagCompound nbttagcompound, UUID uuid) {
		try {
			logger.log(Level.INFO, "NBTTagCompound,UUID] Save player data for {0}", uuid);

			//logInventory(nbttagcompound);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			NBTCompressedStreamTools.a(nbttagcompound, output);

			// Now to run our custom mysql code
			db.savePlayerData(uuid, output, getInvIdentifier(uuid), sect.get(uuid));
		} catch (Exception localException) {
			logger.log(Level.SEVERE, "NBTTagCompound,UUID] Failed to save player data for {0}", uuid);
		}
	}

	@Override
	public NBTTagCompound load(EntityHuman entityhuman) {
		NBTTagCompound nbttagcompound = null;
		try {
			UUID uuid = entityhuman.getUniqueID();
			logger.log(Level.INFO, "EntityHuman]* Load for {0}", uuid);

			ByteArrayInputStream input = db.loadPlayerData(uuid, getInvIdentifier(uuid));

			nbttagcompound = NBTCompressedStreamTools.a(input);
		} catch (Exception localException) {
			logger.log(Level.SEVERE, "EntityHuman]* Failed to load player data for {0}", entityhuman.getName());
		}
		
		if (nbttagcompound != null) {
			//logInventory(nbttagcompound);
			if ((entityhuman instanceof EntityPlayer)) {
				CraftPlayer player = (CraftPlayer) entityhuman.getBukkitEntity();

				long modified = player.getLastPlayed();
				if (modified < player.getFirstPlayed()) {
					player.setFirstPlayed(modified);
				}
			}
			entityhuman.f(nbttagcompound);
		} else {
			logger.log(Level.INFO, "EntityHuman]* No player data found for {0}", entityhuman.getName());
		}
		return nbttagcompound;
	}

	@Override
	public NBTTagCompound getPlayerData(String s) {
		try {
			UUID uuid = UUID.fromString(s);
			logger.log(Level.INFO, "String]* get / load for " + uuid);
			
			ByteArrayInputStream input = db.loadPlayerData(uuid, getInvIdentifier(uuid));
			NBTTagCompound nbttagcompound = NBTCompressedStreamTools.a(input);
			//logInventory(nbttagcompound);
			
			return nbttagcompound;
		} catch (Exception localException) {
			logger.log(Level.SEVERE, "String]* Failed to get / load player data for {0}", s);
		}
		return null;
	}

	public NBTTagCompound getPlayerData(UUID uuid) {
		try {
			logger.log(Level.INFO, "UUID] Get / Load player data for {0}", uuid);
			
			ByteArrayInputStream input = db.loadPlayerDataAsync(uuid, getInvIdentifier(uuid)).get();
			NBTTagCompound nbttagcompound = NBTCompressedStreamTools.a(input);
			//logInventory(nbttagcompound);
			
			return nbttagcompound;
		} catch (Exception localException) {
			logger.log(Level.SEVERE, "UUID] Failed to get / load player data for {0}", uuid);
			logger.log(Level.SEVERE, "UUID] Failed to get / load player data execption:", localException);
		}
		return null;
	}
	
	public void load(Player p, InventoryIdentifier iden) {
		UUID uuid = p.getUniqueId();
		logger.log(Level.INFO, "Player,InvIdent] Load player data for {0}", uuid);
		
		CraftPlayer cPlayer = (CraftPlayer) p;
		
		NBTTagCompound nbttagcompound = null;
		ByteArrayInputStream input = db.loadPlayerData(uuid, iden);
		if (input != null) {
			try {
				nbttagcompound = NBTCompressedStreamTools.a(input);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Player,InvIdent] Failed to load player data for {0}", uuid);
				e.printStackTrace();
			}
		}
		//logInventory(nbttagcompound);
		if (nbttagcompound == null) {
			logger.log(Level.SEVERE, "Player,InvIdent] (Load) No player data for {0}", uuid);
		}
		cPlayer.getHandle().f(nbttagcompound); // why clear the inventory b/c load failed?
	}
	
	public void save(Player p, InventoryIdentifier iden) {
		save(p, iden, false);
	}
	
	public void save(Player p, InventoryIdentifier iden, boolean async) {
		UUID uuid = p.getUniqueId();
		logger.log(Level.INFO, "Player,InvIdent,bool] Save player data for {0}", uuid);
		
		CraftPlayer cPlayer = (CraftPlayer) p;
		
		NBTTagCompound nbttagcompound = new NBTTagCompound();
		cPlayer.getHandle().e(nbttagcompound);
		//logInventory(nbttagcompound);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			NBTCompressedStreamTools.a(nbttagcompound, output);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Player,InvIdent,bool] Failed to save player data for {0}", uuid);
			e.printStackTrace();
		}
		
		if (nbttagcompound == null || output == null) {
			logger.log(Level.SEVERE, "Player,InvIdent,bool] (Save) No player data for {0}", uuid);
		}
		
		if (async){
			db.savePlayerDataAsync(uuid, output, iden, sect.get(uuid));
		} else {
			db.savePlayerData(uuid, output, iden, sect.get(uuid));
		}
	}
	
	public void save(UUID uuid, NBTTagCompound nbttagcompound, InventoryIdentifier iden) {
		save(uuid, nbttagcompound, iden, false);
	}
	
	public void save(UUID uuid, NBTTagCompound nbttagcompound, InventoryIdentifier iden, boolean async) {
		logger.log(Level.INFO, "UUID,NBTTagCompound,InvIdent,bool] Save player data for {0}", uuid);
		
		//logInventory(nbttagcompound);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			NBTCompressedStreamTools.a(nbttagcompound, output);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "UUID,NBTTagCompound,InvIdent,bool] Failed to save player data for {0}", uuid);
			e.printStackTrace();
		}

		if (nbttagcompound == null || output == null) {
			logger.log(Level.SEVERE, "UUID,NBTTagCompound,InvIdent,bool] (Save) No player data for {0}", uuid);
		}
		
		if (async) {
			db.savePlayerDataAsync(uuid, output, iden, sect.get(uuid));
		} else {
			db.savePlayerData(uuid, output, iden, sect.get(uuid));
		}
	}

	public static CustomWorldNBTStorage getWorldNBTStorage() {
		return storage;
	}
	
	public InventoryIdentifier getInvIdentifier(UUID uuid) {
		if (!invs.containsKey(uuid))
			invs.put(uuid, InventoryIdentifier.MAIN_INV);
		return invs.get(uuid);
	}
	
	public void loadConfigurationSectionForPlayer(UUID uuid, ConfigurationSection section) {
		sect.put(uuid, section);
	}
	
	/** 
	 * Gets a ConfigurationSection that allows other plugins to store data that will be saved in the 
	 * PlayerNBT Storage. Keep in mind the ConfigurationSection returned will be autosaved on save.
	 * If there is a previous ConfigurationSection this plugin will load it and return it.
	 * @param uuid The uuid of the player.
	 * @param plugin The name of the plugin for tracking purposes.
	 * @return Returns a ConfigurationSection that can be used by a plugin.  
	 */
	public ConfigurationSection getConfigurationSection(UUID uuid, JavaPlugin plugin) {
		ConfigurationSection configSect = sect.get(uuid);
		if (configSect == null) {
			configSect = new YamlConfiguration();
			sect.put(uuid, configSect);
		}
		ConfigurationSection pluginSect = configSect.getConfigurationSection(plugin.getName());
		if (pluginSect == null) {
			return configSect.createSection(plugin.getName());
		}
		return pluginSect;
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
		logger.log(Level.INFO, "Data as saved: {0} ", nbt.toString());
	}
}
