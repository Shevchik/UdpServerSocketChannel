package udpserversocketchannel.channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DatagramPacket;
import udpserversocketchannel.eventloop.UdpEventLoop;

public class UdpChannel extends AbstractChannel {

	private final ChannelMetadata metadata = new ChannelMetadata(false);
	private final DefaultChannelConfig config = new DefaultChannelConfig(this);
	protected final NioUdpServerChannel serverchannel;
	private final InetSocketAddress remote;

	protected UdpChannel(NioUdpServerChannel serverchannel, InetSocketAddress remote) {
		super(serverchannel);
		this.serverchannel = serverchannel;
		this.remote = remote;
	}

	@Override
	public ChannelMetadata metadata() {
		return metadata;
	}

	@Override
	public ChannelConfig config() {
		return config;
	}

	private boolean open = true;

	@Override
	public boolean isActive() {
		return open;
	}

	@Override
	public boolean isOpen() {
		return isActive();
	}

	@Override
	protected void doClose() throws Exception {
		open = false;
		serverchannel.closeChannel(this);
	}

	@Override
	protected void doDisconnect() throws Exception {
		doClose();
	}

	private ByteBuf buffer;
	public void setReceivedData(ByteBuf buffer) {
		this.buffer = buffer;
	}

	@Override
	protected void doBeginRead() throws Exception {
		ByteBuf data = buffer;
		buffer = null;
		pipeline().fireChannelRead(data);
	}

	@Override
	protected void doWrite(ChannelOutboundBuffer buffer) throws Exception {
		ByteBuf buf = (ByteBuf) buffer.current();
		serverchannel.writeAndFlush(new DatagramPacket(buf, this.remote));
	}

	@Override
	protected boolean isCompatible(EventLoop eventloop) {
		return eventloop instanceof UdpEventLoop;
	}

	@Override
	protected AbstractUnsafe newUnsafe() {
		return new UdpChannelUnsafe();
	}

	@Override
	protected SocketAddress localAddress0() {
		return serverchannel.localAddress0();
	}

	@Override
	protected SocketAddress remoteAddress0() {
		return remote;
	}

	@Override
	protected void doBind(SocketAddress addr) throws Exception {
		throw new UnsupportedOperationException();
	}

	private class UdpChannelUnsafe extends AbstractUnsafe {

		@Override
		public void connect(SocketAddress addr1, SocketAddress addr2, ChannelPromise pr) {
		}

	}

}
