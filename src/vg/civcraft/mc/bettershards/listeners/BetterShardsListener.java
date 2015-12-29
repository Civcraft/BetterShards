package vg.civcraft.mc.bettershards.listeners;

import java.util.UUID;
import java.util.logging.Level;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.material.Bed;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.misc.CustomWorldNBTStorage;
import vg.civcraft.mc.bettershards.misc.Grid;
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
	
	public BetterShardsListener(){
		plugin = BetterShardsPlugin.getInstance();
		db = plugin.getDatabaseManager();
		pm = plugin.getPortalManager();
		config = plugin.GetConfig();
		mercManager = BetterShardsPlugin.getMercuryManager();
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
		if (config.get("lobby").getBool())
			return;
		Location loc = MercuryListener.getTeleportLocation(event.getPlayer().getUniqueId());
		if (loc == null)
			return;
		pm.addArrivedPlayer(event.getPlayer());
		event.getPlayer().teleport(loc);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		UUID uuid = p.getUniqueId();

		plugin.getLogger().log(Level.INFO, "Forcing Player " + p.getName() + " (" + uuid + ") save to DB.");
		st.save(p, st.getInvIdentifier(uuid));
		db.playerQuitServer(uuid);
		if (plugin.isPlayerInTransit(uuid))
			return;
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
        if (p == null) {
        	return;
        }
        // We need this check incase the player just teleported inside the field.
        // We know he does not need to teleport back.
        if (pm.canTransferPlayer(player))
        	p.teleportPlayer(player);
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
		Player p = event.getPlayer();
		UUID uuid = p.getUniqueId();
		BedLocation bed = plugin.getBed(uuid);
		if (bed == null)
			return;
		String info = bed.getUUID().toString() + " " + bed.getLocation(); 
		mercManager.teleportPlayer(info);
		BetterShardsAPI.connectPlayer(p, bed.getServer(), PlayerChangeServerReason.BED);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void bedBreak(BlockBreakEvent event) {
		Block b = event.getBlock();
		if (b.getType() != Material.BED_BLOCK) 
			return;
		Block real = getRealFace(b);
		for (BedLocation bed: plugin.getAllBeds()) {
			if (!bed.getServer().equals(MercuryAPI.serverName()))
					continue;
			String loc = real.getWorld().getUID().toString() + " " + real.getX() + " " + real.getY() + " " + real.getZ();
			if (bed.getLocation().equals(loc)) {
				plugin.removeBed(bed.getUUID()); // remove from local cache.
				db.removeBed(bed.getUUID()); // remove from db.
				mercManager.removeBedLocation(bed); // send remove to other servers.
			}
		}
	}
	
	private Block getRealFace(Block block) {
		if (((Bed) block.getState().getData()).isHeadOfBed())
			block = block.getRelative(((Bed) block.getState().getData()).getFacing().getOppositeFace());
		return block;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void playerSleepInBed(PlayerBedEnterEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		String server = MercuryAPI.serverName();
		Block b = getRealFace(event.getBed());
		Location loc = b.getLocation();
		String bedLoc = loc.getWorld().getUID().toString() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
		BedLocation bed = new BedLocation(uuid, bedLoc, server);
		plugin.addBedLocation(uuid, bed);
		db.addBedLocation(bed); // Need to save the bed out to db
		mercManager.sendBedLocation(bed);
	}
	
	@EventHandler
	public void playerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		if(plugin.isPlayerInTransit(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}
}
