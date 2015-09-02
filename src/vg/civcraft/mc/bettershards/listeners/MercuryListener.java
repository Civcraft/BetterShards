package vg.civcraft.mc.bettershards.listeners;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.mercury.events.AsyncPluginBroadcastMessageEvent;

public class MercuryListener implements Listener{
	
	private String c = "BetterShards";
	private PortalsManager pm = BetterShardsAPI.getPortalsManager();
	
	private static Map<UUID, Location> uuids = new ConcurrentHashMap<UUID, Location>();

	@EventHandler(priority = EventPriority.NORMAL)
	public void asyncPluginBroadcastMessageEvent(AsyncPluginBroadcastMessageEvent event) {
		String channel = event.getChannel();
		System.out.println("The channel was " + channel);
		if (!channel.equals(c))
			return;
		String message = event.getMessage();
		final String[] content = message.split(" ");
		if (content[0].equals("delete")) {
			Portal p = pm.getPortal(content[1]);
			if (p != null)
				pm.deletePortal(p);
		}
		
		if (content[0].equals("teleport")) {
			Bukkit.getScheduler().runTask(BetterShardsPlugin.getInstance(), new Runnable() {

				@Override
				public void run() {
					String action = content[1];
					UUID uuid = null;
					if (action.equals("portal")) {
						uuid = UUID.fromString(content[2]);
						String p = content[3];
						Portal portal = pm.getPortal(p);
						if (!portal.isOnCurrentServer())
							return;
						uuids.put(uuid, portal.findRandomSafeLocation());
						System.out.println(uuid.toString() + " has teleported to " +p);
					}
					else if (action.equals("teleport")) {
						uuid = UUID.fromString(content[2]);
						Location loc = new Location(Bukkit.getWorld(content[3]), Integer.parseInt(content[4]), Integer.parseInt(content[5]), Integer.parseInt(content[6]));
						uuids.put(uuid, loc);
					}
					
					if (Bukkit.getPlayer(uuid) != null){
						System.out.println("Testing123");
						Bukkit.getPlayer(uuid).teleport(getTeleportLocation(uuid));
					}
				}
				
			});
		}
	}
	
	public static Location getTeleportLocation(UUID uuid) {
		System.out.println("Getting the teleport location + " + uuids.get(uuid));
		Location loc = uuids.get(uuid);
		uuids.remove(uuid);
		return loc;
	}
}
