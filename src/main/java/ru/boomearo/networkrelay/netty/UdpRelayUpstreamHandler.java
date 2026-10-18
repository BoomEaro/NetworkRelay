package ru.boomearo.networkrelay.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Getter
@Slf4j
public class UdpRelayUpstreamHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final InetSocketAddress socketAddressDestination;
    private final ChannelFactory<? extends DatagramChannel> channelFactory;
    private final int timeout;
    private final Set<InetAddress> whitelist;

    private final Map<InetSocketAddress, UdpRelayDownstreamHandler> downstreamHandlers = new HashMap<>();

    @Nullable
    private ChannelWrapper currentChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel = new ChannelWrapper(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (this.currentChannel == null) {
            return;
        }

        this.currentChannel.setClosed(true);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        InetSocketAddress senderAddress = msg.sender();

        if (!this.whitelist.isEmpty()) {
            if (!this.whitelist.contains(senderAddress.getAddress())) {
                return;
            }
        }

        UdpRelayDownstreamHandler downstreamHandler = this.downstreamHandlers.get(senderAddress);
        if (downstreamHandler == null) {
            UdpRelayDownstreamHandler newUdpRelayDownstreamHandler = new UdpRelayDownstreamHandler(this, senderAddress);

            new Bootstrap()
                    .group(ctx.channel().eventLoop())
                    .channelFactory(this.channelFactory)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            SimpleChannelInitializer.INSTANCE.initChannel(ch);

                            ch.pipeline().addLast("stats", new StatisticsDownstreamHandler());
                            ch.pipeline().addLast("idle", new IdleStateHandler(0, 0, timeout, TimeUnit.MILLISECONDS) {
                                @Override
                                protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
                                    ctx.close();
                                }
                            });
                            ch.pipeline().addLast("downstream", newUdpRelayDownstreamHandler);
                        }
                    })
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.timeout)
                    .option(ChannelOption.RECVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(65535))
                    .remoteAddress(this.socketAddressDestination)
                    .connect()
                    .addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            future.channel().close();
                            log.error("Failed to open Downstream {}", this.socketAddressDestination, future.cause());
                        }
                    });
            this.downstreamHandlers.put(senderAddress, newUdpRelayDownstreamHandler);
            downstreamHandler = newUdpRelayDownstreamHandler;
        }

        DatagramPacket datagramPacket = new DatagramPacket(msg.content().retain(), this.socketAddressDestination);
        downstreamHandler.writePacket(datagramPacket);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception on Upstream handler", cause);
    }

}
