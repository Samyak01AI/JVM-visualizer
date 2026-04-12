package com.jvm.visualizer.ui;

import com.jvm.visualizer.core.*;
import com.jvm.visualizer.engine.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;


/**
 * Main application window.
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │ HEADER: title + instruction set reference │
 * ├─────────────────────────┬────────────────────────────────────────┤
 * │ LEFT │ RIGHT │
 * │ ┌─────────────────┐ │ ┌─────────────────────────────────┐ │
 * │ │ Code Editor │ │ │ 📦 Heap Memory │ │
 * │ │ (syntax color) │ │ │ (custom painted nodes+arrows) │ │
 * │ │ (line numbers) │ │ └─────────────────────────────────┘ │
 * │ └─────────────────┘ │ ┌─────────────────────────────────┐ │
 * │ │ │ 📚 Call Stack │ │
 * │ [▶ Run] [⏭ Step] [🔄] │ └─────────────────────────────────┘ │
 * │ ───────────────────── │ │
 * │ Event Log │ │
 * ├─────────────────────────┴────────────────────────────────────────┤
 * │ STATS: heap objects | stack depth | GC roots | status │
 * └──────────────────────────────────────────────────────────────────┘
 */
public class MainFrame extends JFrame {

    // ── Simulation Engine ─────────────────────────────────────────────────────
    private final HeapManager heap;
    private final StackManager stackMgr;
    private final GarbageCollector gc;
    private final Parser parser;

    // ── UI Panels ─────────────────────────────────────────────────────────────
    private final CodeEditorPanel editorPanel;
    private final HeapPanel heapPanel;
    private final StackPanel stackPanel;
    private final LogPanel logPanel;
    private final StatsPanel statsPanel;

    // ── Step execution state ──────────────────────────────────────────────────
    private int currentStepLine = 0;
    private List<String> effectiveLines = null; // translated (or raw) lines for current run
    private boolean lastRunWasJava = false;

    public MainFrame() {
        super("🧠 JVM Memory Management Simulator");

        // Build engine
        heap = new HeapManager();
        stackMgr = new StackManager();
        gc = new GarbageCollector(heap, stackMgr);
        parser = new Parser(heap, stackMgr, gc);

        // Build panels
        editorPanel = new CodeEditorPanel();
        heapPanel = new HeapPanel(heap);
        stackPanel = new StackPanel(stackMgr);
        logPanel = new LogPanel();
        statsPanel = new StatsPanel(heap, stackMgr);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 740));
        getContentPane().setBackground(Theme.BG_DARK);

        buildLayout();
        pack();
        setLocationRelativeTo(null);
    }

    // ─── Layout ───────────────────────────────────────────────────────────────

    private void buildLayout() {
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenterSplit(), BorderLayout.CENTER);
        add(statsPanel, BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_DARK);
        header.setPreferredSize(new Dimension(0, 50));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        JLabel title = new JLabel("  🧠  JVM Memory Management Simulator");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        // Quick reference label
        JLabel hint = new JLabel(
                "☕ Java code  OR  ⚙ sim-script auto-detected  │  create A  │  link A B  │  push main A  │  gc    ");
        hint.setFont(Theme.FONT_MONO_SM);
        hint.setForeground(Theme.TEXT_MUTED);
        header.add(hint, BorderLayout.EAST);

        return header;
    }

    private JSplitPane buildCenterSplit() {
        // ── LEFT: Editor + Controls + Log ─────────────────────────────────
        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setBackground(Theme.BG_DARK);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 3));

        JPanel editorWrapper = titled("📝  Code Editor", editorPanel);
        JPanel controlBar = buildControlBar();
        JPanel logWrapper = titled("📋  Event Log", logPanel);

        leftPanel.add(editorWrapper, BorderLayout.CENTER);
        leftPanel.add(controlBar, BorderLayout.SOUTH);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, leftPanel, logWrapper);
        leftSplit.setDividerLocation(420);
        leftSplit.setDividerSize(5);
        leftSplit.setBorder(null);
        leftSplit.setBackground(Theme.BG_DARK);
        leftSplit.setResizeWeight(0.7);

        JPanel leftOuter = new JPanel(new BorderLayout());
        leftOuter.setBackground(Theme.BG_DARK);
        leftOuter.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 3));
        leftOuter.add(leftSplit, BorderLayout.CENTER);

        // ── RIGHT: Heap + Stack ────────────────────────────────────────────
        JScrollPane heapScroll = new JScrollPane(heapPanel);
        heapScroll.setBorder(BorderFactory.createEmptyBorder());
        heapScroll.getViewport().setBackground(Theme.BG_PANEL);
        JPanel heapWrapper = titled("📦  Heap Memory", heapScroll);

        JScrollPane stackScroll = new JScrollPane(stackPanel);
        stackScroll.setBorder(BorderFactory.createEmptyBorder());
        stackScroll.getViewport().setBackground(Theme.BG_PANEL);
        JPanel stackWrapper = titled("📚  Call Stack", stackScroll);

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, heapWrapper, stackWrapper);
        rightSplit.setDividerLocation(400);
        rightSplit.setDividerSize(5);
        rightSplit.setBorder(null);
        rightSplit.setBackground(Theme.BG_DARK);
        rightSplit.setResizeWeight(0.6);

        JPanel rightOuter = new JPanel(new BorderLayout());
        rightOuter.setBackground(Theme.BG_DARK);
        rightOuter.setBorder(BorderFactory.createEmptyBorder(6, 3, 6, 6));
        rightOuter.add(rightSplit, BorderLayout.CENTER);

        // ── Main horizontal split ──────────────────────────────────────────
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftOuter, rightOuter);
        mainSplit.setDividerLocation(480);
        mainSplit.setDividerSize(5);
        mainSplit.setBorder(null);
        mainSplit.setBackground(Theme.BG_DARK);
        mainSplit.setResizeWeight(0.4);
        return mainSplit;
    }

    private JPanel buildControlBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bar.setBackground(Theme.BG_DARK);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));

        JButton runBtn = styledBtn("▶  Run All", Theme.ACCENT_BLUE, 190, 34);
        JButton stepBtn = styledBtn("⏭  Step", Theme.ACCENT_PURPLE, 120, 34);
        JButton resetBtn = styledBtn("🔄  Reset", Theme.TEXT_MUTED, 100, 34);

        JLabel lineLabel = new JLabel("Line: —");
        lineLabel.setFont(Theme.FONT_MONO_SM);
        lineLabel.setForeground(Theme.TEXT_MUTED);

        runBtn.addActionListener(e -> runAll(lineLabel));
        stepBtn.addActionListener(e -> step(lineLabel));
        resetBtn.addActionListener(e -> reset(lineLabel));

        bar.add(runBtn);
        bar.add(stepBtn);
        bar.add(resetBtn);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(lineLabel);
        return bar;
    }

    // ─── Execution Logic ──────────────────────────────────────────────────────

    /**
     * Prepare execution: auto-detect Java vs sim-script, translate if needed.
     * Populates effectiveLines.
     */
    private void prepareExecution() {
        List<String> editorLines = editorPanel.getLines();
        lastRunWasJava = JavaToSimTranslator.isJavaCode(editorLines);

        if (lastRunWasJava) {
            JavaToSimTranslator translator = new JavaToSimTranslator();
            effectiveLines = translator.translate(editorLines);

            logPanel.log("info", "☕  Java code detected — translating to " + effectiveLines.size() + " sim commands");
            logPanel.log("info", "── Translated script ──────────────────────────────────────────");
            int lineNum = 1;
            for (String ln : effectiveLines) {
                String t = ln.trim();
                if (!t.isEmpty()) {
                    if (t.startsWith("//"))
                        logPanel.log("ready", "  " + t);
                    else
                        logPanel.log("ok", "  [" + lineNum++ + "] " + t);
                }
            }
            logPanel.log("info", "── Running translated script ───────────────────────────────────");
        } else {
            effectiveLines = editorLines;
        }
    }

    /** Execute ALL lines from top, with GC animation for gc commands. */
    private void runAll(JLabel lineLabel) {
        reset(lineLabel);
        prepareExecution();

        String modeTag = lastRunWasJava ? " [☕ Java → Sim]" : " [⚙ Script]";
        logPanel.log("info", "▶ Running " + effectiveLines.size() + " line(s)..." + modeTag);
        editorPanel.clearHighlight();

        for (int i = 0; i < effectiveLines.size(); i++) {
            // Only highlight editor line when in sim mode (direct mapping)
            if (!lastRunWasJava)
                editorPanel.setCurrentLine(i);
            execLine(effectiveLines.get(i), i + 1);
        }

        editorPanel.clearHighlight();
        statsPanel.setStatus("✔ Execution complete" + modeTag);
        lineLabel.setText("Done");
        refreshAll();
    }

    /** Execute one line at a time. Maintains step position across calls. */
    private void step(JLabel lineLabel) {
        // On first step of a new session, translate if needed
        if (currentStepLine == 0 || effectiveLines == null) {
            prepareExecution();
            String modeTag = lastRunWasJava ? " [☕ Java → Sim]" : " [⚙ Script]";
            logPanel.log("info", "⏭ Step mode" + modeTag);
        }

        if (currentStepLine >= effectiveLines.size()) {
            logPanel.log("info", "⏭ All lines executed. Click 🔄 Reset to start over.");
            statsPanel.setStatus("✔ Done");
            return;
        }

        String line = effectiveLines.get(currentStepLine);
        // Only highlight editor if in sim mode (direct line mapping)
        if (!lastRunWasJava)
            editorPanel.setCurrentLine(currentStepLine);
        lineLabel.setText("Line: " + (currentStepLine + 1) + " / " + effectiveLines.size());
        statsPanel.setStatus("Stepped to line " + (currentStepLine + 1));

        execLine(line, currentStepLine + 1);
        currentStepLine++;
        refreshAll();
    }

    /**
     * Execute one instruction line, handling special GC animation.
     * For GC: show mark phase (green/red), then sweep after 900 ms.
     */
    private void execLine(String line, int lineNum) {
        String trimmed = line.trim();

        // Check if this is a GC command to trigger animation
        if (!trimmed.startsWith("//") && trimmed.split("\\s+")[0].equalsIgnoreCase("gc")) {
            // Phase 1: Mark
            gc.mark();
            heapPanel.setGcMarkPhase(true);
            heapPanel.repaint();
            logPanel.log("gc", "[Line " + lineNum + "] GC MARK PHASE — green=reachable, red=garbage");

            // Phase 2: Sweep after short delay (only in step mode; run-all does it inline)
            Timer sweepTimer = new Timer(900, ev -> {
                List<String> collected = gc.sweep();
                heapPanel.setGcMarkPhase(false);
                String msg = collected.isEmpty()
                        ? "GC SWEEP — nothing to collect"
                        : "GC SWEEP — collected: " + collected;
                logPanel.log("gc", msg);
                refreshAll();
            });
            sweepTimer.setRepeats(false);
            sweepTimer.start();
            return;
        }

        // All other commands
        ParseResult result = parser.executeLine(line);
        String level = result.success
                ? (result.type == ParseResult.CommandType.NOOP ? "ready" : "ok")
                : "error";
        if (result.type != ParseResult.CommandType.NOOP) {
            logPanel.log(level, "[" + lineNum + "] " + result.message);
        }
    }

    private void reset(JLabel lineLabel) {
        heap.reset();
        stackMgr.reset();
        heapPanel.setGcMarkPhase(false);
        currentStepLine = 0;
        effectiveLines = null;
        lastRunWasJava = false;
        editorPanel.clearHighlight();
        logPanel.clear();
        logPanel.log("info", "🔄 Simulation reset.");
        statsPanel.setStatus("Ready");
        if (lineLabel != null)
            lineLabel.setText("Line: —");
        refreshAll();
    }

    private void refreshAll() {
        heapPanel.revalidate();
        heapPanel.repaint();
        stackPanel.repaint();
        statsPanel.refresh();
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private JPanel titled(String title, JComponent inner) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG_DARK);

        JLabel lbl = new JLabel("  " + title);
        lbl.setFont(Theme.FONT_HEADING);
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setBackground(Theme.BG_DARK);
        lbl.setOpaque(true);
        lbl.setPreferredSize(new Dimension(0, 26));
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(inner, BorderLayout.CENTER);
        wrapper.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        return wrapper;
    }

    private JButton styledBtn(String text, Color accent, int w, int h) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? accent.darker().darker()
                        : getModel().isRollover() ? accent.brighter()
                                : accent;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        b.setFont(Theme.FONT_LABEL.deriveFont(Font.BOLD));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(w, h));
        return b;
    }
}
