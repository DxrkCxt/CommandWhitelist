package eu.endermite.commandwhitelist.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    void commandLabelStripsNamespace() {
        // must match tokenizeCommand's namespace-stripping, since both feed the same
        // whitelist lookup (allow-path vs. block-path) against the same lowercase-only set
        assertEquals("warp", CommandUtil.getCommandLabel("essentials:warp"));
        assertEquals("help", CommandUtil.getCommandLabel("minecraft:help"));
        assertEquals("warp", CommandUtil.getCommandLabel("/essentials:warp vip"));
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

    @Test
    void filterIgnoresCaseOfBufferAndBlockedSubCommand() {
        List<String> suggestions = new ArrayList<>(Arrays.asList("vip", "home"));
        List<String> result = CommandUtil.filterSuggestions("/WARP ", suggestions, List.of("Warp VIP"));
        assertEquals(List.of("home"), result);
    }

    @Test
    void filterCollapsesRepeatedSpacesInBuffer() {
        List<String> suggestions = new ArrayList<>(Arrays.asList("vip", "home"));
        List<String> result = CommandUtil.filterSuggestions("/warp   ", suggestions, List.of("warp vip"));
        assertEquals(List.of("home"), result);
    }

    @Test
    void filterStripsNamespaceFromBuffer() {
        List<String> suggestions = new ArrayList<>(Arrays.asList("vip", "home"));
        List<String> result = CommandUtil.filterSuggestions("/essentials:warp ", suggestions, List.of("warp vip"));
        assertEquals(List.of("home"), result);
    }

    @Test
    void filterRemovesSuggestionRegardlessOfItsOwnCase() {
        // the blocked "last argument" is matched case-insensitively against the actual
        // server-supplied suggestion strings too, not just exact-case List.remove
        List<String> suggestions = new ArrayList<>(Arrays.asList("VIP", "home"));
        List<String> result = CommandUtil.filterSuggestions("/warp ", suggestions, List.of("warp vip"));
        assertEquals(List.of("home"), result);
    }

    // --- isCommandAllowed / isSubCommandBlocked: shared across Bukkit/Velocity/Waterfall,
    // so the whitelist-check logic can be exercised here without any platform Player mock ---

    @Test
    void commandAllowedForDefaultGroupRegardlessOfPermission() {
        Map<String, CWGroup> groups = new LinkedHashMap<>();
        groups.put("default", new CWGroup("default", List.of("spawn"), List.of(), ""));
        assertTrue(CommandUtil.isCommandAllowed(groups, "spawn", permission -> false));
    }

    @Test
    void commandAllowedOnlyWithGroupPermission() {
        Map<String, CWGroup> groups = new LinkedHashMap<>();
        CWGroup vip = new CWGroup("vip", List.of("fly"), List.of(), "");
        groups.put("vip", vip);
        assertFalse(CommandUtil.isCommandAllowed(groups, "fly", permission -> false));
        assertTrue(CommandUtil.isCommandAllowed(groups, "fly", vip.getPermission()::equals));
    }

    @Test
    void commandNotAllowedWhenNotWhitelistedAnywhere() {
        Map<String, CWGroup> groups = new LinkedHashMap<>();
        groups.put("default", new CWGroup("default", List.of("spawn"), List.of(), ""));
        assertFalse(CommandUtil.isCommandAllowed(groups, "ban", permission -> true));
    }

    @Test
    void subCommandBlockedForDefaultGroupRegardlessOfPermission() {
        Map<String, CWGroup> groups = new LinkedHashMap<>();
        groups.put("default", new CWGroup("default", List.of(), List.of("warp vip"), ""));
        assertTrue(CommandUtil.isSubCommandBlocked(groups, "/Warp  vip", permission -> false));
    }

    @Test
    void subCommandBlockedOnlyWithGroupPermission() {
        Map<String, CWGroup> groups = new LinkedHashMap<>();
        CWGroup vip = new CWGroup("vip", List.of(), List.of("warp vip"), "");
        groups.put("vip", vip);
        assertFalse(CommandUtil.isSubCommandBlocked(groups, "/warp vip", permission -> false));
        assertTrue(CommandUtil.isSubCommandBlocked(groups, "/warp vip", vip.getPermission()::equals));
    }
}
