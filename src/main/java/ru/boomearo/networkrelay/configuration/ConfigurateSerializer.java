package ru.boomearo.networkrelay.configuration;

import lombok.Getter;
import org.spongepowered.configurate.serialize.TypeSerializer;

@Getter
public abstract class ConfigurateSerializer<T> implements TypeSerializer<T> {

    private final Class<T> clazz;

    public ConfigurateSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }
}
