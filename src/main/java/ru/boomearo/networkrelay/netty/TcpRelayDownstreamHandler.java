package ru.boomearo.networkrelay.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.boomearo.networkrelay.netty.ChannelWrapper;
import ru.boomearo.networkrelay.utils.ExceptionUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
@Getter
public class TcpRelayDownstreamHandler extends ChannelInboundHandlerAdapter {

    private final Logger logger;
    private final ChannelWrapper upstreamChannel;

    private ChannelWrapper currentChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel = new ChannelWrapper(ctx.channel());

        // Read data from upstream and make it auto read
        this.upstreamChannel.read();
        this.upstreamChannel.setAutoRead(true);

        this.logger.log(Level.INFO, "TCP: Opened Downstream " + this.currentChannel.getRemoteAddress() + " <- " + this.upstreamChannel.getRemoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel.setClosed(true);

        // Close upstream now
        this.upstreamChannel.close();

        this.logger.log(Level.INFO, "TCP: Closed Downstream " + this.currentChannel.getRemoteAddress() + " <- " + this.upstreamChannel.getRemoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!this.upstreamChannel.isActive()) {
            return;
        }

        this.upstreamChannel.writeAndFlushVoidPromise(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!this.currentChannel.isActive()) {
            return;
        }

        this.currentChannel.close();
        ExceptionUtils.formatExceptionLogger(this.logger, "TCP: Exception on Downstream " + this.currentChannel.getRemoteAddress() + " <- " + this.upstreamChannel.getRemoteAddress(), cause);
    }

}
