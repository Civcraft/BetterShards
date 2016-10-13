package vg.civcraft.mc.bettershards.command.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.manager.PortalsManager;
import vg.civcraft.mc.bettershards.misc.Grid;
import vg.civcraft.mc.bettershards.misc.LocationWrapper;
import vg.civcraft.mc.bettershards.misc.Grid.GridLocation;
import vg.civcraft.mc.bettershards.portal.Portal;
import vg.civcraft.mc.bettershards.portal.portals.CircularPortal;
import vg.civcraft.mc.bettershards.portal.portals.CuboidPortal;
import vg.civcraft.mc.bettershards.portal.portals.WorldBorderPortal;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.mercury.MercuryAPI;

public class CreatePortal extends PlayerCommand {

	private PortalsManager pm;
	
	public CreatePortal(String name) {
		super(name);
		setIdentifier("bsc");
		setDescription("Creates a portal from the selection made.");
		setUsage("/bsc <name>, <PortalType>");
		setArguments(1,2);
		pm = BetterShardsPlugin.getPortalManager();
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Need to be instance of player.");
			return true;
		}
		Player p = (Player) sender;
		Grid g = Grid.getPlayerGrid(p);
		if (g.getMissingSelection() == GridLocation.NOSELECTION) 
			return sendPlayerMessage(p, ChatColor.RED + "You have not selected any points.", true);
		else if (g.getMissingSelection() == GridLocation.RIGHTSELECTION)
			return sendPlayerMessage(p, ChatColor.RED + "Your secondary selection has not been chosen.", true);
		else if (g.getMissingSelection() == GridLocation.LEFTSELECTION)
			return sendPlayerMessage(p, ChatColor.RED + "Your primary selection has not been chosen.", true);
		Portal portal = null;
		if (args.length < 1) {
			String[] names = BetterShardsPlugin.getPortalManager().getPortalFactory().getAllPortalNames();
			StringBuilder portals = new StringBuilder();
			for (String x : names) {
				portals.append(x);
				portals.append(" ");
			}
			return sendPlayerMessage(p, ChatColor.RED + "You must specify a portaltype, portal types are: " + portals.toString(), true);
		}
		
		if (pm.getPortal(args[0]) != null) 
			return sendPlayerMessage(p, ChatColor.RED + "That portal name already exists.", true);
		

		Class<? extends Portal> clazz = BetterShardsPlugin.getPortalManager().getPortalFactory().getPortal(args[1]);
		portal = BetterShardsPlugin.getPortalManager().getPortalFactory().buildPortal(clazz);
		portal.setName(args[0]);
		portal.setIsOnCurrentServer(true);
		portal.setServerName(MercuryAPI.serverName());
		portal.setFirstLocation(new LocationWrapper(g.getLeftClickLocation()));
		portal.setSecondLocation(new LocationWrapper(g.getRightClickLocation()));
		portal.valuesPopulated();
		
		pm.createPortal(portal);
		String m = ChatColor.GREEN + "You have successfully created the portal " + args[0] + ".\n"
				+ "To add a connection use the command /bsj <main portal> <connection>";
		return sendPlayerMessage(p, m, true);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return null;
	}

}
