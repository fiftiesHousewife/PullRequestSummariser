package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PrDataMapperTest {

    private final PullRequestDataMapper mapper = new PullRequestDataMapper();

    @Test
    void mapsBasicPrMetadata() {
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
