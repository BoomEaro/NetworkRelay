package ru.boomearo.networkrelay.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import ru.boomearo.networkrelay.utils.ExceptionUtils;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Log4j2
public class TcpRelayUpstreamHandler extends ChannelInboundHandlerAdapter {

    private final ChannelFactory<? extends Channel> channelFactory;
    private final SocketAddress socketAddressDestination;
    private final int timeout;

    private ChannelWrapper currentChannel;
    private TcpRelayDownstreamHandler tcpRelayDownstreamHandler;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel = new ChannelWrapper(ctx.channel());

        log.log(Level.INFO, "TCP: Opening Downstream for Upstream " + this.currentChannel.getRemoteAddress() + " -> " + this.socketAddressDestination + "...");

        this.tcpRelayDownstreamHandler = new TcpRelayDownstreamHandler(this.currentChannel);

        new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channelFactory(this.channelFactory)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        SimpleChannelInitializer.INSTANCE.initChannel(ch);

                        ch.pipeline().addLast("fclh", new FlushConsolidationHandler(20));
                        ch.pipeline().addLast("timeout", new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                        ch.pipeline().addLast("downstream", tcpRelayDownstreamHandler);
                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.timeout)
                .remoteAddress(this.socketAddressDestination)
                .connect()
                .addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        this.currentChannel.close();
                        ExceptionUtils.formatException("TCP: Failed to open Downstream " + this.socketAddressDestination, future.cause());
                    }
                });

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel.setClosed(true);

        // Close downstream now
        if (this.tcpRelayDownstreamHandler != null) {
            ChannelWrapper downstreamChannel = this.tcpRelayDownstreamHandler.getCurrentChannel();
            if (downstreamChannel != null) {
                downstreamChannel.close();
            }
        }

        log.log(Level.INFO, "TCP: Closed Upstream " + this.currentChannel.getRemoteAddress() + " -> " + this.socketAddressDestination);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ChannelWrapper downstreamChannel = this.tcpRelayDownstreamHandler.getCurrentChannel();
        if (!downstreamChannel.isActive()) {
            return;
        }

        downstreamChannel.writeAndFlushVoidPromise(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!this.currentChannel.isActive()) {
            return;
        }

        this.currentChannel.close();

        ExceptionUtils.formatException("TCP: Exception on Upstream " + this.currentChannel.getRemoteAddress() + " -> " + this.socketAddressDestination, cause);
    }

}
