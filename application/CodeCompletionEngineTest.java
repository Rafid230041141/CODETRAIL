package application;

import application.client.dsa.judge.CodeCompletionEngine;
import application.client.dsa.judge.CodeCompletionEngine.CompletionItem;
import application.client.dsa.judge.ProgrammingLanguage;
import java.util.List;

public class CodeCompletionEngineTest {
    public static void main(String[] args) {
        System.out.println("Running CodeCompletionEngineTest...");
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            List<CompletionItem> suggestions = CodeCompletionEngine.getSuggestions(lang, "cou");
            boolean hasCout = suggestions.stream().anyMatch(item -> item.label().equalsIgnoreCase("cout"));
            System.out.println("Language " + lang + " suggestions for 'cou': " + suggestions.stream().map(CompletionItem::label).toList());
            if (!hasCout) {
                System.err.println("FAIL: " + lang + " does not suggest 'cout' for prefix 'cou'");
                System.exit(1);
            }
        }
        System.out.println("ALL 6 LANGUAGES SUGGEST 'cout' FOR 'cou'! PASSED!");
    }
}
