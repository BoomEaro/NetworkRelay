package ru.boomearo.networkrelay.upstream;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import ru.boomearo.networkrelay.app.SimpleChannelInitializer;
import ru.boomearo.networkrelay.downstream.TcpRelayDownstreamHandler;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class TcpRelayUpstreamHandler extends ChannelInboundHandlerAdapter {

    private final Logger logger;
    private final ChannelFactory<? extends Channel> channelFactory;
    private final SocketAddress socketAddress;
    private final int timeout;

    private Channel downstreamChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.logger.log(Level.INFO, "Opening Upstream " + ctx.channel().remoteAddress() + "...");

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
                .remoteAddress(this.socketAddress)
                .connect()
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        ctx.channel().close();
                        this.logger.log(Level.SEVERE, "Failed to open Upstream", future.cause());
                        return;
                    }
                    ctx.channel().read();
                    ctx.channel().config().setAutoRead(true);

                    this.logger.log(Level.INFO, "Opened Upstream " + ctx.channel().remoteAddress() + " -> " + this.downstreamChannel.remoteAddress());
                })
                .channel();

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.logger.log(Level.INFO, "Closed Upstream " + ctx.channel().remoteAddress() + " -> " + this.downstreamChannel.remoteAddress());

        super.channelInactive(ctx);

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
        this.logger.log(Level.SEVERE, "Exception on Upstream handler", cause);
    }

}
