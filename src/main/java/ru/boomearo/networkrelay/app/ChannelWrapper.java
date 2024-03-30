package ru.boomearo.networkrelay.app;

import io.netty.channel.Channel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.net.SocketAddress;

@RequiredArgsConstructor
@Data
public class ChannelWrapper {

    private final Channel channel;

    private boolean closed = false;

    public SocketAddress getRemoteAddress() {
        return this.channel.remoteAddress();
    }

    public boolean isActive() {
        return this.channel.isActive();
    }

    public void write(Object packet) {
        if (this.closed) {
            return;
        }

        this.channel.write(packet);
    }

    public void writeVoidPromise(Object packet) {
        if (this.closed) {
            return;
        }

        this.channel.write(packet, this.channel.voidPromise());
    }

    public void writeAndFlush(Object packet) {
        if (this.closed) {
            return;
        }

        this.channel.writeAndFlush(packet);
    }

    public void writeAndFlushVoidPromise(Object packet) {
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
