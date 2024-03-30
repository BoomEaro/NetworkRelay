package ru.boomearo.networkrelay.upstream;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import ru.boomearo.networkrelay.app.SimpleChannelInitializer;
import ru.boomearo.networkrelay.downstream.TcpRelayDownstreamHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class TcpRelayUpstreamHandler extends ChannelInboundHandlerAdapter {

    private final Logger logger;
    private final ChannelFactory<? extends Channel> channelFactory;
    private final InetSocketAddress inetSocketAddress;
    private final int timeout;

    private Channel downstreamChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.logger.log(Level.INFO, "TCP: Opening Downstream for Upstream " + ctx.channel().remoteAddress() + " -> " + this.inetSocketAddress + "...");

        this.downstreamChannel = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channelFactory(this.channelFactory)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        SimpleChannelInitializer.INSTANCE.initChannel(ch);

                        ch.pipeline().addLast("timeout", new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                        ch.pipeline().addLast("downstream", new TcpRelayDownstreamHandler(logger, ctx.channel()));
                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.timeout)
                .remoteAddress(this.inetSocketAddress)
                .connect()
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        ctx.channel().close();
                        this.logger.log(Level.SEVERE, "TCP: Failed to open Downstream", future.cause());
                        return;
                    }
                    ctx.channel().read();
                    ctx.channel().config().setAutoRead(true);
                })
                .channel();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.logger.log(Level.INFO, "TCP: Closed Upstream " + ctx.channel().remoteAddress() + " -> " + this.downstreamChannel.remoteAddress());

        this.downstreamChannel.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!this.downstreamChannel.isActive()) {
            return;
        }

        this.downstreamChannel.writeAndFlush(msg, this.downstreamChannel.voidPromise());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!ctx.channel().isActive()) {
            return;
        }

        ctx.close();
        this.logger.log(Level.SEVERE, "TCP: Exception on Upstream " + ctx.channel().remoteAddress() + " -> " + this.downstreamChannel.remoteAddress(), cause);
    }

}
