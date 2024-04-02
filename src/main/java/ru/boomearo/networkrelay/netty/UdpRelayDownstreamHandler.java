package ru.boomearo.networkrelay.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Getter
@Log4j2
public class UdpRelayDownstreamHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final UdpRelayUpstreamHandler udpRelayUpstreamHandler;
    private final InetSocketAddress socketAddressSource;

    private final Queue<DatagramPacket> packetQueue = new LinkedList<>();

    private ChannelWrapper currentChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel = new ChannelWrapper(ctx.channel());

        handleQueuedPackets((msg) -> this.currentChannel.writeAndFlushVoidPromise(msg));

        log.log(Level.INFO, "Opened Downstream " + this.currentChannel.getRemoteAddress() + " <- " + this.socketAddressSource);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.currentChannel.setClosed(true);

        this.udpRelayUpstreamHandler.getDownstreamHandlers().remove(this.socketAddressSource);

        log.log(Level.INFO, "Closed Downstream " + this.currentChannel.getRemoteAddress() + " <- " + this.socketAddressSource);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        this.udpRelayUpstreamHandler.getCurrentChannel().writeAndFlushVoidPromise(new DatagramPacket(msg.content().retain(), this.socketAddressSource));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        this.currentChannel.close();

        log.log(Level.ERROR, "Exception on Downstream handler", cause);
    }

    public void writePacket(DatagramPacket msg) {
        if (this.currentChannel == null) {
            this.packetQueue.add(msg);
            return;
        }

        this.currentChannel.writeAndFlushVoidPromise(msg);
    }

    public void handleQueuedPackets(Consumer<DatagramPacket> consumer) {
        DatagramPacket packet;
        while ((packet = this.packetQueue.poll()) != null) {
            consumer.accept(packet);
        }
    }
}
