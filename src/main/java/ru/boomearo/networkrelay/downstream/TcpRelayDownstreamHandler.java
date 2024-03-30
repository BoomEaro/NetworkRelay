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
        this.logger.log(Level.INFO, "TCP: Opened Downstream " + ctx.channel().remoteAddress() + " <- " + this.upstreamChannel.remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.logger.log(Level.INFO, "TCP: Closed Downstream " + ctx.channel().remoteAddress() + " <- " + this.upstreamChannel.remoteAddress());

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
        if (!ctx.channel().isActive()) {
            return;
        }

        ctx.close();
        this.logger.log(Level.SEVERE, "TCP: Exception on Downstream " + ctx.channel().remoteAddress() + " <- " + this.upstreamChannel.remoteAddress(), cause);
    }

}
