package ru.boomearo.networkrelay.downstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class TcpRelayDownstreamHandler extends ChannelInboundHandlerAdapter {

    private final Logger logger;
    private final Channel upstreamChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.logger.log(Level.INFO, "Opened Downstream " + ctx.channel().remoteAddress() + " -> " + this.upstreamChannel.remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.logger.log(Level.INFO, "Closed Downstream " + ctx.channel().remoteAddress() + " -> " + this.upstreamChannel.remoteAddress());
        super.channelInactive(ctx);

        this.upstreamChannel.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!this.upstreamChannel.isActive()) {
            return;
        }

        this.upstreamChannel.writeAndFlush(msg, this.upstreamChannel.voidPromise());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        this.logger.log(Level.SEVERE, "Exception on Downstream handler", cause);
    }

}
