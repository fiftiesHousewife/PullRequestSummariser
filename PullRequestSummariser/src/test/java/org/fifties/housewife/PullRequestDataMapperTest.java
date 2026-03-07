package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PullRequestDataMapperTest {

    private final PullRequestDataMapper mapper = new PullRequestDataMapper();

    @Test
    void mapsBasicPullRequestMetadata() {
        final JsonObject detail = buildMinimalPrDetail();
        final JsonObject result = mapper.map(detail, new JsonArray(), new JsonArray(),
                new JsonArray(), new JsonArray(), new JsonArray());

        assertAll(
                () -> assertThat(result.get("number").getAsInt()).isEqualTo(1),
                () -> assertThat(result.get("title").getAsString()).isEqualTo("Fix bug"),
                () -> assertThat(result.get("state").getAsString()).isEqualTo("open"),
                () -> assertThat(result.get("author").getAsString()).isEqualTo("testuser"),
                () -> assertThat(result.get("merged").getAsBoolean()).isFalse()
        );
    }

    @Test
    void mapsBranchInfo() {
        final JsonObject detail = buildMinimalPrDetail();
        final JsonObject result = mapper.map(detail, new JsonArray(), new JsonArray(),
                new JsonArray(), new JsonArray(), new JsonArray());

        assertAll(
                () -> assertThat(result.get("base_branch").getAsString()).isEqualTo("main"),
                () -> assertThat(result.get("head_branch").getAsString()).isEqualTo("fix-branch")
        );
    }

    @Test
    void mapsLabels() {
        final JsonObject detail = buildMinimalPrDetail();
        final JsonObject label = new JsonObject();
        label.addProperty("name", "bug");
        detail.getAsJsonArray("labels").add(label);

        final JsonObject result = mapper.map(detail, new JsonArray(), new JsonArray(),
                new JsonArray(), new JsonArray(), new JsonArray());

        assertThat(result.getAsJsonArray("labels")).hasSize(1);
        assertThat(result.getAsJsonArray("labels").get(0).getAsString()).isEqualTo("bug");
    }

    @Test
    void mapsCommits() {
        final JsonArray commits = new JsonArray();
        commits.add(buildCommit("abc1234", "Fix the thing", "Author Name", "2024-01-01T00:00:00Z"));

        final JsonObject result = mapper.map(buildMinimalPrDetail(), commits, new JsonArray(),
                new JsonArray(), new JsonArray(), new JsonArray());

        final JsonObject firstCommit = result.getAsJsonArray("commits").get(0).getAsJsonObject();
        assertAll(
                () -> assertThat(firstCommit.get("sha").getAsString()).isEqualTo("abc1234"),
                () -> assertThat(firstCommit.get("message").getAsString()).isEqualTo("Fix the thing"),
                () -> assertThat(firstCommit.get("author").getAsString()).isEqualTo("Author Name")
        );
    }

    @Test
    void mapsFiles() {
        final JsonArray files = new JsonArray();
        files.add(buildFile("src/Main.java", "modified", 10, 5, 15, "@@ -1,5 +1,10 @@"));

        final JsonObject result = mapper.map(buildMinimalPrDetail(), new JsonArray(), files,
                new JsonArray(), new JsonArray(), new JsonArray());

        final JsonObject firstFile = result.getAsJsonArray("files").get(0).getAsJsonObject();
        assertAll(
                () -> assertThat(firstFile.get("filename").getAsString()).isEqualTo("src/Main.java"),
                () -> assertThat(firstFile.get("additions").getAsInt()).isEqualTo(10),
                () -> assertThat(firstFile.get("deletions").getAsInt()).isEqualTo(5),
                () -> assertThat(firstFile.get("patch").getAsString()).isEqualTo("@@ -1,5 +1,10 @@")
        );
    }

    @Test
    void mapsReviews() {
        final JsonArray reviews = new JsonArray();
        reviews.add(buildReview("reviewer1", "APPROVED", "Looks good", "2024-01-02T00:00:00Z"));

        final JsonObject result = mapper.map(buildMinimalPrDetail(), new JsonArray(), new JsonArray(),
                new JsonArray(), new JsonArray(), reviews);

        final JsonObject firstReview = result.getAsJsonArray("reviews").get(0).getAsJsonObject();
        assertAll(
                () -> assertThat(firstReview.get("user").getAsString()).isEqualTo("reviewer1"),
                () -> assertThat(firstReview.get("state").getAsString()).isEqualTo("APPROVED"),
                () -> assertThat(firstReview.get("body").getAsString()).isEqualTo("Looks good")
        );
    }

    @Test
    void mapsReviewComments() {
        final JsonArray reviewComments = new JsonArray();
        final JsonObject comment = new JsonObject();
        final JsonObject commentUser = new JsonObject();
        commentUser.addProperty("login", "reviewer2");
        comment.add("user", commentUser);
        comment.addProperty("body", "Nit: rename this");
        comment.addProperty("path", "src/App.java");
        comment.addProperty("line", 42);
        comment.addProperty("created_at", "2024-01-03T00:00:00Z");
        reviewComments.add(comment);

        final JsonObject result = mapper.map(buildMinimalPrDetail(), new JsonArray(), new JsonArray(),
                reviewComments, new JsonArray(), new JsonArray());

        final JsonObject first = result.getAsJsonArray("review_comments").get(0).getAsJsonObject();
        assertAll(
                () -> assertThat(first.get("user").getAsString()).isEqualTo("reviewer2"),
                () -> assertThat(first.get("body").getAsString()).isEqualTo("Nit: rename this"),
                () -> assertThat(first.get("path").getAsString()).isEqualTo("src/App.java"),
                () -> assertThat(first.get("line").getAsInt()).isEqualTo(42)
        );
    }

    @Test
    void mapsIssueComments() {
        final JsonArray issueComments = new JsonArray();
        final JsonObject comment = new JsonObject();
        final JsonObject commentUser = new JsonObject();
        commentUser.addProperty("login", "commenter");
        comment.add("user", commentUser);
        comment.addProperty("body", "Looks great!");
        comment.addProperty("created_at", "2024-01-04T00:00:00Z");
        issueComments.add(comment);

        final JsonObject result = mapper.map(buildMinimalPrDetail(), new JsonArray(), new JsonArray(),
                new JsonArray(), issueComments, new JsonArray());

        final JsonObject first = result.getAsJsonArray("issue_comments").get(0).getAsJsonObject();
        assertAll(
                () -> assertThat(first.get("user").getAsString()).isEqualTo("commenter"),
                () -> assertThat(first.get("body").getAsString()).isEqualTo("Looks great!")
        );
    }

    @Test
    void mapsMilestoneWhenPresent() {
        final JsonObject detail = buildMinimalPrDetail();
        final JsonObject milestone = new JsonObject();
        milestone.addProperty("title", "v1.0");
        detail.add("milestone", milestone);

        final JsonObject result = mapper.map(detail, new JsonArray(), new JsonArray(),
                new JsonArray(), new JsonArray(), new JsonArray());

        assertThat(result.get("milestone").getAsString()).isEqualTo("v1.0");
    }

    @Test
    void mapsMilestoneAsNullWhenAbsent() {
        final JsonObject result = mapper.map(buildMinimalPrDetail(), new JsonArray(), new JsonArray(),
                new JsonArray(), new JsonArray(), new JsonArray());

        assertThat(result.get("milestone").isJsonNull()).isTrue();
    }

    private JsonObject buildMinimalPrDetail() {
        final JsonObject pr = new JsonObject();
        pr.addProperty("number", 1);
        pr.addProperty("title", "Fix bug");
        pr.addProperty("state", "open");
        pr.addProperty("merged", false);
        pr.addProperty("created_at", "2024-01-01T00:00:00Z");
        pr.addProperty("updated_at", "2024-01-01T00:00:00Z");
        pr.addProperty("body", "Fixes a bug");
        pr.add("labels", new JsonArray());

        final JsonObject user = new JsonObject();
        user.addProperty("login", "testuser");
        pr.add("user", user);

        final JsonObject base = new JsonObject();
        base.addProperty("ref", "main");
        pr.add("base", base);

        final JsonObject head = new JsonObject();
        head.addProperty("ref", "fix-branch");
        pr.add("head", head);

        return pr;
    }

    private JsonObject buildCommit(final String sha, final String message,
                                   final String authorName, final String date) {
        final JsonObject commit = new JsonObject();
        commit.addProperty("sha", sha);
        final JsonObject inner = new JsonObject();
        inner.addProperty("message", message);
        final JsonObject author = new JsonObject();
        author.addProperty("name", authorName);
        author.addProperty("date", date);
        inner.add("author", author);
        commit.add("commit", inner);
        return commit;
    }

    private JsonObject buildFile(final String filename, final String status,
                                 final int additions, final int deletions,
                                 final int changes, final String patch) {
        final JsonObject file = new JsonObject();
        file.addProperty("filename", filename);
        file.addProperty("status", status);
        file.addProperty("additions", additions);
        file.addProperty("deletions", deletions);
        file.addProperty("changes", changes);
        file.addProperty("patch", patch);
        return file;
    }

    private JsonObject buildReview(final String user, final String state,
                                   final String body, final String submittedAt) {
        final JsonObject review = new JsonObject();
        final JsonObject userObj = new JsonObject();
        userObj.addProperty("login", user);
        review.add("user", userObj);
        review.addProperty("state", state);
        review.addProperty("body", body);
        review.addProperty("submitted_at", submittedAt);
        return review;
    }
}
