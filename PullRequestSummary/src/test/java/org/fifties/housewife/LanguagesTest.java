package org.fifties.housewife;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class LanguagesTest {

    @Test
    void detectsJavaFiles() {
        assertThat(Languages.detect("Main.java")).isEqualTo("Java");
    }

    @Test
    void detectsPythonFiles() {
        assertThat(Languages.detect("script.py")).isEqualTo("Python");
    }

    @Test
    void detectsTypeScriptFiles() {
        assertThat(Languages.detect("app.ts")).isEqualTo("TypeScript");
    }

    @Test
    void detectsNestedFilePaths() {
        assertThat(Languages.detect("src/main/java/App.java")).isEqualTo("Java");
    }

    @Test
    void returnsUnknownForNoExtension() {
        assertThat(Languages.detect("Makefile")).isEqualTo("Unknown");
    }

    @Test
    void returnsExtensionForUnmappedType() {
        assertThat(Languages.detect("data.parquet")).isEqualTo(".parquet");
    }

    @Test
    void handlesUpperCaseExtensions() {
        assertThat(Languages.detect("README.MD")).isEqualTo("Markdown");
    }

    @Test
    void detectsMultipleLanguagesCorrectly() {
        assertAll(
                () -> assertThat(Languages.detect("index.js")).isEqualTo("JavaScript"),
                () -> assertThat(Languages.detect("styles.css")).isEqualTo("CSS"),
                () -> assertThat(Languages.detect("config.yml")).isEqualTo("YAML"),
                () -> assertThat(Languages.detect("config.yaml")).isEqualTo("YAML"),
                () -> assertThat(Languages.detect("query.sql")).isEqualTo("SQL"),
                () -> assertThat(Languages.detect("Dockerfile.sh")).isEqualTo("Shell")
        );
    }
}
