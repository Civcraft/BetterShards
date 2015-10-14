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
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.mercury.events.AsyncPluginBroadcastMessageEvent;

public class MercuryListener implements Listener{
	
	private String c = "BetterShards";
	private PortalsManager pm = BetterShardsAPI.getPortalsManager();
	private BetterShardsPlugin plugin = BetterShardsPlugin.getInstance();
	
	private static Map<UUID, Location> uuids = new ConcurrentHashMap<UUID, Location>();

	@EventHandler(priority = EventPriority.NORMAL)
	public void asyncPluginBroadcastMessageEvent(AsyncPluginBroadcastMessageEvent event) {
		String channel = event.getChannel();
		if (!channel.equals(c))
			return;
		String message = event.getMessage();
		final String[] content = message.split(" ");
		if (content[0].equals("delete")) {
			Portal p = pm.getPortal(content[1]);
			if (p != null)
				pm.deletePortal(p);
		}
		else if (content[0].equals("teleport")) {
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
					}
					else if (action.equals("command")) {
						uuid = UUID.fromString(content[2]);
						Location loc = null;
						try {
							loc = new Location(Bukkit.getWorld(UUID.fromString(content[3])), Integer.parseInt(content[4]), Integer.parseInt(content[5]), Integer.parseInt(content[6]));
						} catch(IllegalArgumentException e) {
							// The world uuid is none existent so it must be a world name.
							loc = new Location(Bukkit.getWorld(content[3]), Integer.parseInt(content[4]), Integer.parseInt(content[5]), Integer.parseInt(content[6]));
						}
						uuids.put(uuid, loc);
					}
					
					if (Bukkit.getPlayer(uuid) != null){
						Bukkit.getPlayer(uuid).teleport(getTeleportLocation(uuid));
					}
				}
				
			});
		}
		else if (content[0].equals("bed")) { 
			if (content[1].equals("add")) {
				StringBuilder builder = new StringBuilder();
				UUID uuid = UUID.fromString(content[2]);
				String server = content[3];
				for (int x = 4; x < content.length; x++)
					builder.append(content[x] + " ");
				BedLocation bed = new BedLocation(uuid, builder.toString(), server);
				plugin.addBedLocation(uuid, bed);
			}
			else if (content[1].equals("remove")) {
				UUID uuid = UUID.fromString(content[2]);
				plugin.removeBed(uuid);
			}
		}
	}
	
	public static Location getTeleportLocation(UUID uuid) {
		Location loc = uuids.get(uuid);
		uuids.remove(uuid);
		return loc;
	}
}
