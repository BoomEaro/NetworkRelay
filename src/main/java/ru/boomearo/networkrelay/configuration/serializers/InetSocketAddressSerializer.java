package ru.boomearo.networkrelay.configuration.serializers;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import ru.boomearo.networkrelay.configuration.ConfigurateSerializer;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;

public class InetSocketAddressSerializer extends ConfigurateSerializer<InetSocketAddress> {

    public InetSocketAddressSerializer() {
        super(InetSocketAddress.class);
    }

    @Override
    public InetSocketAddress deserialize(@NotNull Type type, ConfigurationNode node) throws SerializationException {
        String value = node.getString("");
        InetSocketAddress inetSocketAddress;
        try {
            String[] args = value.split(":");
            String hostName = args[0];
            int port = Integer.parseInt(args[1]);

            inetSocketAddress = new InetSocketAddress(hostName, port);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
        return inetSocketAddress;
    }

    @Override
    public void serialize(@NotNull Type type, InetSocketAddress obj, @NotNull ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }
        node.set(String.class, obj.getHostString() + ":" + obj.getPort());
    }
}
