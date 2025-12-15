package com.validator.semantic;

/**
 * Visibility modifiers for SysML v2 elements.
 */
public enum Visibility {
    PUBLIC,
    PRIVATE,
    PROTECTED;

    /**
     * Parse visibility from a string.
     *
     * @param vis the visibility string (may be null)
     * @return the corresponding Visibility enum value
     */
    public static Visibility fromString(String vis) {
        if (vis == null) {
            return PUBLIC; // Default
        }
        return switch (vis.toLowerCase()) {
            case "private" -> PRIVATE;
            case "protected" -> PROTECTED;
            case "public" -> PUBLIC;
            default -> PUBLIC;
        };
    }
}
