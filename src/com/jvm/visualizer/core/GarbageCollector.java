package com.jvm.visualizer.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Mark-and-Sweep Garbage Collector.
 * Handles both SimObjects and SimArrays.
 * GC Roots = all object/array IDs held in any stack frame's localRefs.
 */
public class GarbageCollector {

    private final HeapManager  heap;
    private final StackManager stackMgr;

    public GarbageCollector(HeapManager heap, StackManager stackMgr) {
        this.heap     = heap;
        this.stackMgr = stackMgr;
    }

    /**
     * PHASE 1 – MARK.
     * Clears all marks, then DFS from GC roots to mark reachable objects AND arrays.
     */
    public void mark() {
        // Clear all object marks
        for (SimObject obj : heap.getObjects().values()) obj.setMarked(false);
        // Clear all array marks
        for (SimArray arr : heap.getArrays().values()) arr.setMarked(false);

        // DFS from all GC roots on the stack
        for (String rootId : stackMgr.getActiveReferences()) {
            markId(rootId);
        }
    }

    /** Mark an object or array reachable by ID, then recursively mark its references. */
    private void markId(String id) {
        if (id == null) return;

        // Is it an object?
        SimObject obj = heap.getObject(id);
        if (obj != null && !obj.isMarked()) {
            obj.setMarked(true);
            for (String refId : obj.getReferences()) markId(refId);
            return;
        }

        // Is it an array?
        SimArray arr = heap.getArray(id);
        if (arr != null && !arr.isMarked()) {
            arr.setMarked(true);
            // Array elements that are object references also become roots
            for (String elem : arr.getElements()) {
                if (elem != null && heap.exists(elem)) markId(elem);
            }
        }
    }

    /**
     * PHASE 2 – SWEEP.
     * Removes all unmarked objects AND arrays from the heap.
     * @return List of IDs that were collected.
     */
    public List<String> sweep() {
        List<String> collected = new ArrayList<>();

        // Sweep objects
        List<String> objKeys = new ArrayList<>(heap.getObjects().keySet());
        for (String id : objKeys) {
            SimObject obj = heap.getObject(id);
            if (obj != null && !obj.isMarked()) {
                heap.deleteObject(id);
                collected.add(id);
            }
        }

        // Sweep arrays
        List<String> arrKeys = new ArrayList<>(heap.getArrays().keySet());
        for (String id : arrKeys) {
            SimArray arr = heap.getArray(id);
            if (arr != null && !arr.isMarked()) {
                heap.deleteArray(id);
                collected.add(id + "[]");
            }
        }

        return collected;
    }

    /** Full GC: mark then sweep. */
    public List<String> runGC() {
        mark();
        return sweep();
    }
}

