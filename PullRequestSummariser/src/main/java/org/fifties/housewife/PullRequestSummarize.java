package org.fifties.housewife;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class PullRequestSummarize {

    public static void main(final String[] args) throws Exception {
        final List<String> inputDirectories = new ArrayList<>();
        int maxDiffLines = 500;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input":
                    i++;
                    while (i < args.length && !args[i].startsWith("--")) {
                        inputDirectories.add(args[i]);
                        i++;
                    }
                    i--;
                    break;
                case "--max-diff-lines":
                    maxDiffLines = Integer.parseInt(args[++i]);
                    break;
                default:
                    log.error("Unknown argument: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }

        if (inputDirectories.isEmpty()) {
            printUsage();
            System.exit(1);
        }

        final PullRequestMarkdownWriter pullRequestWriter = new PullRequestMarkdownWriter(maxDiffLines);
        final RepoMarkdownWriter repoWriter = new RepoMarkdownWriter();

        for (final String inputDir : inputDirectories) {
            summarizeDirectory(Paths.get(inputDir), pullRequestWriter, repoWriter);
        }
    }

    static void summarizeDirectory(final Path inputPath, final PullRequestMarkdownWriter pullRequestWriter,
                                   final RepoMarkdownWriter repoWriter) throws IOException {
        if (!Files.isDirectory(inputPath)) {
            log.warn("Skipping " + inputPath + " — not a directory");
            return;
        }

        final List<JsonObject> pullRequests = loadPullRequests(inputPath);
        final JsonObject meta = loadMeta(inputPath);

        if (pullRequests.isEmpty()) {
            log.info("No pull requests found in " + inputPath);
            return;
        }

        log.info("Summarizing " + inputPath + " (" + pullRequests.size() + " pull requests)");

        for (final JsonObject pr : pullRequests) {
            final String summary = pullRequestWriter.write(pr);
            final int number = JsonFields.num(pr, "number");
            Files.writeString(inputPath.resolve("pr_" + number + "_summary.md"), summary);
        }

        final String repoSummary = repoWriter.write(pullRequests, meta);
        Files.writeString(inputPath.resolve("repo_summary.md"), repoSummary);

        log.info("Summaries written to " + inputPath + "/");
    }

    static List<JsonObject> loadPullRequests(final Path inputDirectory) throws IOException {
        final List<JsonObject> pullRequests = new ArrayList<>();
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(inputDirectory, "pr_*.json")) {
            final List<Path> sorted = new ArrayList<>();
            stream.forEach(sorted::add);
            Collections.sort(sorted);
            for (final Path file : sorted) {
                final String content = Files.readString(file);
                pullRequests.add(JsonParser.parseString(content).getAsJsonObject());
            }
        }
        return pullRequests;
    }

    private static JsonObject loadMeta(final Path inputDirectory) throws IOException {
        final Path metaFile = inputDirectory.resolve("extraction_meta.json");
        if (Files.exists(metaFile)) {
            return JsonParser.parseString(Files.readString(metaFile)).getAsJsonObject();
        }
        return new JsonObject();
    }

    private static void printUsage() {
        log.info("Usage:\n  PullRequestSummarize --input output/owner/repo/ [--max-diff-lines 500]");
    }
}
