package vg.civcraft.mc.bettershards.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.PortalsManager;
import vg.civcraft.mc.bettershards.database.DatabaseManager;
import vg.civcraft.mc.bettershards.portal.portals.NetherPortal;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.config.NameConfigListener;
import vg.civcraft.mc.namelayer.config.NameConfigManager;
import vg.civcraft.mc.namelayer.config.annotations.NameConfig;
import vg.civcraft.mc.namelayer.config.annotations.NameConfigType;

public class BetterShardsListener implements Listener, NameConfigListener{
	
	private BetterShardsPlugin plugin;
	private DatabaseManager db;
	private NameConfigManager config;
	private PortalsManager pm;
	
	public BetterShardsListener(){
		plugin = BetterShardsPlugin.getInstance();
		db = plugin.getDatabaseManager();
		config = NameAPI.getNameConfigManager();
		pm = plugin.getPortalManager();
	}

	@NameConfig(name = "lobby", def = "false", type = NameConfigType.Bool)
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerJoinEvent(PlayerJoinEvent event){
		Player p = event.getPlayer();
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerQuitEvent(PlayerQuitEvent event){
		Player p = event.getPlayer();
		UUID uuid = NameAPI.getUUID(p.getName());
		if (plugin.isPlayerInTransit(uuid))
			return;
	}
	
	@NameConfig(name = "allow_portals_build", def = "false", type = NameConfigType.Bool)
	@EventHandler(priority = EventPriority.LOWEST)
	public void portalCreateEvent(EntityCreatePortalEvent event){
		if (event.getPortalType() != PortalType.NETHER && !config.get(plugin, "allow_portals_build").getBool())
			return;
		if (!(event.getEntity() instanceof Player))
			return;
		Player p = (Player) event.getEntity();
		boolean hasPerm = p.isOp() || p.hasPermission("bettershards.create");
		if (!hasPerm)
			return;
		p.sendMessage(ChatColor.GREEN + "You have permission to create a cross server portal, please type in this portal's name in chat.");
		players.put(p.getUniqueId(), event.getBlocks());
	}
	
	private Map<UUID, List<BlockState>> players = new HashMap<UUID, List<BlockState>>();
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerChatEvent(AsyncPlayerChatEvent event){
		Player p = event.getPlayer();
		UUID uuid = p.getUniqueId();
		if (!players.containsKey(uuid))
			return;
		String name = event.getMessage().split(" ")[0];
		List<Location> locs = new ArrayList<Location>();
		for (BlockState state: players.get(uuid))
			locs.add(state.getLocation());
		
		NetherPortal portal = new NetherPortal(name, locs, null);
	}
}
