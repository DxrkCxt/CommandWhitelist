package eu.endermite.commandwhitelist.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CWGroupTest {

    @Test
    void commandsAreStoredLowercase() {
        CWGroup g = new CWGroup("default", List.of("Spawn", "TPA"), List.of(), "");
        assertTrue(g.getCommands().contains("spawn"));
        assertTrue(g.getCommands().contains("tpa"));
        assertFalse(g.getCommands().contains("Spawn"));
    }

    @Test
    void addAndRemoveCommandLowercase() {
        CWGroup g = new CWGroup("default", List.of(), List.of(), "");
        g.addCommand("Fly");
        assertTrue(g.getCommands().contains("fly"));
        g.removeCommand("FLY");
        assertFalse(g.getCommands().contains("fly"));
    }

    @Test
    void subCommandTokensArePrecomputedAndNormalized() {
        CWGroup g = new CWGroup("default", List.of(), List.of("Help About", "warp vip"), "");
        List<String[]> tokens = g.getSubCommandTokens();
        assertEquals(2, tokens.size());
        assertTrue(tokens.stream().anyMatch(t -> t.length == 2 && t[0].equals("help") && t[1].equals("about")));
        assertTrue(tokens.stream().anyMatch(t -> t.length == 2 && t[0].equals("warp") && t[1].equals("vip")));
    }

    @Test
    void subCommandTokensUpdateOnChange() {
        CWGroup g = new CWGroup("default", List.of(), List.of(), "");
        assertEquals(0, g.getSubCommandTokens().size());
        g.addSubCommand("gamemode creative");
        assertEquals(1, g.getSubCommandTokens().size());
        g.removeSubCommand("gamemode creative");
        assertEquals(0, g.getSubCommandTokens().size());
    }
}
