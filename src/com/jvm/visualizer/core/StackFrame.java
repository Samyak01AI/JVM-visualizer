package com.jvm.visualizer.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one frame on the simulated call stack.
 * Holds the method/function name and object IDs that are local references.
 */
public class StackFrame {
    private final String       functionName;
    private final List<String> localRefs;   // object IDs held as local variables

    public StackFrame(String functionName) {
        this.functionName = functionName;
        this.localRefs    = new ArrayList<>();
    }

    public void addRef(String id) {
        if (id != null && !localRefs.contains(id)) localRefs.add(id);
    }

    public void removeRef(String id) { localRefs.remove(id); }

    public String       getFunctionName() { return functionName; }
    public List<String> getLocalRefs()    { return localRefs; }

    @Override public String toString() { return functionName + "()"; }
}
