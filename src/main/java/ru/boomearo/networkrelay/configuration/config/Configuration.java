package ru.boomearo.networkrelay.configuration.config;

import lombok.Data;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

@ConfigSerializable
@Data
public class Configuration {

    private List<ServerConfiguration> tcpServers = Arrays.asList(
            new ServerConfiguration(new InetSocketAddress("127.0.0.1", 25577), new InetSocketAddress("127.0.0.1", 25576), 30000),
            new ServerConfiguration(new InetSocketAddress("127.0.0.1", 25573), new InetSocketAddress("127.0.0.1", 25572), 30000)
    );

    private List<ServerConfiguration> udpServers = Arrays.asList(
            new ServerConfiguration(new InetSocketAddress("127.0.0.1", 25577), new InetSocketAddress("127.0.0.1", 25576), 30000),
            new ServerConfiguration(new InetSocketAddress("127.0.0.1", 25573), new InetSocketAddress("127.0.0.1", 25572), 30000)
    );
}
