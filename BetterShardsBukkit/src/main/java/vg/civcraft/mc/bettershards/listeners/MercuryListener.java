package vg.civcraft.mc.bettershards.listeners;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.events.PlayerArrivedChangeServerEvent;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.misc.RandomSpawn;
import vg.civcraft.mc.bettershards.misc.TeleportInfo;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.bettershards.portal.portals.CircularPortal;
import vg.civcraft.mc.bettershards.portal.portals.WorldBorderPortal;
import vg.civcraft.mc.mercury.events.AsyncPluginBroadcastMessageEvent;

public class MercuryListener implements Listener{
	
	private String c = "BetterShards";
	private PortalsManager pm = BetterShardsAPI.getPortalsManager();
	private BetterShardsPlugin plugin = BetterShardsPlugin.getInstance();
	private RandomSpawn rs = BetterShardsPlugin.getRandomSpawn();
	
	private static Map<UUID, Location> uuids = new ConcurrentHashMap<UUID, Location>();
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void asyncPluginBroadcastMessageEvent(AsyncPluginBroadcastMessageEvent event) {
		String channel = event.getChannel();
		if (!channel.equals(c))
			return;
		String message = event.getMessage();
		final String[] content = message.split("\\|");
		if (content[0].equals("delete")) {
			Portal p = pm.getPortal(content[1]);
			if (p != null)
				pm.deletePortalLocally(p);
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
						Location targetLoc = null;
						if (content.length > 4) {
							if (portal instanceof WorldBorderPortal) {
								double angle = Double.valueOf(content[4]);
								targetLoc = ((WorldBorderPortal) portal).calculateSpawnLocation(angle);
							}
							else if(portal instanceof CircularPortal) {
								double xScale = Double.valueOf(content[4]);
								double zScale = Double.valueOf(content[5]);
								targetLoc = ((CircularPortal) portal).calculateLocation(xScale, zScale);
							}
						}
						if (targetLoc == null) {
							targetLoc = portal.findSpawnLocation();
							//just randomize it
							plugin.getLogger().log(Level.INFO,
									"No known portal handler for {0}, sending player {1} to {2} instead",
									new Object[]{p, uuid, targetLoc});
						}
						uuids.put(uuid, targetLoc);
					}
					else if (action.equals("command")) {
						uuid = UUID.fromString(content[2]);
						Location loc = null;
						if(content.length == 4){
							Player targetPlayer = Bukkit.getPlayer(UUID.fromString(content[3]));
							loc = targetPlayer.getLocation();
						} else if(content.length == 6){ //use default overworld
							loc = new Location(Bukkit.getWorlds().get(0), Integer.parseInt(content[3]), Integer.parseInt(content[4]), Integer.parseInt(content[5]));
						} else if(content.length == 7){
							loc = new Location(Bukkit.getWorld(content[3]), Integer.parseInt(content[4]), Integer.parseInt(content[5]), Integer.parseInt(content[6]));
						}
						uuids.put(uuid, loc);
					}
					else if (action.equals("randomspawn")) {
						Location loc = plugin.getRandomSpawn().getLocation();
						uuid =UUID.fromString(content[2]);		
						uuids.put(uuid, loc);
					}
					else if (action.equals("connect")){
						Player player = Bukkit.getPlayer(UUID.fromString(content[2]));
						if(player != null){
							try {
								BetterShardsAPI.connectPlayer(player, content[3], PlayerChangeServerReason.valueOf(content[4]));
							} catch (PlayerStillDeadException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							plugin.getLogger().log(Level.INFO, "Connect received from Mercury, letting randomspawn happen while waiting for connect for {0}",
									content[2]);
						}
					}
					
					Player p = Bukkit.getPlayer(uuid);
					if (p != null){
						Location loc = getTeleportLocation(uuid);
						plugin.getLogger().log(Level.INFO, "Sending player {0} to location: {1}", new Object[] { uuid, loc });
						PlayerArrivedChangeServerEvent event = new PlayerArrivedChangeServerEvent(p, loc);
						Bukkit.getPluginManager().callEvent(event);
						loc = event.getLocation();
						p.teleport(loc);
					}
				}
				
			});
		}
		else if (content[0].equals("bed")) { 
			if (content[1].equals("add")) {
				UUID uuid = UUID.fromString(content[2]);
				String server = content[3];
				TeleportInfo info = new TeleportInfo(content[4], server, Integer.parseInt(content[5]), Integer.parseInt(content[6]),
						Integer.parseInt(content[7]));
				BedLocation bed = new BedLocation(uuid, info);
				plugin.addBedLocation(uuid, bed);
			}
			else if (content[1].equals("remove")) {
				UUID uuid = UUID.fromString(content[2]);
				plugin.removeBed(uuid);
			}
		}
		else if (content[0].equals("portal")) {
			if (content[1].equals("connect")) {
				Portal p1 = pm.getPortal(content[2]);
				Portal p2 = pm.getPortal(content[3]);
				if (p1 == null || p2 == null)
					return;
				p1.setPartnerPortal(p2);
			}
			else if (content[1].equals("remove")) {
				Portal p1 = pm.getPortal(content[2]);
				if (p1 == null)
					return;
				p1.setPartnerPortal(null);
			}
		}
		else if (content[0].equals("info")) {
			if (content[1].equals("randomspawn")) {
				if (content[2].equals("disable")) {
					rs.setFirstJoin(true);
				}
			}
		}
	}
	
	public static void stageTeleport(UUID uuid, Location loc){
		uuids.put(uuid, loc);
	}
	
	public static Location getTeleportLocation(UUID uuid) {
		Location loc = uuids.get(uuid);
		uuids.remove(uuid);
		return loc;
	}
	
	public static Map<UUID, Location> getAllRemainingTeleports() {
		return uuids;
	}
}
