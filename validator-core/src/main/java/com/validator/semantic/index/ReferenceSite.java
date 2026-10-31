package com.validator.semantic.index;

import java.util.List;

/**
 * A name in a validated file that must resolve: an import target, a type in a typing (':'),
 * a supertype of a definition (':>'), or a metadata annotation ('@T', '#T').
 */
public final class ReferenceSite {

    /** What the name is used for; determines the wording of the error. */
    public enum Kind { IMPORT, TYPE, SPECIALIZATION, METADATA, REFERENCE }

    private final Kind kind;
    private final List<String> segments;
    private final String text;
    private final String scope;
    private final int line;
    private final int column;

    public ReferenceSite(Kind kind, List<String> segments, String text, String scope, int line, int column) {
        this.kind = kind;
        this.segments = List.copyOf(segments);
        this.text = text;
        this.scope = scope;
        this.line = line;
        this.column = column;
    }

    public Kind getKind() { return kind; }
    /** The qualified name, one segment per name, without quotes. */
    public List<String> getSegments() { return segments; }
    /** The name as written. */
    public String getText() { return text; }
    /** Qualified name of the namespace the name is resolved from. */
    public String getScope() { return scope; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
}
