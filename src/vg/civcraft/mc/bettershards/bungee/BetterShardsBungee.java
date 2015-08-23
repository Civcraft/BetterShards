package vg.civcraft.mc.bettershards.bungee;

import net.md_5.bungee.api.plugin.Plugin;

public class BetterShardsBungee extends Plugin {

	@Override
	public void onEnable() {
		getProxy().getPluginManager().registerListener(this, new BungeeListener());
	}
	
	@Override
	public void onDisable() {
		
	}
}
