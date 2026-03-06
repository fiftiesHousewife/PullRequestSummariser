package org.fifties.housewife;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownOutputIntegrationTest {

    private final PullRequestMarkdownWriter pullRequestWriter = new PullRequestMarkdownWriter(500);
    private final RepoMarkdownWriter repoWriter = new RepoMarkdownWriter();

    @Test
    void pullRequestSummaryMatchesExpectedOutput() throws IOException {
        final JsonObject input = loadJson("pr_1_input.json");
        final String expected = loadText("pr_1_expected_summary.md");

        final String actual = pullRequestWriter.write(input);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void repoSummaryMatchesExpectedOutput() throws IOException {
        final JsonObject pullRequest = loadJson("pr_1_input.json");
        final JsonObject meta = loadJson("extraction_meta_input.json");
        final String expected = loadText("repo_expected_summary.md");

        final String actual = repoWriter.write(List.of(pullRequest), meta);

        assertThat(actual).isEqualTo(expected);
    }

    private JsonObject loadJson(final String filename) throws IOException {
        return JsonParser.parseString(loadText(filename)).getAsJsonObject();
    }

    private String loadText(final String filename) throws IOException {
        try (final InputStream stream = getClass().getResourceAsStream(filename)) {
            if (stream == null) {
                throw new IOException("Test resource not found: " + filename);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
