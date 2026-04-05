package com.jvm.visualizer.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Scrollable event log panel that records every operation with color coding.
 */
public class LogPanel extends JPanel {

    private final JTextPane logPane;
    private final javax.swing.text.StyledDocument doc;

    private static final javax.swing.text.SimpleAttributeSet STYLE_OK    = new javax.swing.text.SimpleAttributeSet();
    private static final javax.swing.text.SimpleAttributeSet STYLE_ERR   = new javax.swing.text.SimpleAttributeSet();
    private static final javax.swing.text.SimpleAttributeSet STYLE_GC    = new javax.swing.text.SimpleAttributeSet();
    private static final javax.swing.text.SimpleAttributeSet STYLE_MUTED = new javax.swing.text.SimpleAttributeSet();
    private static final javax.swing.text.SimpleAttributeSet STYLE_INFO  = new javax.swing.text.SimpleAttributeSet();

    static {
        javax.swing.text.StyleConstants.setForeground(STYLE_OK,    new Color(0x88, 0xFF, 0xBB));
        javax.swing.text.StyleConstants.setForeground(STYLE_ERR,   new Color(0xFF, 0x6B, 0x6B));
        javax.swing.text.StyleConstants.setForeground(STYLE_GC,    new Color(0x00, 0xE8, 0x80));
        javax.swing.text.StyleConstants.setForeground(STYLE_MUTED, new Color(0x55, 0x66, 0x88));
        javax.swing.text.StyleConstants.setForeground(STYLE_INFO,  new Color(0x80, 0xAA, 0xFF));
        for (var s : new javax.swing.text.SimpleAttributeSet[]{STYLE_OK, STYLE_ERR, STYLE_GC, STYLE_MUTED, STYLE_INFO}) {
            javax.swing.text.StyleConstants.setBackground(s,   new Color(0x0A, 0x0E, 0x1C));
            javax.swing.text.StyleConstants.setFontFamily(s,   "Consolas");
            javax.swing.text.StyleConstants.setFontSize(s,     11);
        }
    }

    public LogPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_EDITOR);

        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(Theme.BG_EDITOR);
        logPane.setFont(Theme.FONT_MONO_SM);
        doc = logPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(logPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG_EDITOR);

        add(scroll, BorderLayout.CENTER);
        log("ready", "JVM Memory Simulator ready. Edit code and click ▶ Run or ⏭ Step.");
    }

    public void log(String level, String message) {
        javax.swing.text.SimpleAttributeSet style;
        String prefix;
        switch (level) {
            case "ok":    style = STYLE_OK;   prefix = "✔ "; break;
            case "error": style = STYLE_ERR;  prefix = "✖ "; break;
            case "gc":    style = STYLE_GC;   prefix = "🧹 "; break;
            case "info":  style = STYLE_INFO; prefix = "ℹ "; break;
            default:      style = STYLE_MUTED; prefix = "  "; break;
        }
        try {
            doc.insertString(doc.getLength(), prefix + message + "\n", style);
            logPane.setCaretPosition(doc.getLength());
        } catch (javax.swing.text.BadLocationException ignored) {}
    }

    public void clear() {
        try {
            doc.remove(0, doc.getLength());
        } catch (javax.swing.text.BadLocationException ignored) {}
    }
}
