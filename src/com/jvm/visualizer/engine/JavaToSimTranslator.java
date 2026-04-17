package com.jvm.visualizer.engine;

import java.util.*;
import java.util.regex.*;


/**
 * Translates a subset of real Java source code into JVM Visualizer sim-script.
 *
 * Supported Java patterns:
 *   Type name = new Type(...)     →  create name
 *   Type[] name = new Type[N]     →  newarray name Type N
 *   a.field = b                   →  link a b
 *   a.field = null                →  (commented unlink)
 *   name = null                   →  remove name
 *   arr[i] = val                  →  astore arr i val
 *   void/Type method(params) {    →  push method [obj-params]
 *   }  (closing method)           →  pop
 *   System.gc()                   →  gc
 *
 * Also detects whether input looks like Java or native sim-script.
 */
public class JavaToSimTranslator {

    // ── Known sim-script commands ──────────────────────────────────────────────
    public static final Set<String> SIM_COMMANDS = new HashSet<>(Arrays.asList(
            "create", "link", "unlink", "push", "pop", "remove",
            "gc", "newarray", "astore", "aload"
    ));

    // ── Primitive/wrapper types – we don't create heap objects for these ───────
    private static final Set<String> PRIMITIVE_TYPES = new HashSet<>(Arrays.asList(
            "int", "long", "double", "float", "boolean", "char", "byte", "short",
            "String", "Integer", "Long", "Double", "Float", "Boolean",
            "Character", "Byte", "Short", "Number"
    ));

    // ── Regex patterns ─────────────────────────────────────────────────────────

    /** Type[] name = new Type[N];   or   Type name[] = new Type[N]; */
    private static final Pattern PAT_ARRAY_CREATION = Pattern.compile(
        "(?:\\w+\\[\\]|\\w+\\s*\\[\\s*\\])\\s+(\\w+)\\s*=\\s*new\\s+(\\w+)\\s*\\[(\\d+)\\]"
    );

    /** Type name = new Type(...); */
    private static final Pattern PAT_OBJ_CREATION = Pattern.compile(
        "^(\\w+)\\s+(\\w+)\\s*=\\s*new\\s+\\w+\\s*\\("
    );

    /** name = new Type(...);    (re-assignment, no type declaration) */
    private static final Pattern PAT_REASSIGN_OBJ = Pattern.compile(
        "^(\\w+)\\s*=\\s*new\\s+\\w+\\s*\\("
    );

    /** a.field = null; */
    private static final Pattern PAT_NULL_FIELD = Pattern.compile(
        "(\\w+)\\.(\\w+)\\s*=\\s*null\\s*;"
    );

    /** name = null; */
    private static final Pattern PAT_NULL_VAR = Pattern.compile(
        "^(\\w+)\\s*=\\s*null\\s*;"
    );

    /** arr[i] = val; */
    private static final Pattern PAT_ARRAY_STORE = Pattern.compile(
        "(\\w+)\\[(\\d+)\\]\\s*=\\s*([\\w\"'.]+)\\s*;"
    );

    /** a.field = b;  (reference assignment) */
    private static final Pattern PAT_FIELD_ASSIGN = Pattern.compile(
        "(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\s*;"
    );

    /** Method / constructor declaration ending with { */
    private static final Pattern PAT_METHOD_DECL = Pattern.compile(
        "^(?:(?:public|private|protected|static|final|synchronized|abstract|native|strictfp)\\s+)*" +
        "(?:<[^>]+>\\s+)?" +   // optional generics
        "(\\w[\\w<>\\[\\]]*+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:throws\\s+[\\w,\\s]+)?\\{"
    );

    /** Standalone closing brace line */
    private static final Pattern PAT_CLOSE_BRACE = Pattern.compile("^\\}\\s*$");

    /** For-each / for loop line (skip) */
    private static final Pattern PAT_FOR = Pattern.compile(
        "^for\\s*\\(.*\\)\\s*\\{?$"
    );

    /** Field declaration without initializer: Type name; */
    private static final Pattern PAT_FIELD_DECL = Pattern.compile(
        "^(?:(?:public|private|protected|static|final)\\s+)*(\\w+)\\s+(\\w+)\\s*;"
    );

    // ── Translator state ───────────────────────────────────────────────────────
    private final Set<String>    objectVars      = new LinkedHashSet<>();
    private final Set<String>    arrayVars       = new LinkedHashSet<>();
    private final Deque<Integer> methodDepths    = new ArrayDeque<>(); // brace-depth at open
    private final Deque<String>  methodNames     = new ArrayDeque<>();
    private int braceDepth = 0;

    /**
     * After calling translate(), this list has one entry per output line.
     * Each entry is the 0-based index of the Java source line that produced it,
     * or -1 for the header comment line.
     */
    private final List<Integer> sourceLineMap = new ArrayList<>();

    /** Returns the source-line map produced by the last translate() call. */
    public List<Integer> getSourceLineMap() { return Collections.unmodifiableList(sourceLineMap); }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Heuristic: returns true when the lines look more like Java than sim-script.
     */
    public static boolean isJavaCode(List<String> lines) {
        int simScore  = 0;
        int javaScore = 0;

        for (String raw : lines) {
            String line  = raw.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;

            // Sim-script first token
            String first = line.split("\\s+")[0].toLowerCase();
            if (SIM_COMMANDS.contains(first)) simScore += 2;

            // Java indicators
            if (line.endsWith(";"))                              javaScore += 2;
            if (line.matches(".*\\bnew\\s+\\w+.*"))             javaScore += 2;
            if (line.matches(".*\\b(class|interface|enum)\\b.*")) javaScore += 3;
            if (line.matches(".*\\b(void|public|private|protected|static|final|return)\\b.*")) javaScore += 2;
            if (line.contains("System.gc()"))                    javaScore += 3;
            if (line.matches(".*\\.\\w+\\s*=.*"))               javaScore++;
            if (line.matches(".*\\w+\\s*\\[\\d*\\].*"))         javaScore++;
        }
        return javaScore > simScore;
    }

    /**
     * Translate Java source lines into sim-script lines.
     * Unrecognised lines are emitted as // comments.
     */
    public List<String> translate(List<String> javaLines) {
        // Reset state
        objectVars.clear();
        arrayVars.clear();
        methodDepths.clear();
        methodNames.clear();
        braceDepth = 0;
        sourceLineMap.clear();

        List<String> result = new ArrayList<>();
        // Header line maps to no source line
        result.add("// ── Auto-translated from Java ──────────────────────────");
        sourceLineMap.add(-1);

        for (int srcIdx = 0; srcIdx < javaLines.size(); srcIdx++) {
            String raw  = javaLines.get(srcIdx);
            String line = raw.trim();

            /* empty */
            if (line.isEmpty()) {
                result.add("");
                sourceLineMap.add(srcIdx);
                continue;
            }

            /* pass-through comments */
            if (line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
                result.add(line);
                sourceLineMap.add(srcIdx);
                continue;
            }

            /* skip package / import / annotations */
            if (line.startsWith("package ") || line.startsWith("import ") || line.startsWith("@")) continue;

            /* skip class/interface declaration lines */
            if (line.matches(".*\\b(class|interface|enum)\\b.*")) {
                if (line.endsWith("{")) braceDepth++;
                continue;
            }

            /* translate — each emitted sim line maps back to srcIdx */
            List<String> translated = translateLine(line);
            for (String t : translated) {
                result.add(t);
                sourceLineMap.add(srcIdx);
            }
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE TRANSLATION LOGIC
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> translateLine(String line) {
        List<String> out = new ArrayList<>();
        Matcher m;

        // ── System.gc() ───────────────────────────────────────────────────────
        if (line.contains("System.gc()")) {
            out.add("gc");
            return out;
        }

        // ── Array creation: Type[] name = new Type[N] ─────────────────────────
        m = PAT_ARRAY_CREATION.matcher(line);
        if (m.find()) {
            String name     = m.group(1);
            String elemType = m.group(2);
            String size     = m.group(3);
            arrayVars.add(name);
            out.add("newarray " + name + " " + elemType + " " + size);
            processBracesInLine(line, out);
            return out;
        }

        // ── Object creation: Type name = new Type(...) ────────────────────────
        m = PAT_OBJ_CREATION.matcher(line);
        if (m.find()) {
            String declType = m.group(1);
            String name     = m.group(2);
            if (!PRIMITIVE_TYPES.contains(declType) && !declType.equals("var")) {
                objectVars.add(name);
                out.add("create " + name);
            } else {
                out.add("// skip: primitive " + declType + " " + name);
            }
            return out;
        }

        // ── Reassign object: name = new Type(...) ────────────────────────────
        m = PAT_REASSIGN_OBJ.matcher(line);
        if (m.find()) {
            String name = m.group(1);
            if (objectVars.contains(name) || !PRIMITIVE_TYPES.contains(name)) {
                objectVars.add(name);
                out.add("create " + name);
            }
            return out;
        }

        // ── Null field: a.field = null ────────────────────────────────────────
        m = PAT_NULL_FIELD.matcher(line);
        if (m.find()) {
            String obj   = m.group(1);
            String field = m.group(2);
            out.add("// unlink " + obj + " " + field + "  (field set to null)");
            return out;
        }

        // ── Null var: name = null ─────────────────────────────────────────────
        m = PAT_NULL_VAR.matcher(line);
        if (m.find()) {
            String var = m.group(1);
            if (objectVars.contains(var) || arrayVars.contains(var)) {
                out.add("remove " + var);
            }
            return out;
        }

        // ── Array store: arr[i] = val ─────────────────────────────────────────
        m = PAT_ARRAY_STORE.matcher(line);
        if (m.find()) {
            String arrName = m.group(1);
            String index   = m.group(2);
            String val     = m.group(3);
            if (arrayVars.contains(arrName)) {
                out.add("astore " + arrName + " " + index + " " + val);
                return out;
            }
        }

        // ── Field reference assignment: a.field = b ──────────────────────────
        m = PAT_FIELD_ASSIGN.matcher(line);
        if (m.find()) {
            String from  = m.group(1);
            String field = m.group(2);
            String val   = m.group(3);
            if (objectVars.contains(val) || arrayVars.contains(val)) {
                out.add("link " + from + " " + val);
            } else {
                out.add("// field: " + from + "." + field + " = " + val);
            }
            return out;
        }

        // ── Closing brace only ────────────────────────────────────────────────
        m = PAT_CLOSE_BRACE.matcher(line);
        if (m.matches()) {
            popIfMethodDepth(out);
            braceDepth = Math.max(0, braceDepth - 1);
            return out;
        }

        // ── For / while / if blocks (skip, but track braces) ─────────────────
        if (line.matches("^(?:for|while|if|else|switch|try|catch|finally)\\b.*")) {
            processBracesInLine(line, out);
            return out;
        }

        // ── Method / constructor declaration ──────────────────────────────────
        m = PAT_METHOD_DECL.matcher(line);
        if (m.find()) {
            String methName = m.group(2);
            String params   = m.group(3).trim();

            braceDepth++;
            methodDepths.push(braceDepth);
            methodNames.push(methName);

            // Collect object/array params as GC roots
            List<String> refParams = extractObjectParams(params);
            refParams.forEach(p -> { objectVars.add(p); });

            StringBuilder sb = new StringBuilder("push ").append(methName);
            for (String r : refParams) sb.append(" ").append(r);
            out.add(sb.toString());
            return out;
        }

        // ── Field declaration: Type name; or ReturnType name = prim; ─────────
        m = PAT_FIELD_DECL.matcher(line);
        if (m.find()) {
            // just track; don't emit anything for bare field decls
            return out;
        }

        // ── Unrecognized — emit as comment ────────────────────────────────────
        processBracesInLine(line, out);
        if (!line.isEmpty() && out.isEmpty()) {
            out.add("// " + line);
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Pop if this closing brace returns to a method's opening depth. */
    private void popIfMethodDepth(List<String> out) {
        if (!methodDepths.isEmpty() && methodDepths.peek() == braceDepth) {
            methodDepths.pop();
            String name = methodNames.isEmpty() ? "unknown" : methodNames.pop();
            out.add("pop  // end of " + name + "()");
        }
    }

    /** Count { and } in a line that isn't itself a pure brace line. */
    private void processBracesInLine(String line, List<String> out) {
        for (char c : line.toCharArray()) {
            if (c == '{') {
                braceDepth++;
            } else if (c == '}') {
                popIfMethodDepth(out);
                braceDepth = Math.max(0, braceDepth - 1);
            }
        }
    }

    /**
     * From a parameter list string like "Node a, int[] arr, String s"
     * extract names of non-primitive parameters to use as GC roots.
     */
    private List<String> extractObjectParams(String params) {
        List<String> result = new ArrayList<>();
        if (params.isEmpty()) return result;
        for (String param : params.split(",")) {
            param = param.trim();
            if (param.isEmpty()) continue;
            String[] parts = param.split("\\s+");
            if (parts.length < 2) continue;
            String type  = parts[0].replaceAll("[\\[\\]]", "");
            String pname = parts[parts.length - 1].replaceAll("[\\[\\]]", "");
            boolean isArr = param.contains("[]");
            if (isArr || !PRIMITIVE_TYPES.contains(type)) {
                if (isArr) arrayVars.add(pname);
                result.add(pname);
            }
        }
        return result;
    }
}
