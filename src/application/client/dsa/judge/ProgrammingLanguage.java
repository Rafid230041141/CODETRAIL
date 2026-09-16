package application.client.dsa.judge;

public enum ProgrammingLanguage {
    CPP(
            "cpp",
            "C++ (C++17 / C++20)",
            ".cpp",
            "solution.cpp",
            true,
            """
            #include <iostream>
            #include <vector>
            #include <string>
            #include <algorithm>

            using namespace std;

            int main() {
                ios_base::sync_with_stdio(false);
                cin.tie(NULL);

                // Write your solution here
                

                return 0;
            }
            """
    ),
    C(
            "c",
            "C (C11 / C17)",
            ".c",
            "solution.c",
            true,
            """
            #include <stdio.h>
            #include <stdlib.h>
            #include <string.h>

            int main() {
                // Write your solution here
                

                return 0;
            }
            """
    ),
    PYTHON(
            "python",
            "Python 3",
            ".py",
            "solution.py",
            false,
            """
            import sys

            def solve():
                input_data = sys.stdin.read().split()
                if not input_data:
                    return
                # Write your solution here
                

            if __name__ == '__main__':
                solve()
            """
    ),
    JAVA(
            "java",
            "Java 21",
            ".java",
            "Main.java",
            true,
            """
            // Online Java Compiler
            // Use this editor to write, compile and run your Java code online

            import java.util.*;
            import java.io.*;

            public class Main {
                public static void main(String[] args) throws Exception {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    String line = br.readLine();
                    if (line == null) return;
                    StringTokenizer st = new StringTokenizer(line);

                    // Write your solution here
                    
                }
            }
            """
    ),
    CSHARP(
            "csharp",
            "C# (.NET)",
            ".cs",
            "Solution.cs",
            true,
            """
            using System;
            using System.Collections.Generic;
            using System.Linq;

            public class Solution {
                public static void Main(string[] args) {
                    string line = Console.ReadLine();
                    if (string.IsNullOrEmpty(line)) return;

                    // Write your solution here
                    
                }
            }
            """
    ),
    JAVASCRIPT(
            "javascript",
            "JavaScript (Node.js)",
            ".js",
            "solution.js",
            false,
            """
            const fs = require('fs');

            function main() {
                const input = fs.readFileSync(0, 'utf-8').trim();
                if (!input) return;

                const tokens = input.split(/\\s+/);
                // Write your solution here
                
            }

            main();
            """
    );

    private final String id;
    private final String displayName;
    private final String extension;
    private final String defaultFileName;
    private final boolean compiled;
    private final String starterTemplate;

    ProgrammingLanguage(String id, String displayName, String extension, String defaultFileName, boolean compiled, String starterTemplate) {
        this.id = id;
        this.displayName = displayName;
        this.extension = extension;
        this.defaultFileName = defaultFileName;
        this.compiled = compiled;
        this.starterTemplate = starterTemplate.stripIndent();
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String extension() { return extension; }
    public String defaultFileName() { return defaultFileName; }
    public boolean isCompiled() { return compiled; }
    public String starterTemplate() { return starterTemplate; }

    public static ProgrammingLanguage fromId(String id) {
        if (id == null) return CPP;
        for (ProgrammingLanguage lang : values()) {
            if (lang.id.equalsIgnoreCase(id) || lang.name().equalsIgnoreCase(id)) {
                return lang;
            }
        }
        return CPP;
    }
}
