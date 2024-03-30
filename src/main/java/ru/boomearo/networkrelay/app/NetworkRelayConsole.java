package ru.boomearo.networkrelay.app;

import net.minecrell.terminalconsole.SimpleTerminalConsole;
import ru.boomearo.networkrelay.commands.CommandMain;
import ru.boomearo.networkrelay.commands.CommandNodeApp;
import ru.boomearo.networkrelay.commands.CommandStop;
import ru.boomearo.networkrelay.commands.ConsoleSender;

public class NetworkRelayConsole extends SimpleTerminalConsole {

    private final NetworkRelayApp networkRelayApp;
    private final CommandNodeApp commandNode;

    public NetworkRelayConsole(NetworkRelayApp networkRelayApp) {
        this.networkRelayApp = networkRelayApp;

        CommandMain root = new CommandMain();
        root.addNode(new CommandStop(root, networkRelayApp));

        this.commandNode = root;
    }

    @Override
    protected boolean isRunning() {
        return true;
    }

    @Override
    protected void runCommand(String s) {
        ConsoleSender consoleSender = new ConsoleSender(this.networkRelayApp.getLogger());
        String[] args = s.split(" ");
        this.commandNode.execute(consoleSender, args);
    }

    @Override
    protected void shutdown() {
        this.networkRelayApp.unload();
    }
}