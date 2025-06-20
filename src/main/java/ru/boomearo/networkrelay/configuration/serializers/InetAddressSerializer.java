package ru.boomearo.networkrelay.configuration.serializers;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import ru.boomearo.networkrelay.configuration.ConfigurateSerializer;

import java.lang.reflect.Type;
import java.net.InetAddress;

public class InetAddressSerializer extends ConfigurateSerializer<InetAddress> {

    public InetAddressSerializer() {
        super(InetAddress.class);
    }

    @Override
    public InetAddress deserialize(@NotNull Type type, ConfigurationNode node) throws SerializationException {
        try {
            return InetAddress.getByName(node.getString(""));
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void serialize(@NotNull Type type, InetAddress obj, @NotNull ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }
        node.set(String.class, obj.getHostAddress());
    }
}
