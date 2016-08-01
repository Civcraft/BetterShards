package vg.civcraft.mc.bettershards;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import net.minecraft.server.v1_10_R1.IDataManager;
import net.minecraft.server.v1_10_R1.MinecraftServer;
import net.minecraft.server.v1_10_R1.NBTTagDouble;
import net.minecraft.server.v1_10_R1.NBTTagList;
import net.minecraft.server.v1_10_R1.WorldNBTStorage;
import net.minecraft.server.v1_10_R1.WorldServer;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import vg.civcraft.mc.bettershards.command.BetterCommandHandler;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerEvent;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.events.PlayerEnsuredToTransitEvent;
import vg.civcraft.mc.bettershards.events.PlayerFailedToTransitEvent;
import vg.civcraft.mc.bettershards.external.CombatTagManager;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.listeners.BetterShardsListener;
import vg.civcraft.mc.bettershards.listeners.MercuryListener;
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.misc.CustomWorldNBTStorage;
import vg.civcraft.mc.bettershards.misc.Grid;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.misc.RandomSpawn;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.Config;
import vg.civcraft.mc.civmodcore.annotations.CivConfig;
import vg.civcraft.mc.civmodcore.annotations.CivConfigType;
import vg.civcraft.mc.civmodcore.annotations.CivConfigs;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.config.MercuryConfigManager;

public class BetterShardsPlugin extends ACivMod{

	private static BetterShardsPlugin plugin;
	private PortalsManager pm;
	private DatabaseManager db;
	private static Config config;
	private static String servName;
	private static CombatTagManager combatManager;
	private static MercuryManager mercuryManager;
	private static RandomSpawn randomSpawn;

	private Map<Player, Grid> grids = new HashMap<Player, Grid>();
	private Map<UUID, BedLocation> beds = new HashMap<UUID, BedLocation>();
	
	private List<UUID> transit = new ArrayList<UUID>();
	@CivConfigs({
		@CivConfig(name = "randomspawn.range", def = "1000", type = CivConfigType.Int),
		@CivConfig(name = "randomspawn.spawnworld", def = "world", type = CivConfigType.String),
		@CivConfig(name = "randomspawn.ignoreMaterials", type = CivConfigType.String_List)
	})
	@Override
	public void onEnable(){
		super.onEnable();
		plugin = this;
		config = GetConfig();
		servName = MercuryConfigManager.getServerName();
		mercuryManager = new MercuryManager();
		db = new DatabaseManager();
		if (!db.isConnected())
			Bukkit.getPluginManager().disablePlugin(this);
		pm = new PortalsManager();
		pm.loadPortalsManager();
		setWorldNBTStorage();
		combatManager = new CombatTagManager(getServer());
		randomSpawn = new RandomSpawn(config.get("randomspawn.range").getInt(), config.get("randomspawn.spawnworld").getString(), config.get("randomspawn.ignoreMaterials").getStringList());
		registerListeners();
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
		loadAllBeds();
	}
	
	@Override
	public void onDisable(){
		if (db != null){
			db.cleanup(); // releases all locks.
		}
		// We now want to grab every player who hasn't logged in yet but still needs their location set and we are 
		// going to manually set it.
		/*
		CustomWorldNBTStorage storage = CustomWorldNBTStorage.getWorldNBTStorage();
		Map<UUID, Location> uuids = MercuryListener.getAllRemainingTeleports();
		for (UUID uuid: uuids.keySet()) {
			Location loc = uuids.get(uuid);
			if (loc == null)
				continue;
			NBTTagCompound data = storage.getPlayerData(uuid.toString());
			if (data == null)
				continue;
			data.set("Pos", this.a(new double[] { loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()}));
			storage.save(data, uuid);
		}
		*/
	}
	
	private NBTTagList a(double... adouble) {
        NBTTagList nbttaglist = new NBTTagList();
        double[] adouble1 = adouble;
        int i = adouble.length;

        for (int j = 0; j < i; ++j) {
            double d0 = adouble1[j];

            nbttaglist.add(new NBTTagDouble(d0));
        }

        return nbttaglist;
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
	private void addPlayerToTransit(final UUID uuid, final String server){
		transit.add(uuid);
		Bukkit.getScheduler().runTaskLater(this, new Runnable(){

			@Override
			public void run() {
				transit.remove(uuid);
				Player player = Bukkit.getPlayer(uuid);
				if (player != null && !player.isOnline()) {
					PlayerFailedToTransitEvent event = new PlayerFailedToTransitEvent(uuid, server);
					Bukkit.getPluginManager().callEvent(event);
				}
				else {
					PlayerEnsuredToTransitEvent event = new PlayerEnsuredToTransitEvent(uuid, server);
					Bukkit.getPluginManager().callEvent(event);
				}
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
	 * Sends a bungee request to get the player to be sent to current server.
	 * There must be at least one player on the server for this to work.
	 * @param name Name of a player.
	 */
	public void teleportOtherServerPlayer(String name) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("ConnectOther");
		out.writeUTF(name);
		out.writeUTF(MercuryAPI.serverName());
		Bukkit.getOnlinePlayers().iterator().next().sendPluginMessage(this, "BungeeCord", out.toByteArray());
	}
	
	/**
	 * Teleports a player to a specific server.
	 * @param p The Player to teleport.
	 * @param server The server to teleport the player to.
	 */
	public boolean teleportPlayerToServer(Player p, String server, PlayerChangeServerReason reason) throws PlayerStillDeadException {
		if (isPlayerInTransit(p.getUniqueId())) // Somehow this got triggered twice for one reason or another
				return false; // We dont wan't to continue twice because it could cause issues with the db.
		PlayerChangeServerEvent event = new PlayerChangeServerEvent(reason, p.getUniqueId(), server);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled())
			return false;
		if (combatManager.isCombatTagNPC(p)) 
			return false;
		combatManager.unCombatTag(p);
		if (p.isInsideVehicle())
			p.getVehicle().eject();
		if (p.isDead()) {
			throw new PlayerStillDeadException();
		}
		addPlayerToTransit(p.getUniqueId(), server); // So the player isn't tried to be sent twice.
		CustomWorldNBTStorage st = CustomWorldNBTStorage.getWorldNBTStorage();
		st.save(p, st.getInvIdentifier(p.getUniqueId()), true);
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(server);
		p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
		return true;
	}
	
	public boolean teleportPlayerToServer(UUID playerUUID, String server, PlayerChangeServerReason reason) throws PlayerStillDeadException {
		//Get the server of the player
		String currentServer = MercuryAPI.getServerforAccount(playerUUID).getServerName();
		if(currentServer == null){
			return false;
		}
		
		if(currentServer.equals(MercuryAPI.serverName())){
			teleportPlayerToServer(Bukkit.getPlayer(playerUUID), server, reason);
			return true;
		}
		
		MercuryAPI.sendMessage(currentServer, "teleport|connect|" + playerUUID + "|" + server + "|" + reason, "BetterShards");
		return true;
	}
	
	public DatabaseManager getDatabaseManager() {
		return db;
	}
	
	public static RandomSpawn getRandomSpawn() {
		return randomSpawn;
	}
	
	private void registerListeners(){
		// Register bukkit channel
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		// Register Bukkit Event Listener.
		BetterShardsListener l = new BetterShardsListener();
		getServer().getPluginManager().registerEvents(l, this);
		getServer().getPluginManager().registerEvents(new MercuryListener(), this);
		// The register for the listener to CombatTagListener class can be found in CombatTagManager class
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
	
	private void setWorldNBTStorage() {
		for (World w: Bukkit.getWorlds()) {
			WorldServer nmsWorld = ((CraftWorld) w).getHandle();
			Field fieldName;
			try {
				fieldName = net.minecraft.server.v1_10_R1.World.class.getDeclaredField("dataManager");
				fieldName.setAccessible(true);
				
				IDataManager manager = nmsWorld.getDataManager();
				
				// Spigot has a file lock we want to try remove before invoking our own stuff.
				WorldNBTStorage nbtManager = ((WorldNBTStorage) manager);
				
				try {
					Field f = nbtManager.getClass().getSuperclass().getDeclaredField("sessionLock");
					f.setAccessible(true);
					FileLock sessionLock = (FileLock) f.get(nbtManager);
					sessionLock.close();
				} catch (Exception e) {
				}
				
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
	
	@CivConfig(name = "import_playerdata", def = "false", type = CivConfigType.Bool)
	private void uploadExistingPlayers() {
		if (!config.get("import_playerdata").getBool())
			return;
		for (World w: Bukkit.getWorlds()) {
			WorldServer nmsWorld = ((CraftWorld) w).getHandle();
			IDataManager data = nmsWorld.getDataManager();
			String[] names = data.getPlayerFileData().getSeenPlayers();
			for (String name: names) {
				Bukkit.getLogger().log(Level.INFO, "Updating player " + name + " to mysql.");
				try {
					File file = new File(data.getDirectory() + File.separator + "playerdata", name + ".dat");
					if (!file.exists())
						continue;
					InputStream stream = new FileInputStream(file);
					
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					output.write(IOUtils.toByteArray(stream));
					// Now to run our custom mysql code
					UUID uuid = UUID.fromString(name);
					CustomWorldNBTStorage storage = CustomWorldNBTStorage.getWorldNBTStorage();
					db.savePlayerDataAsync(uuid, output, storage.getInvIdentifier(uuid), new YamlConfiguration());
					file.delete();
					stream.close();
				} catch (Exception localException) {
					localException.printStackTrace();
				}
			}
		}
	}
	
	public void sendBungeeUpdateMessage() {
		List <String> exc = db.getAllExclude();
		StringBuilder sb = new StringBuilder();
		for(String s : exc) {
			sb.append(s + "|");
		}
		if (sb.length() == 0) {
			mercuryManager.sendBungeeUpdateMessage("");
			return;
		}
		mercuryManager.sendBungeeUpdateMessage(sb.toString().substring(0, sb.length()-1));
	}
	
	public static CombatTagManager getCombatTagManager() {
		return combatManager;
	}
	
	private void loadAllBeds() {
		List<BedLocation> db_beds = db.getAllBedLocations();
		for (BedLocation bed: db_beds) {
			beds.put(bed.getUUID(), bed);
		}
	}
	
	/**
	 * Only loads the BedLocation into cache.  If you are looking 
	 * to store a BedLocation into db look at BetterShardsListener
	 * @param uuid The UUID of the player
	 * @param bed The BedLocation object
	 */
	public void addBedLocation(UUID uuid, BedLocation bed) {
		beds.put(uuid, bed);
	}
	
	/**
	 * Checks if the player has a bed.
	 * @param uuid The uuid of the player.
	 * @return Returns the BedLocation if it exists.
	 */
	public BedLocation getBed(UUID uuid) {
		return beds.get(uuid);
	}
	
	/**
	 * This only removed the BedLocation object from the cache.
	 * To clear from the database you must call the db.removeBed(uuid);
	 * @param uuid The UUID of the player.
	 */
	public void removeBed(UUID uuid) {
		beds.remove(uuid);
	}
	
	public Collection<BedLocation> getAllBeds() {
		return beds.values();
	}
	
	public static MercuryManager getMercuryManager() { 
		return mercuryManager;
	}
	
	public static boolean isNameLayerEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled("NameLayer");
	}
}
