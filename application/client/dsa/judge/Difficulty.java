package application.client.dsa.judge;

public enum Difficulty {
    EASY("Easy", "800 - 1100", "#10b981", "#059669"),
    MEDIUM("Medium", "1300 - 1600", "#f59e0b", "#d97706"),
    HARD("Tough", "1900 - 2200", "#ef4444", "#dc2626");

    private final String label;
    private final String ratingRange;
    private final String badgeColor;
    private final String darkBadgeColor;

    Difficulty(String label, String ratingRange, String badgeColor, String darkBadgeColor) {
        this.label = label;
        this.ratingRange = ratingRange;
        this.badgeColor = badgeColor;
        this.darkBadgeColor = darkBadgeColor;
    }

    public String label() { return label; }
    public String ratingRange() { return ratingRange; }
    public String badgeColor() { return badgeColor; }
    public String darkBadgeColor() { return darkBadgeColor; }
}
