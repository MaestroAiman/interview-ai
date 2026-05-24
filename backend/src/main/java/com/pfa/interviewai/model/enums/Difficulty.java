package com.pfa.interviewai.model.enums;

public enum Difficulty {
    JUNIOR("Junior", "Entry level position, 0-2 years experience"),
    MID("Mid-level", "Intermediate position, 2-5 years experience"),
    SENIOR("Senior", "Senior position, 5+ years experience");

    private final String label;
    private final String description;

    Difficulty(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel()       { return label; }
    public String getDescription() { return description; }
}
