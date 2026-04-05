package com.jvm.visualizer.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a simulated object on the Heap.
 * Identified by a string ID (e.g. "A", "B", "node1").
 */
public class SimObject {
    private final String id;
    private final List<String> references; // IDs of objects this object refers to
    private boolean marked;                // GC mark flag
    // Visual layout position (set by HeapPanel)
    private int x, y;

    public SimObject(String id) {
        this.id         = id;
        this.references = new ArrayList<>();
        this.marked     = false;
    }

    public void addReference(String targetId) {
        if (targetId != null && !references.contains(targetId))
            references.add(targetId);
    }

    public void removeReference(String targetId) {
        references.remove(targetId);
    }

    public String        getId()          { return id; }
    public List<String>  getReferences()  { return references; }
    public boolean       isMarked()       { return marked; }
    public void          setMarked(boolean m) { this.marked = m; }
    public int           getX()           { return x; }
    public int           getY()           { return y; }
    public void          setX(int x)      { this.x = x; }
    public void          setY(int y)      { this.y = y; }

    @Override public String toString() { return "Obj[" + id + "]"; }
}
