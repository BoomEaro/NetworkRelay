package ru.boomearo.networkrelay.app;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ResourceLeakDetector;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.io.IoBuilder;
import ru.boomearo.networkrelay.configuration.ConfigurationProvider;
import ru.boomearo.networkrelay.configuration.config.Configuration;
import ru.boomearo.networkrelay.configuration.config.ServerConfiguration;
import ru.boomearo.networkrelay.logger.Log4JLogHandler;
import ru.boomearo.networkrelay.upstream.TcpRelayUpstreamHandler;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public class NetworkRelayApp {

    private final Logger logger;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private ConfigurationProvider<Configuration> configurationProvider;

    public NetworkRelayApp() {
        org.apache.logging.log4j.Logger redirect = LogManager.getRootLogger();
        System.setOut(IoBuilder.forLogger(redirect).setLevel(org.apache.logging.log4j.Level.INFO).buildPrintStream());
        System.setErr(IoBuilder.forLogger(redirect).setLevel(org.apache.logging.log4j.Level.ERROR).buildPrintStream());

        Logger root = Logger.getLogger("");
        root.setUseParentHandlers(false);

        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }

        root.setLevel(java.util.logging.Level.ALL);
        root.addHandler(new Log4JLogHandler());

        this.logger = Logger.getLogger("NetworkRelay");
    }

    public void load() {
        System.setProperty("io.netty.selectorAutoRebuildThreshold", "0");
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

        boolean epoll = false;
        if (Epoll.isAvailable()) {
            epoll = true;
            this.logger.log(Level.INFO, "Epoll is working, utilising it!");
        } else {
            this.logger.log(Level.WARNING, "Epoll is not working, falling back to NIO");
        }

        ChannelFactory<? extends Channel> channelFactory = epoll ? EpollSocketChannel::new : NioSocketChannel::new;
        ChannelFactory<? extends ServerChannel> serverChannelFactory = epoll ? EpollServerSocketChannel::new : NioServerSocketChannel::new;

        this.bossGroup = newEventLoopGroup(epoll, 0, new ThreadFactoryBuilder().setNameFormat("Netty Boss IO Thread #%1$d").build());
        this.workerGroup = newEventLoopGroup(epoll, 0, new ThreadFactoryBuilder().setNameFormat("Netty Worker IO Thread #%1$d").build());

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
                    .group(this.bossGroup, this.workerGroup)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .channelFactory(serverChannelFactory)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            SimpleChannelInitializer.INSTANCE.initChannel(ch);

                            ch.pipeline().addLast("fclh", new FlushConsolidationHandler(20));
                            ch.pipeline().addLast("timeout", new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS));
                            ch.pipeline().addLast("upstream", new TcpRelayUpstreamHandler(logger, channelFactory, destinationAddress, timeout));
                        }
                    })
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                    .childOption(ChannelOption.AUTO_READ, false)
                    .localAddress(sourceAddress)
                    .bind()
                    .addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            this.logger.log(Level.SEVERE, "TCP: Could not bind to host " + sourceAddress, future.channel());
                            return;
                        }

                        this.logger.log(Level.INFO, "TCP: Listening on " + sourceAddress + "...");
                    });
        }

        new NetworkRelayConsole(this).start();
    }

    public void unload() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        while (true) {
            try {
                this.bossGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                this.workerGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
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
