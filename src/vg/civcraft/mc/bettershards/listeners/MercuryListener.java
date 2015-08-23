package vg.civcraft.mc.bettershards.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.mercury.events.AsyncPluginBroadcastMessageEvent;

public class MercuryListener implements Listener{
	
	private String c = "BetterShards";
	private PortalsManager pm = BetterShardsAPI.getPortalsManager();

	@EventHandler(priority = EventPriority.NORMAL)
	public void asyncPluginBroadcastMessageEvent(AsyncPluginBroadcastMessageEvent event) {
		String channel = event.getChannel();
		if (!channel.equals(c))
			return;
		String message = event.getMessage();
		String[] content = message.split(" ");
		if (content[0].equals("delete")) {
			Portal p = pm.getPortal(content[1]);
			if (p != null)
				pm.deletePortal(p);
		}
			
	}
}
