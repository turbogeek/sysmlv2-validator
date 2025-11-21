package com.validator.suggestions;

import java.util.Objects;

/**
 * Represents a spelling suggestion with metadata about confidence and source.
 * Immutable and comparable by distance/confidence for ranking.
 */
public final class SuggestionResult implements Comparable<SuggestionResult> {

    private final String suggestion;
    private final int distance;
    private final double confidence;
    private final String source;

    /**
     * Creates a new suggestion result.
     *
     * @param suggestion the suggested correction
     * @param distance the Levenshtein edit distance from original
     * @param confidence confidence score from 0.0 to 1.0
     * @param source the source of this suggestion ("keyword", "stdlib", "model")
     */
    public SuggestionResult(String suggestion, int distance, double confidence, String source) {
        this.suggestion = Objects.requireNonNull(suggestion, "suggestion cannot be null");
        this.distance = distance;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.source = Objects.requireNonNull(source, "source cannot be null");
    }

    public String getSuggestion() {
        return suggestion;
    }

    public int getDistance() {
        return distance;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSource() {
        return source;
    }

    /**
     * Compares suggestions for ranking.
     * Lower distance = better match = comes first.
     * Equal distance: higher confidence wins.
     * Equal confidence: model > stdlib > keyword priority.
     */
    @Override
    public int compareTo(SuggestionResult other) {
        // First compare by distance (ascending)
        int distanceCompare = Integer.compare(this.distance, other.distance);
        if (distanceCompare != 0) {
            return distanceCompare;
        }

        // Then by confidence (descending)
        int confidenceCompare = Double.compare(other.confidence, this.confidence);
        if (confidenceCompare != 0) {
            return confidenceCompare;
        }

        // Then by source priority: model > stdlib > keyword
        return Integer.compare(getSourcePriority(other.source), getSourcePriority(this.source));
    }

    private static int getSourcePriority(String source) {
        return switch (source) {
            case "model" -> 4;
            case "cameo" -> 3;
            case "stdlib" -> 2;
            case "keyword" -> 1;
            default -> 0;
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SuggestionResult that = (SuggestionResult) obj;
        return distance == that.distance
            && Double.compare(that.confidence, confidence) == 0
            && suggestion.equals(that.suggestion)
            && source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suggestion, distance, confidence, source);
    }

    @Override
    public String toString() {
        return String.format("'%s' (%s, distance=%d, confidence=%.2f)",
            suggestion, source, distance, confidence);
    }

    /**
     * Formats the suggestion for display in error messages.
     *
     * @return formatted suggestion string like "'Integer' (standard library)"
     */
    public String toDisplayString() {
        String sourceDisplay = switch (source) {
            case "model" -> "user model";
            case "cameo" -> "Cameo library";
            case "stdlib" -> "standard library";
            case "keyword" -> "keyword";
            default -> source;
        };
        return String.format("'%s' (%s)", suggestion, sourceDisplay);
    }
}
