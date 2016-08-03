package udpserversocketchannel.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
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

	private static DatagramChannel newSocket() throws IOException {
		return SelectorProvider.provider().openDatagramChannel(StandardProtocolFamily.INET);
	}

	public NioUdpServerChannel() throws IOException {
		this(newSocket());
	}

	protected NioUdpServerChannel(DatagramChannel dchannel) {
		super(null, dchannel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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

	private final ConcurrentHashMap<InetSocketAddress, UdpChannel> udpchannels = new ConcurrentHashMap<>();

	@Override
	protected void doClose() throws Exception {
		for (UdpChannel channel : udpchannels.values()) {
			channel.close().syncUninterruptibly();
		}
		javaChannel().close();
	}

	public void closeChannel(UdpChannel udpChannel) {
		udpchannels.remove(udpChannel.remoteAddress());
	}

	private RecvByteBufAllocator.Handle allocHandle;

	@Override
	protected int doReadMessages(List<Object> list) throws Exception {
		DatagramChannel javaChannel = this.javaChannel();
		if (this.allocHandle == null) {
			this.allocHandle = this.config.getRecvByteBufAllocator().newHandle();
		}
		ByteBuf buffer = allocHandle.allocate(config.getAllocator());
		boolean release = true;
		try {
			ByteBuffer internalNioBuffer = buffer.internalNioBuffer(buffer.writerIndex(), buffer.writableBytes());
			int position = internalNioBuffer.position();
			InetSocketAddress inetSocketAddress = (InetSocketAddress) javaChannel.receive(internalNioBuffer);
			if (inetSocketAddress == null) {
				return 0;
			}
            int n = internalNioBuffer.position() - position;
            buffer.writerIndex(buffer.writerIndex() + n);
            allocHandle.record(n);
            release = false;
			UdpChannel udpchannel = udpchannels.get(inetSocketAddress);
			if (udpchannel == null) {
				udpchannel = new UdpChannel(this, inetSocketAddress);
				udpchannels.put(inetSocketAddress, udpchannel);
				list.add(udpchannel);
				udpchannel.setReceivedData(buffer);
				return 1;
			} else {
				udpchannel.setReceivedData(buffer);
				udpchannel.read();
				return 0;
			}
		} catch (Throwable t) {
			PlatformDependent.throwException(t);
			return -1;
		} finally {
			if (release) {
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
