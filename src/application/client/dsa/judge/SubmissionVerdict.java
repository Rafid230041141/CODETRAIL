package application.client.dsa.judge;

public enum SubmissionVerdict {
    ACCEPTED("Accepted", "AC", "#10b981", true),
    WRONG_ANSWER("Wrong Answer", "WA", "#ef4444", false),
    TIME_LIMIT_EXCEEDED("Time Limit Exceeded", "TLE", "#f97316", false),
    COMPILATION_ERROR("Compilation Error", "CE", "#8b5cf6", false),
    RUNTIME_ERROR("Runtime Error", "RE", "#e11d48", false),
    INTERNAL_ERROR("Judge Error", "IE", "#64748b", false),
    JUDGING("Running & Judging...", "...", "#0284c7", false);

    private final String label;
    private final String shortCode;
    private final String color;
    private final boolean isSuccess;

    SubmissionVerdict(String label, String shortCode, String color, boolean isSuccess) {
        this.label = label;
        this.shortCode = shortCode;
        this.color = color;
        this.isSuccess = isSuccess;
    }

    public String label() { return label; }
    public String shortCode() { return shortCode; }
    public String color() { return color; }
    public boolean isSuccess() { return isSuccess; }
}
