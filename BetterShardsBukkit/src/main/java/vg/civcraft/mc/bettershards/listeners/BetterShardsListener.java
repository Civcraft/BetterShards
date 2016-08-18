package vg.civcraft.mc.bettershards.listeners;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.material.Bed;
import org.bukkit.util.Vector;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerArrivedChangeServerEvent;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.manager.PortalsManager;
import vg.civcraft.mc.bettershards.manager.RandomSpawnManager;
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.misc.CustomWorldNBTStorage;
import vg.civcraft.mc.bettershards.misc.Grid;
import vg.civcraft.mc.bettershards.misc.InventoryIdentifier;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
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
	private CustomWorldNBTStorage st;
	private RandomSpawnManager rs;
	
	public BetterShardsListener(){
		plugin = BetterShardsPlugin.getInstance();
		db = BetterShardsPlugin.getDatabaseManager();
		pm = BetterShardsPlugin.getPortalManager();
		config = plugin.GetConfig();
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
		if (uuid != null) {
			plugin.getLogger().log(Level.FINER, "Preparing to pre-load player data: {0}", uuid);
		} else { 
			return;
		}
		if (st == null){ // Small race condition if someone logs on as soon as the server starts.
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Please try to log in again in a moment, server is not ready to accept log-ins.");
			plugin.getLogger().log(Level.INFO, "Player {0} logged on before async process was ready, skipping.", uuid);
			return;
		}
		Future<ByteArrayInputStream> soondata = db.loadPlayerDataAsync(uuid, st.getInvIdentifier(uuid)); // wedon't use the data, but know that it caches behind the scenes.
		
		try {
			ByteArrayInputStream after = soondata.get(); // I want to _INTENTIONALLY_ delay accepting the user's login until I know for sure I've got the data loaded asynchronously.
			if (after == null) {
				plugin.getLogger().log(Level.INFO, "Pre-load for player data {0} came back empty. New player? Error?", uuid);
			} else {
				plugin.getLogger().log(Level.FINER, "Pre-load for player data {0} complete.", uuid);
			}
			
		} catch (InterruptedException | ExecutionException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to pre-load player data: {0}", uuid);
			e.printStackTrace();
		}
		
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
		// Defer to next tick so join is fully complete before we unlock
		final UUID player = event.getPlayer().getUniqueId();
		Bukkit.getScheduler().runTask(plugin, new Runnable() {
			public void run() {
				//tell other server to remove player from transit
				MercuryManager.notifyOfArrival(player);
				BetterShardsPlugin.getTransitManager().notifySuccessfullArrival(player);
			}
		});
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
		db.playerQuitServer(uuid);
		if (!BetterShardsPlugin.getTransitManager().isPlayerInTransit(uuid)) {
			st.save(p, st.getInvIdentifier(uuid), true);
		}
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
			st.save(p, st.getInvIdentifier(p.getUniqueId()),false);
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
				(p.getInventory().getItemInMainHand().getType() != Material.WATER_LILY) || 
				!(p.hasPermission("bettershards.build") || p.isOp()))
			return;
		Block block = event.getClickedBlock();
		if (block == null)
			return;
		
		Location loc = block.getLocation();
		Grid g = Grid.getPlayerGrid(p);
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
		final BedLocation bed = BetterShardsPlugin.getBedManager().getBed(uuid);
		//if the player has no bed or an invalid bed location, we just want to randomspawn
		//him. This needs to be delayed by 1 tick, because the respawn event has to complete first
		if (bed == null || (bed.getServer() != null && bed.getServer().equals(MercuryAPI.serverName()) 
			&& Bukkit.getWorld(bed.getTeleportInfo().getWorld()) == null)) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					rs.handleDeath(p);
				}
				
			});
			return;
		}
		final TeleportInfo teleportInfo = bed.getTeleportInfo();
		if(bed.getServer() != null && bed.getServer().equals(MercuryAPI.serverName())){ //Player's bed is on the current server
			event.setRespawnLocation(new Location(Bukkit.getWorld(teleportInfo.getWorld()), teleportInfo.getX(), teleportInfo.getY(), teleportInfo.getZ()));
			return;
		}
		//bed is on a different server, so we send the player there and send the according mercury message
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

			@Override
			public void run() {
				//just a badly named method, this only sends a mercury message
				MercuryManager.teleportPlayer(bed.getServer(), bed.getUUID(), teleportInfo);
				try {
					BetterShardsAPI.connectPlayer(p, bed.getServer(), PlayerChangeServerReason.BED);
				} catch (PlayerStillDeadException e) {
					e.printStackTrace();
				}
			}
			
		});
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void bedBreak(BlockBreakEvent event) {
		if (config.get("lobby").getBool())
			return;
		bedBreak(event.getBlock());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void bedBreakExplosion(BlockExplodeEvent event) {
		if (config.get("lobby").getBool())
			return;
		for (Block b : event.blockList())
			bedBreak(b);
	}
	
	private void bedBreak(Block b) {
		if (b.getType() != Material.BED_BLOCK) 
			return;
		Block real = getRealFace(b);
		List<BedLocation> toBeRemoved = new ArrayList<BedLocation>();
		for (BedLocation bed: BetterShardsPlugin.getBedManager().getAllBeds()) {
			if (!bed.getServer().equals(MercuryAPI.serverName()))
					continue;
			TeleportInfo loc = new TeleportInfo(real.getWorld().getName(), bed.getServer(), real.getX(), real.getY(), real.getZ());
			if (bed.getTeleportInfo().equals(loc)) {
				toBeRemoved.add(bed);
			}
		}
		if (toBeRemoved.size() == 0) {
		    BetterShardsPlugin.getInstance().warning("Bed was broken at " + b.getLocation() + " but no bed was found in storage?");
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
		TeleportInfo info = new TeleportInfo(loc.getWorld().getName(), server, loc.getBlockX(),
				loc.getBlockY(), loc.getBlockZ());
		plugin.getLogger().log(Level.INFO, "Player {0} bed location set to {1}", 
				new Object[] { uuid, info });
		BedLocation bed = new BedLocation(uuid, info);
		BetterShardsAPI.addBedLocation(uuid, bed);
		event.getPlayer().sendMessage(ChatColor.GREEN + "You set your bed location.");
	}
	
	// Start of methods to try and stop interaction when transferring.
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		if(BetterShardsPlugin.getTransitManager().isPlayerInTransit(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerDamageEvent(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		Player p = (Player) event.getEntity();
		if (BetterShardsPlugin.getTransitManager().isPlayerInTransit(p.getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerMoveEventWhenInTransit(PlayerMoveEvent event) {
		Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()
                && from.getWorld().equals(to.getWorld())) {
            // Player didn't move by at least one block.
            return;
        }
		Player p = event.getPlayer();
		if (BetterShardsPlugin.getTransitManager().isPlayerInTransit(p.getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void inventoryClick(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked();
		if (BetterShardsPlugin.getTransitManager().isPlayerInTransit(p.getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerPickupEvent(PlayerPickupItemEvent event) {
		Player p = event.getPlayer();
		if (BetterShardsPlugin.getTransitManager().isPlayerInTransit(p.getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Player p = (Player) event.getPlayer();
		if (BetterShardsPlugin.getTransitManager().isPlayerInTransit(p.getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void blockBreakEvent(BlockBreakEvent event) {
		Player p = event.getPlayer();
		if (BetterShardsPlugin.getTransitManager().isPlayerInTransit(p.getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Player p = event.getPlayer();
		if (BetterShardsPlugin.getTransitManager().isPlayerInTransit(p.getUniqueId())) {
			event.setCancelled(true);
		}
	}
}
