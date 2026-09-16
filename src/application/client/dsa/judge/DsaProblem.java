package application.client.dsa.judge;

import java.util.List;

public record DsaProblem(
        String id,
        String topicKey,
        String topicTitle,
        String title,
        Difficulty difficulty,
        int timeLimitMs,
        int memoryLimitMb,
        String statement,
        String inputFormat,
        String outputFormat,
        String constraints,
        List<TestCase> testCases,
        String note
) {
    public List<TestCase> getSampleTests() {
        return testCases.stream().filter(TestCase::isSample).toList();
    }

    public List<TestCase> getHiddenTests() {
        return testCases.stream().filter(tc -> !tc.isSample()).toList();
    }

    public String getTimeLimitFormatted() {
        if (timeLimitMs >= 1000) {
            double secs = timeLimitMs / 1000.0;
            return (secs == (long) secs ? String.format("%d.0", (long) secs) : String.format("%.1f", secs)) + " seconds";
        }
        return timeLimitMs + " ms";
    }

    public String getMemoryLimitFormatted() {
        return memoryLimitMb + " megabytes";
    }
}
