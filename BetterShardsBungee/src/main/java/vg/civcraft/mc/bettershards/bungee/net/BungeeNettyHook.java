package vg.civcraft.mc.bettershards.bungee.net;

import java.lang.reflect.Field;
import io.netty.channel.Channel;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import vg.civcraft.mc.bettershards.bungee.BetterShardsBungee;

public class BungeeNettyHook {
	private BungeeNettyHook() {}

	// Netty pipeline names.
	// The BUNGEECORD_* constants must match BungeeCord's Netty pipeline names defined in
	//   proxy/src/main/java/net/md_5/bungee/netty/PipelineUtils.java
	public static final String BUNGEECORD_BOSS_HANDLER = "inbound-boss";
	public static final String NAMELAYER_FREEZE_HANDLER = "namelayer-freeze";

	private static boolean initialized_ = false;
	private static Field channelWrapperField_ = null;
	private static Field channelField_ = null;

	public static boolean initialize() {
		if (initialized_) {
			return true;
		}
		try {
			channelWrapperField_ = InitialHandler.class.getDeclaredField("ch");
			channelWrapperField_.setAccessible(true);
			channelField_ = ChannelWrapper.class.getDeclaredField("ch");
			channelField_.setAccessible(true);
			initialized_ = true;
			return true;
		} catch (NoSuchFieldException ex) {
			return false;
		}
	}

	public static NettyInboundHandler setupHook(PendingConnection pc) {
		if (!initialized_) {
			BetterShardsBungee.getInstance().getLogger().severe("BungeeNettyHook is uninitalized.");
			return null;
		}
		if (!(pc instanceof InitialHandler)) {
			BetterShardsBungee.getInstance().getLogger().severe("PendingConnection is not an InitialHandler.");
			return null;
		}
		ChannelWrapper cw;
		Channel chan;
		InitialHandler ih = (InitialHandler)pc;
		try {
			cw = (ChannelWrapper)channelWrapperField_.get(ih);
			chan = (Channel)channelField_.get(cw);
		} catch (IllegalAccessException ex) {
			BetterShardsBungee.getInstance().getLogger().severe("Illegal field access.");
			return null;
		}
		NettyInboundHandler nih = new NettyInboundHandler(pc, chan);
		// Inject our handler before BungeeCord's HandlerBoss so we can prevent its packet read.
		chan.pipeline().addBefore(BUNGEECORD_BOSS_HANDLER, NAMELAYER_FREEZE_HANDLER, nih);
		return nih;
	}
}
