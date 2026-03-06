package org.fifties.housewife;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Tag("integration")
class SelfTest {

    private static final String OWNER = "fiftiesHousewife";
    private static final String REPO = "PullRequestSummariser";
    private static final int PULL_REQUEST_NUMBER = 1;

    @TempDir
    private Path tempDirectory;

    @Test
    void extractPullRequestAndTransformToMarkdown() throws IOException, InterruptedException {
        final JsonObject mapped = extractFromApi();

        assertAll(
                () -> assertThat(mapped.get("number").getAsInt()).isEqualTo(PULL_REQUEST_NUMBER),
                () -> assertThat(mapped.get("title").getAsString()).isNotEmpty(),
                () -> assertThat(mapped.get("author").getAsString()).isEqualTo(OWNER),
                () -> assertThat(mapped.get("state").getAsString()).isIn("open", "closed"),
                () -> assertThat(mapped.getAsJsonArray("commits").size()).isGreaterThan(0),
                () -> assertThat(mapped.getAsJsonArray("files").size()).isGreaterThan(0),
                () -> assertThat(mapped.get("head_branch").getAsString()).isNotEmpty(),
                () -> assertThat(mapped.get("base_branch").getAsString()).isNotEmpty()
        );

        final String pullRequestMarkdown = new PullRequestMarkdownWriter(500).write(mapped);

        assertAll(
                () -> assertThat(pullRequestMarkdown).contains("# Pull Request #1:"),
                () -> assertThat(pullRequestMarkdown).contains("**Author:** " + OWNER),
                () -> assertThat(pullRequestMarkdown).contains("## Intent"),
                () -> assertThat(pullRequestMarkdown).contains("## Scope of Changes"),
                () -> assertThat(pullRequestMarkdown).contains("## Key Diffs"),
                () -> assertThat(pullRequestMarkdown).contains("## Verdict")
        );

        final JsonObject meta = new JsonObject();
        meta.addProperty("repo", OWNER + "/" + REPO);
        meta.addProperty("extracted_at", "self-test");

        final String repoMarkdown = new RepoMarkdownWriter().write(List.of(mapped), meta);

        assertAll(
                () -> assertThat(repoMarkdown).contains("# Repository Summary: " + OWNER + "/" + REPO),
                () -> assertThat(repoMarkdown).contains("## Pull Request Statistics"),
                () -> assertThat(repoMarkdown).contains("## Contributors"),
                () -> assertThat(repoMarkdown).contains("## Languages"),
                () -> assertThat(repoMarkdown).contains("## Pull Request Timeline"),
                () -> assertThat(repoMarkdown).contains("## Pull Request Index")
        );
    }

    @Test
    void extractPullRequestFromCsvAndTransformToMarkdown() throws IOException, InterruptedException {
        final Path csvFile = tempDirectory.resolve("self_test.csv");
        Files.writeString(csvFile, "https://github.com/" + OWNER + "/" + REPO + "/pull/" + PULL_REQUEST_NUMBER + "\n");

        final List<PullRequestUrl> urls = new CsvPullRequestReader().read(csvFile);
        assertThat(urls).hasSize(1);

        final PullRequestUrl url = urls.get(0);

        assertAll(
                () -> assertThat(url.owner()).isEqualTo(OWNER),
                () -> assertThat(url.repo()).isEqualTo(REPO),
                () -> assertThat(url.number()).isEqualTo(PULL_REQUEST_NUMBER)
        );

        final JsonObject mapped = extractFromApi();
        final Path outputDirectory = tempDirectory.resolve("output").resolve(OWNER).resolve(REPO);
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("pr_" + PULL_REQUEST_NUMBER + ".json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(mapped));

        final JsonObject meta = new JsonObject();
        meta.addProperty("repo", url.fullName());
        meta.addProperty("source", "csv");
        meta.addProperty("prs_extracted", 1);
        meta.addProperty("extracted_at", "self-test");
        Files.writeString(outputDirectory.resolve("extraction_meta.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(meta));

        final List<JsonObject> loadedPullRequests = PullRequestSummarize.loadPullRequests(outputDirectory);
        assertThat(loadedPullRequests).hasSize(1);

        final String pullRequestMarkdown = new PullRequestMarkdownWriter(500).write(loadedPullRequests.get(0));

        assertAll(
                () -> assertThat(pullRequestMarkdown).contains("# Pull Request #1:"),
                () -> assertThat(pullRequestMarkdown).contains("**Author:** " + OWNER),
                () -> assertThat(pullRequestMarkdown).contains("## Key Diffs"),
                () -> assertThat(pullRequestMarkdown).contains("```diff")
        );

        final JsonObject loadedMeta = JsonParser.parseString(
                Files.readString(outputDirectory.resolve("extraction_meta.json"))).getAsJsonObject();
        final String repoMarkdown = new RepoMarkdownWriter().write(loadedPullRequests, loadedMeta);

        assertAll(
                () -> assertThat(repoMarkdown).contains("# Repository Summary: " + OWNER + "/" + REPO),
                () -> assertThat(repoMarkdown).contains("**Total pull requests extracted:** 1"),
                () -> assertThat(repoMarkdown).contains("## Pull Request Index")
        );
    }

    private JsonObject extractFromApi() throws IOException, InterruptedException {
        final GitHubClient client = new GitHubClient(requireToken());
        final JsonObject detail = client.fetchPullRequestDetail(OWNER, REPO, PULL_REQUEST_NUMBER);
        return new PullRequestDataMapper().map(
                detail,
                client.fetchPullRequestCommits(OWNER, REPO, PULL_REQUEST_NUMBER),
                client.fetchPullRequestFiles(OWNER, REPO, PULL_REQUEST_NUMBER),
                client.fetchReviewComments(OWNER, REPO, PULL_REQUEST_NUMBER),
                client.fetchIssueComments(OWNER, REPO, PULL_REQUEST_NUMBER),
                client.fetchReviews(OWNER, REPO, PULL_REQUEST_NUMBER)
        );
    }

    private static String requireToken() {
        final String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException(
                    "GITHUB_TOKEN environment variable is required for integration tests");
        }
        return token;
    }
}
