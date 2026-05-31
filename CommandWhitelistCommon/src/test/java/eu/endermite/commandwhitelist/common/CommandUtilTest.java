package eu.endermite.commandwhitelist.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandUtilTest {

    // --- tokenizeCommand: normalization (slash, case, spaces, namespace) ---

    @Test
    void tokenizeBasic() {
        assertArrayEquals(new String[]{"warp", "vip"}, CommandUtil.tokenizeCommand("warp vip"));
    }

    @Test
    void tokenizeStripsLeadingSlash() {
        assertArrayEquals(new String[]{"warp", "vip"}, CommandUtil.tokenizeCommand("/warp vip"));
    }

    @Test
    void tokenizeLowercases() {
        assertArrayEquals(new String[]{"warp", "vip"}, CommandUtil.tokenizeCommand("Warp VIP"));
    }

    @Test
    void tokenizeCollapsesRepeatedSpaces() {
        assertArrayEquals(new String[]{"warp", "vip"}, CommandUtil.tokenizeCommand("warp   vip"));
    }

    @Test
    void tokenizeStripsNamespaceFromLabelOnly() {
        assertArrayEquals(new String[]{"warp", "vip"}, CommandUtil.tokenizeCommand("essentials:warp vip"));
    }

    @Test
    void tokenizeHandlesEverythingAtOnce() {
        assertArrayEquals(new String[]{"gamemode", "creative"},
                CommandUtil.tokenizeCommand("  /Minecraft:Gamemode   Creative "));
    }

    @Test
    void tokenizeBlankIsEmpty() {
        assertEquals(0, CommandUtil.tokenizeCommand("   ").length);
        assertEquals(0, CommandUtil.tokenizeCommand("/").length);
    }

    // --- subCommandMatches: the chosen "token-based" semantics ---

    @Test
    void blocksExactSubCommand() {
        assertTrue(matches("warp vip", "warp vip"));
    }

    @Test
    void blocksBypassAttempts() {
        assertTrue(matches("Warp vip", "warp vip"), "case");
        assertTrue(matches("warp  vip", "warp vip"), "double space");
        assertTrue(matches("essentials:warp vip", "warp vip"), "namespace");
        assertTrue(matches("warp vip now please", "warp vip"), "extra trailing args");
    }

    @Test
    void doesNotBlockPartialWordPrefix() {
        // intended trade-off of token matching: "vipfoo" is a different word than "vip"
        assertFalse(matches("warp vipfoo", "warp vip"));
    }

    @Test
    void doesNotBlockShorterOrDifferentCommand() {
        assertFalse(matches("warp", "warp vip"), "fewer tokens than the subcommand");
        assertFalse(matches("warpx vip", "warp vip"), "different label");
        assertFalse(matches("home vip", "warp vip"), "different command");
    }

    private static boolean matches(String message, String subCommand) {
        return CommandUtil.subCommandMatches(CommandUtil.tokenizeCommand(message), subCommand);
    }

    // --- getCommandLabel (PERF-007) ---

    @Test
    void commandLabel() {
        assertEquals("warp", CommandUtil.getCommandLabel("warp vip"));
        assertEquals("warp", CommandUtil.getCommandLabel("/warp vip"));
        assertEquals("warp", CommandUtil.getCommandLabel("warp"));
        assertEquals("warp", CommandUtil.getCommandLabel("/warp"));
    }

    // --- filterSuggestions (PERF-002) ---

    @Test
    void filterRemovesBlockedSuggestion() {
        List<String> suggestions = new ArrayList<>(Arrays.asList("vip", "home", "spawn"));
        List<String> result = CommandUtil.filterSuggestions("/warp ", suggestions, List.of("warp vip"));
        assertEquals(Arrays.asList("home", "spawn"), result);
        // input list is not mutated when filtering happens
        assertEquals(Arrays.asList("vip", "home", "spawn"), suggestions);
    }

    @Test
    void filterReturnsInputWhenNothingToFilter() {
        List<String> suggestions = new ArrayList<>(Arrays.asList("vip", "home"));
        List<String> result = CommandUtil.filterSuggestions("/warp ", suggestions, List.of());
        // no copy is allocated when the blocked set is empty
        assertSame(suggestions, result);
    }
}
