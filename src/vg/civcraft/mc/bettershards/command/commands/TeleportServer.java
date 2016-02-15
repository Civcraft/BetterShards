package vg.civcraft.mc.bettershards.command.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.external.MercuryManager;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;

public class TeleportServer extends PlayerCommand{
	private MercuryManager mercManager = BetterShardsPlugin.getMercuryManager();

	public TeleportServer(String name) {
		super(name);
		setIdentifier("ts");
		setDescription("Teleports you to another server");
		setUsage("/ts <server>\n"
				+ "/ts <server> <x> <y> <z>\n"
				+ "/ts <server> <x> <y> <z> <world>");
		setArguments(1, 5);
	}

	@Override
	public boolean execute(CommandSender arg0, String[] arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> tabComplete(CommandSender arg0, String[] arg1) {
		// TODO Auto-generated method stub
		return null;
	}
}
