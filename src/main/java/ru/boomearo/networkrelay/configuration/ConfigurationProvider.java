package ru.boomearo.networkrelay.configuration;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import ru.boomearo.networkrelay.configuration.serializers.SocketAddressSerializer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

@RequiredArgsConstructor
@AllArgsConstructor
public class ConfigurationProvider<T> {

    private final Class<T> clazz;
    private final Path file;
    private Collection<ConfigurateSerializer<?>> serializers = Arrays.asList(
            new SocketAddressSerializer()
    );

    private T loadedConfiguration = null;

    public T get() {
        if (this.loadedConfiguration == null) {
            doReload();
        }

        return this.loadedConfiguration;
    }

    public void reload() {
        doReload();
    }

    @SneakyThrows
    public void save(Class<T> clazz, T data) {
        YamlConfigurationLoader loader = createLoader();

        ConfigurationNode node = loader.createNode().set(clazz, data);

        loader.save(node);
    }

    @SneakyThrows
    public void save() {
        YamlConfigurationLoader loader = createLoader();

        T loaded = get();

        ConfigurationNode node = loader.createNode().set(loaded.getClass(), loaded);

        loader.save(node);
    }

    private YamlConfigurationLoader createLoader() {
        return YamlConfigurationLoader.builder()
                .path(this.file)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .defaultOptions(configurationOptions -> configurationOptions.serializers(builder -> {
                    for (ConfigurateSerializer<?> serializer : this.serializers) {
                        registerSerializer(
                                builder,
                                serializer.getClazz(),
                                serializer
                        );
                    }
                }))
                .build();
    }

    @SneakyThrows
    private void doReload() {
        YamlConfigurationLoader loader = createLoader();

        ConfigurationNode node = loader.load();
        this.loadedConfiguration = node.get(this.clazz);
        loader.save(node);
    }

    private <K> void registerSerializer(
            TypeSerializerCollection.Builder builder,
            Class<K> clazz,
            TypeSerializer<?> serializer
    ) {
        builder.register(clazz, (TypeSerializer<K>) serializer);
    }

}
