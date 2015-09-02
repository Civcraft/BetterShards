package vg.civcraft.mc.bettershards;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import net.minecraft.server.v1_8_R3.IDataManager;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.WorldServer;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import vg.civcraft.mc.bettershards.command.BetterCommandHandler;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerEvent;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.listeners.BetterShardsListener;
import vg.civcraft.mc.bettershards.listeners.MercuryListener;
import vg.civcraft.mc.bettershards.misc.CustomWorldNBTStorage;
import vg.civcraft.mc.bettershards.misc.Grid;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.Config;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.config.MercuryConfigManager;

public class BetterShardsPlugin extends ACivMod{

	private static BetterShardsPlugin plugin;
	private PortalsManager pm;
	private DatabaseManager db;
	private static Config config;
	private static String servName;

	private Map<Player, Grid> grids = new HashMap<Player, Grid>();
	
	private List<UUID> transit = new ArrayList<UUID>();
	
	@Override
	public void onEnable(){
		super.onEnable();
		plugin = this;
		config = GetConfig();
		servName = MercuryConfigManager.getServerName();
		registerMercuryChannels();
		db = new DatabaseManager();
		if (!db.isConnected())
			Bukkit.getPluginManager().disablePlugin(this);
		pm = new PortalsManager();
		pm.loadPortalsManager();
		registerListeners();
		setWorldNBTStorage();
		uploadExistingPlayers();
		
		new BetterShardsAPI();
		
		handle = new BetterCommandHandler();
		handle.registerCommands();
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

			@Override
			public void run() {
				sendBungeeUpdateMessage();
			}
			
		}, 100, 1000);
	}
	
	@Override
	public void onDisable(){
		for (Player p: Bukkit.getOnlinePlayers())
			p.kickPlayer("Kicking in order to make sure your data is saved!");
	}
	
	/**
	 * @return Returns the instance of the JavaPlugin.
	 */
	public static BetterShardsPlugin getInstance(){
		return plugin;
	}
	/**
	 * This adds a player to a list that can be checked to see if a player is in transit.
	 */
	public void addPlayerToTransit(final UUID uuid){
		transit.add(uuid);
		Bukkit.getScheduler().runTaskLater(this, new Runnable(){

			@Override
			public void run() {
				transit.remove(uuid);
			}
			
		}, 20);
	}
	/**
	 * Checks if a player is in transit.
	 */
	public boolean isPlayerInTransit(UUID uuid){
		return transit.contains(uuid);
	}
	/**
	 * Teleports a player to a specific server.
	 * @param p- The Player to teleport.
	 * @param server- The server to teleport the player to.
	 */
	public boolean teleportPlayerToServer(Player p, String server, PlayerChangeServerReason reason){
		if (isPlayerInTransit(p.getUniqueId())) // Somehow this got triggered twice for one reason or another
				return false; // We dont wan't to continue twice because it could cause issues with the db.
		PlayerChangeServerEvent event = new PlayerChangeServerEvent(reason, p.getUniqueId(), server);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return false;
		CustomWorldNBTStorage.getWorldNBTStorage().save(((CraftPlayer) p).getHandle());
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(server);
		p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
		return true;
	}
	
	public DatabaseManager getDatabaseManager(){
		return db;
	}
	
	private void registerListeners(){
		// Register bukkit channel
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		// Register Bukkit Event Listener.
		BetterShardsListener l = new BetterShardsListener();
		getServer().getPluginManager().registerEvents(l, this);
		getServer().getPluginManager().registerEvents(new MercuryListener(), this);
	}
	
	public static String getCurrentServerName(){
		return servName;
	}
	
	public PortalsManager getPortalManager(){
		return pm;
	}

	@Override
	protected String getPluginName() {
		return "BetterShardsPlugin";
	}
	/*
	 * This will always return a grid object for the player.
	 */
	public Grid getPlayerGrid(Player p) {
		Grid g = grids.get(p);
		if (g == null) {
			g = new Grid(p, null, null);
			grids.put(p, g);
		}
		return g;
	}
	
	public void sendPortalDelete(String name) {
		MercuryAPI.instance.sendMessage("all", "delete " + name, "BetterShards");
	}
	
	public void teleportPlayer(UUID uuid, String location) {
		MercuryAPI.instance.sendMessage("all", "teleport teleport " + uuid.toString() + " " + location, "BetterShards");
	}
	
	public void teleportPlayer(UUID uuid, Portal p) {
		MercuryAPI.instance.sendMessage("all", "teleport portal " + uuid.toString() + " " + p.getName(), "BetterShards");
	}
	
	private void registerMercuryChannels() {
		MercuryAPI.instance.registerPluginMessageChannel("BetterShards");
	}
	
	private void setWorldNBTStorage() {
		for (World w: Bukkit.getWorlds()) {
			WorldServer nmsWorld = ((CraftWorld) w).getHandle();
			Field fieldName;
			try {
				fieldName = net.minecraft.server.v1_8_R3.World.class.getDeclaredField("dataManager");
				fieldName.setAccessible(true);
				
				IDataManager manager = nmsWorld.getDataManager();
				CustomWorldNBTStorage newStorage = new CustomWorldNBTStorage(manager.getDirectory(), "", true);
				setFinalStatic(fieldName, newStorage, nmsWorld);
				
				MinecraftServer.getServer().getPlayerList().playerFileData = newStorage;
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void setFinalStatic(Field field, Object newValue, Object obj) {
		try {
			field.setAccessible(true);

			// remove final modifier from field
			Field modifiersField;
			modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField
					.setInt(field, field.getModifiers() & ~Modifier.PROTECTED);
			
			field.set(obj, newValue);
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void uploadExistingPlayers() {
		for (World w: Bukkit.getWorlds()) {
			WorldServer nmsWorld = ((CraftWorld) w).getHandle();
			IDataManager data = nmsWorld.getDataManager();
			String[] names = data.getPlayerFileData().getSeenPlayers();
			for (String name: names) {
				Bukkit.getLogger().log(Level.INFO, "Updating player " + name + " to mysql.");
				try {
					File file = new File(data.getDirectory() + File.separator + "playerdata", name + ".dat");
					InputStream stream = new FileInputStream(file);
					
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					output.write(IOUtils.toByteArray(stream));
					// Now to run our custom mysql code
					db.savePlayerData(UUID.fromString(name), output);
					file.delete();
				} catch (Exception localException) {
					localException.printStackTrace();
				}
			}
		}
	}
	
	public void sendBungeeUpdateMessage() {
		MercuryAPI.instance.sendMessage("all", "removeServer " + db.getAllExclude(), "BetterShards");
	}
}