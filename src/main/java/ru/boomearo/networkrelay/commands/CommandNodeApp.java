package ru.boomearo.networkrelay.commands;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class CommandNodeApp extends CommandNode<ConsoleSender, String> {

    public static final String SEPARATOR = "--------------------------------------";

    public CommandNodeApp(@Nullable CommandNodeApp root, @NonNull String name, @NonNull List<String> aliases) {
        super(root, name, aliases);
    }

    public CommandNodeApp(@Nullable CommandNodeApp root, @NonNull String name) {
        this(root, name, Collections.emptyList());
    }

    @Override
    public void onExecuteException(@NonNull ConsoleSender sender, @NonNull String[] args, @NonNull Exception e) {
        sender.sendMessage("Failed to execute command", e);
    }

    @NonNull
    @Override
    public Collection<String> onSuggestException(@NonNull ConsoleSender sender, @NonNull String[] args, @NonNull Exception e) {
        sender.sendMessage("Failed to suggest command", e);
        return Collections.emptyList();
    }

    @Override
    public void onPermissionFailedExecute(@NonNull ConsoleSender sender, @NonNull String[] args) {
    }

    @NonNull
    @Override
    public Collection<String> onPermissionFailedSuggest(@NonNull ConsoleSender sender, @NonNull String[] args) {
        return Collections.emptyList();
    }

    public void sendCurrentHelp(@NonNull ConsoleSender sender) {
        List<String> description = getDescription(sender);
        if (description != null) {
            sender.sendMessage(SEPARATOR);
            for (String text : description) {
                sender.sendMessage(text);
            }
            sender.sendMessage(SEPARATOR);
        }
    }

    public void sendHelp(@NonNull ConsoleSender sender) {
        sender.sendMessage(SEPARATOR);
        for (String text : getDescriptionListFromRoot(sender)) {
            sender.sendMessage(text);
        }
        sender.sendMessage(SEPARATOR);
    }
}
