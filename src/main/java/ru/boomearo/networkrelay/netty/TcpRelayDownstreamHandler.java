package ru.boomearo.networkrelay.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Slf4j
public class TcpRelayDownstreamHandler extends ChannelInboundHandlerAdapter {

    public static final LongAdder OPENED_CONNECTIONS = new LongAdder();

    private final ChannelWrapper upstreamChannel;

    private final Queue<Object> packetQueue = new LinkedList<>();

    private ChannelWrapper currentChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        OPENED_CONNECTIONS.increment();

        this.currentChannel = new ChannelWrapper(ctx.channel());

        handleQueuedPackets((msg) -> this.currentChannel.writeAndFlushVoidPromise(msg));

        log.info("Opened Downstream {} <- {}", this.currentChannel.getRemoteAddress(), this.upstreamChannel.getRemoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        OPENED_CONNECTIONS.decrement();

        this.currentChannel.setClosed(true);

        // Close upstream now
        this.upstreamChannel.close();

        log.info("Closed Downstream {} <- {}", this.currentChannel.getRemoteAddress(), this.upstreamChannel.getRemoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        this.upstreamChannel.writeAndFlushVoidPromise(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!this.currentChannel.isActive()) {
            return;
        }

        this.currentChannel.close();
        log.error("Exception on Downstream {} <- {}", this.currentChannel.getRemoteAddress(), this.upstreamChannel.getRemoteAddress(), cause);
    }

    public void writePacket(@NonNull Object msg) {
        if (this.currentChannel == null) {
            this.packetQueue.add(msg);
            return;
        }

        this.currentChannel.writeAndFlushVoidPromise(msg);
    }

    public void handleQueuedPackets(@NonNull Consumer<Object> consumer) {
        Object packet;
        while ((packet = this.packetQueue.poll()) != null) {
            consumer.accept(packet);
        }
    }
}
