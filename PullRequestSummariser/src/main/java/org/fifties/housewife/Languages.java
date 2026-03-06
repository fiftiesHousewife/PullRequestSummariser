package org.fifties.housewife;

import java.util.Map;

final class Languages {

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry(".py", "Python"), Map.entry(".js", "JavaScript"),
            Map.entry(".ts", "TypeScript"), Map.entry(".jsx", "React JSX"),
            Map.entry(".tsx", "React TSX"), Map.entry(".rb", "Ruby"),
            Map.entry(".go", "Go"), Map.entry(".rs", "Rust"),
            Map.entry(".java", "Java"), Map.entry(".kt", "Kotlin"),
            Map.entry(".swift", "Swift"), Map.entry(".c", "C"),
            Map.entry(".cpp", "C++"), Map.entry(".h", "C/C++ Header"),
            Map.entry(".cs", "C#"), Map.entry(".php", "PHP"),
            Map.entry(".html", "HTML"), Map.entry(".css", "CSS"),
            Map.entry(".scss", "SCSS"), Map.entry(".sql", "SQL"),
            Map.entry(".sh", "Shell"), Map.entry(".yml", "YAML"),
            Map.entry(".yaml", "YAML"), Map.entry(".json", "JSON"),
            Map.entry(".md", "Markdown"), Map.entry(".txt", "Text"),
            Map.entry(".toml", "TOML"), Map.entry(".xml", "XML"),
            Map.entry(".vue", "Vue")
    );

    private Languages() {
    }

    static String detect(final String filename) {
        final int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "Unknown";
        }
        final String extension = filename.substring(dot).toLowerCase();
        return EXTENSIONS.getOrDefault(extension, extension);
    }
}
