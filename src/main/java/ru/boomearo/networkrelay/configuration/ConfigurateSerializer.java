package ru.boomearo.networkrelay.configuration;

import lombok.Getter;
import lombok.NonNull;
import org.spongepowered.configurate.serialize.TypeSerializer;

@Getter
public abstract class ConfigurateSerializer<T> implements TypeSerializer<T> {

    private final Class<T> clazz;

    public ConfigurateSerializer(@NonNull Class<T> clazz) {
        this.clazz = clazz;
    }
}
