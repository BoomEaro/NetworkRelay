package ru.boomearo.networkrelay.configuration.serializers;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import ru.boomearo.networkrelay.configuration.ConfigurateSerializer;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class InetSocketAddressSerializer extends ConfigurateSerializer<InetSocketAddress> {

    public InetSocketAddressSerializer() {
        super(InetSocketAddress.class);
    }

    @Override
    public InetSocketAddress deserialize(@NotNull Type type, ConfigurationNode node) throws SerializationException {
        InetSocketAddress inetSocketAddress;
        try {
            InetAddress address = node.node("address").get(InetAddress.class, InetAddress.getByName("127.0.0.1"));
            int port = node.node("port").getInt(1);
            inetSocketAddress = new InetSocketAddress(address, port);
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
        node.node("address").set(InetAddress.class, obj.getAddress());
        node.node("port").set(Integer.class, obj.getPort());
    }
}
