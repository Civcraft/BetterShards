package vg.civcraft.mc.bettershards.command;

import vg.civcraft.mc.bettershards.command.commands.BetterTeleportServer;
import vg.civcraft.mc.bettershards.command.commands.CreatePortal;
import vg.civcraft.mc.bettershards.command.commands.DeletePortal;
import vg.civcraft.mc.bettershards.command.commands.ExcludeServer;
import vg.civcraft.mc.bettershards.command.commands.JoinPortal;
import vg.civcraft.mc.bettershards.command.commands.RemovePortal;
import vg.civcraft.mc.bettershards.command.commands.Teleport;
import vg.civcraft.mc.bettershards.command.commands.TeleportServer;
import vg.civcraft.mc.civmodcore.command.CommandHandler;

public class BetterCommandHandler extends CommandHandler{

	@Override
	public void registerCommands() {
		addCommands(new BetterTeleportServer("BetterTeleportServer"));
		addCommands(new CreatePortal("CreatePortal"));
		addCommands(new DeletePortal("DeletePortal"));
		addCommands(new ExcludeServer("ExcludeServer"));
		addCommands(new JoinPortal("JoinPortal"));
		addCommands(new RemovePortal("RemovePortal"));
		addCommands(new Teleport("Teleport"));
		addCommands(new TeleportServer("TeleportServer"));
	}

}
