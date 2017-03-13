package udpserversocketchannel.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.LinkedHashMap;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.util.internal.PlatformDependent;

public class NioUdpServerChannel extends AbstractNioMessageChannel implements ServerSocketChannel {

	private final ChannelMetadata metadata = new ChannelMetadata(true);
	private final UdpServerChannelConfig config;

	public NioUdpServerChannel() throws IOException {
		this(SelectorProvider.provider().openDatagramChannel(StandardProtocolFamily.INET));
	}

	protected NioUdpServerChannel(DatagramChannel dchannel) {
		super(null, dchannel, SelectionKey.OP_READ);
		this.config = new UdpServerChannelConfig(this, dchannel);
	}

	@Override
	public ServerSocketChannelConfig config() {
		return config;
	}

	@Override
	public ChannelMetadata metadata() {
		return metadata;
	}

	@Override
	protected DatagramChannel javaChannel() {
		return (DatagramChannel) super.javaChannel();
	}

	@Override
	public boolean isActive() {
		return this.javaChannel().isOpen() && this.javaChannel().socket().isBound();
	}

	@Override
	protected SocketAddress localAddress0() {
		return this.javaChannel().socket().getLocalSocketAddress();
	}

	@Override
	public InetSocketAddress localAddress() {
		return (InetSocketAddress) super.localAddress();
	}

	@Override
	protected SocketAddress remoteAddress0() {
		return null;
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return null;
	}

	@Override
	protected void doBind(SocketAddress addr) throws Exception {
		javaChannel().socket().bind(addr);
	}

	@Override
	protected void doClose() throws Exception {
		for (UdpChannel channel : channels.values()) {
			channel.close();
		}
		javaChannel().close();
	}

	protected final LinkedHashMap<InetSocketAddress, UdpChannel> channels = new LinkedHashMap<>();

	public void removeChannel(final Channel channel) {
		eventLoop().submit(new Runnable() {
			@Override
			public void run() {
				InetSocketAddress remote = (InetSocketAddress) channel.remoteAddress();
				if (channels.get(remote) == channel) {
					channels.remove(remote);
				}
			}
		});
	}

	private RecvByteBufAllocator.Handle recvAllocatorHandle = null;
	private RecvByteBufAllocator.Handle getRecvAllocatorHandle() {
		if (recvAllocatorHandle == null) {
			recvAllocatorHandle = config.getRecvByteBufAllocator().newHandle();
		}
		return recvAllocatorHandle;
	}

	@Override
	protected int doReadMessages(List<Object> list) throws Exception {
		DatagramChannel javaChannel = javaChannel();
		RecvByteBufAllocator.Handle allocatorHandle = getRecvAllocatorHandle();
		ByteBuf buffer = allocatorHandle.allocate(config.getAllocator());
		boolean freeBuffer = true;
		try {
			//read message
			ByteBuffer nioBuffer = buffer.internalNioBuffer(buffer.writerIndex(), buffer.writableBytes());
			int nioPos = nioBuffer.position();
			InetSocketAddress inetSocketAddress = (InetSocketAddress) javaChannel.receive(nioBuffer);
			if (inetSocketAddress == null) {
				return 0;
			}
			int recvBytes = nioBuffer.position() - nioPos;
			buffer.writerIndex(buffer.writerIndex() + recvBytes);
			allocatorHandle.record(recvBytes);
			//allocate new channel or use existing one and push message to it
			UdpChannel udpchannel = channels.get(inetSocketAddress);
			if (udpchannel == null || !udpchannel.isOpen()) {
				udpchannel = new UdpChannel(this, inetSocketAddress);
				channels.put(inetSocketAddress, udpchannel);
				list.add(udpchannel);
				udpchannel.setReceivedData(buffer);
				freeBuffer = false;
				return 1;
			} else {
				udpchannel.setReceivedData(buffer);
				freeBuffer = false;
				udpchannel.read();
				return 0;
			}
		} catch (Throwable t) {
			PlatformDependent.throwException(t);
			return -1;
		} finally {
			if (freeBuffer) {
				buffer.release();
			}
		}
	}

	@Override
	protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer buffer) throws Exception {
		DatagramPacket dpacket = (DatagramPacket) msg;
		InetSocketAddress recipient = dpacket.recipient();
		ByteBuf byteBuf = dpacket.content();
		int readableBytes = byteBuf.readableBytes();
		if (readableBytes == 0) {
			return true;
		}
		ByteBuffer internalNioBuffer = byteBuf.internalNioBuffer(byteBuf.readerIndex(), readableBytes);
		return javaChannel().send(internalNioBuffer, recipient) > 0;
	}

	@Override
	protected boolean doConnect(SocketAddress addr1, SocketAddress addr2) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doFinishConnect() throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doDisconnect() throws Exception {
		throw new UnsupportedOperationException();
	}

}
