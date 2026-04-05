package com.jvm.visualizer.core;

/**
 * Represents a simulated Array on the Heap.
 * Each slot can hold either a primitive value string or an object reference ID.
 *
 * Commands:
 *   newarray <id> <type> <size>   e.g.  newarray arr int 5
 *   astore   <id> <index> <val>   e.g.  astore arr 0 A
 *   aload    <id> <index>         e.g.  aload arr 0
 */
public class SimArray {
    private final String   id;
    private final String   elementType;
    private final int      size;
    private final String[] elements;  // null = empty slot; otherwise objId or primitive string
    private boolean        marked;
    // Visual layout position set by HeapPanel
    private int x, y;

    public SimArray(String id, String elementType, int size) {
        this.id          = id;
        this.elementType = elementType;
        this.size        = size;
        this.elements    = new String[size];
        this.marked      = false;
    }

    /**
     * Store a value (object ID or primitive) at the given index.
     * @return false if index is out of bounds.
     */
    public boolean set(int index, String value) {
        if (index < 0 || index >= size) return false;
        elements[index] = value;
        return true;
    }

    /**
     * Load value at given index.
     * @return null if empty or out of bounds.
     */
    public String get(int index) {
        if (index < 0 || index >= size) return null;
        return elements[index];
    }

    public String   getId()           { return id; }
    public String   getElementType()  { return elementType; }
    public int      getSize()         { return size; }
    public String[] getElements()     { return elements; }
    public boolean  isMarked()        { return marked; }
    public void     setMarked(boolean m) { this.marked = m; }
    public int      getX()            { return x; }
    public int      getY()            { return y; }
    public void     setX(int x)       { this.x = x; }
    public void     setY(int y)       { this.y = y; }

    @Override public String toString() { return id + "[" + size + "]"; }
}
