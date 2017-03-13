package udpserversocketchannel.eventloop;

import java.util.concurrent.ThreadFactory;

import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;

public class UdpEventLoopGroup extends MultithreadEventLoopGroup {

	public UdpEventLoopGroup() {
		super(0, null, new Object[0]);
	}

	@Override
	protected EventExecutor newChild(ThreadFactory tf, Object... args) throws Exception {
		return new UdpEventLoop(this, tf);
	}

}
