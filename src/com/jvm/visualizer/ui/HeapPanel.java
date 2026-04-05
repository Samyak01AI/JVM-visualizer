package com.jvm.visualizer.ui;

import com.jvm.visualizer.core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Visualizes the Heap as a grid of SimObject nodes and SimArray nodes with reference arrows.
 */
public class HeapPanel extends JPanel {

    private static final int NODE_W = 110;
    private static final int NODE_H = 58;
    private static final int GAP_X = 50;
    private static final int GAP_Y = 40;
    private static final int COLS = 4;
    private static final int SLOT_W = 28;
    private static final int SLOT_H = 24;

    private final HeapManager heap;
    private boolean gcMarkPhase = false; 

    public HeapPanel(HeapManager heap) {
        this.heap = heap;
        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
    }

    public void setGcMarkPhase(boolean v) {
        this.gcMarkPhase = v;
    }

    @Override
    public Dimension getPreferredSize() {
        int n = Math.max(1, heap.size());
        int rows = (int) Math.ceil((double) n / COLS);
        // Arrays might be wide, so give generous width
        int w = Theme.PADDING * 2 + COLS * (NODE_W + GAP_X * 2);
        int h = Theme.PADDING * 2 + rows * (NODE_H + GAP_Y) + 40;
        return new Dimension(w, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Map<String, SimObject> objects = heap.getObjects();
        Map<String, SimArray>  arrays  = heap.getArrays();

        if (objects.isEmpty() && arrays.isEmpty()) {
            g2.setFont(Theme.FONT_LABEL);
            g2.setColor(Theme.TEXT_MUTED);
            String msg = "Heap is empty — use 'create A' or 'newarray arr int 3'";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            return;
        }

        // Layout nodes in grid & store positions (centers)
        int idx = 0;
        
        // 1. Objects
        for (SimObject obj : objects.values()) {
            int col = idx % COLS;
            int row = idx / COLS;
            int x = Theme.PADDING + col * (NODE_W + GAP_X);
            int y = Theme.PADDING + row * (NODE_H + GAP_Y);
            obj.setX(x + NODE_W / 2);
            obj.setY(y + NODE_H / 2);
            idx++;
        }
        
        // 2. Arrays
        for (SimArray arr : arrays.values()) {
            int col = idx % COLS;
            int row = idx / COLS;
            int arrWidth = Math.max(NODE_W, arr.getSize() * SLOT_W + 16);
            int x = Theme.PADDING + col * (NODE_W + GAP_X); // Keep starting X aligned somewhat
            int y = Theme.PADDING + row * (NODE_H + GAP_Y + 20); // Give arrays a bit more vertical padding
            arr.setX(x + arrWidth / 2);
            arr.setY(y + NODE_H / 2 + 10);
            idx++;
        }

        // Draw arrows first (behind nodes)
        for (SimObject obj : objects.values()) {
            for (String refId : obj.getReferences()) {
                drawArrowToId(g2, obj.getX(), obj.getY(), refId);
            }
        }
        for (SimArray arr : arrays.values()) {
            for (int i = 0; i < arr.getSize(); i++) {
                String elem = arr.get(i);
                if (elem != null && heap.exists(elem)) {
                    // Start arrow slightly below array center
                    drawArrowToId(g2, arr.getX(), arr.getY() + 10, elem);
                }
            }
        }

        // Draw nodes on top
        for (SimObject obj : objects.values()) {
            drawNode(g2, obj, obj.getX() - NODE_W / 2, obj.getY() - NODE_H / 2);
        }
        for (SimArray arr : arrays.values()) {
            int arrWidth = Math.max(NODE_W, arr.getSize() * SLOT_W + 16);
            drawArray(g2, arr, arr.getX() - arrWidth / 2, arr.getY() - NODE_H / 2 - 10, arrWidth);
        }
    }
    
    private void drawArrowToId(Graphics2D g2, int sx, int sy, String targetId) {
        if (heap.getObjects().containsKey(targetId)) {
            SimObject t = heap.getObject(targetId);
            drawArrow(g2, sx, sy, t.getX(), t.getY());
        } else if (heap.getArrays().containsKey(targetId)) {
            SimArray t = heap.getArray(targetId);
            drawArrow(g2, sx, sy, t.getX(), t.getY());
        }
    }

    // ─── Object Node Drawing ──────────────────────────────────────────────────

    private void drawNode(Graphics2D g2, SimObject obj, int x, int y) {
        Color cardBg = Theme.BG_CARD, cardBorder = Theme.ACCENT_BLUE, labelColor = Theme.TEXT_PRIMARY;
        if (gcMarkPhase) {
            boolean m = obj.isMarked();
            cardBg     = m ? new Color(0x0A, 0x28, 0x1A) : new Color(0x2A, 0x08, 0x08);
            cardBorder = m ? Theme.GC_GREEN : Theme.GC_RED;
            labelColor = m ? Theme.GC_GREEN : Theme.GC_RED;
        }

        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillRoundRect(x + 3, y + 4, NODE_W, NODE_H, Theme.ARC, Theme.ARC);
        g2.setColor(cardBg);
        g2.fillRoundRect(x, y, NODE_W, NODE_H, Theme.ARC, Theme.ARC);
        g2.setColor(cardBorder);
        g2.setStroke(new BasicStroke(gcMarkPhase ? 2.0f : 1.5f));
        g2.drawRoundRect(x, y, NODE_W, NODE_H, Theme.ARC, Theme.ARC);

        g2.setPaint(new GradientPaint(x, y, cardBorder.darker(), x + NODE_W, y, new Color(0, 0, 0, 0)));
        g2.fillRoundRect(x, y, NODE_W, 6, Theme.ARC, Theme.ARC);
        g2.setColor(cardBg);
        g2.fillRect(x, y + 3, NODE_W, 6);

        g2.setFont(Theme.FONT_MONO.deriveFont(Font.BOLD, 16f));
        g2.setColor(labelColor);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(obj.getId(), x + (NODE_W - fm.stringWidth(obj.getId())) / 2, y + 30);

        if (!obj.getReferences().isEmpty()) {
            g2.setFont(Theme.FONT_MONO_SM);
            g2.setColor(Theme.TEXT_MUTED);
            String refs = "→ " + obj.getReferences().size() + " refs";
            g2.drawString(refs, x + (NODE_W - fm.stringWidth(refs)) / 2, y + 46);
        }
        
        drawGCIcon(g2, gcMarkPhase, obj.isMarked(), x + NODE_W - 16, y + 14);
    }

    // ─── Array Node Drawing ───────────────────────────────────────────────────

    private void drawArray(Graphics2D g2, SimArray arr, int x, int y, int width) {
        int height = NODE_H + 24; 
        Color cardBg = Theme.BG_CARD, cardBorder = Theme.ACCENT_PINK, labelColor = Theme.TEXT_PRIMARY;
        if (gcMarkPhase) {
            boolean m = arr.isMarked();
            cardBg     = m ? new Color(0x0A, 0x28, 0x1A) : new Color(0x2A, 0x08, 0x08);
            cardBorder = m ? Theme.GC_GREEN : Theme.GC_RED;
            labelColor = m ? Theme.GC_GREEN : Theme.GC_RED;
        }

        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillRoundRect(x + 3, y + 4, width, height, Theme.ARC, Theme.ARC);
        g2.setColor(cardBg);
        g2.fillRoundRect(x, y, width, height, Theme.ARC, Theme.ARC);
        g2.setColor(cardBorder);
        g2.setStroke(new BasicStroke(gcMarkPhase ? 2.0f : 1.5f));
        g2.drawRoundRect(x, y, width, height, Theme.ARC, Theme.ARC);

        g2.setPaint(new GradientPaint(x, y, cardBorder.darker(), x + width, y, new Color(0, 0, 0, 0)));
        g2.fillRoundRect(x, y, width, 6, Theme.ARC, Theme.ARC);
        g2.setColor(cardBg);
        g2.fillRect(x, y + 3, width, 6);

        // Title
        g2.setFont(Theme.FONT_MONO.deriveFont(Font.BOLD, 14f));
        g2.setColor(labelColor);
        String title = arr.getId() + " (" + arr.getElementType() + "[])";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, x + (width - fm.stringWidth(title)) / 2, y + 26);

        // Slots
        int slotsX = x + (width - (arr.getSize() * SLOT_W)) / 2;
        int slotsY = y + 40;
        
        g2.setFont(Theme.FONT_SMALL);
        for (int i = 0; i < arr.getSize(); i++) {
            int sx = slotsX + i * SLOT_W;
            g2.setColor(Theme.BORDER);
            g2.drawRect(sx, slotsY, SLOT_W, SLOT_H);
            
            String val = arr.get(i);
            if (val != null) {
                g2.setColor(heap.exists(val) ? Theme.ACCENT_TEAL : Theme.TEXT_PRIMARY); // Highlight refs
                if (val.length() > 3) val = val.substring(0, 2) + "…";
                g2.drawString(val, sx + (SLOT_W - g2.getFontMetrics().stringWidth(val))/2, slotsY + 16);
            }
        }
        
        drawGCIcon(g2, gcMarkPhase, arr.isMarked(), x + width - 16, y + 14);
    }
    
    private void drawGCIcon(Graphics2D g2, boolean phase, boolean marked, int x, int y) {
        if (!phase) return;
        g2.setFont(Theme.FONT_MONO.deriveFont(Font.BOLD, 11f));
        g2.setColor(marked ? Theme.GC_GREEN : Theme.GC_RED);
        g2.drawString(marked ? "✓" : "✗", x, y);
    }

    // ─── Arrow Drawing ────────────────────────────────────────────────────────

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        if (x1 == x2 && y1 == y2) {
            g2.setColor(Theme.ACCENT_TEAL);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawArc(x1 + 10, y1 - 24, 30, 30, 0, 270);
            return;
        }
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.ACCENT_TEAL);

        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        double cx = dx * 0.4 + (-dy / len) * 20;
        double cy = dy * 0.4 + (dx / len) * 20;

        CubicCurve2D curve = new CubicCurve2D.Double(x1, y1, x1 + cx, y1 + cy, x2 - cx, y2 - cy, x2, y2);
        g2.draw(curve);

        double angle = Math.atan2(y2 - (y2 - cy), x2 - (x2 - cx));
        int aLen = 10;
        double aA = Math.toRadians(22);
        int ax1 = (int) (x2 - aLen * Math.cos(angle - aA));
        int ay1 = (int) (y2 - aLen * Math.sin(angle - aA));
        int ax2 = (int) (x2 - aLen * Math.cos(angle + aA));
        int ay2 = (int) (y2 - aLen * Math.sin(angle + aA));
        g2.fillPolygon(new int[] { x2, ax1, ax2 }, new int[] { y2, ay1, ay2 }, 3);
    }
}
