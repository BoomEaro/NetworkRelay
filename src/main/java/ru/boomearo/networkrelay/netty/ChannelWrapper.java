package ru.boomearo.networkrelay.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.net.SocketAddress;

@RequiredArgsConstructor
@Data
public class ChannelWrapper {

    @NonNull
    private final Channel channel;

    private boolean closed = false;

    @NonNull
    public SocketAddress getRemoteAddress() {
        return this.channel.remoteAddress() == null ? this.channel.parent().localAddress() : this.channel.remoteAddress();
    }

    public boolean isActive() {
        return this.channel.isActive();
    }

    public void writeAndFlushVoidPromise(@NonNull Object packet) {
        if (this.closed) {
            ReferenceCountUtil.release(packet);
            return;
        }

        this.channel.writeAndFlush(packet, this.channel.voidPromise());
    }

    public void setAutoRead(boolean autoRead) {
        this.channel.config().setAutoRead(autoRead);
    }

    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;

        this.channel.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
    }
}
