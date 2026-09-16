package application.client.dsa.judge;

public record TestCaseResult(
        int testIndex,
        SubmissionVerdict verdict,
        long timeMs,
        String input,
        String expectedOutput,
        String actualOutput,
        String errorOutput,
        boolean isSample
) {
    public boolean isPassed() {
        return verdict == SubmissionVerdict.ACCEPTED;
    }
}
