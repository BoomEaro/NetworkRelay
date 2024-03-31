package ru.boomearo.networkrelay.netty;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;

public class SimpleChannelInitializer extends ChannelInitializer<Channel> {

    public static SimpleChannelInitializer INSTANCE = new SimpleChannelInitializer();

    private static final int LOW_MARK = 2 << 18;
    private static final int HIGH_MARK = 2 << 20;
    private static final WriteBufferWaterMark MARK = new WriteBufferWaterMark(LOW_MARK, HIGH_MARK);

    @Override
    public void initChannel(Channel ch) throws Exception {
        ch.config().setOption(ChannelOption.IP_TOS, 0x18);
        ch.config().setOption(ChannelOption.TCP_NODELAY, true);
        ch.config().setOption(ChannelOption.SO_KEEPALIVE, true);
        ch.config().setAllocator(PooledByteBufAllocator.DEFAULT);
        ch.config().setWriteBufferWaterMark(MARK);
    }

}
