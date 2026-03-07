package org.fifties.housewife;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class PullRequestSummarizeDirectoryTest {

    private final PullRequestMarkdownWriter pullRequestWriter = new PullRequestMarkdownWriter(500);
    private final RepoMarkdownWriter repoWriter = new RepoMarkdownWriter();

    @Test
    void writesSummaryFilesAlongsideJson(@TempDir final Path tempDir) throws Exception {
        writePullRequestFile(tempDir, 1);
        writeMetaFile(tempDir);

        PullRequestSummarize.summarizeDirectory(tempDir, pullRequestWriter, repoWriter);

        assertAll(
                () -> assertThat(tempDir.resolve("pr_1_summary.md")).exists(),
                () -> assertThat(tempDir.resolve("repo_summary.md")).exists()
        );
    }

    @Test
    void summaryContainsExpectedSections(@TempDir final Path tempDir) throws Exception {
        writePullRequestFile(tempDir, 1);
        writeMetaFile(tempDir);

        PullRequestSummarize.summarizeDirectory(tempDir, pullRequestWriter, repoWriter);

        final String prSummary = Files.readString(tempDir.resolve("pr_1_summary.md"));
        final String repoSummary = Files.readString(tempDir.resolve("repo_summary.md"));

        assertAll(
                () -> assertThat(prSummary).contains("# Pull Request #1:"),
                () -> assertThat(prSummary).contains("## Verdict"),
                () -> assertThat(repoSummary).contains("# Repository Summary:"),
                () -> assertThat(repoSummary).contains("## Pull Request Index")
        );
    }

    @Test
    void skipsNonDirectoryInput(@TempDir final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("notadir.txt");
        Files.writeString(file, "not a directory");

        PullRequestSummarize.summarizeDirectory(file, pullRequestWriter, repoWriter);

        assertThat(tempDir.resolve("repo_summary.md")).doesNotExist();
    }

    @Test
    void skipsDirectoryWithNoPullRequests(@TempDir final Path tempDir) throws Exception {
        PullRequestSummarize.summarizeDirectory(tempDir, pullRequestWriter, repoWriter);
        assertThat(tempDir.resolve("repo_summary.md")).doesNotExist();
    }

    private void writePullRequestFile(final Path directory, final int number) throws Exception {
        final JsonObject pr = new JsonObject();
        pr.addProperty("number", number);
        pr.addProperty("title", "Test pull request #" + number);
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

        Files.writeString(directory.resolve("pr_" + number + ".json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(pr));
    }

    private void writeMetaFile(final Path directory) throws Exception {
        final JsonObject meta = new JsonObject();
        meta.addProperty("repo", "owner/repo");
        meta.addProperty("extracted_at", "2024-01-15T00:00:00Z");
        Files.writeString(directory.resolve("extraction_meta.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(meta));
    }
}
