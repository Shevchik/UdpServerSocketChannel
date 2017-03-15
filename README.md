# UdpServerSocketChannel:

UDP ServerSocketChannel for https://github.com/netty/netty

# Jenkins:

http://build.true-games.org/view/ProtocolSupport/job/UdpServerSocketChannel/

# License:

GNU GPLv3

# Usage example:

```java
import java.nio.charset.StandardCharsets;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.ReadTimeoutHandler;
import udpserversocketchannel.channel.NioUdpServerChannel;

public class ExampleUdpServer {

	public static void main(String[] args) {
		ServerBootstrap bootstrap = new ServerBootstrap()
		.group(new NioEventLoopGroup(), new DefaultEventLoopGroup())
		.channel(NioUdpServerChannel.class)
		.childHandler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel channel) throws Exception {
				channel.pipeline()
				.addLast(new ReadTimeoutHandler(2))
				.addLast(new Echo());
			}
		});
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
			System.err.print(bytebuf.toString(StandardCharsets.UTF_8));
			ctx.channel().writeAndFlush(Unpooled.copiedBuffer(bytebuf));
		}
	}

}
```