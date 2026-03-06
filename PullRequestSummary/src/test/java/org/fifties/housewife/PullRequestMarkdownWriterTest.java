package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PrMarkdownWriterTest {

    private final PullRequestMarkdownWriter writer = new PullRequestMarkdownWriter(500);

    @Test
    void includesPrTitleAndNumber() {
        final String markdown = writer.write(buildPullRequest(1, "Add login feature", "open", false));
        assertThat(markdown).contains("# Pull Request #1: Add login feature");
    }

    @Test
    void showsMergedStatus() {
        final JsonObject pr = buildPullRequest(2, "Fix auth", "closed", true);
        pr.addProperty("merged_at", "2024-01-15T10:00:00Z");

        final String markdown = writer.write(pr);

        assertAll(
                () -> assertThat(markdown).contains("**Status:** Merged"),
                () -> assertThat(markdown).contains("**Merged** on 2024-01-15T10:00:00Z")
        );
    }

    @Test
    void showsClosedWithoutMerge() {
        final JsonObject pr = buildPullRequest(3, "Abandoned pull request", "closed", false);
        pr.addProperty("closed_at", "2024-02-01T00:00:00Z");
        final String markdown = writer.write(pr);
        assertThat(markdown).contains("**Closed without merge** on 2024-02-01T00:00:00Z");
    }

    @Test
    void showsStillOpenForOpenPrs() {
        final String markdown = writer.write(buildPullRequest(4, "WIP", "open", false));
        assertThat(markdown).contains("**Still open**");
    }

    @Test
    void includesPrBody() {
        final JsonObject pr = buildPullRequest(5, "Feature", "open", false);
        pr.addProperty("body", "This pull request adds a great feature.");
        final String markdown = writer.write(pr);
        assertThat(markdown).contains("This pull request adds a great feature.");
    }

    @Test
    void showsPlaceholderWhenNoBody() {
        final JsonObject pr = buildPullRequest(6, "No desc", "open", false);
        pr.addProperty("body", "");
        final String markdown = writer.write(pr);
        assertThat(markdown).contains("*No pull request description provided.*");
    }

    @Test
    void includesCommitMessages() {
        final JsonObject pr = buildPullRequest(7, "Changes", "open", false);
        final JsonArray commits = new JsonArray();
        final JsonObject commit = new JsonObject();
        commit.addProperty("sha", "abcdef1234567");
        commit.addProperty("message", "Fix null pointer\nMore details here");
        commit.addProperty("author", "Dev Name");
        commit.addProperty("date", "2024-01-01T00:00:00Z");
        commits.add(commit);
        pr.add("commits", commits);

        final String markdown = writer.write(pr);

        assertAll(
                () -> assertThat(markdown).contains("`abcdef1`"),
                () -> assertThat(markdown).contains("Fix null pointer"),
                () -> assertThat(markdown).doesNotContain("More details here")
        );
    }

    @Test
    void includesFileSummary() {
        final JsonObject pr = buildPullRequest(8, "Refactor", "open", false);
        final JsonArray files = new JsonArray();
        final JsonObject file = new JsonObject();
        file.addProperty("filename", "src/App.java");
        file.addProperty("status", "modified");
        file.addProperty("additions", 20);
        file.addProperty("deletions", 5);
        file.addProperty("changes", 25);
        file.addProperty("patch", "+new line\n-old line");
        files.add(file);
        pr.add("files", files);

        final String markdown = writer.write(pr);

        assertAll(
                () -> assertThat(markdown).contains("**Files changed:** 1"),
                () -> assertThat(markdown).contains("**Lines added:** 20"),
                () -> assertThat(markdown).contains("**Lines removed:** 5"),
                () -> assertThat(markdown).contains("**Languages:** Java (1)"),
                () -> assertThat(markdown).contains("`src/App.java`")
        );
    }

    @Test
    void truncatesDiffsExceedingLimit() {
        final PullRequestMarkdownWriter limitedWriter = new PullRequestMarkdownWriter(3);
        final JsonObject pr = buildPullRequest(9, "Big change", "open", false);
        final JsonArray files = new JsonArray();
        final JsonObject file = new JsonObject();
        file.addProperty("filename", "big.java");
        file.addProperty("status", "modified");
        file.addProperty("additions", 100);
        file.addProperty("deletions", 0);
        file.addProperty("changes", 100);
        file.addProperty("patch", "+line1\n+line2\n+line3\n+line4\n+line5");
        files.add(file);
        pr.add("files", files);

        final String markdown = limitedWriter.write(pr);
        assertThat(markdown).contains("truncated");
    }

    @Test
    void includesReviewDecisions() {
        final JsonObject pr = buildPullRequest(10, "Reviewed", "closed", true);
        pr.addProperty("merged_at", "2024-01-15T10:00:00Z");
        final JsonArray reviews = new JsonArray();
        final JsonObject review = new JsonObject();
        review.addProperty("user", "reviewer");
        review.addProperty("state", "APPROVED");
        review.addProperty("body", "Ship it");
        review.addProperty("submitted_at", "2024-01-14T00:00:00Z");
        reviews.add(review);
        pr.add("reviews", reviews);

        final String markdown = writer.write(pr);

        assertAll(
                () -> assertThat(markdown).contains("### Review Decisions"),
                () -> assertThat(markdown).contains("**reviewer**: Approved"),
                () -> assertThat(markdown).contains("Ship it")
        );
    }

    @Test
    void includesLabelsAndBranchInfo() {
        final JsonObject pr = buildPullRequest(11, "Labeled", "open", false);
        final JsonArray labels = new JsonArray();
        labels.add("bug");
        labels.add("priority");
        pr.add("labels", labels);

        final String markdown = writer.write(pr);

        assertAll(
                () -> assertThat(markdown).contains("**Labels:** bug, priority"),
                () -> assertThat(markdown).contains("`fix-branch` -> `main`")
        );
    }

    @Test
    void capitalizesCorrectly() {
        assertAll(
                () -> assertThat(PullRequestMarkdownWriter.capitalize("open")).isEqualTo("Open"),
                () -> assertThat(PullRequestMarkdownWriter.capitalize("CLOSED")).isEqualTo("Closed"),
                () -> assertThat(PullRequestMarkdownWriter.capitalize("changes requested")).isEqualTo("Changes requested"),
                () -> assertThat(PullRequestMarkdownWriter.capitalize("")).isEmpty(),
                () -> assertThat(PullRequestMarkdownWriter.capitalize(null)).isNull()
        );
    }

    private JsonObject buildPullRequest(final int number, final String title, final String state, final boolean merged) {
        final JsonObject pr = new JsonObject();
        pr.addProperty("number", number);
        pr.addProperty("title", title);
        pr.addProperty("state", state);
        pr.addProperty("merged", merged);
        pr.addProperty("author", "testuser");
        pr.addProperty("created_at", "2024-01-01T00:00:00Z");
        pr.addProperty("updated_at", "2024-01-02T00:00:00Z");
        pr.addProperty("body", "Pull request description");
        pr.addProperty("head_branch", "fix-branch");
        pr.addProperty("base_branch", "main");
        pr.add("labels", new JsonArray());
        pr.add("commits", new JsonArray());
        pr.add("files", new JsonArray());
        pr.add("review_comments", new JsonArray());
        pr.add("issue_comments", new JsonArray());
        pr.add("reviews", new JsonArray());
        return pr;
    }
}
