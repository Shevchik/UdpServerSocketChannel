# UdpServerSocketChannel:

UDP server for netty 4.1 which allocates user channel per remote address (just like netty TCP server) <br>
By default allocated channels are never closed, so it's up to user to close channels upon read timeout or any other event <br>

Can also use multiple threads for IO if running epoll capable system (via SO_REUSEPORT) <br>

# Jenkins:

http://build.true-games.org/view/ProtocolSupport/job/UdpServerSocketChannel/

# License:

GNU LGPLv3

# Usage example:

```import java.nio.charset.StandardCharsets;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import udpserversocketchannel.channel.UdpServerChannel;

public class ExampleUdpServer {

	public static void main(String[] args) {
		ServerBootstrap bootstrap = new ServerBootstrap()
		.group(new DefaultEventLoopGroup())
		.childHandler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel channel) throws Exception {
				channel.pipeline()
				.addLast(new ReadTimeoutHandler(2))
				.addLast(new Echo());
			}
		});
		if (args.length > 0) {
			int ioThreads = Integer.parseInt(args[0]);
			bootstrap.channelFactory(new ChannelFactory<ServerChannel>() {
				@Override
				public ServerChannel newChannel() {
					return new UdpServerChannel(ioThreads);
				}
			});
		} else {
			bootstrap.channel(UdpServerChannel.class);
		}
		bootstrap.bind("0.0.0.0", 1122).syncUninterruptibly();
	}

	private static class Echo extends SimpleChannelInboundHandler<ByteBuf> {
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
			ctx.channel().close();
		}
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			super.channelActive(ctx);
			System.err.println("ACTIVE: "+ctx.channel().remoteAddress());
		}
		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
			System.err.println("INACTIVE: "+ctx.channel().remoteAddress());
		}
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, ByteBuf bytebuf) throws Exception {
			System.err.print("DATA: "+bytebuf.toString(StandardCharsets.UTF_8));
			ctx.channel().writeAndFlush(Unpooled.copiedBuffer(bytebuf));
		}
	}

}```