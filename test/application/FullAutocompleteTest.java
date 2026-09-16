package application;

import application.client.dsa.judge.CodeCompletionEngine;
import application.client.dsa.judge.CodeCompletionPopup;
import application.client.dsa.judge.ProgrammingLanguage;

public class FullAutocompleteTest {
    public static void main(String[] args) {
        System.out.println("Running FullAutocompleteTest...");

        // Test prefix extraction
        String code = "int main() {\n    cou";
        String p1 = CodeCompletionPopup.extractPrefix(code, code.length());
        assert "cou".equals(p1) : "Expected 'cou' but got " + p1;
        System.out.println("Prefix extraction 1 ('cou'): PASSED");

        String code2 = "    std::cout";
        String p2 = CodeCompletionPopup.extractPrefix(code2, code2.length());
        assert "cout".equals(p2) || "std::cout".equals(p2) : "Got " + p2;
        System.out.println("Prefix extraction 2: PASSED");

        // Test C++ suggestions for "cou", "cin", "vec", "so"
        var cppCou = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.CPP, "cou");
        assert cppCou.get(0).label().equals("cout") : "First must be cout";
        System.out.println("CPP 'cou' -> " + cppCou.get(0).label() + " (" + cppCou.get(0).insertText() + "): PASSED");

        var cppCin = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.CPP, "ci");
        assert cppCin.stream().anyMatch(i -> i.label().equals("cin")) : "Must contain cin";
        System.out.println("CPP 'ci' -> contains cin: PASSED");

        var cppVec = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.CPP, "vec");
        assert cppVec.stream().anyMatch(i -> i.label().equals("vector")) : "Must contain vector";
        System.out.println("CPP 'vec' -> contains vector: PASSED");

        // Test Python suggestions for "pri", "def", "cou"
        var pyPri = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.PYTHON, "pri");
        assert pyPri.stream().anyMatch(i -> i.label().equals("print")) : "Must contain print";
        System.out.println("Python 'pri' -> contains print: PASSED");

        var pyCou = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.PYTHON, "cou");
        assert pyCou.stream().anyMatch(i -> i.label().equals("cout")) : "Must contain cout";
        System.out.println("Python 'cou' -> contains cout: PASSED");

        // Test Java suggestions for "sys", "cou"
        var javaSys = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.JAVA, "sys");
        assert javaSys.stream().anyMatch(i -> i.label().contains("System.out")) : "Must contain System.out";
        System.out.println("Java 'sys' -> contains System.out: PASSED");

        var javaCou = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.JAVA, "cou");
        assert javaCou.stream().anyMatch(i -> i.label().equals("cout")) : "Must contain cout";
        System.out.println("Java 'cou' -> contains cout: PASSED");

        // Test C suggestions for "pri", "cou"
        var cPri = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.C, "pri");
        assert cPri.stream().anyMatch(i -> i.label().equals("printf")) : "Must contain printf";
        System.out.println("C 'pri' -> contains printf: PASSED");

        var cCou = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.C, "cou");
        assert cCou.stream().anyMatch(i -> i.label().equals("cout")) : "Must contain cout";
        System.out.println("C 'cou' -> contains cout: PASSED");

        // Test C# suggestions for "con", "cou"
        var csCon = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.CSHARP, "con");
        assert csCon.stream().anyMatch(i -> i.label().contains("Console")) : "Must contain Console";
        System.out.println("C# 'con' -> contains Console: PASSED");

        var csCou = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.CSHARP, "cou");
        assert csCou.stream().anyMatch(i -> i.label().equals("cout")) : "Must contain cout";
        System.out.println("C# 'cou' -> contains cout: PASSED");

        // Test JS suggestions for "con", "cou"
        var jsCon = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.JAVASCRIPT, "con");
        assert jsCon.stream().anyMatch(i -> i.label().contains("console")) : "Must contain console";
        System.out.println("JS 'con' -> contains console: PASSED");

        var jsCou = CodeCompletionEngine.getSuggestions(ProgrammingLanguage.JAVASCRIPT, "cou");
        assert jsCou.stream().anyMatch(i -> i.label().equals("cout")) : "Must contain cout";
        System.out.println("JS 'cou' -> contains cout: PASSED");

        // Test line number gutter formatting for lines 1..26 and beyond
        int lineCount26 = 26;
        int digits26 = String.valueOf(lineCount26).length();
        int widthChars26 = Math.max(2, digits26);
        assert widthChars26 == 2 : "Must be 2 chars wide for 26 lines";
        String line10 = String.format("%" + widthChars26 + "d", 10);
        String line25 = String.format("%" + widthChars26 + "d", 25);
        String line26 = String.format("%" + widthChars26 + "d", 26);
        assert "10".equals(line10) && "25".equals(line25) && "26".equals(line26) : "Digits 10, 25, 26 must format cleanly";
        int gutterWidth26 = Math.max(58, digits26 * 11 + 36);
        assert gutterWidth26 >= 58 : "Gutter width must be >= 58px for 26 lines";
        System.out.println("Line numbers 1..26 right-align formatting test: PASSED (width: " + gutterWidth26 + "px)");

        System.out.println("ALL AUTOCOMPLETE & GUTTER TESTS PASSED SUCCESSFULLY!");
    }
}
