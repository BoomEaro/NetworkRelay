package ru.boomearo.networkrelay.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import ru.boomearo.networkrelay.utils.ExceptionUtils;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Getter
@Log4j2
public class UdpRelayUpstreamHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final InetSocketAddress socketAddressDestination;
    private final ChannelFactory<? extends DatagramChannel> channelFactory;
    private final int timeout;

    private final Map<InetSocketAddress, UdpRelayDownstreamHandler> downstreamHandlers = new HashMap<>();

    private ChannelWrapper currentChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel = new ChannelWrapper(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel.setClosed(true);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        InetSocketAddress senderAddress = msg.sender();

        UdpRelayDownstreamHandler downstreamHandler = this.downstreamHandlers.get(senderAddress);
        if (downstreamHandler == null) {
            UdpRelayDownstreamHandler newUdpRelayDownstreamHandler = new UdpRelayDownstreamHandler(this, senderAddress);

            new Bootstrap()
                    .group(ctx.channel().eventLoop())
                    .channelFactory(this.channelFactory)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            SimpleChannelInitializer.INSTANCE.initChannel(ch);

                            ch.pipeline().addLast("timeout", new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                            ch.pipeline().addLast("downstream", newUdpRelayDownstreamHandler);
                        }
                    })
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.timeout)
                    .remoteAddress(this.socketAddressDestination)
                    .connect()
                    .addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            future.channel().close();
                            ExceptionUtils.formatException(log, "Failed to open Downstream " + this.socketAddressDestination, future.cause());
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
        ExceptionUtils.formatException(log, "Exception on Upstream handler ", cause);
    }

}
