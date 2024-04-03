package ru.boomearo.networkrelay.commands.app;

import ru.boomearo.networkrelay.commands.CommandNodeApp;
import ru.boomearo.networkrelay.commands.ConsoleSender;

import java.util.List;

public class CommandMain extends CommandNodeApp {

    public CommandMain() {
        super(null, "root");
    }

    @Override
    public List<String> getDescription(ConsoleSender commandSender) {
        return null;
    }

    @Override
    public void onExecute(ConsoleSender sender, String[] args) {
        sendHelp(sender);
    }
}
