package org.fifties.housewife;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PrSummarizeTest {

    @Test
    void loadsPullRequestsFromJsonFiles(@TempDir final Path tempDir) throws Exception {
        writePullRequestFile(tempDir, 1, "First pull request");
        writePullRequestFile(tempDir, 2, "Second pull request");

        final List<JsonObject> pullRequests = PullRequestSummarize.loadPullRequests(tempDir);

        assertAll(
                () -> assertThat(pullRequests).hasSize(2),
                () -> assertThat(pullRequests.get(0).get("title").getAsString()).isEqualTo("First pull request"),
                () -> assertThat(pullRequests.get(1).get("title").getAsString()).isEqualTo("Second pull request")
        );
    }

    @Test
    void returnsEmptyListWhenNoPrFiles(@TempDir final Path tempDir) throws Exception {
        final List<JsonObject> pullRequests = PullRequestSummarize.loadPullRequests(tempDir);
        assertThat(pullRequests).isEmpty();
    }

    @Test
    void ignoresNonPrJsonFiles(@TempDir final Path tempDir) throws Exception {
        writePullRequestFile(tempDir, 1, "Real pull request");
        Files.writeString(tempDir.resolve("extraction_meta.json"), "{}");

        final List<JsonObject> pullRequests = PullRequestSummarize.loadPullRequests(tempDir);
        assertThat(pullRequests).hasSize(1);
    }

    @Test
    void loadsPullRequestsInSortedOrder(@TempDir final Path tempDir) throws Exception {
        writePullRequestFile(tempDir, 10, "Tenth pull request");
        writePullRequestFile(tempDir, 2, "Second pull request");
        writePullRequestFile(tempDir, 1, "First pull request");

        final List<JsonObject> pullRequests = PullRequestSummarize.loadPullRequests(tempDir);

        assertAll(
                () -> assertThat(pullRequests).hasSize(3),
                () -> assertThat(pullRequests.get(0).get("number").getAsInt()).isEqualTo(1),
                () -> assertThat(pullRequests.get(1).get("number").getAsInt()).isEqualTo(10),
                () -> assertThat(pullRequests.get(2).get("number").getAsInt()).isEqualTo(2)
        );
    }

    private void writePullRequestFile(final Path directory, final int number, final String title) throws Exception {
        final JsonObject pr = new JsonObject();
        pr.addProperty("number", number);
        pr.addProperty("title", title);
        pr.addProperty("state", "open");
        pr.addProperty("merged", false);
        pr.addProperty("author", "testuser");
        pr.addProperty("created_at", "2024-01-01T00:00:00Z");
        pr.addProperty("updated_at", "2024-01-02T00:00:00Z");
        pr.addProperty("body", "Description");
        pr.addProperty("head_branch", "feature");
        pr.addProperty("base_branch", "main");
        pr.add("labels", new JsonArray());
        pr.add("commits", new JsonArray());
        pr.add("files", new JsonArray());
        pr.add("review_comments", new JsonArray());
        pr.add("issue_comments", new JsonArray());
        pr.add("reviews", new JsonArray());

        final String json = new GsonBuilder().setPrettyPrinting().create().toJson(pr);
        Files.writeString(directory.resolve("pr_" + number + ".json"), json);
    }
}
