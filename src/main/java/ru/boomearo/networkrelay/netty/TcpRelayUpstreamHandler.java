package ru.boomearo.networkrelay.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.haproxy.*;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@RequiredArgsConstructor
@Slf4j
public class TcpRelayUpstreamHandler extends ChannelInboundHandlerAdapter {

    public static final LongAdder OPENED_CONNECTIONS = new LongAdder();

    private final ChannelFactory<? extends Channel> channelFactory;
    private final SocketAddress socketAddressDestination;
    private final int timeout;
    private final boolean proxyProtocol;

    private ChannelWrapper currentChannel;
    private TcpRelayDownstreamHandler tcpRelayDownstreamHandler;
    private Channel downstreamChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        OPENED_CONNECTIONS.increment();

        Channel channel = ctx.channel();
        this.currentChannel = new ChannelWrapper(channel);

        this.tcpRelayDownstreamHandler = new TcpRelayDownstreamHandler(this.currentChannel);

        SocketAddress remoteAddress = this.currentChannel.getRemoteAddress();

        log.info("Opening Downstream for Upstream {} -> {}...", remoteAddress, this.socketAddressDestination);

        ChannelFuture channelFuture = new Bootstrap()
                .group(channel.eventLoop())
                .channelFactory(this.channelFactory)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        SimpleChannelInitializer.INSTANCE.initChannel(ch);

                        if (proxyProtocol &&
                                remoteAddress instanceof InetSocketAddress sourceAddress &&
                                socketAddressDestination instanceof InetSocketAddress destinationAddress) {
                            ch.pipeline().addLast(HAProxyMessageEncoder.INSTANCE);

                            ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    HAProxyMessage proxyMessage = new HAProxyMessage(
                                            HAProxyProtocolVersion.V2,
                                            HAProxyCommand.PROXY,
                                            HAProxyProxiedProtocol.TCP4,
                                            sourceAddress.getHostString(),
                                            destinationAddress.getHostString(),
                                            sourceAddress.getPort(),
                                            destinationAddress.getPort()
                                    );

                                    ctx.writeAndFlush(proxyMessage);

                                    super.channelActive(ctx);
                                }
                            });
                        }

                        ch.pipeline().addLast("stats", new StatisticsDownstreamHandler());
                        ch.pipeline().addLast("fch", new FlushConsolidationHandler(20));
                        ch.pipeline().addLast("idle", new IdleStateHandler(0, 0, timeout, TimeUnit.MILLISECONDS) {
                            @Override
                            protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
                                ctx.close();
                            }
                        });
                        ch.pipeline().addLast("downstream", tcpRelayDownstreamHandler);

                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.timeout)
                .remoteAddress(this.socketAddressDestination)
                .connect().addListener((ChannelFuture future) -> {
                    if (!future.isSuccess() && channel.isActive()) {
                        log.warn("Failed to connect to {}: {}", this.socketAddressDestination, future.cause());
                        this.currentChannel.close();
                    }
                });
        this.downstreamChannel = channelFuture.channel();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        OPENED_CONNECTIONS.decrement();

        this.currentChannel.setClosed(true);

        // Close downstream now
        this.downstreamChannel.close();

        // Release possible queued packets
        this.tcpRelayDownstreamHandler.handleQueuedPackets(ReferenceCountUtil::release);

        log.info("Closed Upstream {} -> {}", this.currentChannel.getRemoteAddress(), this.socketAddressDestination);
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

        log.error("Exception on Upstream {} -> {}", this.currentChannel.getRemoteAddress(), this.socketAddressDestination, cause);
    }

}
