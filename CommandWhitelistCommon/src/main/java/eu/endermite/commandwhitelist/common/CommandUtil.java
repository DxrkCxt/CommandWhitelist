package eu.endermite.commandwhitelist.common;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class CommandUtil {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Filters blocked command suggestions from provided collection of strings
     *
     * @param buffer             Command buffer
     * @param suggestions        Full suggestions list
     * @param blockedSubCommands Subcommands to filter out
     * @return Filtered list of suggestions
     */
    public static List<String> filterSuggestions(String buffer, Collection<String> suggestions, Collection<String> blockedSubCommands) {
        if (suggestions.isEmpty() || blockedSubCommands.isEmpty())
            return suggestions instanceof List ? (List<String>) suggestions : new ArrayList<>(suggestions);
        String normalizedBuffer = normalizeForPrefixMatch(buffer);
        List<String> suggestionsList = new ArrayList<>(suggestions);
        for (String s : blockedSubCommands) {
            String normalizedSubCommand = normalizeForPrefixMatch(s);
            String scommand = cutLastArgument(normalizedSubCommand);
            if (normalizedBuffer.startsWith(scommand)) {
                String slast = getLastArgument(normalizedSubCommand);
                suggestionsList.removeIf(suggestion -> suggestion.equalsIgnoreCase(slast));
            }
        }
        return suggestionsList;
    }

    /**
     * Normalizes a command/subcommand string for prefix matching in {@link #filterSuggestions}
     * the same way {@link #tokenizeCommand} normalizes for exact matching: strips a leading
     * slash, lowercases, collapses repeated whitespace, and strips a leading "namespace:" from
     * the label. Unlike {@code tokenizeCommand}, this preserves a meaningful trailing space
     * (signaling "the player has moved on to the next argument") instead of trimming it away.
     *
     * @param command command string (with or without leading slash)
     * @return normalized string, safe to use with {@link String#startsWith(String)}
     */
    private static String normalizeForPrefixMatch(String command) {
        String result = command.startsWith("/") ? command.substring(1) : command;
        boolean trailingSpace = result.endsWith(" ");
        result = WHITESPACE.matcher(result.trim()).replaceAll(" ").toLowerCase(Locale.ROOT);
        if (trailingSpace && !result.isEmpty())
            result = result + " ";
        int space = result.indexOf(' ');
        String label = space == -1 ? result : result.substring(0, space);
        int colon = label.indexOf(':');
        if (colon >= 0)
            result = label.substring(colon + 1) + (space == -1 ? "" : result.substring(space));
        return result;
    }

    /**
     * @param cmd The command
     * @return Last argument of the command
     */
    public static String getLastArgument(String cmd) {
        String[] parts = cmd.split(" ");
        if (parts.length == 0) return "";
        return parts[parts.length - 1];
    }

    /**
     * @param cmd The command
     * @return Command without the last argument.
     */
    public static String cutLastArgument(String cmd) {
        String[] cmdSplit = cmd.split(" ");
        StringBuilder cmdBuilder = new StringBuilder();
        for (int i = 0; i <= cmdSplit.length - 2; i++)
            cmdBuilder.append(cmdSplit[i]).append(" ");
        return cmdBuilder.toString();
    }

    /**
     * Strips a leading slash and namespace (e.g. "essentials:") the same way
     * {@link #tokenizeCommand(String)} does, so this and the block-path tokenizer
     * agree on what "the command label" is for the same input.
     *
     * @param cmd The command
     * @return Command label
     */
    public static String getCommandLabel(String cmd) {
        int space = cmd.indexOf(' ');
        String label = space == -1 ? cmd : cmd.substring(0, space);
        if (label.startsWith("/"))
            label = label.substring(1);
        int colon = label.indexOf(':');
        if (colon >= 0)
            label = label.substring(colon + 1);
        return label;
    }

    /**
     * Splits a command into lowercase tokens for robust subcommand matching.
     * Drops a leading slash, collapses repeated whitespace, and strips a leading
     * "namespace:" from the command label so a blocked subcommand cannot be
     * bypassed with letter case, extra spaces, or a namespaced command
     * (e.g. "essentials:warp").
     *
     * @param command command string (with or without leading slash)
     * @return normalized tokens, empty array if the command is blank
     */
    public static String[] tokenizeCommand(String command) {
        String trimmed = command.trim();
        if (trimmed.startsWith("/"))
            trimmed = trimmed.substring(1);
        trimmed = trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty())
            return new String[0];
        String[] tokens = WHITESPACE.split(trimmed);
        int colon = tokens[0].indexOf(':');
        if (colon >= 0)
            tokens[0] = tokens[0].substring(colon + 1);
        return tokens;
    }

    /**
     * Token-based match of an executed command against a configured blocked
     * subcommand. The message is blocked when its leading tokens equal the
     * subcommand's tokens (so "warp vip" matches "/warp vip", "/Warp vip",
     * "/warp  vip" and "/essentials:warp vip", but not "/warp vipfoo").
     *
     * @param messageTokens tokens of the executed command, from {@link #tokenizeCommand(String)}
     * @param subCommand    a configured blocked subcommand
     * @return true if the subcommand matches
     */
    public static boolean subCommandMatches(String[] messageTokens, String subCommand) {
        return tokensMatch(messageTokens, tokenizeCommand(subCommand));
    }

    /**
     * Token-prefix match against an already tokenized subcommand. Lets callers
     * reuse precomputed subcommand tokens instead of re-tokenizing on every call.
     *
     * @param messageTokens tokens of the executed command, from {@link #tokenizeCommand(String)}
     * @param subTokens     pre-tokenized blocked subcommand, see {@link CWGroup#getSubCommandTokens()}
     * @return true if the message's leading tokens equal the subcommand's tokens
     */
    public static boolean tokensMatch(String[] messageTokens, String[] subTokens) {
        if (subTokens.length == 0 || messageTokens.length < subTokens.length)
            return false;
        for (int i = 0; i < subTokens.length; i++) {
            if (!messageTokens[i].equals(subTokens[i]))
                return false;
        }
        return true;
    }

    /**
     * Allocation-free check whether a user may run the given command label. Shared by all
     * three platforms (Bukkit/Velocity/Waterfall) so the whitelist-check logic, and its
     * normalization requirements, only has to be correct in one place instead of being
     * copy-pasted per platform.
     *
     * @param groupList     the currently active groups, see {@link ConfigCache#getGroupList()}
     * @param label         normalized (lowercase, namespace-stripped) command label,
     *                      see {@link #getCommandLabel(String)}
     * @param hasPermission tests whether the user holds a given permission node
     * @return true if any group available to the user whitelists the command
     */
    public static boolean isCommandAllowed(Map<String, CWGroup> groupList, String label, Predicate<String> hasPermission) {
        for (Map.Entry<String, CWGroup> group : groupList.entrySet()) {
            if (group.getKey().equalsIgnoreCase("default") || hasPermission.test(group.getValue().getPermission())) {
                if (group.getValue().getCommands().contains(label))
                    return true;
            }
        }
        return false;
    }

    /**
     * Allocation-free check whether the given command starts with a subcommand blocked for
     * the user. Shared by all three platforms, see {@link #isCommandAllowed}.
     *
     * @param groupList     the currently active groups, see {@link ConfigCache#getGroupList()}
     * @param command       command message (with or without leading slash), see {@link #tokenizeCommand(String)}
     * @param hasPermission tests whether the user holds a given permission node
     * @return true if a blocked subcommand matches
     */
    public static boolean isSubCommandBlocked(Map<String, CWGroup> groupList, String command, Predicate<String> hasPermission) {
        String[] messageTokens = tokenizeCommand(command);
        if (messageTokens.length == 0) return false;
        for (Map.Entry<String, CWGroup> group : groupList.entrySet()) {
            if (group.getKey().equalsIgnoreCase("default") || hasPermission.test(group.getValue().getPermission())) {
                for (String[] subTokens : group.getValue().getSubCommandTokens()) {
                    if (tokensMatch(messageTokens, subTokens))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Dumps command list to a file
     *
     * @param serverCommands Commands to dump
     * @return True on successful file save
     */
    public static boolean dumpAllBukkitCommands(ArrayList<String> serverCommands, File file) {
        try {
            File parent = new File(file.getParent());
            if (!parent.exists())
                parent.mkdir();
            if (!file.exists())
                file.createNewFile();
        } catch (IOException e) {
            return false;
        }


        try {
            ConfigFile dumpFile = ConfigFile.loadConfig(file);
            dumpFile.set("commands", serverCommands);
            dumpFile.save();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
