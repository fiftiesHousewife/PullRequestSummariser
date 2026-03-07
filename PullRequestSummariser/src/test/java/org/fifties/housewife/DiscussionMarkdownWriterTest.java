package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DiscussionMarkdownWriterTest {

    private final DiscussionMarkdownWriter writer = new DiscussionMarkdownWriter();

    @Test
    void appendsNothingWhenNoDiscussion() {
        final StringBuilder markdown = new StringBuilder();
        writer.append(markdown, buildPullRequest(new JsonArray(), new JsonArray(), new JsonArray()));
        assertThat(markdown).isEmpty();
    }

    @Test
    void appendsReviewDecisions() {
        final JsonArray reviews = new JsonArray();
        reviews.add(buildReview("alice", "APPROVED", "Looks good", "2024-01-01T00:00:00Z"));
        final JsonObject pr = buildPullRequest(reviews, new JsonArray(), new JsonArray());

        final StringBuilder markdown = new StringBuilder();
        writer.append(markdown, pr);

        assertAll(
                () -> assertThat(markdown.toString()).contains("## Discussion"),
                () -> assertThat(markdown.toString()).contains("### Review Decisions"),
                () -> assertThat(markdown.toString()).contains("**alice**: Approved — Looks good")
        );
    }

    @Test
    void truncatesLongReviewBody() {
        final String longBody = "x".repeat(250);
        final JsonArray reviews = new JsonArray();
        reviews.add(buildReview("bob", "CHANGES_REQUESTED", longBody, "2024-01-01T00:00:00Z"));
        final JsonObject pr = buildPullRequest(reviews, new JsonArray(), new JsonArray());

        final StringBuilder markdown = new StringBuilder();
        writer.append(markdown, pr);

        assertThat(markdown.toString()).contains("...");
    }

    @Test
    void appendsReviewComments() {
        final JsonArray reviewComments = new JsonArray();
        final JsonObject comment = new JsonObject();
        comment.addProperty("user", "carol");
        comment.addProperty("path", "src/Main.java");
        comment.addProperty("line", 42);
        comment.addProperty("body", "Consider extracting this.");
        comment.addProperty("created_at", "2024-01-02T00:00:00Z");
        reviewComments.add(comment);

        final JsonObject pr = buildPullRequest(new JsonArray(), reviewComments, new JsonArray());

        final StringBuilder markdown = new StringBuilder();
        writer.append(markdown, pr);

        assertAll(
                () -> assertThat(markdown.toString()).contains("### Code Review Comments"),
                () -> assertThat(markdown.toString()).contains("**carol** on `src/Main.java` line 42"),
                () -> assertThat(markdown.toString()).contains("> Consider extracting this.")
        );
    }

    @Test
    void appendsIssueComments() {
        final JsonArray issueComments = new JsonArray();
        final JsonObject comment = new JsonObject();
        comment.addProperty("user", "dave");
        comment.addProperty("body", "This looks great.");
        comment.addProperty("created_at", "2024-01-03T00:00:00Z");
        issueComments.add(comment);

        final JsonObject pr = buildPullRequest(new JsonArray(), new JsonArray(), issueComments);

        final StringBuilder markdown = new StringBuilder();
        writer.append(markdown, pr);

        assertAll(
                () -> assertThat(markdown.toString()).contains("### General Comments"),
                () -> assertThat(markdown.toString()).contains("**dave**"),
                () -> assertThat(markdown.toString()).contains("> This looks great.")
        );
    }

    @Test
    void appendsAllSectionsWhenAllPresent() {
        final JsonArray reviews = new JsonArray();
        reviews.add(buildReview("alice", "APPROVED", "", "2024-01-01T00:00:00Z"));

        final JsonArray reviewComments = new JsonArray();
        final JsonObject rc = new JsonObject();
        rc.addProperty("user", "bob");
        rc.addProperty("path", "App.java");
        rc.addProperty("body", "Nit");
        rc.addProperty("created_at", "2024-01-02T00:00:00Z");
        reviewComments.add(rc);

        final JsonArray issueComments = new JsonArray();
        final JsonObject ic = new JsonObject();
        ic.addProperty("user", "carol");
        ic.addProperty("body", "LGTM");
        ic.addProperty("created_at", "2024-01-03T00:00:00Z");
        issueComments.add(ic);

        final JsonObject pr = buildPullRequest(reviews, reviewComments, issueComments);

        final StringBuilder markdown = new StringBuilder();
        writer.append(markdown, pr);

        assertAll(
                () -> assertThat(markdown.toString()).contains("### Review Decisions"),
                () -> assertThat(markdown.toString()).contains("### Code Review Comments"),
                () -> assertThat(markdown.toString()).contains("### General Comments")
        );
    }

    private JsonObject buildReview(final String user, final String state, final String body, final String submittedAt) {
        final JsonObject review = new JsonObject();
        review.addProperty("user", user);
        review.addProperty("state", state);
        review.addProperty("body", body);
        review.addProperty("submitted_at", submittedAt);
        return review;
    }

    private JsonObject buildPullRequest(final JsonArray reviews, final JsonArray reviewComments,
                                        final JsonArray issueComments) {
        final JsonObject pr = new JsonObject();
        pr.add("reviews", reviews);
        pr.add("review_comments", reviewComments);
        pr.add("issue_comments", issueComments);
        return pr;
    }
}
