package vg.civcraft.mc.bettershards.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.WorldNBTStorage;

public class CustomWorldNBTStorage extends WorldNBTStorage {

	private DatabaseManager db = BetterShardsPlugin.getInstance()
			.getDatabaseManager();

	public CustomWorldNBTStorage(File file, String s, boolean flag) {
		super(file, s, flag);
	}

	@Override
	public void save(EntityHuman entityhuman) {
		try {
			NBTTagCompound nbttagcompound = new NBTTagCompound();

			entityhuman.e(nbttagcompound);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			NBTCompressedStreamTools.a(nbttagcompound, output);
			// Now to run our custom mysql code
			db.savePlayerData(entityhuman.getUniqueID(), output);

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
			ByteArrayInputStream input = db.loadPlayerData(entityhuman
					.getUniqueID());

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
			if ((entityhuman instanceof EntityPlayer)) {
				CraftPlayer player = (CraftPlayer) entityhuman
						.getBukkitEntity();

				long modified = player.getLastPlayed();
				if (modified < player.getFirstPlayed()) {
					player.setFirstPlayed(modified);
				}
			}
			entityhuman.f(nbttagcompound);
		}
		return nbttagcompound;
	}

	@Override
	public NBTTagCompound getPlayerData(String s) {
		try {
			ByteArrayInputStream input = db.loadPlayerData(UUID.fromString(s));
			return NBTCompressedStreamTools.a(input);
		} catch (Exception localException) {
			BetterShardsPlugin
					.getInstance()
					.getLogger()
					.log(Level.SEVERE,
							"Failed to load player data for " + s);
		}
		return null;
	}
}
