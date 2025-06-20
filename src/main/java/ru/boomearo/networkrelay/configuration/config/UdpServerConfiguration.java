package ru.boomearo.networkrelay.configuration.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

@ConfigSerializable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UdpServerConfiguration {

    private InetSocketAddress source;
    private InetSocketAddress destination;
    private int timeout;
    private Set<InetAddress> whitelist;
}
