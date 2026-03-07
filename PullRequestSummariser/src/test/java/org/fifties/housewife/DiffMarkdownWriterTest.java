package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DiffMarkdownWriterTest {

    @Test
    void appendsNothingWhenNoFiles() {
        final DiffMarkdownWriter writer = new DiffMarkdownWriter(500);
        final StringBuilder markdown = new StringBuilder();
        final JsonObject pr = new JsonObject();
        pr.add("files", new JsonArray());

        writer.append(markdown, pr);

        assertThat(markdown).isEmpty();
    }

    @Test
    void skipsFilesWithEmptyPatch() {
        final DiffMarkdownWriter writer = new DiffMarkdownWriter(500);
        final StringBuilder markdown = new StringBuilder();
        final JsonObject pr = new JsonObject();
        final JsonArray files = new JsonArray();
        files.add(buildFile("binary.png", ""));
        pr.add("files", files);

        writer.append(markdown, pr);

        assertThat(markdown.toString()).contains("## Key Diffs");
        assertThat(markdown.toString()).doesNotContain("binary.png");
    }

    @Test
    void rendersFullDiffWithinLimit() {
        final DiffMarkdownWriter writer = new DiffMarkdownWriter(500);
        final StringBuilder markdown = new StringBuilder();
        final JsonObject pr = new JsonObject();
        final JsonArray files = new JsonArray();
        files.add(buildFile("App.java", "+line1\n+line2\n+line3"));
        pr.add("files", files);

        writer.append(markdown, pr);

        assertAll(
                () -> assertThat(markdown.toString()).contains("### `App.java`"),
                () -> assertThat(markdown.toString()).contains("```diff"),
                () -> assertThat(markdown.toString()).contains("+line1"),
                () -> assertThat(markdown.toString()).doesNotContain("truncated")
        );
    }

    @Test
    void truncatesDiffExceedingLimit() {
        final DiffMarkdownWriter writer = new DiffMarkdownWriter(3);
        final StringBuilder markdown = new StringBuilder();
        final JsonObject pr = new JsonObject();
        final JsonArray files = new JsonArray();
        files.add(buildFile("Big.java", "+line1\n+line2\n+line3\n+line4\n+line5"));
        pr.add("files", files);

        writer.append(markdown, pr);

        assertAll(
                () -> assertThat(markdown.toString()).contains("+line1"),
                () -> assertThat(markdown.toString()).contains("+line2"),
                () -> assertThat(markdown.toString()).contains("+line3"),
                () -> assertThat(markdown.toString()).doesNotContain("+line4"),
                () -> assertThat(markdown.toString()).contains("truncated")
        );
    }

    @Test
    void truncatesAcrossMultipleFiles() {
        final DiffMarkdownWriter writer = new DiffMarkdownWriter(4);
        final StringBuilder markdown = new StringBuilder();
        final JsonObject pr = new JsonObject();
        final JsonArray files = new JsonArray();
        files.add(buildFile("First.java", "+a\n+b\n+c"));
        files.add(buildFile("Second.java", "+d\n+e\n+f"));
        pr.add("files", files);

        writer.append(markdown, pr);

        assertAll(
                () -> assertThat(markdown.toString()).contains("First.java"),
                () -> assertThat(markdown.toString()).contains("Second.java"),
                () -> assertThat(markdown.toString()).contains("truncated"),
                () -> assertThat(markdown.toString()).contains("Remaining files omitted")
        );
    }

    private JsonObject buildFile(final String filename, final String patch) {
        final JsonObject file = new JsonObject();
        file.addProperty("filename", filename);
        file.addProperty("patch", patch);
        return file;
    }
}
