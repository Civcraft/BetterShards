package vg.civcraft.mc.bettershards.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.material.Bed;
import org.bukkit.util.Vector;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerArrivedChangeServerEvent;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.misc.CustomWorldNBTStorage;
import vg.civcraft.mc.bettershards.misc.Grid;
import vg.civcraft.mc.bettershards.misc.InventoryIdentifier;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.misc.RandomSpawn;
import vg.civcraft.mc.bettershards.misc.TeleportInfo;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.civmodcore.Config;
import vg.civcraft.mc.civmodcore.annotations.CivConfig;
import vg.civcraft.mc.civmodcore.annotations.CivConfigType;
import vg.civcraft.mc.mercury.MercuryAPI;

public class BetterShardsListener implements Listener{
	
	private BetterShardsPlugin plugin;
	private DatabaseManager db;
	private Config config;
	private PortalsManager pm;
	private MercuryManager mercManager;
	private CustomWorldNBTStorage st;
	private RandomSpawn rs;
	
	public BetterShardsListener(){
		plugin = BetterShardsPlugin.getInstance();
		db = plugin.getDatabaseManager();
		pm = plugin.getPortalManager();
		config = plugin.GetConfig();
		mercManager = BetterShardsPlugin.getMercuryManager();
		rs = BetterShardsPlugin.getRandomSpawn();
		Bukkit.getScheduler().runTask(plugin, new Runnable() {

			@Override
			public void run() {
				st = CustomWorldNBTStorage.getWorldNBTStorage();
			}
			
		});
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void playerPreLoginCacheInv(AsyncPlayerPreLoginEvent event) {
		UUID uuid = event.getUniqueId();
		if (st == null){ // Small race condition if someone logs on as soon as the server starts.
			plugin.getLogger().log(Level.INFO, "Player logged on before async process was ready, skipping.");
			return;
		}
		db.loadPlayerData(uuid, st.getInvIdentifier(uuid)); 
		// We do this so it fetches the cache, then when called for real
		// by our CustomWorldNBTStorage class it doesn't have to wait and server won't lock.
	}

	@CivConfig(name = "lobby", def = "false", type = CivConfigType.Bool)
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerJoinEvent(PlayerJoinEvent event){
		if (config.get("lobby").getBool()) {
		    World w = BetterShardsPlugin.getRandomSpawn().getWorld();
		    event.getPlayer().teleport(w.getSpawnLocation());
		    return;
		}
		if (!event.getPlayer().hasPlayedBefore()) {
			rs.handleFirstJoin(event.getPlayer());
			return;
		}
		Location loc = MercuryListener.getTeleportLocation(event.getPlayer().getUniqueId());
		if (loc == null)
			return;
		loc.setDirection(new Vector(loc.getBlockX() * -1, 0, loc.getBlockZ() * -1));
		PlayerArrivedChangeServerEvent e = new PlayerArrivedChangeServerEvent(event.getPlayer(), loc);
		Bukkit.getPluginManager().callEvent(e);
		loc = e.getLocation();
		pm.addArrivedPlayer(event.getPlayer());
		event.getPlayer().teleport(loc);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerJoinedLobbyServer(AsyncPlayerPreLoginEvent event) {
		if (!config.get("lobby").getBool())
			return;
		UUID uuid = event.getUniqueId();
		if (st == null){ // Small race condition if someone logs on as soon as the server starts.
			plugin.getLogger().log(Level.INFO, "Player logged on before async process was ready, skipping.");
			return;
		}
		st.setInventoryIdentifier(uuid, InventoryIdentifier.IGNORE_INV);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		UUID uuid = p.getUniqueId();
		st.save(p, st.getInvIdentifier(uuid));
		if (plugin.isPlayerInTransit(uuid))
			return;
	}
	
	//without this method players are able to detect who is in their shard as 
	//the default behavior for tab completion is trying to complete the name of a player on the server
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerTabComplete(PlayerChatTabCompleteEvent event) {
		Collection <String> res = event.getTabCompletions();
		res.clear();
		String lower = event.getLastToken() != null ? event.getLastToken() : "";
		for(String player : MercuryAPI.getAllPlayers()) {
			if (player.toLowerCase().startsWith(lower)) {
				res.add(player);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void consoleStopEvent(ServerCommandEvent event) {
		if (event.getCommand().equals("stop") || event.getCommand().equals("/stop")) {
			forcePlayerSave();
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void opStopEvent(PlayerCommandPreprocessEvent event) {
		if (event.getPlayer() != null && event.getPlayer().isOp() && (event.getMessage().equals("stop") || event.getMessage().equals("/stop") ) ) {
			forcePlayerSave();
		}
	}
	
	/**
	 * Forces all player save. Should be tied to low-priority event listeners (so they get called first) surrounding /stop.
	 **/
	private void forcePlayerSave() {
		plugin.getLogger().log(Level.INFO, "Forcing online Player save to DB.");

		Collection<Player> online = (Collection<Player>) Bukkit.getOnlinePlayers();
		for (Player p : online) {
			st.save(p, st.getInvIdentifier(p.getUniqueId()));
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void playerMoveEvent(PlayerMoveEvent event) {
		Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()
                && from.getWorld().equals(to.getWorld())) {
            // Player didn't move by at least one block.
            return;
        }
        Player player = event.getPlayer();
        
        Portal p = pm.getPortal(to);
        if (p == null || p.getPartnerPortal() == null) {
        	return;
        }
        // We need this check incase the player just teleported inside the field.
        // We know he does not need to teleport back.
        if (pm.canTransferPlayer(player)) {
        	p.teleport(player);
        }
	}
	
	@CivConfig(name = "allow_portals_build", def = "false", type = CivConfigType.Bool)
	@EventHandler(priority = EventPriority.HIGHEST)
	public void portalCreateEvent(PlayerInteractEvent event){
		if (!config.get("allow_portals_build").getBool())
			return;
		Player p = event.getPlayer();
		Action a = event.getAction();
		if ((a != Action.RIGHT_CLICK_BLOCK ||
				a != Action.LEFT_CLICK_BLOCK) &&
				(p.getItemInHand().getType() != Material.WATER_LILY) || 
				!(p.hasPermission("bettershards.build") || p.isOp()))
			return;
		Block block = event.getClickedBlock();
		if (block == null)
			return;
		
		Location loc = block.getLocation();
		Grid g = plugin.getPlayerGrid(p);
		String message = ChatColor.YELLOW + "";
		if (a == Action.LEFT_CLICK_BLOCK) {
			g.setLeftClickLocation(loc);
			message += "Your primary block selection has been set.";
		}
		else {
			g.setRightClickLocation(loc);
			message += "Your secondary block selection has been set.";
		}
		p.sendMessage(message);
		// Send fake block update.
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void playerRespawnEvent(PlayerRespawnEvent event) {
		if (config.get("lobby").getBool())
			return;
		final Player p = event.getPlayer();
		UUID uuid = p.getUniqueId();
		final BedLocation bed = plugin.getBed(uuid);
		if (bed == null) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

				@Override
				public void run() {
					rs.handleDeath(p);
				}
				
			});
			return;
		}
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

			@Override
			public void run() {
				TeleportInfo teleportInfo = bed.getTeleportInfo();
				if(bed.getServer() == MercuryAPI.serverName()){ //Player's bed is on the current server
					if(Bukkit.getWorld(teleportInfo.getWorld()) == null){
						rs.handleDeath(p);
						return;
					}
					p.teleport(new Location(Bukkit.getWorld(teleportInfo.getWorld()), teleportInfo.getX(), teleportInfo.getY(), teleportInfo.getZ()));
					return;
				}
				
				mercManager.teleportPlayer(bed.getServer(), bed.getUUID(), teleportInfo);
				try {
					BetterShardsAPI.connectPlayer(p, bed.getServer(), PlayerChangeServerReason.BED);
				} catch (PlayerStillDeadException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void bedBreak(BlockBreakEvent event) {
		if (config.get("lobby").getBool())
			return;
		Block b = event.getBlock();
		if (b.getType() != Material.BED_BLOCK) 
			return;
		Block real = getRealFace(b);
		List<BedLocation> toBeRemoved = new ArrayList<BedLocation>();
		for (BedLocation bed: plugin.getAllBeds()) {
			if (!bed.getServer().equals(MercuryAPI.serverName()))
					continue;
			TeleportInfo loc = new TeleportInfo(real.getWorld().getName(), bed.getServer(), real.getX(), real.getY(), real.getZ());
			if (bed.getTeleportInfo().equals(loc)) {
				toBeRemoved.add(bed);
			}
		}
		for (BedLocation bed: toBeRemoved) {
			BetterShardsAPI.removeBedLocation(bed);
		}
	}
	
	private Block getRealFace(Block block) {
		if (((Bed) block.getState().getData()).isHeadOfBed())
			block = block.getRelative(((Bed) block.getState().getData()).getFacing().getOppositeFace());
		return block;
	}
	
	/**
	 * Sets the player bed location by right-clicking on a bed.
	 * This allows players to set their bed at any time of the day.
	 * Highest event priority so it ignores events canceled by citadel
	 * @param event The event args
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerSleepInBed(PlayerInteractEvent event) {
		if (config.get("lobby").getBool())
			return;
		
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || !event.getClickedBlock().getType().equals(Material.BED_BLOCK)) {
			return;
		}
		
		UUID uuid = event.getPlayer().getUniqueId();
		String server = MercuryAPI.serverName();
		Location loc = getRealFace(event.getClickedBlock()).getLocation();
		TeleportInfo info = new TeleportInfo(loc.getWorld().getName(), MercuryAPI.serverName(), loc.getBlockX(),
				loc.getBlockY(), loc.getBlockZ());
		BedLocation bed = new BedLocation(uuid, info);
		BetterShardsAPI.addBedLocation(uuid, bed);
		event.getPlayer().sendMessage(ChatColor.GREEN + "You set your bed location.");
	}
	
	@EventHandler
	public void playerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		if(plugin.isPlayerInTransit(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}
}
