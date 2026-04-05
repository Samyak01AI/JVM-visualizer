package com.jvm.visualizer.ui;

import com.jvm.visualizer.core.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Visualizes the simulated call stack.
 * Frames are drawn from top (active) downward.
 * Each frame shows function name and its local object references.
 */
public class StackPanel extends JPanel {

    private final StackManager stackMgr;

    public StackPanel(StackManager stackMgr) {
        this.stackMgr = stackMgr;
        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        List<StackFrame> frames = new java.util.ArrayList<>(stackMgr.getStack());
        java.util.Collections.reverse(frames); // show top-of-stack first

        if (frames.isEmpty()) {
            g2.setFont(Theme.FONT_LABEL);
            g2.setColor(Theme.TEXT_MUTED);
            String msg = "Stack empty";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            return;
        }

        int y = Theme.PADDING;
        for (int i = 0; i < frames.size(); i++) {
            boolean isTop = (i == 0);
            y = drawFrame(g2, frames.get(i), y, isTop);
            y += 8;
        }
    }

    /** Draw one stack frame card. Returns the new Y position after the card. */
    private int drawFrame(Graphics2D g2, StackFrame frame, int y, boolean isTop) {
        List<String> refs  = frame.getLocalRefs();
        int numLines       = Math.max(1, refs.size());
        int cardH          = 40 + numLines * 18;
        int x              = Theme.PADDING;
        int w              = getWidth() - 2 * Theme.PADDING;

        Color bg     = isTop ? new Color(0x14, 0x1E, 0x40) : Theme.BG_CARD;
        Color border = isTop ? Theme.ACCENT_PURPLE : Theme.BORDER;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 45));
        g2.fillRoundRect(x + 2, y + 3, w, cardH, Theme.ARC, Theme.ARC);

        // Card body
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, cardH, Theme.ARC, Theme.ARC);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(isTop ? 1.8f : 1.0f));
        g2.drawRoundRect(x, y, w, cardH, Theme.ARC, Theme.ARC);

        // Left accent bar (active frame)
        if (isTop) {
            g2.setColor(Theme.ACCENT_PURPLE);
            g2.fillRoundRect(x, y, 4, cardH, 2, 2);
        }

        // Function name
        g2.setFont(Theme.FONT_MONO.deriveFont(Font.BOLD, 12f));
        g2.setColor(isTop ? Theme.ACCENT_PURPLE : Theme.TEXT_PRIMARY);
        String nameStr = (isTop ? "▶ " : "  ") + frame.getFunctionName() + "()";
        g2.drawString(nameStr, x + 12, y + 22);

        // Divider
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(x + 8, y + 30, x + w - 8, y + 30);

        // Local references
        int vy = y + 46;
        if (refs.isEmpty()) {
            g2.setFont(Theme.FONT_MONO_SM);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString("  (no local refs)", x + 12, vy);
        } else {
            for (String refId : refs) {
                g2.setFont(Theme.FONT_MONO_SM);
                g2.setColor(Theme.ACCENT_TEAL);
                g2.drawString("  • ", x + 10, vy);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Theme.TEXT_PRIMARY);
                g2.drawString(refId, x + 10 + fm.stringWidth("  • "), vy);
                vy += 18;
            }
        }

        return y + cardH;
    }
}
