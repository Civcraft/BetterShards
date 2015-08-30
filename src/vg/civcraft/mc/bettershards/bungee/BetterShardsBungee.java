package vg.civcraft.mc.bettershards.bungee;

import net.md_5.bungee.api.plugin.Plugin;

public class BetterShardsBungee extends Plugin {

	private static BetterShardsBungee plugin;
	@Override
	public void onEnable() {
		plugin = this;
		getProxy().getPluginManager().registerListener(this, new BungeeListener());
	}
	
	@Override
	public void onDisable() {
		
	}
	
	public static BetterShardsBungee getInstance() {
		return plugin;
	}
}
