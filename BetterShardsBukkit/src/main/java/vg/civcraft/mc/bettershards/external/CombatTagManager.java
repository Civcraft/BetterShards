package vg.civcraft.mc.bettershards.external;

import net.minelink.ctplus.CombatTagPlus;
import net.minelink.ctplus.TagManager;
import net.minelink.ctplus.compat.api.NpcIdentity;
import net.minelink.ctplus.compat.api.NpcPlayerHelper;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.listeners.CombatTagListener;

public class CombatTagManager {

	private NpcPlayerHelper combatTagPlusApi;
	private boolean combatTagPlusEnabled = false;
	private TagManager tagManager;
	
	public CombatTagManager(Server server) {
		if (server.getPluginManager().getPlugin("CombatTagPlus") != null){
			combatTagPlusApi = ((CombatTagPlus) server.getPluginManager().getPlugin("CombatTagPlus")).getNpcPlayerHelper();
			tagManager = ((CombatTagPlus) server.getPluginManager().getPlugin("CombatTagPlus")).getTagManager();
			combatTagPlusEnabled = true;
			server.getPluginManager().registerEvents(new CombatTagListener(), BetterShardsPlugin.getInstance());
		}
	}

	public boolean isCombatTagNPC(Player player) {
		return combatTagPlusEnabled && combatTagPlusApi.isNpc(player);
	}
	
	public NpcIdentity getCombatTagNPCIdentity(Player player){
		return combatTagPlusApi.getIdentity(player);
	}
	
	public boolean isInCombatTag(Player p) {
		return combatTagPlusEnabled && tagManager != null && tagManager.isTagged(p.getUniqueId());
	}
	
	public boolean unCombatTag(Player p){
		return combatTagPlusEnabled && tagManager != null && tagManager.untag(p.getUniqueId());
	}
}
