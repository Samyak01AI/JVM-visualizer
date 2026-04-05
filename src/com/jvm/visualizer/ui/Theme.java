package com.jvm.visualizer.ui;

import java.awt.*;

/** Shared design tokens for the JVM Simulator UI. */
public final class Theme {
    private Theme() {}

    public static final Color BG_DARK        = new Color(0x0D, 0x11, 0x1F);
    public static final Color BG_PANEL       = new Color(0x13, 0x18, 0x2A);
    public static final Color BG_CARD        = new Color(0x1A, 0x20, 0x38);
    public static final Color BG_EDITOR      = new Color(0x0A, 0x0E, 0x1C);
    public static final Color ACCENT_BLUE    = new Color(0x4A, 0x9F, 0xFF);
    public static final Color ACCENT_PURPLE  = new Color(0xA0, 0x65, 0xFF);
    public static final Color ACCENT_TEAL    = new Color(0x00, 0xD8, 0xCC);
    public static final Color ACCENT_PINK    = new Color(0xFF, 0x4F, 0x8B);
    public static final Color ACCENT_ORANGE  = new Color(0xFF, 0x96, 0x44);
    public static final Color GC_GREEN       = new Color(0x00, 0xE8, 0x80);
    public static final Color GC_RED         = new Color(0xFF, 0x3B, 0x3B);
    public static final Color TEXT_PRIMARY   = new Color(0xE8, 0xED, 0xFF);
    public static final Color TEXT_MUTED     = new Color(0x60, 0x72, 0x9A);
    public static final Color TEXT_COMMENT   = new Color(0x45, 0x56, 0x70);
    public static final Color BORDER         = new Color(0x28, 0x32, 0x52);
    public static final Color LINE_CURRENT   = new Color(0x1E, 0x28, 0x48);
    public static final Color LINE_HIGHLIGHT = new Color(0xFF, 0xD7, 0x00, 40);

    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  18);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN, 13);
    public static final Font FONT_MONO_SM = new Font("Consolas",  Font.PLAIN, 11);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 10);

    public static final int  ARC     = 10;
    public static final int  PADDING = 12;
}
