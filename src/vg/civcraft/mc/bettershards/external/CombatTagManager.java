package vg.civcraft.mc.bettershards.external;

import net.minelink.ctplus.CombatTagPlus;
import net.minelink.ctplus.TagManager;
import net.minelink.ctplus.compat.api.NpcIdentity;
import net.minelink.ctplus.compat.api.NpcPlayerHelper;

import org.bukkit.Server;
import org.bukkit.entity.Player;

public class CombatTagManager {

	private NpcPlayerHelper combatTagPlusApi;
	private boolean combatTagPlusEnabled = false;
	private TagManager tagManager;
	
	public CombatTagManager(Server server) {
		if (server.getPluginManager().getPlugin("CombatTagPlus") != null){
			combatTagPlusApi = ((CombatTagPlus) server.getPluginManager().getPlugin("CombatTagPlus")).getNpcPlayerHelper();
			tagManager = ((CombatTagPlus) server.getPluginManager().getPlugin("CombatTagPlus")).getTagManager();
			combatTagPlusEnabled = true;
		}
	}
	
	public boolean isCombatTagPlusNPC(Player player) {
		return combatTagPlusEnabled && combatTagPlusApi.isNpc(player);
	}
	
	public NpcIdentity getCombatTagPlusNPCIdentity(Player player){
		return combatTagPlusApi.getIdentity(player);
	}
	
	public boolean isInCombatTag(Player p) {
		return combatTagPlusEnabled && tagManager != null && tagManager.isTagged(p.getUniqueId());
	}
}
