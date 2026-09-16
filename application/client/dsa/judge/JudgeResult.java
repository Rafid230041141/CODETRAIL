package application.client.dsa.judge;

import java.util.List;

public record JudgeResult(
        SubmissionVerdict overallVerdict,
        int passedTests,
        int totalTests,
        long maxTimeMs,
        long totalTimeMs,
        String compilationError,
        List<TestCaseResult> testResults,
        int failedTestIndex
) {
    public static JudgeResult compilationError(String errorLog) {
        return new JudgeResult(
                SubmissionVerdict.COMPILATION_ERROR,
                0,
                0,
                0,
                0,
                errorLog,
                List.of(),
                -1
        );
    }

    public static JudgeResult internalError(String message) {
        return new JudgeResult(
                SubmissionVerdict.INTERNAL_ERROR,
                0,
                0,
                0,
                0,
                message,
                List.of(),
                -1
        );
    }

    public boolean isAccepted() {
        return overallVerdict == SubmissionVerdict.ACCEPTED;
    }
}
