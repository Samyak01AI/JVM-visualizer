package com.jvm.visualizer.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Code editor panel with:
 *  - Line numbers gutter
 *  - Syntax highlighting (keyword-coloured text)
 *  - Current execution line highlight (arrow + yellow tint)
 */
public class CodeEditorPanel extends JPanel {

    private final JTextPane      editorPane;
    private final StyledDocument doc;
    private final LineGutter      gutter;
    private final JScrollPane     scroll;

    // Styles
    private final SimpleAttributeSet styleNormal   = new SimpleAttributeSet();
    private final SimpleAttributeSet styleComment  = new SimpleAttributeSet();
    private final SimpleAttributeSet styleCreate   = new SimpleAttributeSet();
    private final SimpleAttributeSet styleLink     = new SimpleAttributeSet();
    private final SimpleAttributeSet stylePush     = new SimpleAttributeSet();
    private final SimpleAttributeSet stylePop      = new SimpleAttributeSet();
    private final SimpleAttributeSet styleGC       = new SimpleAttributeSet();
    private final SimpleAttributeSet styleError    = new SimpleAttributeSet();
    private final SimpleAttributeSet styleArg      = new SimpleAttributeSet();
    private final SimpleAttributeSet styleArray    = new SimpleAttributeSet();

    private int currentLine = -1; // 0-based current execution line; -1 = none
    private boolean updating = false;

    public CodeEditorPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_EDITOR);

        editorPane = new JTextPane();
        editorPane.setBackground(Theme.BG_EDITOR);
        editorPane.setForeground(Theme.TEXT_PRIMARY);
        editorPane.setCaretColor(Theme.TEXT_PRIMARY);
        editorPane.setFont(Theme.FONT_MONO);
        editorPane.setSelectionColor(new Color(0x4A, 0x9F, 0xFF, 60));

        doc = editorPane.getStyledDocument();
        initStyles();

        // Syntax highlighting on every edit
        doc.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { schedule(); }
            public void removeUpdate(DocumentEvent e)  { schedule(); }
            public void changedUpdate(DocumentEvent e) {}
            private void schedule() {
                SwingUtilities.invokeLater(() -> {
                    if (!updating) applySyntaxHighlighting();
                });
            }
        });

        gutter = new LineGutter();
        scroll = new JScrollPane(editorPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG_EDITOR);
        scroll.setRowHeaderView(gutter);
        scroll.getVerticalScrollBar().setBackground(Theme.BG_DARK);

        // Sync gutter on scroll
        scroll.getViewport().addChangeListener(e -> gutter.repaint());

        add(scroll, BorderLayout.CENTER);

        // Seed with demo program
        setCode(
            "// JVM Memory Simulator — edit and run!\n" +
            "create A\n" +
            "create B\n" +
            "newarray arr int 4\n" +
            "astore arr 0 A\n" +
            "astore arr 3 B\n" +
            "link A B\n" +
            "push main arr A\n" +
            "pop\n" +
            "remove arr\n" +
            "gc\n"
        );
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public String getText()   { return editorPane.getText(); }

    public List<String> getLines() {
        List<String> lines = new ArrayList<>();
        String[] parts = getText().split("\n", -1);
        for (String p : parts) lines.add(p);
        return lines;
    }

    public void setCode(String code) {
        editorPane.setText(code);
        SwingUtilities.invokeLater(this::applySyntaxHighlighting);
    }

    /** Set the currently executing line (0-based). Pass -1 to clear. */
    public void setCurrentLine(int line) {
        this.currentLine = line;
        gutter.repaint();
        scrollToLine(line);
    }

    public void clearHighlight() { setCurrentLine(-1); }

    // ─── Syntax Highlighting ─────────────────────────────────────────────────

    private void initStyles() {
        Color bg = Theme.BG_EDITOR;
        setStyle(styleNormal,  Theme.TEXT_PRIMARY,  bg, false);
        setStyle(styleComment, Theme.TEXT_COMMENT,  bg, false);
        setStyle(styleCreate,  Theme.ACCENT_BLUE,   bg, true);
        setStyle(styleLink,    Theme.ACCENT_TEAL,   bg, true);
        setStyle(stylePush,    Theme.ACCENT_PURPLE, bg, true);
        setStyle(stylePop,     Theme.ACCENT_ORANGE, bg, true);
        setStyle(styleGC,      Theme.GC_GREEN,      bg, true);
        setStyle(styleError,   Theme.GC_RED,        bg, false);
        setStyle(styleArg,     Theme.TEXT_PRIMARY,  bg, false);
        setStyle(styleArray,   Theme.ACCENT_PINK,   bg, true);
    }

    private void setStyle(SimpleAttributeSet s, Color fg, Color bg, boolean bold) {
        StyleConstants.setForeground(s, fg);
        StyleConstants.setBackground(s, bg);
        StyleConstants.setBold(s, bold);
        StyleConstants.setFontFamily(s, "Consolas");
        StyleConstants.setFontSize(s, 13);
    }

    private void applySyntaxHighlighting() {
        updating = true;
        try {
            String text = doc.getText(0, doc.getLength());
            doc.setCharacterAttributes(0, doc.getLength(), styleNormal, true);

            String[] lines = text.split("\n", -1);
            int offset = 0;
            for (String line : lines) {
                String trimmed = line.stripLeading();
                int localOffset = offset + (line.length() - trimmed.length());

                if (trimmed.startsWith("//") || trimmed.isEmpty()) {
                    doc.setCharacterAttributes(offset, line.length(), styleComment, true);
                } else {
                    String[] parts = trimmed.split("\\s+");
                    String kw = parts[0].toLowerCase();
                    SimpleAttributeSet kwStyle = getKeywordStyle(kw);
                    // colour the keyword
                    doc.setCharacterAttributes(localOffset, parts[0].length(), kwStyle, true);
                    // colour arguments as normal text
                    int argStart = localOffset + parts[0].length();
                    if (argStart < offset + line.length()) {
                        doc.setCharacterAttributes(argStart, offset + line.length() - argStart, styleArg, true);
                    }
                }
                offset += line.length() + 1; // +1 for newline
            }
        } catch (BadLocationException ignored) {
        } finally {
            updating = false;
        }
    }

    private SimpleAttributeSet getKeywordStyle(String kw) {
        switch (kw) {
            case "create":              return styleCreate;
            case "link": case "unlink": return styleLink;
            case "push":                return stylePush;
            case "pop":                 return stylePop;
            case "remove":              return stylePop;
            case "gc":                  return styleGC;
            case "newarray": case "astore": case "aload": return styleArray;
            default:                    return styleError;
        }
    }

    // ─── Scroll to line ───────────────────────────────────────────────────────

    private void scrollToLine(int lineIndex) {
        if (lineIndex < 0) return;
        try {
            String text = doc.getText(0, doc.getLength());
            String[] lines = text.split("\n", -1);
            int offset = 0;
            for (int i = 0; i < lineIndex && i < lines.length; i++) {
                offset += lines[i].length() + 1;
            }
            Rectangle rect = editorPane.modelToView(offset);
            if (rect != null) {
                rect.height = scroll.getViewport().getHeight();
                editorPane.scrollRectToVisible(rect);
            }
        } catch (BadLocationException ignored) {}
    }

    // ─── Line Gutter ─────────────────────────────────────────────────────────

    private class LineGutter extends JComponent {
        LineGutter() {
            setPreferredSize(new Dimension(46, 0));
            setBackground(Theme.BG_DARK);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.BG_DARK);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Right border line
            g2.setColor(Theme.BORDER);
            g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

            FontMetrics fm;
            try {
                fm = editorPane.getFontMetrics(Theme.FONT_MONO_SM);
            } catch (Exception e) { return; }

            int lineHeight = fm.getHeight();
            Rectangle viewRect = scroll.getViewport().getViewRect();
            int startLine = viewRect.y / lineHeight;
            int endLine   = (viewRect.y + viewRect.height) / lineHeight + 1;

            try {
                String[] lines = doc.getText(0, doc.getLength()).split("\n", -1);
                endLine = Math.min(endLine, lines.length);
            } catch (BadLocationException ignored) {}

            for (int i = startLine; i < endLine; i++) {
                int y = (i * lineHeight) - viewRect.y + fm.getAscent() + 4;

                // Highlight current execution line
                if (i == currentLine) {
                    g2.setColor(Theme.LINE_HIGHLIGHT);
                    g2.fillRect(0, (i * lineHeight) - viewRect.y, getWidth() - 1, lineHeight);
                    g2.setColor(Theme.ACCENT_BLUE);
                    g2.setFont(Theme.FONT_MONO_SM.deriveFont(Font.BOLD));
                    g2.drawString("▶", 4, y);
                } else {
                    g2.setColor(Theme.TEXT_MUTED);
                    g2.setFont(Theme.FONT_MONO_SM);
                    String num = String.valueOf(i + 1);
                    g2.drawString(num, getWidth() - 8 - fm.stringWidth(num), y);
                }
            }
        }
    }
}
