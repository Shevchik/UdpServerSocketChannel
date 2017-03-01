package udpserversocketchannel.channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.DatagramPacket;
import udpserversocketchannel.eventloop.UdpEventLoop;

public class UdpChannel extends AbstractChannel {

	protected final ChannelMetadata metadata = new ChannelMetadata(false);
	protected final DefaultChannelConfig config = new DefaultChannelConfig(this);
	protected final NioUdpServerChannel serverchannel;
	protected final InetSocketAddress remote;

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

	private volatile boolean open = true;

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
		serverchannel.removeChannel(this);
	}

	@Override
	protected void doDisconnect() throws Exception {
		doClose();
	}

	private volatile ByteBuf buffer;
	public void setReceivedData(ByteBuf buffer) {
		this.buffer = buffer;
	}

	@Override
	protected void doBeginRead() throws Exception {
		if (buffer == null) {
			return;
		}
		ByteBuf data = buffer;
		buffer = null;
		pipeline().fireChannelRead(data);
	}

	@Override
	protected void doWrite(final ChannelOutboundBuffer buffer) throws Exception {
		ByteBuf buf = ((ByteBuf) buffer.current()).retain();
		ChannelFuture sendfuture = serverchannel.writeAndFlush(new DatagramPacket(buf, this.remote)).sync();
		if (sendfuture.isSuccess()) {
			buffer.remove();
		} else {
			buffer.remove(sendfuture.cause());
		}
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
