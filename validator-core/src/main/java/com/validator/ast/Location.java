package com.validator.ast;

import java.util.Objects;

/**
 * Represents a location in a source file (line and column).
 */
public class Location {
    private final String fileName;
    private final int line;
    private final int column;

    public Location(String fileName, int line, int column) {
        this.fileName = Objects.requireNonNull(fileName, "File name cannot be null");
        this.line = line;
        this.column = column;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return line == location.line &&
               column == location.column &&
               fileName.equals(location.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, line, column);
    }

    @Override
    public String toString() {
        return String.format("%s:%d:%d", fileName, line, column);
    }
}
