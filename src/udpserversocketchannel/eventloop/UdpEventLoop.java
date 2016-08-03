package udpserversocketchannel.eventloop;

import java.util.concurrent.ThreadFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;

public class UdpEventLoop extends SingleThreadEventLoop {

	protected UdpEventLoop(EventLoopGroup lg, ThreadFactory tf) {
		super(lg, tf, false);
	}

	@Override
	protected void run() {
		do {
			Runnable takeTask = this.takeTask();
			if (takeTask != null) {
				takeTask.run();
				updateLastExecutionTime();
			}
		} while (!confirmShutdown());
	}

}
