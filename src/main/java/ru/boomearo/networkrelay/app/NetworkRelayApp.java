package ru.boomearo.networkrelay.app;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ResourceLeakDetector;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import ru.boomearo.networkrelay.configuration.ConfigurationProvider;
import ru.boomearo.networkrelay.configuration.config.Configuration;
import ru.boomearo.networkrelay.configuration.config.ServerConfiguration;
import ru.boomearo.networkrelay.netty.SimpleChannelInitializer;
import ru.boomearo.networkrelay.netty.StatisticsUpstreamHandler;
import ru.boomearo.networkrelay.netty.TcpRelayUpstreamHandler;
import ru.boomearo.networkrelay.netty.UdpRelayUpstreamHandler;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Getter
@Log4j2
public class NetworkRelayApp {

    private EventLoopGroup tcpBossGroup;
    private EventLoopGroup tcpWorkerGroup;
    private EventLoopGroup udpWorkerGroup;

    private ConfigurationProvider<Configuration> configurationProvider;

    public NetworkRelayApp() {
        Logger redirect = LogManager.getRootLogger();
        System.setOut(IoBuilder.forLogger(redirect).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(redirect).setLevel(Level.ERROR).buildPrintStream());
    }

    public void load() {
        System.setProperty("io.netty.selectorAutoRebuildThreshold", "0");
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

        boolean epoll = false;
        if (Epoll.isAvailable()) {
            epoll = true;
            log.log(Level.INFO, "Epoll is working, utilising it!");
        } else {
            log.log(Level.WARN, "Epoll is not working, falling back to NIO");
        }

        ChannelFactory<? extends Channel> tcpChannelFactory = epoll ? EpollSocketChannel::new : NioSocketChannel::new;
        ChannelFactory<? extends ServerChannel> tcpServerChannelFactory = epoll ? EpollServerSocketChannel::new : NioServerSocketChannel::new;

        ChannelFactory<? extends DatagramChannel> udpChannelFactory = epoll ? EpollDatagramChannel::new : NioDatagramChannel::new;

        this.tcpBossGroup = newEventLoopGroup(epoll, 0, new ThreadFactoryBuilder().setNameFormat("Netty TCP Boss IO Thread #%1$d").build());
        this.tcpWorkerGroup = newEventLoopGroup(epoll, 0, new ThreadFactoryBuilder().setNameFormat("Netty TCP Worker IO Thread #%1$d").build());
        this.udpWorkerGroup = newEventLoopGroup(epoll, 1, new ThreadFactoryBuilder().setNameFormat("Netty UDP Worker IO Thread #%1$d").build());

        this.configurationProvider = new ConfigurationProvider<>(
                Configuration.class,
                Paths.get("config.yml")
        );
        this.configurationProvider.reload();

        for (ServerConfiguration serverConfiguration : this.configurationProvider.get().getTcpServers()) {
            InetSocketAddress sourceAddress = serverConfiguration.getSource();
            InetSocketAddress destinationAddress = serverConfiguration.getDestination();
            int timeout = serverConfiguration.getTimeout();

            new ServerBootstrap()
                    .group(this.tcpBossGroup, this.tcpWorkerGroup)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .channelFactory(tcpServerChannelFactory)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            SimpleChannelInitializer.INSTANCE.initChannel(ch);

                            ch.pipeline().addLast("stats", new StatisticsUpstreamHandler());
                            ch.pipeline().addLast("fch", new FlushConsolidationHandler(20));
                            ch.pipeline().addLast("timeout", new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                            ch.pipeline().addLast("upstream", new TcpRelayUpstreamHandler(tcpChannelFactory, destinationAddress, timeout));
                        }
                    })
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                    .localAddress(sourceAddress)
                    .bind()
                    .addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            log.log(Level.ERROR, "TCP: Could not bind to host " + sourceAddress, future.cause());
                            return;
                        }

                        log.log(Level.INFO, "TCP: Listening on " + sourceAddress + "...");
                    });
        }

        for (ServerConfiguration serverConfiguration : this.configurationProvider.get().getUdpServers()) {
            InetSocketAddress sourceAddress = serverConfiguration.getSource();
            InetSocketAddress destinationAddress = serverConfiguration.getDestination();
            int timeout = serverConfiguration.getTimeout();

            new Bootstrap()
                    .channelFactory(udpChannelFactory)
                    .group(this.udpWorkerGroup)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        public void initChannel(DatagramChannel ch) throws Exception {
                            SimpleChannelInitializer.INSTANCE.initChannel(ch);

                            ch.pipeline().addLast("stats", new StatisticsUpstreamHandler());
                            ch.pipeline().addLast("relay", new UdpRelayUpstreamHandler(destinationAddress, udpChannelFactory, timeout));
                        }
                    })
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                    .localAddress(sourceAddress)
                    .bind()
                    .addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            log.log(Level.ERROR, "UDP: Could not bind to host " + sourceAddress, future.cause());
                            return;
                        }

                        log.log(Level.INFO, "UDP: Listening on " + sourceAddress + "...");
                    });
        }

        new NetworkRelayConsole(this).start();
    }

    public void unload() {
        this.tcpBossGroup.shutdownGracefully();
        this.tcpWorkerGroup.shutdownGracefully();
        this.udpWorkerGroup.shutdownGracefully();
        while (true) {
            try {
                this.tcpBossGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                this.tcpWorkerGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                this.udpWorkerGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                break;
            } catch (InterruptedException ignored) {
            }
        }

        System.exit(0);
    }

    private static EventLoopGroup newEventLoopGroup(boolean epoll, int threads, ThreadFactory threadFactory) {
        return epoll ? new EpollEventLoopGroup(threads, threadFactory) : new NioEventLoopGroup(threads, threadFactory);
    }

}
