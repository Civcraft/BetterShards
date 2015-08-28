package vg.civcraft.mc.bettershards.listeners;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.misc.Grid;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.civmodcore.Config;
import vg.civcraft.mc.civmodcore.annotations.CivConfig;
import vg.civcraft.mc.civmodcore.annotations.CivConfigType;

public class BetterShardsListener implements Listener{
	
	private BetterShardsPlugin plugin;
	private DatabaseManager db;
	private Config config;
	private PortalsManager pm;
	
	public BetterShardsListener(){
		plugin = BetterShardsPlugin.getInstance();
		db = plugin.getDatabaseManager();
		pm = plugin.getPortalManager();
		config = plugin.GetConfig();
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
		if (plugin.isPlayerInTransit(uuid))
			return;
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
        if (p != null);
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
				(p.getItemInHand().getType() != Material.COMPASS ||
				!(p.hasPermission("bettershards.build") || p.isOp())))
			return;
		Grid g = plugin.getPlayerGrid(p);
		Location loc = event.getClickedBlock().getLocation();
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
}