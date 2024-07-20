package ru.boomearo.networkrelay.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class CommandNodeApp extends CommandNode<ConsoleSender, String> {

    public static final String SEP = "--------------------------------------";

    public CommandNodeApp(CommandNodeApp root, String name, List<String> aliases) {
        super(root, name, aliases);
    }

    public CommandNodeApp(CommandNodeApp root, String name) {
        this(root, name, Collections.emptyList());
    }

    @Override
    public void onExecuteException(ConsoleSender sender, String[] args, Exception e) {
        sender.sendMessage("Failed to execute command", e);
    }

    @Override
    public Collection<String> onSuggestException(ConsoleSender sender, String[] args, Exception e) {
        sender.sendMessage("Failed to suggest command", e);
        return Collections.emptyList();
    }

    @Override
    public void onPermissionFailedExecute(ConsoleSender sender, String[] args) {
    }

    @Override
    public Collection<String> onPermissionFailedSuggest(ConsoleSender sender, String[] args) {
        return Collections.emptyList();
    }

    public void sendCurrentHelp(ConsoleSender sender) {
        List<String> description = getDescription(sender);
        if (description != null) {
            sender.sendMessage(SEP);
            for (String text : description) {
                sender.sendMessage(text);
            }
            sender.sendMessage(SEP);
        }
    }

    public void sendHelp(ConsoleSender sender) {
        sender.sendMessage(SEP);
        for (String text : getDescriptionListFromRoot(sender)) {
            sender.sendMessage(text);
        }
        sender.sendMessage(SEP);
    }
}
