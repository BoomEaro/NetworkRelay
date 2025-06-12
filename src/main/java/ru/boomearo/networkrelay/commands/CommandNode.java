package ru.boomearo.networkrelay.commands;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
public abstract class CommandNode<T, D> {

    protected final CommandNode<T, D> rootNode;
    protected final String name;
    protected final List<String> aliases;

    protected final Map<String, CommandNode<T, D>> nodes = new Object2ObjectLinkedOpenHashMap<>();
    protected final Map<String, CommandNode<T, D>> withAliasesNodes = new Object2ObjectLinkedOpenHashMap<>();

    public CommandNode(@Nullable CommandNode<T, D> rootNode, @NonNull String name) {
        this(rootNode, name, Collections.emptyList());
    }

    public CommandNode(@Nullable CommandNode<T, D> rootNode, String name, @NonNull List<String> aliases) {
        this.rootNode = rootNode;
        this.name = name;
        this.aliases = aliases;
    }

    public void execute(@NonNull T sender, @NonNull String[] args) {
        if (!hasPermission(sender)) {
            onPermissionFailedExecute(sender, args);
            return;
        }

        if (args.length == 0) {
            onExecuteSafe(sender, args);
            return;
        }
        CommandNode<T, D> nextNode = this.withAliasesNodes.get(args[0].toLowerCase());
        if (nextNode == null) {
            onExecuteSafe(sender, args);
            return;
        }

        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        nextNode.execute(sender, newArgs);
    }

    public void onExecuteSafe(@NonNull T sender, @NonNull String[] args) {
        try {
            onExecute(sender, args);
        } catch (Exception e) {
            onExecuteException(sender, args, e);
        }
    }

    @NonNull
    public Collection<String> suggest(@NonNull T sender, @NonNull String[] args) {
        if (!hasPermission(sender)) {
            return onPermissionFailedSuggest(sender, args);
        }
        if (args.length == 0) {
            return onSuggestSafe(sender, args);
        }

        CommandNode<T, D> nextNode = this.withAliasesNodes.get(args[0].toLowerCase());
        if (nextNode == null) {
            return onSuggestSafe(sender, args);
        }

        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        return nextNode.suggest(sender, newArgs);
    }

    @NonNull
    public Collection<String> onSuggestSafe(@NonNull T sender, @NonNull String[] args) {
        try {
            return onSuggest(sender, args);
        } catch (Exception e) {
            return onSuggestException(sender, args, e);
        }
    }

    public void addNode(@NonNull CommandNode<T, D> node) {
        this.nodes.put(node.getName().toLowerCase(), node);
        this.withAliasesNodes.put(node.getName().toLowerCase(), node);
        for (String alias : node.getAliases()) {
            this.withAliasesNodes.put(alias.toLowerCase(), node);
        }
    }

    @NonNull
    public List<D> getDescriptionList(@NonNull T sender) {
        List<D> tmp = new ArrayList<>();
        if (hasPermission(sender)) {
            List<D> descs = getDescription(sender);
            if (descs != null) {
                tmp.addAll(descs);
            }
        }

        for (CommandNode<T, D> node : this.nodes.values()) {
            tmp.addAll(node.getDescriptionList(sender));
        }
        return tmp;
    }

    @NonNull
    public Collection<D> getDescriptionListFromRoot(@NonNull T sender) {
        if (this.rootNode == null) {
            return getDescriptionList(sender);
        }

        return this.rootNode.getDescriptionList(sender);
    }

    public boolean hasPermission(@NonNull T sender) {
        return true;
    }

    @NonNull
    public Collection<String> onSuggest(@NonNull T sender, @NonNull String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        Set<String> matches = new ObjectOpenHashSet<>();
        String search = args[0].toLowerCase();
        for (Map.Entry<String, CommandNode<T, D>> entry : this.withAliasesNodes.entrySet()) {
            if (entry.getValue().hasPermission(sender)) {
                if (entry.getKey().toLowerCase().startsWith(search)) {
                    matches.add(entry.getKey());
                }
            }
        }
        return matches;
    }

    @Nullable
    public abstract List<D> getDescription(@NonNull T sender);

    public abstract void onExecute(@NonNull T sender, @NonNull String[] args);

    public abstract void onExecuteException(@NonNull T sender, @NonNull String[] args, @NonNull Exception e);

    @NonNull
    public abstract Collection<String> onSuggestException(@NonNull T sender, @NonNull String[] args, @NonNull Exception e);

    public abstract void onPermissionFailedExecute(@NonNull T sender, @NonNull String[] args);

    @NonNull
    public abstract Collection<String> onPermissionFailedSuggest(@NonNull T sender, @NonNull String[] args);

}
