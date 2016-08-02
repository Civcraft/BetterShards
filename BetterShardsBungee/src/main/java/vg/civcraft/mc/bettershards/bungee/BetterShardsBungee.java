package vg.civcraft.mc.bettershards.bungee;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.ReconnectHandler;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import vg.civcraft.mc.mercury.MercuryAPI;

public class BetterShardsBungee extends Plugin {

	private File configFile;
	private static BungeeDatabaseHandler db;
	private static BetterShardsBungee plugin;
	private ConfigurationProvider configManager;
	private Configuration config;
	private ReconnectHandler reconnectHandler;
	private LobbyHandler lobbyHandler;
	
	private static Map<String, Integer> serverMaxPlayerCount = new HashMap<String, Integer>();
	
	@Override
	public void onEnable() {
		plugin = this;
		configFile = new File(this.getDataFolder(), "config.yml");
		loadConfiguration();
		// Lets connect to the db.
		loadDB();
		MercuryAPI.addChannels("BetterShards");
		reconnectHandler = new BetterShardsReconnectHandler();
		getProxy().setReconnectHandler(reconnectHandler);
		BungeeListener listener = new BungeeListener();
		MercuryAPI.registerListener(listener, "BetterShards");
		lobbyHandler = new LobbyHandler();
		QueueHandler.initialize();
		getProxy().getPluginManager().registerListener(this, listener);
		BungeeMercuryManager.disableLocalRandomSpawn();
	}
	
	@Override
	public void onDisable() {
		
	}
	
	private void loadDB() {
		String host = config.getString("mysql.host", "localhost");
		int port = config.getInt("mysql.port", 3306);
		String dbname = config.getString("mysql.dbname", "BetterShardsDB");
		String username = config.getString("mysql.username");
		String password = config.getString("mysql.password");
		db = new BungeeDatabaseHandler(host, port, dbname, username, password);
	}
	
	public void loadConfiguration() {
		if (!getDataFolder().exists())
			getDataFolder().mkdir();
		
		configManager = ConfigurationProvider.getProvider(YamlConfiguration.class);
		if (!configFile.exists()) {
			try {
                configFile.createNewFile();
                try (InputStream is = getResourceAsStream("config.yml");
                		OutputStream os = new FileOutputStream(configFile)) {
                	ByteStreams.copy(is, os);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to create configuration file", e);
            }
        }
		try {
			config = configManager.load(configFile);
		} catch (IOException e) {
			config = new Configuration();
			saveConfiguration();
		}
	}
	
	public void saveConfiguration() {
		try {
			configManager.save(config, configFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static BetterShardsBungee getInstance() {
		return plugin;
	}
	
	public static void setDBHandler(BungeeDatabaseHandler d) {
		db = d;
	}
	
	public static BungeeDatabaseHandler getDBHandler() {
		return db;
	}
	
	public static void setServerCount(String server, int count) {
		serverMaxPlayerCount.put(server, count);
	}
	
	public static int getServerCount(String server) {
		return serverMaxPlayerCount.containsKey(server) ? serverMaxPlayerCount.get(server) : 100; // We can assume that at least 100 people
		// until we get the correct count.
	}
	
	public Configuration getConfig() {
		return config;
	}
	
	public ReconnectHandler getReconnectHandler() {
		return reconnectHandler;
	}
	
	public LobbyHandler getLobbyHandler() {
		return lobbyHandler;
	}
	
	public static void info(String message) {
		plugin.getLogger().info(message);
	}
}
