package vg.civcraft.mc.bettershards.bungee.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class ConnectionTracker {
	private static ConcurrentMap<PendingConnection, NettyInboundHandler> nettyHandlers_ = new ConcurrentHashMap<>(128);

	public static NettyInboundHandler get(PendingConnection pc) {
		return nettyHandlers_.get(pc);
	}

	public static void add(PendingConnection pc, NettyInboundHandler nih) {
		nettyHandlers_.put(pc, nih);
	}

	public static Boolean isFrozen(ProxiedPlayer pp) {
		return isFrozen(pp.getPendingConnection());
	}

	public static Boolean isFrozen(PendingConnection pc) {
		NettyInboundHandler nih = ConnectionTracker.nettyHandlers_.get(pc);
		if (nih == null) {
			return null;
		}
		return nih.isFrozen();
	}

	public static Boolean toggleFreeze(ProxiedPlayer pp) {
		return toggleFreeze(pp.getPendingConnection());
	}

	public static Boolean toggleFreeze(PendingConnection pc) {
		NettyInboundHandler nih = ConnectionTracker.nettyHandlers_.get(pc);
		if (nih == null) {
			return null;
		}
		boolean toggled = !nih.isFrozen();
		nih.setFrozen(toggled);
		return toggled;
	}

	public static void remove(ProxiedPlayer pp) {
		remove(pp.getPendingConnection());
	}

	public static void remove(PendingConnection pc) {
		nettyHandlers_.remove(pc);
	}

	private ConnectionTracker() {}
}
