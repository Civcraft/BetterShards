package vg.civcraft.mc.bettershards.bungee.commands;

import java.util.List;
import java.util.Set;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import vg.civcraft.mc.bettershards.bungee.BungeeListener;
import vg.civcraft.mc.mercury.MercuryAPI;

public class ShardCommand extends Command{

	public ShardCommand() {
		super("shard");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (!(sender.hasPermission("bettershards.shard") || sender.hasPermission(
				"bettershards.admin"))) {
			return;
		}

		TextComponent t = new TextComponent();
		if (sender instanceof ProxiedPlayer) {
			ProxiedPlayer p = (ProxiedPlayer) sender;
			t.addExtra("The current shard that you are on is: " + 
			p.getServer().getInfo().getName() + ".");
			t.setColor(ChatColor.GREEN);
		}
		t.addExtra("\n" + "Current servers online are:");
		Set<String> servers = MercuryAPI.getAllConnectedServers();
		servers.removeAll(BungeeListener.getAllExcludedServers());
		TextComponent textServer = new TextComponent();
		textServer.setColor(ChatColor.BLUE);
		for (String server: servers) {
			textServer.addExtra(" " + server);
		}
		textServer.addExtra(".");
		t.addExtra(textServer);
		sender.sendMessage(t);
	}

}
