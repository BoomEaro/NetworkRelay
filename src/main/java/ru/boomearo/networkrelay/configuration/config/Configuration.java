package ru.boomearo.networkrelay.configuration.config;

import lombok.Data;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@ConfigSerializable
@Data
public class Configuration {

    private List<TcpServerConfiguration> tcpServers = Arrays.asList(
            new TcpServerConfiguration(
                    new InetSocketAddress("127.0.0.1", 25577),
                    new InetSocketAddress("127.0.0.1", 25576),
                    30000,
                    false,
                    Set.of()
            ),
            new TcpServerConfiguration(
                    new InetSocketAddress("127.0.0.1", 25573),
                    new InetSocketAddress("127.0.0.1", 25572),
                    30000,
                    false,
                    Set.of()
            )
    );

    private List<UdpServerConfiguration> udpServers = Arrays.asList(
            new UdpServerConfiguration(
                    new InetSocketAddress("127.0.0.1", 25577),
                    new InetSocketAddress("127.0.0.1", 25576),
                    30000,
                    Set.of()
            ),
            new UdpServerConfiguration(
                    new InetSocketAddress("127.0.0.1", 25573),
                    new InetSocketAddress("127.0.0.1", 25572),
                    30000,
                    Set.of()
            )
    );
}
