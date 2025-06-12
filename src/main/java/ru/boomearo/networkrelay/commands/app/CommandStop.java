package ru.boomearo.networkrelay.commands.app;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import ru.boomearo.networkrelay.app.NetworkRelayApp;
import ru.boomearo.networkrelay.commands.CommandNodeApp;
import ru.boomearo.networkrelay.commands.ConsoleSender;

import java.util.List;

public class CommandStop extends CommandNodeApp {

    private final NetworkRelayApp networkRelayApp;

    public CommandStop(@NonNull CommandNodeApp root, @NonNull NetworkRelayApp networkRelayAp) {
        super(root, "stop");
        this.networkRelayApp = networkRelayAp;
    }

    @Nullable
    @Override
    public List<String> getDescription(@NonNull ConsoleSender consoleSender) {
        return List.of("stop - stop the application");
    }

    @Override
    public void onExecute(@NonNull ConsoleSender sender, @NonNull String[] args) {
        if (args.length != 0) {
            sendCurrentHelp(sender);
            return;
        }

        this.networkRelayApp.unload();
    }

}
