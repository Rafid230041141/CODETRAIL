package application.client.dsa.judge;

public record TestCase(
        String input,
        String expectedOutput,
        boolean isSample,
        String explanation
) {
    public TestCase(String input, String expectedOutput, boolean isSample) {
        this(input, expectedOutput, isSample, "");
    }
}
