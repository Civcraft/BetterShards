package vg.civcraft.mc.bettershards.bungee.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;

public class NettyInboundHandler extends ChannelInboundHandlerAdapter {
	private PendingConnection pendingConnection_;
	private Channel nettyChannel_;
	private boolean netFrozen_ = false;

	public NettyInboundHandler(PendingConnection pc, Channel chan) {
		super();
		pendingConnection_ = pc;
		nettyChannel_ = chan;
	}

	public PendingConnection getPendingConnection() { return pendingConnection_; }
	public Channel getNettyChannel() { return nettyChannel_; }
	public boolean isFrozen() { return netFrozen_; }

	public void setFrozen(boolean value) {
		netFrozen_ = value;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof PacketWrapper)) {
			throw new Exception("BAD PACKET OBJ " + msg.getClass().getName());
		}
		PacketWrapper pw = (PacketWrapper)msg;
		ByteBuf bufCopy = pw.buf.copy();  // DefinedPacket.readVarInt is destructive
		int packetId = DefinedPacket.readVarInt(bufCopy);
		switch (packetId) {
			default:
				if (netFrozen_) {
					break;
				}
			case 0x01:  // TabComplete
			case 0x02:  // ChatMessage
			case 0x04:  // ClientSettings
			case 0x05:  // ConfirmTransaction
			case 0x0B:  // KeepAlive
				ctx.fireChannelRead(msg);
				break;
		}
	}
}
