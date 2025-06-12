package ru.boomearo.networkrelay.commands.app;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import ru.boomearo.networkrelay.commands.CommandNodeApp;
import ru.boomearo.networkrelay.commands.ConsoleSender;

import java.util.List;

public class CommandMain extends CommandNodeApp {

    public CommandMain() {
        super(null, "root");
    }

    @Nullable
    @Override
    public List<String> getDescription(@NonNull ConsoleSender commandSender) {
        return null;
    }

    @Override
    public void onExecute(@NonNull ConsoleSender sender, @NonNull String[] args) {
        sendHelp(sender);
    }
}
