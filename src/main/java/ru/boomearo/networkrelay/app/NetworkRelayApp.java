package ru.boomearo.networkrelay.app;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.ResourceLeakDetector;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import ru.boomearo.networkrelay.configuration.ConfigurationProvider;
import ru.boomearo.networkrelay.configuration.config.Configuration;
import ru.boomearo.networkrelay.configuration.config.TcpServerConfiguration;
import ru.boomearo.networkrelay.configuration.config.UdpServerConfiguration;
import ru.boomearo.networkrelay.netty.SimpleChannelInitializer;
import ru.boomearo.networkrelay.netty.StatisticsUpstreamHandler;
import ru.boomearo.networkrelay.netty.TcpRelayUpstreamHandler;
import ru.boomearo.networkrelay.netty.UdpRelayUpstreamHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

@Getter
@Log4j2
public class NetworkRelayApp {

    private final List<Channel> channels = new ArrayList<>();

    private EventLoopGroup tcpBossGroup;
    private EventLoopGroup tcpWorkerGroup;
    private EventLoopGroup udpWorkerGroup;

    private ConfigurationProvider<Configuration> configurationProvider;

    public NetworkRelayApp() {
        Logger redirect = LogManager.getRootLogger();
        System.setOut(IoBuilder.forLogger(redirect).setLevel(Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(redirect).setLevel(Level.ERROR).buildPrintStream());

        java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        root.setUseParentHandlers(false);

        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }

        root.setLevel(java.util.logging.Level.ALL);
        root.addHandler(new Log4JLogHandler());
    }

    public void load() {
        System.setProperty("io.netty.selectorAutoRebuildThreshold", "0");
        if (System.getProperty("io.netty.leakDetectionLevel") == null && System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }

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

        for (TcpServerConfiguration tcpServerConfiguration : this.configurationProvider.get().getTcpServers()) {
            InetSocketAddress sourceAddress = tcpServerConfiguration.getSource();
            InetSocketAddress destinationAddress = tcpServerConfiguration.getDestination();
            int timeout = tcpServerConfiguration.getTimeout();
            boolean isProxyProtocol = tcpServerConfiguration.isProxyProtocol();
            Set<InetAddress> whitelist = tcpServerConfiguration.getWhitelist();

            new ServerBootstrap()
                    .group(this.tcpBossGroup, this.tcpWorkerGroup)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .channelFactory(tcpServerChannelFactory)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            SocketAddress remoteAddress = ch.remoteAddress();
                            if (remoteAddress instanceof InetSocketAddress inetSocketAddress) {
                                if (!whitelist.isEmpty()) {
                                    if (!whitelist.contains(inetSocketAddress.getAddress())) {
                                        log.log(Level.WARN, "Closed blacklisted address " + remoteAddress);
                                        ch.close();
                                        return;
                                    }
                                }
                            }

                            SimpleChannelInitializer.INSTANCE.initChannel(ch);

                            ch.pipeline().addLast("stats", new StatisticsUpstreamHandler());
                            ch.pipeline().addLast("fch", new FlushConsolidationHandler(20));
                            ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                            ch.pipeline().addLast("write-timeout", new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                            ch.pipeline().addLast("upstream", new TcpRelayUpstreamHandler(
                                    tcpChannelFactory,
                                    destinationAddress,
                                    timeout,
                                    isProxyProtocol
                            ));
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

                        this.channels.add(future.channel());

                        log.log(Level.INFO, "TCP: Listening on " + sourceAddress + "...");
                    });
        }

        for (UdpServerConfiguration udpServerConfiguration : this.configurationProvider.get().getUdpServers()) {
            InetSocketAddress sourceAddress = udpServerConfiguration.getSource();
            InetSocketAddress destinationAddress = udpServerConfiguration.getDestination();
            int timeout = udpServerConfiguration.getTimeout();
            Set<InetAddress> whitelist = udpServerConfiguration.getWhitelist();

            new Bootstrap()
                    .channelFactory(udpChannelFactory)
                    .group(this.udpWorkerGroup)
                    .handler(new ChannelInitializer<>() {
                        @Override
                        public void initChannel(Channel ch) throws Exception {
                            SimpleChannelInitializer.INSTANCE.initChannel(ch);

                            ch.pipeline().addLast("stats", new StatisticsUpstreamHandler());
                            ch.pipeline().addLast("relay", new UdpRelayUpstreamHandler(
                                    destinationAddress,
                                    udpChannelFactory,
                                    timeout,
                                    whitelist
                            ));
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

                        this.channels.add(future.channel());

                        log.log(Level.INFO, "UDP: Listening on " + sourceAddress + "...");
                    });
        }

        new NetworkRelayConsole(this).start();
    }

    public void unload() {
        for (Channel channel : this.channels) {
            try {
                channel.close().syncUninterruptibly();
            } catch (Throwable t) {
                log.log(Level.ERROR, "Failed to close channel", t);
            }
        }
        this.channels.clear();

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
        return new MultiThreadIoEventLoopGroup(threads, threadFactory, epoll ? EpollIoHandler.newFactory() : NioIoHandler.newFactory());
    }

}
