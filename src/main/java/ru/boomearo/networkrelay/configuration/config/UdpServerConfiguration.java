package ru.boomearo.networkrelay.configuration.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.InetSocketAddress;

@ConfigSerializable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UdpServerConfiguration {

    private InetSocketAddress source;
    private InetSocketAddress destination;
    private int timeout;

}
