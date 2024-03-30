package ru.boomearo.networkrelay.commands;

import ru.boomearo.networkrelay.app.NetworkRelayApp;

import java.util.List;

public class CommandStop extends CommandNodeApp {

    private final NetworkRelayApp networkRelayApp;

    public CommandStop(CommandNodeApp root, NetworkRelayApp networkRelayAp) {
        super(root, "stop");
        this.networkRelayApp = networkRelayAp;
    }

    @Override
    public List<String> getDescription(ConsoleSender consoleSender) {
        return List.of("stop - stop application");
    }

    @Override
    public void onExecute(ConsoleSender sender, String[] args) {
        if (args.length != 0) {
            sendCurrentHelp(sender);
            return;
        }

        this.networkRelayApp.unload();
    }

}
