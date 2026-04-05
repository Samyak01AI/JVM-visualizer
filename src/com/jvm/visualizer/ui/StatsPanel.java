package com.jvm.visualizer.ui;

import com.jvm.visualizer.core.*;

import javax.swing.*;
import java.awt.*;

/** Stats bar at the bottom of the window. */
public class StatsPanel extends JPanel {

    private final HeapManager  heap;
    private final StackManager stackMgr;

    private final JLabel heapObjLabel;
    private final JLabel stackDepthLabel;
    private final JLabel gcRootsLabel;
    private final JLabel statusLabel;

    public StatsPanel(HeapManager heap, StackManager stackMgr) {
        this.heap     = heap;
        this.stackMgr = stackMgr;

        setBackground(Theme.BG_DARK);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        setLayout(new FlowLayout(FlowLayout.LEFT, 24, 5));
        setPreferredSize(new Dimension(0, 32));

        heapObjLabel    = lbl("Heap Objects: 0");
        stackDepthLabel = lbl("Stack Depth: 0");
        gcRootsLabel    = lbl("GC Roots: 0");
        statusLabel     = lbl(" ");
        statusLabel.setForeground(Theme.ACCENT_BLUE);

        add(muted("◈  HEAP"));
        add(heapObjLabel);
        add(sep());
        add(muted("◈  STACK"));
        add(stackDepthLabel);
        add(sep());
        add(muted("◈  GC ROOTS"));
        add(gcRootsLabel);
        add(sep());
        add(statusLabel);
    }

    public void refresh() {
        heapObjLabel.setText("Objects: " + heap.size());
        stackDepthLabel.setText("Depth: " + stackMgr.depth());
        gcRootsLabel.setText("Roots: " + stackMgr.getActiveReferences().size());
    }

    public void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Theme.FONT_MONO_SM);
        l.setForeground(Theme.TEXT_PRIMARY);
        return l;
    }

    private JLabel muted(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private JLabel sep() {
        JLabel l = new JLabel("│");
        l.setForeground(Theme.BORDER);
        return l;
    }
}
