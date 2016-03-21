package vg.civcraft.mc.bettershards.bungee;

import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class BetterShardsBungee extends Plugin {

	private static BungeeDatabaseHandler db;
	private static BetterShardsBungee plugin;
	private static ScheduledTask t;
	
	@Override
	public void onEnable() {
		plugin = this;
		getProxy().getPluginManager().registerListener(this, new BungeeListener());
		BungeeMercuryManager.disableLocalRandomSpawn();
		t = getProxy().getScheduler().schedule(this, new Runnable() {

			@Override
			public void run() {
				if (db == null)
					BungeeMercuryManager.requestDBInfo();
				else
					t.cancel();
			}
			
		}, 0, 30, TimeUnit.SECONDS);
	}
	
	@Override
	public void onDisable() {
		t.cancel();
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
}
