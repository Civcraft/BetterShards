package vg.civcraft.mc.bettershards;

import org.bukkit.Bukkit;

import vg.civcraft.mc.bettershards.command.BetterCommandHandler;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.external.CombatTagManager;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.listeners.BetterShardsListener;
import vg.civcraft.mc.bettershards.listeners.MercuryListener;
import vg.civcraft.mc.bettershards.manager.BedManager;
import vg.civcraft.mc.bettershards.manager.ConnectionManager;
import vg.civcraft.mc.bettershards.manager.PortalsManager;
import vg.civcraft.mc.bettershards.manager.RandomSpawnManager;
import vg.civcraft.mc.bettershards.manager.TransitManager;
import vg.civcraft.mc.bettershards.misc.CustomWorldNBTStorage;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.mercury.config.MercuryConfigManager;

public class BetterShardsPlugin extends ACivMod{

	private static BetterShardsPlugin instance;
	private PortalsManager pm;
	private DatabaseManager db;
	private String servName;
	private CombatTagManager combatManager;
	private RandomSpawnManager randomSpawn;
	private ConnectionManager connectionManager;
	private BedManager bedManager;
	private TransitManager transitManager;
	
	private boolean isNameLayerEnabled;
	
	@Override
	public void onEnable(){
		super.onEnable();
		instance = this;
		servName = MercuryConfigManager.getServerName();
		MercuryManager.initializeChannelsAndPing();
		db = new DatabaseManager();
		if (!db.isConnected())
			Bukkit.getPluginManager().disablePlugin(this);
		pm = new PortalsManager();
		pm.loadPortalsManager();
		CustomWorldNBTStorage.setWorldNBTStorage();
		combatManager = new CombatTagManager(getServer());
		randomSpawn = new RandomSpawnManager();
		registerListeners();
		CustomWorldNBTStorage.uploadExistingPlayers();
		new BetterShardsAPI();
		handle = new BetterCommandHandler();
		handle.registerCommands();
		transitManager = new TransitManager();
		connectionManager = new ConnectionManager();
		bedManager = new BedManager();
		isNameLayerEnabled = Bukkit.getPluginManager().isPluginEnabled("NameLayer");
	}
	
	@Override
	public void onDisable(){
		if (db != null){
			db.cleanup(); // releases all locks.
		}
	}
	

	@Override
	protected String getPluginName() {
		return "BetterShardsPlugin";
	}
	
	/**
	 * @return Returns the instance of the JavaPlugin.
	 */
	public static BetterShardsPlugin getInstance(){
		return instance;
	}
	
	private void registerListeners(){
		// Register bukkit channel
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		
		// Register Bukkit Event Listener.
		BetterShardsListener l = new BetterShardsListener();
		getServer().getPluginManager().registerEvents(l, this);
		getServer().getPluginManager().registerEvents(new MercuryListener(), this);
		// CombatTag listener is registered in CombatTagManager depending on whether CombatTagPlus is installed
	}
	
	public static String getCurrentServerName(){
		return getInstance().servName;
	}
	
	public static PortalsManager getPortalManager(){
		return getInstance().pm;
	}
	
	
	public static DatabaseManager getDatabaseManager() {
		return getInstance().db;
	}
	
	public static RandomSpawnManager getRandomSpawn() {
		return getInstance().randomSpawn;
	}
	public static CombatTagManager getCombatTagManager() {
		return getInstance().combatManager;
	}
	
	public static ConnectionManager getConnectionManager() {
		return getInstance().connectionManager;
	}
	
	public static BedManager getBedManager() {
		return getInstance().bedManager;
	}
	
	public static TransitManager getTransitManager() {
		return getInstance().transitManager;
	}
	
	public static boolean isNameLayerEnabled() {
		return getInstance().isNameLayerEnabled;
	}
}
