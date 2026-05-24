package com.pfa.interviewai.model.enums;

public enum InterviewType {
    TECHNICAL("Technical", "💻"),
    HR("Human Resources", "🤝"),
    DOMAIN("Domain Specific", "📊");

    private final String label;
    private final String icon;

    InterviewType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon()  { return icon; }
}
