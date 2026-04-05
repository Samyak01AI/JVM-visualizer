package com.jvm.visualizer.engine;

import java.util.List;

/**
 * Result returned by the Parser after executing one instruction line.
 */
public class ParseResult {

    public enum CommandType {
        CREATE, LINK, UNLINK, PUSH, POP, REMOVE,
        GC,      // GC fully ran (mark + sweep)
        GC_MARK, // First phase only — UI should repaint before sweep
        NEWARRAY, ASTORE, ALOAD,
        NOOP,    // Comment or empty line
        ERROR
    }

    public final boolean     success;
    public final String      message;
    public final CommandType type;
    public final List<String> gcCollected; // non-null only for GC commands

    public ParseResult(boolean success, String message, CommandType type) {
        this(success, message, type, null);
    }

    public ParseResult(boolean success, String message, CommandType type, List<String> gcCollected) {
        this.success      = success;
        this.message      = message;
        this.type         = type;
        this.gcCollected  = gcCollected;
    }
}
