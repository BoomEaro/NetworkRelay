package ru.boomearo.networkrelay.netty;

import io.netty.channel.Channel;
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

    public void write(@NonNull Object packet) {
        if (this.closed) {
            return;
        }

        this.channel.write(packet);
    }

    public void writeVoidPromise(@NonNull Object packet) {
        if (this.closed) {
            return;
        }

        this.channel.write(packet, this.channel.voidPromise());
    }

    public void writeAndFlush(@NonNull Object packet) {
        if (this.closed) {
            return;
        }

        this.channel.writeAndFlush(packet);
    }

    public void writeAndFlushVoidPromise(@NonNull Object packet) {
        if (this.closed) {
            return;
        }

        this.channel.writeAndFlush(packet, this.channel.voidPromise());
    }

    public void flush() {
        if (this.closed) {
            return;
        }

        this.channel.flush();
    }

    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;

        this.channel.close();
    }
}
