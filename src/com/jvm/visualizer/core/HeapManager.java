package com.jvm.visualizer.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages the simulated Heap: both SimObjects and SimArrays.
 */
public class HeapManager {

    private final Map<String, SimObject> objects = new LinkedHashMap<>();
    private final Map<String, SimArray>  arrays  = new LinkedHashMap<>();

    // ─── Object Operations ────────────────────────────────────────────────────

    /** Create a new object. Returns false if id already exists (object or array). */
    public boolean createObject(String id) {
        if (exists(id)) return false;
        objects.put(id, new SimObject(id));
        return true;
    }

    public boolean addReference(String fromId, String toId) {
        SimObject from = objects.get(fromId);
        SimObject to   = objects.get(toId);
        if (from == null || to == null) return false;
        from.addReference(toId);
        return true;
    }

    public boolean removeReference(String fromId, String toId) {
        SimObject from = objects.get(fromId);
        if (from == null) return false;
        from.removeReference(toId);
        return true;
    }

    public boolean deleteObject(String id) {
        if (!objects.containsKey(id)) return false;
        objects.remove(id);
        for (SimObject obj : objects.values()) obj.removeReference(id);
        // Also null out array slots pointing to this object
        for (SimArray arr : arrays.values()) {
            for (int i = 0; i < arr.getSize(); i++) {
                if (id.equals(arr.get(i))) arr.set(i, null);
            }
        }
        return true;
    }

    // ─── Array Operations ─────────────────────────────────────────────────────

    /**
     * Create a new array on the heap.
     * @param id           identifier (e.g. "arr")
     * @param elementType  declared type (e.g. "int", "String", "Object")
     * @param size         number of slots
     * @return false if id already exists
     */
    public boolean createArray(String id, String elementType, int size) {
        if (exists(id)) return false;
        if (size <= 0 || size > 64) return false; // guard
        arrays.put(id, new SimArray(id, elementType, size));
        return true;
    }

    /**
     * Store a value (object ID or primitive) at array[index].
     * If value is an object ID that exists, it's treated as a reference.
     */
    public boolean arrayStore(String arrId, int index, String value) {
        SimArray arr = arrays.get(arrId);
        if (arr == null) return false;
        return arr.set(index, value);
    }

    /**
     * Load value from array[index]. Returns null if empty/OOB.
     */
    public String arrayLoad(String arrId, int index) {
        SimArray arr = arrays.get(arrId);
        if (arr == null) return null;
        return arr.get(index);
    }

    public boolean deleteArray(String id) {
        return arrays.remove(id) != null;
    }

    // ─── Shared ───────────────────────────────────────────────────────────────

    /** Returns true if either an object or array with this ID exists. */
    public boolean exists(String id) {
        return objects.containsKey(id) || arrays.containsKey(id);
    }

    public SimObject              getObject(String id)  { return objects.get(id); }
    public SimArray               getArray(String id)   { return arrays.get(id); }
    public Map<String, SimObject> getObjects()          { return objects; }
    public Map<String, SimArray>  getArrays()           { return arrays; }

    public int size() { return objects.size() + arrays.size(); }

    public void reset() {
        objects.clear();
        arrays.clear();
    }
}

