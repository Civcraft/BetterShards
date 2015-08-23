package vg.civcraft.mc.bettershards.command;

import vg.civcraft.mc.bettershards.command.commands.CreatePortal;
import vg.civcraft.mc.civmodcore.command.CommandHandler;

public class BetterCommandHandler extends CommandHandler{

	@Override
	public void registerCommands() {
		addCommands(new CreatePortal("CreatePortal"));
	}

}
