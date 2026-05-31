package eu.endermite.commandwhitelist.common;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        if (buffer.startsWith("/"))
            buffer = buffer.substring(1);
        List<String> suggestionsList = new ArrayList<>(suggestions);
        for (String s : blockedSubCommands) {
            String scommand = cutLastArgument(s);
            if (buffer.startsWith(scommand)) {
                String slast = getLastArgument(s);
                while (suggestionsList.contains(slast))
                    suggestionsList.remove(slast);
            }
        }
        return suggestionsList;
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
     * @param cmd The command
     * @return Command label
     */
    public static String getCommandLabel(String cmd) {
        int space = cmd.indexOf(' ');
        String label = space == -1 ? cmd : cmd.substring(0, space);
        if (label.startsWith("/"))
            label = label.substring(1);
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
        trimmed = trimmed.toLowerCase();
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
