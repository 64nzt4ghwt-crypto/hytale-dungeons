package com.howlstudio.dungeons.util;

import com.hypixel.hytale.server.core.Message;
import java.awt.Color;

/**
 * Clean message formatting utility for Hytale's Message API.
 * Uses proper .color() / .bold() / .italic() instead of legacy codes.
 * ASCII only - no Unicode symbols (Hytale font doesn't support them).
 */
public final class Msg {

    // Dungeon theme colors
    public static final Color GOLD    = new Color(255, 185, 50);
    public static final Color AMBER   = new Color(255, 170, 0);
    public static final Color GREEN   = new Color(100, 255, 100);
    public static final Color RED     = new Color(255, 85, 85);
    public static final Color AQUA    = new Color(85, 255, 255);
    public static final Color GRAY    = new Color(170, 170, 170);
    public static final Color DARK    = new Color(120, 120, 120);
    public static final Color WHITE   = new Color(255, 255, 255);
    public static final Color PINK    = new Color(255, 130, 200);

    private Msg() {}

    /** Simple colored text */
    public static Message text(String s, Color c) {
        return Message.raw(s).color(c);
    }

    /** Bold colored text */
    public static Message bold(String s, Color c) {
        return Message.raw(s).color(c).bold(true);
    }

    /** Label: Value pair on one line */
    public static Message pair(String label, String value, Color labelColor, Color valueColor) {
        return Message.raw("")
            .insert(Message.raw(label).color(labelColor))
            .insert(Message.raw(value).color(valueColor));
    }

    /** Header bar - ASCII only */
    public static Message header(String title) {
        return Message.raw("")
            .insert(Message.raw("=== ").color(GOLD))
            .insert(Message.raw(title).color(GOLD).bold(true))
            .insert(Message.raw(" ===").color(GOLD));
    }

    /** Divider line - ASCII only */
    public static Message divider() {
        return Message.raw("----------------------").color(DARK);
    }

    /** Success message - ASCII only */
    public static Message success(String s) {
        return Message.raw("[+] ").color(GREEN).insert(Message.raw(s).color(GREEN));
    }

    /** Error message - ASCII only */
    public static Message error(String s) {
        return Message.raw("[!] ").color(RED).insert(Message.raw(s).color(RED));
    }

    /** Info/hint message - ASCII only */
    public static Message hint(String s) {
        return Message.raw("> ").color(GRAY).insert(Message.raw(s).color(GRAY).italic(true));
    }

    /** Bullet point item - ASCII only */
    public static Message bullet(String label, String value) {
        return Message.raw("")
            .insert(Message.raw("  * ").color(DARK))
            .insert(Message.raw(label + ": ").color(AQUA))
            .insert(Message.raw(value).color(WHITE));
    }

    /** Warning message */
    public static Message warn(String s) {
        return Message.raw("[*] ").color(AMBER).insert(Message.raw(s).color(AMBER));
    }
}
