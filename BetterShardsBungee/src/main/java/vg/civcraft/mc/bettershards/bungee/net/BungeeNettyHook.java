package vg.civcraft.mc.bettershards.bungee.net;

import java.lang.reflect.Field;
import io.netty.channel.Channel;
import net.md_5.bungee.api.connection.PendingConnection;
import vg.civcraft.mc.bettershards.bungee.BetterShardsBungee;

public class BungeeNettyHook {
	private BungeeNettyHook() {}

	// Netty pipeline names.
	// The BUNGEECORD_* constants must match BungeeCord's Netty pipeline names defined in
	//   proxy/src/main/java/net/md_5/bungee/netty/PipelineUtils.java
	public static final String BUNGEECORD_BOSS_HANDLER = "inbound-boss";
	public static final String NAMELAYER_FREEZE_HANDLER = "namelayer-freeze";

	private static int initialized_ = 0;
	private static Class initialHandler_ = null;
	private static Field channelWrapperField_ = null;
	private static Class channelWrapper_ = null;
	private static Field channelField_ = null;

	public static boolean initialize() {
		if (initialized_ < 0) {
			return false;
		}
		if (initialized_ > 0) {
			return true;
		}
		try {
			initialHandler_ = Class.forName("net.md_5.bungee.connection.InitialHandler");
			channelWrapperField_ = initialHandler_.getDeclaredField("ch");
			channelWrapperField_.setAccessible(true);

			channelWrapper_ = Class.forName("net.md_5.bungee.netty.ChannelWrapper");
			channelField_ = channelWrapper_.getDeclaredField("ch");
			channelField_.setAccessible(true);

			initialized_ = 1;
			BetterShardsBungee.getInstance().getLogger().info("BungeeNettyHook is initalized.");
			return true;
		} catch (ClassNotFoundException | NoSuchFieldException ex) {
			initialized_ = -1;
			BetterShardsBungee.getInstance().getLogger().severe("BungeeNettyHook is uninitalized.");
			return false;
		}
	}

	public static NettyInboundHandler setupHook(PendingConnection pc) {
		if (initialized_ < 1) {
			BetterShardsBungee.getInstance().getLogger().severe("BungeeNettyHook is uninitalized.");
			return null;
		}
		if (!pc.getClass().equals(initialHandler_)) {
			BetterShardsBungee.getInstance().getLogger().severe("PendingConnection is not an InitialHandler.");
			return null;
		}
		Object init_handler = pc;
		Object chan_wrapper;
		Channel chan;
		try {
			chan_wrapper = channelWrapperField_.get(init_handler);
			chan = (Channel)channelField_.get(chan_wrapper);
		} catch (IllegalAccessException ex) {
			BetterShardsBungee.getInstance().getLogger().severe("Illegal field access extracting Channel.");
			return null;
		}
		NettyInboundHandler nih = new NettyInboundHandler(pc, chan);
		// Inject our handler before BungeeCord's HandlerBoss so we can prevent its packet read.
		chan.pipeline().addBefore(BUNGEECORD_BOSS_HANDLER, NAMELAYER_FREEZE_HANDLER, nih);
		return nih;
	}
}
