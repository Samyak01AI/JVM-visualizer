package com.jvm.visualizer.core;

import java.util.*;

/**
 * Manages the simulated call stack of StackFrames.
 */
public class StackManager {

    private final Stack<StackFrame> stack = new Stack<>();

    /** Push a new stack frame with optional local object references. */
    public StackFrame push(String funcName, List<String> refs) {
        StackFrame frame = new StackFrame(funcName);
        for (String r : refs) frame.addRef(r);
        stack.push(frame);
        return frame;
    }

    /** Pop the top frame. Returns null if stack is empty. */
    public StackFrame pop() {
        return stack.isEmpty() ? null : stack.pop();
    }

    /** Remove a reference ID from the top frame only. */
    public void removeRefFromTop(String id) {
        if (!stack.isEmpty()) stack.peek().removeRef(id);
    }

    /** Collect all referenced object IDs across ALL stack frames (GC roots). */
    public List<String> getActiveReferences() {
        Set<String> all = new LinkedHashSet<>();
        for (StackFrame f : stack) all.addAll(f.getLocalRefs());
        return new ArrayList<>(all);
    }

    public Stack<StackFrame> getStack()   { return stack; }
    public boolean           isEmpty()    { return stack.isEmpty(); }
    public int               depth()      { return stack.size(); }
    public void              reset()      { stack.clear(); }
}
