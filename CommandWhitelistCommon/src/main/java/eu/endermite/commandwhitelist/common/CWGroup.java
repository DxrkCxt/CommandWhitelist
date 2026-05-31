package eu.endermite.commandwhitelist.common;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CWGroup {

    private final String id, permission, commandDeniedMessage;
    private final HashSet<String> commands = new HashSet<>();
    private final HashSet<String> subCommands = new HashSet<>();
    private List<String[]> subCommandTokens = new ArrayList<>();

    public CWGroup(String id, Collection<String> commands, Collection<String> subCommands, String custom_command_denied_message) {
        this.id = id;
        this.permission = "commandwhitelist.group." + id;
        for (String command : commands)
            this.commands.add(command.toLowerCase());
        this.commandDeniedMessage = custom_command_denied_message;
        this.subCommands.addAll(subCommands);
        recomputeSubCommandTokens();
    }

    public String getId() {
        return id;
    }

    public String getPermission() {
        return permission;
    }

    public HashSet<String> getCommands() {
        return commands;
    }

    public @Nullable String getCommandDeniedMessage() {
        return commandDeniedMessage;
    }

    public void addCommand(String command) {
        commands.add(command.toLowerCase());
    }

    public void removeCommand(String command) {
        commands.remove(command.toLowerCase());
    }

    public HashSet<String> getSubCommands() {
        return subCommands;
    }

    /**
     * Pre-tokenized, normalized blocked subcommands for allocation-free matching
     * on the command path. Rebuilt whenever the subcommand set changes.
     *
     * @see CommandUtil#tokenizeCommand(String)
     */
    public List<String[]> getSubCommandTokens() {
        return subCommandTokens;
    }

    public void addSubCommand(String subCommand) {
        subCommands.add(subCommand);
        recomputeSubCommandTokens();
    }

    public void removeSubCommand(String subCommand) {
        subCommands.remove(subCommand);
        recomputeSubCommandTokens();
    }

    private void recomputeSubCommandTokens() {
        List<String[]> tokens = new ArrayList<>(subCommands.size());
        for (String subCommand : subCommands) {
            String[] subTokens = CommandUtil.tokenizeCommand(subCommand);
            if (subTokens.length > 0)
                tokens.add(subTokens);
        }
        subCommandTokens = tokens;
    }

    public HashMap<String, Object> serialize() {
        HashMap<String, Object> serializedGroup = new LinkedHashMap<>();
        List<String> commands = new ArrayList<>(this.commands);
        List<String> subCommands = new ArrayList<>(this.subCommands);
        serializedGroup.put("commands", commands);
        serializedGroup.put("subcommands", subCommands);
        return serializedGroup;
    }
}
