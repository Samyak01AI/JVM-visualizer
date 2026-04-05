package com.jvm.visualizer;

import com.jvm.visualizer.ui.MainFrame;
import com.jvm.visualizer.ui.Theme;

import javax.swing.*;

/** Application entry point. */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Global dark theme defaults
        UIManager.put("Panel.background",            Theme.BG_DARK);
        UIManager.put("ScrollPane.background",       Theme.BG_PANEL);
        UIManager.put("Viewport.background",         Theme.BG_PANEL);
        UIManager.put("ScrollBar.thumb",             Theme.BG_CARD);
        UIManager.put("ScrollBar.track",             Theme.BG_DARK);
        UIManager.put("SplitPane.background",        Theme.BG_DARK);
        UIManager.put("SplitPaneDivider.background", Theme.BORDER);
        UIManager.put("TextField.background",        Theme.BG_EDITOR);
        UIManager.put("TextField.foreground",        Theme.TEXT_PRIMARY);
        UIManager.put("TextArea.background",         Theme.BG_EDITOR);
        UIManager.put("TextArea.foreground",         Theme.TEXT_PRIMARY);
        UIManager.put("TextPane.background",         Theme.BG_EDITOR);
        UIManager.put("TextPane.foreground",         Theme.TEXT_PRIMARY);
        UIManager.put("Label.foreground",            Theme.TEXT_PRIMARY);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
