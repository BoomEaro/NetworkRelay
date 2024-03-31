package ru.boomearo.networkrelay.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import ru.boomearo.networkrelay.utils.ExceptionUtils;

@RequiredArgsConstructor
@Getter
@Log4j2
public class TcpRelayDownstreamHandler extends ChannelInboundHandlerAdapter {

    private final ChannelWrapper upstreamChannel;

    private ChannelWrapper currentChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel = new ChannelWrapper(ctx.channel());

        // Read all pending data from upstream and forward it to downstream
        this.upstreamChannel.setAutoRead(true);

        log.log(Level.INFO, "TCP: Opened Downstream " + this.currentChannel.getRemoteAddress() + " <- " + this.upstreamChannel.getRemoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel.setClosed(true);

        // Close upstream now
        this.upstreamChannel.close();

        log.log(Level.INFO, "TCP: Closed Downstream " + this.currentChannel.getRemoteAddress() + " <- " + this.upstreamChannel.getRemoteAddress());
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
        ExceptionUtils.formatException("TCP: Exception on Downstream " + this.currentChannel.getRemoteAddress() + " <- " + this.upstreamChannel.getRemoteAddress(), cause);
    }

}
