package com.jvm.visualizer.engine;

import com.jvm.visualizer.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser Engine — reads one instruction line at a time and dispatches to
 * HeapManager, StackManager, or GarbageCollector.
 *
 * Supported commands:
 *   create <id>               – create object on heap
 *   link   <from> <to>        – add reference from→to
 *   unlink <from> <to>        – remove reference
 *   push   <func> [id...]     – push stack frame with optional local refs
 *   pop                       – pop top stack frame
 *   remove <id>               – remove ref from top frame
 *   gc                        – run Mark-and-Sweep GC
 *   // ...                    – comment (ignored)
 */
public class Parser {

    private final HeapManager      heap;
    private final StackManager     stackMgr;
    private final GarbageCollector gc;

    public Parser(HeapManager heap, StackManager stackMgr, GarbageCollector gc) {
        this.heap     = heap;
        this.stackMgr = stackMgr;
        this.gc       = gc;
    }

    /**
     * Execute one raw instruction line.
     * @param rawLine the text of the line (may include leading/trailing whitespace or comments)
     * @return ParseResult describing what happened
     */
    public ParseResult executeLine(String rawLine) {
        if (rawLine == null) return noop("null");

        // Strip inline comment
        String line = rawLine.trim();
        int commentIdx = line.indexOf("//");
        if (commentIdx >= 0) line = line.substring(0, commentIdx).trim();

        if (line.isEmpty()) return noop("(empty line)");

        String[] parts = line.split("\\s+");
        String   cmd   = parts[0].toLowerCase();

        switch (cmd) {

            // ── create <id> ────────────────────────────────────────────────
            case "create": {
                if (parts.length < 2) return err("Usage: create <id>");
                String id = parts[1];
                if (heap.exists(id)) return err("Object '" + id + "' already exists in heap");
                heap.createObject(id);
                return ok("Created object '" + id + "' on heap", ParseResult.CommandType.CREATE);
            }

            // ── link <from> <to> ───────────────────────────────────────────
            case "link": {
                if (parts.length < 3) return err("Usage: link <from> <to>");
                String from = parts[1], to = parts[2];
                if (!heap.exists(from)) return err("Object '" + from + "' not found");
                if (!heap.exists(to))   return err("Object '" + to   + "' not found");
                heap.addReference(from, to);
                return ok("Linked: " + from + " → " + to, ParseResult.CommandType.LINK);
            }

            // ── unlink <from> <to> ─────────────────────────────────────────
            case "unlink": {
                if (parts.length < 3) return err("Usage: unlink <from> <to>");
                heap.removeReference(parts[1], parts[2]);
                return ok("Unlinked: " + parts[1] + " ↛ " + parts[2], ParseResult.CommandType.UNLINK);
            }

            // ── push <func> [ref1 ref2 ...] ────────────────────────────────
            case "push": {
                if (parts.length < 2) return err("Usage: push <funcName> [objRefs...]");
                String funcName = parts[1];
                List<String> refs = new ArrayList<>();
                for (int i = 2; i < parts.length; i++) {
                    String refId = parts[i];
                    if (!heap.exists(refId)) return err("Object '" + refId + "' not found (for push ref)");
                    refs.add(refId);
                }
                stackMgr.push(funcName, refs);
                String refsStr = refs.isEmpty() ? "(no local refs)" : "refs=" + refs;
                return ok("Pushed frame: " + funcName + "()  " + refsStr, ParseResult.CommandType.PUSH);
            }

            // ── pop ────────────────────────────────────────────────────────
            case "pop": {
                StackFrame f = stackMgr.pop();
                if (f == null) return err("Stack is empty — nothing to pop");
                return ok("Popped frame: " + f.getFunctionName() + "()", ParseResult.CommandType.POP);
            }

            // ── remove <id> ────────────────────────────────────────────────
            case "remove": {
                if (parts.length < 2) return err("Usage: remove <id>");
                String id = parts[1];
                stackMgr.removeRefFromTop(id);
                return ok("Removed ref '" + id + "' from top stack frame", ParseResult.CommandType.REMOVE);
            }

            // ── gc ─────────────────────────────────────────────────────────
            case "gc": {
                List<String> collected = gc.runGC();
                String msg = collected.isEmpty()
                        ? "GC complete — no garbage found"
                        : "GC complete — collected: " + collected;
                return new ParseResult(true, msg, ParseResult.CommandType.GC, collected);
            }

            // ── newarray <id> <type> <size> ────────────────────────────────
            case "newarray": {
                if (parts.length < 4) return err("Usage: newarray <id> <type> <size>");
                String id = parts[1];
                String type = parts[2];
                int size;
                try { size = Integer.parseInt(parts[3]); } catch (NumberFormatException e) { return err("Size must be integer"); }
                if (heap.exists(id)) return err("ID '" + id + "' already exists");
                if (!heap.createArray(id, type, size)) return err("Invalid array size (must be 1-64)");
                return ok("Created array '" + id + "'[" + size + "]", ParseResult.CommandType.NEWARRAY);
            }

            // ── astore <id> <index> <val> ──────────────────────────────────
            case "astore": {
                if (parts.length < 4) return err("Usage: astore <arr_id> <index> <val>");
                String id = parts[1];
                int index;
                try { index = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { return err("Index must be integer"); }
                String val = parts[3];
                if (!heap.arrayStore(id, index, val)) return err("Failed to store at index " + index + " in array '" + id + "'");
                return ok("Stored '" + val + "' in " + id + "[" + index + "]", ParseResult.CommandType.ASTORE);
            }

            // ── aload <id> <index> ─────────────────────────────────────────
            case "aload": {
                if (parts.length < 3) return err("Usage: aload <arr_id> <index>");
                String id = parts[1];
                int index;
                try { index = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { return err("Index must be integer"); }
                String val = heap.arrayLoad(id, index);
                if (val == null) return err("Failed to load from index " + index + " in array '" + id + "'");
                return ok("Loaded '" + val + "' from " + id + "[" + index + "]", ParseResult.CommandType.ALOAD);
            }

            default:
                return err("Unknown command: '" + cmd + "'. "
                         + "Valid: create, link, unlink, push, pop, remove, gc, newarray, astore, aload");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ParseResult ok(String msg, ParseResult.CommandType type) {
        return new ParseResult(true, msg, type);
    }

    private ParseResult err(String msg) {
        return new ParseResult(false, "⚠ " + msg, ParseResult.CommandType.ERROR);
    }

    private ParseResult noop(String info) {
        return new ParseResult(true, "skip: " + info, ParseResult.CommandType.NOOP);
    }
}
