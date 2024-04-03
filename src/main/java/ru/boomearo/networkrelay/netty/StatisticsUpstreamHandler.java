package ru.boomearo.networkrelay.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.atomic.LongAdder;

public class StatisticsUpstreamHandler extends ChannelDuplexHandler {

    public static final LongAdder TOTAL_READ = new LongAdder();
    public static final LongAdder TOTAL_WRITE = new LongAdder();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf byteBuf) {
            TOTAL_READ.add(byteBuf.readableBytes());
        }

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf byteBuf) {
            TOTAL_WRITE.add(byteBuf.readableBytes());
        }

        super.write(ctx, msg, promise);
    }
}
