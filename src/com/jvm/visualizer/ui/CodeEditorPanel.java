package com.jvm.visualizer.ui;

import com.jvm.visualizer.engine.JavaToSimTranslator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.text.Highlighter.HighlightPainter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Code editor panel with:
 *  - Line numbers gutter
 *  - Dual-mode syntax highlighting: Java keywords OR sim-script keywords
 *  - Auto-detected mode badge (☕ Java / ⚙ Script)
 *  - Current execution line highlight (arrow + yellow tint)
 */
public class CodeEditorPanel extends JPanel {

    public enum EditorMode { JAVA, SIM }

    private final JTextPane      editorPane;
    private final StyledDocument doc;
    private final LineGutter      gutter;
    private final JScrollPane     scroll;
    private final JLabel          modeBadge;

    // ── Sim-script styles ─────────────────────────────────────────────────────
    private final SimpleAttributeSet styleNormal  = new SimpleAttributeSet();
    private final SimpleAttributeSet styleComment = new SimpleAttributeSet();
    private final SimpleAttributeSet styleCreate  = new SimpleAttributeSet();
    private final SimpleAttributeSet styleLink    = new SimpleAttributeSet();
    private final SimpleAttributeSet stylePush    = new SimpleAttributeSet();
    private final SimpleAttributeSet stylePop     = new SimpleAttributeSet();
    private final SimpleAttributeSet styleGC      = new SimpleAttributeSet();
    private final SimpleAttributeSet styleError   = new SimpleAttributeSet();
    private final SimpleAttributeSet styleArg     = new SimpleAttributeSet();
    private final SimpleAttributeSet styleArray   = new SimpleAttributeSet();

    // ── Java-mode styles ──────────────────────────────────────────────────────
    private final SimpleAttributeSet styleJavaKw     = new SimpleAttributeSet(); // class, void, …
    private final SimpleAttributeSet styleJavaNew    = new SimpleAttributeSet(); // new
    private final SimpleAttributeSet styleJavaNull   = new SimpleAttributeSet(); // null
    private final SimpleAttributeSet styleJavaType   = new SimpleAttributeSet(); // capitalised types
    private final SimpleAttributeSet styleJavaStr    = new SimpleAttributeSet(); // "string"
    private final SimpleAttributeSet styleJavaNum    = new SimpleAttributeSet(); // numbers
    private final SimpleAttributeSet styleJavaDot    = new SimpleAttributeSet(); // a.b = c  (field assign)
    private final SimpleAttributeSet styleJavaBrace  = new SimpleAttributeSet(); // { }
    private final SimpleAttributeSet styleJavaGc     = new SimpleAttributeSet(); // System.gc()

    private int        currentLine = -1; // 0-based; -1 = none
    private Object     lineHighlightTag = null; // Highlighter tag for current-line background
    private boolean    updating    = false;
    private EditorMode mode        = EditorMode.SIM;

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

        // Syntax highlighting + mode detection on every edit
        doc.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { schedule(); }
            public void removeUpdate(DocumentEvent e) { schedule(); }
            public void changedUpdate(DocumentEvent e) {}
            private void schedule() {
                SwingUtilities.invokeLater(() -> {
                    if (!updating) {
                        detectAndHighlight();
                        updateModeBadge();
                    }
                });
            }
        });

        gutter = new LineGutter();
        scroll = new JScrollPane(editorPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG_EDITOR);
        scroll.setRowHeaderView(gutter);
        scroll.getVerticalScrollBar().setBackground(Theme.BG_DARK);
        scroll.getViewport().addChangeListener(e -> gutter.repaint());

        // ── Mode badge ───────────────────────────────────────────────────────
        modeBadge = new JLabel("  ⚙ Sim Script", SwingConstants.LEFT);
        modeBadge.setFont(Theme.FONT_MONO_SM.deriveFont(Font.BOLD));
        modeBadge.setOpaque(true);
        modeBadge.setForeground(Color.WHITE);
        modeBadge.setBackground(Theme.ACCENT_PURPLE);
        modeBadge.setPreferredSize(new Dimension(0, 20));
        modeBadge.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        add(modeBadge, BorderLayout.NORTH);
        add(scroll,    BorderLayout.CENTER);

        // Default demo — shows Java code to guide users
        setCode(
            "// ☕  Paste real Java code  OR  write sim-script — auto-detected!\n" +
            "//\n" +
            "// ── Java example ──────────────────────────────────────────\n" +
            "class LinkedList {\n" +
            "    Node head;\n" +
            "\n" +
            "    void buildList() {\n" +
            "        Node a = new Node();\n" +
            "        Node b = new Node();\n" +
            "        Node c = new Node();\n" +
            "        a.next = b;\n" +
            "        b.next = c;\n" +
            "    }\n" +
            "\n" +
            "    void freeHead() {\n" +
            "        head = null;\n" +
            "        System.gc();\n" +
            "    }\n" +
            "}\n"
        );
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public String getText() { return editorPane.getText(); }

    public List<String> getLines() {
        List<String> lines = new ArrayList<>();
        for (String p : getText().split("\n", -1)) lines.add(p);
        return lines;
    }

    public void setCode(String code) {
        editorPane.setText(code);
        SwingUtilities.invokeLater(() -> {
            detectAndHighlight();
            updateModeBadge();
        });
    }

    public EditorMode getMode() { return mode; }

    /** Set the currently executing line (0-based). Pass -1 to clear. */
    public void setCurrentLine(int line) {
        this.currentLine = line;
        applyLineHighlight(line);
        gutter.repaint();
        scrollToLine(line);
    }

    /** Paints a yellow tint over the current executing line in the editor. */
    private void applyLineHighlight(int lineIndex) {
        Highlighter hl = editorPane.getHighlighter();
        // Remove previous highlight
        if (lineHighlightTag != null) {
            hl.removeHighlight(lineHighlightTag);
            lineHighlightTag = null;
        }
        if (lineIndex < 0) return;
        try {
            String text   = doc.getText(0, doc.getLength());
            String[] lines = text.split("\n", -1);
            if (lineIndex >= lines.length) return;
            int start = 0;
            for (int i = 0; i < lineIndex; i++) start += lines[i].length() + 1;
            int end = start + lines[lineIndex].length();
            // Use a solid background painter for the whole line
            HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(
                    new Color(0xFF, 0xE0, 0x60, 55)); // soft yellow tint
            lineHighlightTag = hl.addHighlight(start, end, painter);
        } catch (BadLocationException ignored) {}
    }

    public void clearHighlight() { setCurrentLine(-1); }

    // ─── Mode detection ───────────────────────────────────────────────────────

    private void detectAndHighlight() {
        List<String> lines = getLines();
        mode = JavaToSimTranslator.isJavaCode(lines) ? EditorMode.JAVA : EditorMode.SIM;
        if (mode == EditorMode.JAVA) applyJavaHighlighting();
        else                         applySimHighlighting();
    }

    private void updateModeBadge() {
        if (mode == EditorMode.JAVA) {
            modeBadge.setText("  ☕  Java Mode  — click ▶ Run to auto-translate & visualize");
            modeBadge.setBackground(new Color(0xB5, 0x45, 0x00)); // Java orange-brown
        } else {
            modeBadge.setText("  ⚙  Sim Script Mode");
            modeBadge.setBackground(Theme.ACCENT_PURPLE);
        }
    }

    // ─── Sim-script Syntax Highlighting ──────────────────────────────────────

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

        // Java styles
        setStyle(styleJavaKw,    new Color(0x80, 0xB4, 0xFF), bg, true);  // soft blue
        setStyle(styleJavaNew,   Theme.ACCENT_ORANGE,          bg, true);
        setStyle(styleJavaNull,  Theme.GC_RED,                 bg, false);
        setStyle(styleJavaType,  Theme.ACCENT_TEAL,            bg, false);
        setStyle(styleJavaStr,   Theme.GC_GREEN,               bg, false);
        setStyle(styleJavaNum,   new Color(0xFF, 0xC0, 0x67), bg, false); // warm yellow
        setStyle(styleJavaDot,   new Color(0xCC, 0xCC, 0xFF), bg, false); // lavender
        setStyle(styleJavaBrace, new Color(0xFF, 0xD7, 0x00), bg, true);  // gold
        setStyle(styleJavaGc,    Theme.GC_GREEN,               bg, true);
    }

    private void setStyle(SimpleAttributeSet s, Color fg, Color bg, boolean bold) {
        StyleConstants.setForeground(s, fg);
        StyleConstants.setBackground(s, bg);
        StyleConstants.setBold(s, bold);
        StyleConstants.setFontFamily(s, "Consolas");
        StyleConstants.setFontSize(s, 13);
    }

    private void applySimHighlighting() {
        updating = true;
        try {
            String text  = doc.getText(0, doc.getLength());
            doc.setCharacterAttributes(0, doc.getLength(), styleNormal, true);
            String[] lines = text.split("\n", -1);
            int offset = 0;
            for (String line : lines) {
                String trimmed    = line.stripLeading();
                int localOffset   = offset + (line.length() - trimmed.length());
                if (trimmed.startsWith("//") || trimmed.isEmpty()) {
                    doc.setCharacterAttributes(offset, line.length(), styleComment, true);
                } else {
                    String[] parts = trimmed.split("\\s+");
                    String   kw    = parts[0].toLowerCase();
                    SimpleAttributeSet kwStyle = getSimKeywordStyle(kw);
                    doc.setCharacterAttributes(localOffset, parts[0].length(), kwStyle, true);
                    int argStart = localOffset + parts[0].length();
                    if (argStart < offset + line.length()) {
                        doc.setCharacterAttributes(argStart, offset + line.length() - argStart, styleArg, true);
                    }
                }
                offset += line.length() + 1;
            }
        } catch (BadLocationException ignored) {
        } finally { updating = false; }
    }

    private SimpleAttributeSet getSimKeywordStyle(String kw) {
        switch (kw) {
            case "create":                          return styleCreate;
            case "link": case "unlink":             return styleLink;
            case "push":                            return stylePush;
            case "pop": case "remove":              return stylePop;
            case "gc":                              return styleGC;
            case "newarray": case "astore": case "aload": return styleArray;
            default:                                return styleError;
        }
    }

    // ─── Java Syntax Highlighting ─────────────────────────────────────────────

    private static final java.util.regex.Pattern JAVA_KW = java.util.regex.Pattern.compile(
        "\\b(class|interface|enum|extends|implements|import|package|" +
        "public|private|protected|static|final|abstract|synchronized|" +
        "void|return|if|else|for|while|do|switch|case|break|continue|" +
        "try|catch|finally|throw|throws|int|long|double|float|boolean|" +
        "char|byte|short|var|this|super)\\b"
    );

    private static final java.util.regex.Pattern JAVA_TYPE = java.util.regex.Pattern.compile(
        "\\b([A-Z][A-Za-z0-9_]*)\\b"
    );

    private static final java.util.regex.Pattern JAVA_NUM = java.util.regex.Pattern.compile(
        "\\b(\\d+\\.?\\d*[fFdDlL]?)\\b"
    );

    private static final java.util.regex.Pattern JAVA_STR = java.util.regex.Pattern.compile(
        "\"[^\"]*\""
    );

    private void applyJavaHighlighting() {
        updating = true;
        try {
            String text = doc.getText(0, doc.getLength());
            // Base coat
            doc.setCharacterAttributes(0, doc.getLength(), styleNormal, true);

            String[] lines = text.split("\n", -1);
            int offset = 0;

            for (String line : lines) {
                String trimmed = line.stripLeading();

                if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                    // Full comment line
                    doc.setCharacterAttributes(offset, line.length(), styleComment, true);
                    offset += line.length() + 1;
                    continue;
                }

                int baseOff = offset;
                int len     = line.length();

                // Types (capitalized) first so keywords can override
                applyPattern(JAVA_TYPE,  text, baseOff, len, styleJavaType);
                // Java keywords override types
                applyPattern(JAVA_KW,    text, baseOff, len, styleJavaKw);
                // `new` gets its own orange highlight
                applyKeywordInLine(text, baseOff, len, "\\bnew\\b", styleJavaNew);
                // `null`
                applyKeywordInLine(text, baseOff, len, "\\bnull\\b", styleJavaNull);
                // Braces
                applyKeywordInLine(text, baseOff, len, "[{}]", styleJavaBrace);
                // Numbers
                applyPattern(JAVA_NUM,   text, baseOff, len, styleJavaNum);
                // Strings
                applyPattern(JAVA_STR,   text, baseOff, len, styleJavaStr);
                // System.gc()
                if (line.contains("System.gc()")) {
                    int gc = text.indexOf("System.gc()", baseOff);
                    if (gc >= 0 && gc < baseOff + len) {
                        doc.setCharacterAttributes(gc, 11, styleJavaGc, true);
                    }
                }
                // Inline comment after code
                int commentIdx = line.indexOf("//");
                if (commentIdx >= 0) {
                    doc.setCharacterAttributes(baseOff + commentIdx, len - commentIdx, styleComment, true);
                }

                offset += len + 1;
            }
        } catch (BadLocationException ignored) {
        } finally { updating = false; }
    }

    private void applyPattern(java.util.regex.Pattern pat, String text,
                              int lineStart, int lineLen,
                              SimpleAttributeSet style) throws BadLocationException {
        String segment = text.substring(lineStart, Math.min(lineStart + lineLen, text.length()));
        java.util.regex.Matcher m = pat.matcher(segment);
        while (m.find()) {
            doc.setCharacterAttributes(lineStart + m.start(), m.end() - m.start(), style, true);
        }
    }

    private void applyKeywordInLine(String text, int lineStart, int lineLen,
                                    String regex, SimpleAttributeSet style)
            throws BadLocationException {
        String segment = text.substring(lineStart, Math.min(lineStart + lineLen, text.length()));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(segment);
        while (m.find()) {
            doc.setCharacterAttributes(lineStart + m.start(), m.end() - m.start(), style, true);
        }
    }

    // ─── Scroll to line ───────────────────────────────────────────────────────

    private void scrollToLine(int lineIndex) {
        if (lineIndex < 0) return;
        try {
            String text   = doc.getText(0, doc.getLength());
            String[] lines = text.split("\n", -1);
            int offset     = 0;
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

            g2.setColor(Theme.BORDER);
            g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

            FontMetrics fm = editorPane.getFontMetrics(editorPane.getFont());
            int lineHeight = fm.getHeight();
            Rectangle viewRect = scroll.getViewport().getViewRect();

            String[] lines;
            try {
                lines = doc.getText(0, doc.getLength()).split("\n", -1);
            } catch (BadLocationException e) { return; }

            // Build cumulative char offsets so we can call modelToView per line
            int[] offsets = new int[lines.length];
            int off = 0;
            for (int i = 0; i < lines.length; i++) {
                offsets[i] = off;
                off += lines[i].length() + 1;
            }

            for (int i = 0; i < lines.length; i++) {
                // Get actual Y of this line using the model position
                Rectangle lineRect;
                try {
                    lineRect = editorPane.modelToView(offsets[i]);
                } catch (BadLocationException e) { continue; }
                if (lineRect == null) continue;

                int lineY = lineRect.y - viewRect.y;
                // Skip lines outside visible area
                if (lineY + lineHeight < 0 || lineY > viewRect.height) continue;

                int baseline = lineY + fm.getAscent();

                if (i == currentLine) {
                    // Yellow tint row in gutter
                    g2.setColor(Theme.LINE_HIGHLIGHT);
                    g2.fillRect(0, lineY, getWidth() - 1, lineRect.height > 0 ? lineRect.height : lineHeight);
                    // Arrow indicator
                    g2.setColor(Theme.ACCENT_BLUE);
                    g2.setFont(editorPane.getFont().deriveFont(Font.BOLD, 11f));
                    g2.drawString("▶", 4, baseline);
                } else {
                    g2.setColor(Theme.TEXT_MUTED);
                    g2.setFont(editorPane.getFont().deriveFont(Font.PLAIN, 11f));
                    String num = String.valueOf(i + 1);
                    g2.drawString(num, getWidth() - 8 - fm.stringWidth(num), baseline);
                }
            }
        }
    }
}
