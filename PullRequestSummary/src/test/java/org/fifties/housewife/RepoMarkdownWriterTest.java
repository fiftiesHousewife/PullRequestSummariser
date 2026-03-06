package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class RepoMarkdownWriterTest {

    private final RepoMarkdownWriter writer = new RepoMarkdownWriter();

    @Test
    void includesRepoNameFromMeta() {
        final JsonObject meta = new JsonObject();
        meta.addProperty("repo", "owner/my-repo");
        meta.addProperty("extracted_at", "2024-01-15T00:00:00Z");

        final String markdown = writer.write(List.of(buildPullRequest(1, "open", false)), meta);
        assertThat(markdown).contains("# Repository Summary: owner/my-repo");
    }

    @Test
    void showsNoSummaryForEmptyList() {
        final String markdown = writer.write(List.of(), new JsonObject());
        assertThat(markdown).contains("No pull requests to summarize.");
    }

    @Test
    void calculatesStatisticsCorrectly() {
        final List<JsonObject> pullRequests = List.of(
                buildPullRequest(1, "closed", true),
                buildPullRequest(2, "closed", true),
                buildPullRequest(3, "closed", false),
                buildPullRequest(4, "open", false)
        );

        final String markdown = writer.write(pullRequests, new JsonObject());

        assertAll(
                () -> assertThat(markdown).contains("**Merged:** 2"),
                () -> assertThat(markdown).contains("**Closed (not merged):** 1"),
                () -> assertThat(markdown).contains("**Open:** 1"),
                () -> assertThat(markdown).contains("**Merge rate:** 67%")
        );
    }

    @Test
    void listsContributors() {
        final JsonObject pr1 = buildPullRequest(1, "open", false);
        pr1.addProperty("author", "alice");
        final JsonObject pr2 = buildPullRequest(2, "open", false);
        pr2.addProperty("author", "alice");
        final JsonObject pr3 = buildPullRequest(3, "open", false);
        pr3.addProperty("author", "bob");

        final String markdown = writer.write(List.of(pr1, pr2, pr3), new JsonObject());

        assertAll(
                () -> assertThat(markdown).contains("**alice**: 2 pull requests"),
                () -> assertThat(markdown).contains("**bob**: 1 pull requests")
        );
    }

    @Test
    void detectsLanguagesAcrossPrs() {
        final JsonObject pr = buildPullRequest(1, "open", false);
        final JsonArray files = new JsonArray();
        files.add(buildFile("src/Main.java"));
        files.add(buildFile("test/MainTest.java"));
        files.add(buildFile("README.md"));
        pr.add("files", files);

        final String markdown = writer.write(List.of(pr), new JsonObject());

        assertAll(
                () -> assertThat(markdown).contains("**Java**: 2 files changed"),
                () -> assertThat(markdown).contains("**Markdown**: 1 files changed")
        );
    }

    @Test
    void groupsFilesByTopLevelDirectory() {
        final JsonObject pr = buildPullRequest(1, "open", false);
        final JsonArray files = new JsonArray();
        files.add(buildFile("src/main/App.java"));
        files.add(buildFile("src/test/AppTest.java"));
        files.add(buildFile("docs/README.md"));
        pr.add("files", files);

        final String markdown = writer.write(List.of(pr), new JsonObject());

        assertAll(
                () -> assertThat(markdown).contains("`src/`: 2 file changes"),
                () -> assertThat(markdown).contains("`docs/`: 1 file changes")
        );
    }

    @Test
    void includesTimelineInChronologicalOrder() {
        final JsonObject pr1 = buildPullRequest(1, "open", false);
        pr1.addProperty("created_at", "2024-03-01T00:00:00Z");
        pr1.addProperty("title", "Later pull request");
        final JsonObject pr2 = buildPullRequest(2, "closed", true);
        pr2.addProperty("created_at", "2024-01-01T00:00:00Z");
        pr2.addProperty("title", "Earlier pull request");

        final String markdown = writer.write(List.of(pr1, pr2), new JsonObject());
        final int earlierIndex = markdown.indexOf("Earlier pull request");
        final int laterIndex = markdown.indexOf("Later pull request");

        assertThat(earlierIndex).isLessThan(laterIndex);
    }

    @Test
    void includesPrIndexTable() {
        final String markdown = writer.write(
                List.of(buildPullRequest(1, "open", false)), new JsonObject());

        assertAll(
                () -> assertThat(markdown).contains("## Pull Request Index"),
                () -> assertThat(markdown).contains("| # | Title | Author | Status | Created |")
        );
    }

    private JsonObject buildPullRequest(final int number, final String state, final boolean merged) {
        final JsonObject pr = new JsonObject();
        pr.addProperty("number", number);
        pr.addProperty("title", "Pull request #" + number);
        pr.addProperty("state", state);
        pr.addProperty("merged", merged);
        pr.addProperty("author", "testuser");
        pr.addProperty("created_at", "2024-01-01T00:00:00Z");
        pr.addProperty("updated_at", "2024-01-02T00:00:00Z");
        pr.add("files", new JsonArray());
        return pr;
    }

    private JsonObject buildFile(final String filename) {
        final JsonObject file = new JsonObject();
        file.addProperty("filename", filename);
        file.addProperty("status", "modified");
        file.addProperty("additions", 1);
        file.addProperty("deletions", 0);
        file.addProperty("changes", 1);
        file.addProperty("patch", "+line");
        return file;
    }
}
