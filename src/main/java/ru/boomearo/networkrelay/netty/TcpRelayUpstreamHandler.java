package ru.boomearo.networkrelay.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@RequiredArgsConstructor
@Log4j2
public class TcpRelayUpstreamHandler extends ChannelInboundHandlerAdapter {

    public static final LongAdder OPENED_CONNECTIONS = new LongAdder();

    private final ChannelFactory<? extends Channel> channelFactory;
    private final SocketAddress socketAddressDestination;
    private final int timeout;

    private ChannelWrapper currentChannel;
    private TcpRelayDownstreamHandler tcpRelayDownstreamHandler;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        OPENED_CONNECTIONS.increment();

        this.currentChannel = new ChannelWrapper(ctx.channel());

        this.tcpRelayDownstreamHandler = new TcpRelayDownstreamHandler(this.currentChannel);

        log.log(Level.INFO, "Opening Downstream for Upstream " + this.currentChannel.getRemoteAddress() + " -> " + this.socketAddressDestination + "...");

        new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channelFactory(this.channelFactory)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        SimpleChannelInitializer.INSTANCE.initChannel(ch);

                        ch.pipeline().addLast("stats", new StatisticsDownstreamHandler());
                        ch.pipeline().addLast("fch", new FlushConsolidationHandler(20));
                        ch.pipeline().addLast("timeout", new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                        ch.pipeline().addLast("downstream", tcpRelayDownstreamHandler);
                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.timeout)
                .remoteAddress(this.socketAddressDestination)
                .connect();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        OPENED_CONNECTIONS.decrement();

        this.currentChannel.setClosed(true);

        // Close downstream now
        ChannelWrapper downstreamChannel = this.tcpRelayDownstreamHandler.getCurrentChannel();
        if (downstreamChannel != null) {
            downstreamChannel.close();
        }

        // Release possible queued packets
        this.tcpRelayDownstreamHandler.handleQueuedPackets(ReferenceCountUtil::release);

        log.log(Level.INFO, "Closed Upstream " + this.currentChannel.getRemoteAddress() + " -> " + this.socketAddressDestination);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        this.tcpRelayDownstreamHandler.writePacket(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!this.currentChannel.isActive()) {
            return;
        }

        this.currentChannel.close();

        log.log(Level.ERROR, "Exception on Upstream " + this.currentChannel.getRemoteAddress() + " -> " + this.socketAddressDestination, cause);
    }

}
