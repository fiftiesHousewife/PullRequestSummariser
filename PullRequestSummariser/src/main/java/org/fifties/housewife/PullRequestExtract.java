package org.fifties.housewife;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class PullRequestExtract {
    private static final Path OUTPUT_DIR = Paths.get("output");

    private final GitHubClient client;
    private final PullRequestDataMapper mapper = new PullRequestDataMapper();

    PullRequestExtract(final GitHubClient client) {
        this.client = client;
    }

    int extractRepo(final String fullName, final String state, final int limit)
            throws IOException, InterruptedException {
        final String[] parts = fullName.split("/");
        final String owner = parts[0];
        final String repo = parts[1];
        final Path outputDirectory = OUTPUT_DIR.resolve(owner).resolve(repo);
        Files.createDirectories(outputDirectory);

        log.info("Extracting pull requests from " + fullName + " (state=" + state + ")");

        JsonArray pullRequests = client.fetchPullRequestList(owner, repo, state);
        if (limit > 0 && pullRequests.size() > limit) {
            final JsonArray trimmed = new JsonArray();
            for (int i = 0; i < limit; i++) {
                trimmed.add(pullRequests.get(i));
            }
            pullRequests = trimmed;
        }

        if (pullRequests.isEmpty()) {
            log.info("No pull requests found for " + fullName);
            return 0;
        }

        log.info("Found " + pullRequests.size() + " pull requests, extracting details...");

        int extracted = 0;
        for (int i = 0; i < pullRequests.size(); i++) {
            final JsonObject pullRequest = pullRequests.get(i).getAsJsonObject();
            final int number = pullRequest.get("number").getAsInt();
            final String title = pullRequest.get("title").getAsString();
            log.info("[" + (i + 1) + "/" + pullRequests.size() + "] Pull request #" + number + ": " + title);

            try {
                final JsonObject data = extractSinglePullRequest(owner, repo, number);
                final String json = new GsonBuilder().setPrettyPrinting().create().toJson(data);
                Files.writeString(outputDirectory.resolve("pr_" + number + ".json"), json);
                extracted++;
            } catch (final IOException exception) {
                log.warn("Failed to extract pull request #" + number + ": " + exception.getMessage());
            }
        }

        writeExtractionMeta(outputDirectory, fullName, state, extracted);
        return extracted;
    }

    private JsonObject extractSinglePullRequest(final String owner, final String repo, final int number)
            throws IOException, InterruptedException {
        final JsonObject detail = client.fetchPullRequestDetail(owner, repo, number);
        final JsonArray commits = client.fetchPullRequestCommits(owner, repo, number);
        final JsonArray files = client.fetchPullRequestFiles(owner, repo, number);
        final JsonArray reviewComments = client.fetchReviewComments(owner, repo, number);
        final JsonArray issueComments = client.fetchIssueComments(owner, repo, number);
        final JsonArray reviews = client.fetchReviews(owner, repo, number);
        return mapper.map(detail, commits, files, reviewComments, issueComments, reviews);
    }

    private void writeExtractionMeta(final Path outputDirectory, final String fullName,
                                     final String state, final int extracted)
            throws IOException, InterruptedException {
        final JsonObject rate = client.fetchRateLimit();
        final String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC));
        final long resetEpoch = rate.get("reset").getAsLong();
        final String resetTime = DateTimeFormatter.ISO_INSTANT.format(
                Instant.ofEpochSecond(resetEpoch).atOffset(ZoneOffset.UTC));

        final JsonObject meta = new JsonObject();
        meta.addProperty("repo", fullName);
        meta.addProperty("state_filter", state);
        meta.addProperty("prs_extracted", extracted);
        meta.addProperty("extracted_at", now);
        meta.addProperty("rate_limit_remaining", rate.get("remaining").getAsInt());
        meta.addProperty("rate_limit_reset", resetTime);

        final String json = new GsonBuilder().setPrettyPrinting().create().toJson(meta);
        Files.writeString(outputDirectory.resolve("extraction_meta.json"), json);

        log.info("Done. " + extracted + " pull requests saved. Rate limit: "
                + rate.get("remaining").getAsInt() + "/" + rate.get("limit").getAsInt());
    }

    public static void main(final String[] args) throws Exception {
        final ExtractArguments arguments = ExtractArguments.parse(args);
        final GitHubClient client = new GitHubClient(requireToken());

        if (arguments.csvFile() != null) {
            new CsvPullRequestExtract(client).extractFromCsv(Paths.get(arguments.csvFile()));
        } else if (arguments.user() != null) {
            extractAllReposForUser(client, arguments);
        } else {
            new PullRequestExtract(client).extractRepo(arguments.repo(), arguments.state(), arguments.limit());
        }
    }

    private static String requireToken() {
        final String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isEmpty()) {
            log.error("GITHUB_TOKEN environment variable is required.\n"
                    + "Create a Personal Access Token at https://github.com/settings/tokens\n"
                    + "Then: export GITHUB_TOKEN=ghp_your_token_here");
            System.exit(1);
        }
        return token;
    }

    private static void extractAllReposForUser(final GitHubClient client, final ExtractArguments arguments)
            throws IOException, InterruptedException {
        final List<String> repos = client.fetchUserRepos(arguments.user());
        if (repos.isEmpty()) {
            log.info("No repos found for user '" + arguments.user() + "'");
            System.exit(1);
        }
        log.info("Found " + repos.size() + " repos for " + arguments.user());
        final PullRequestExtract extractor = new PullRequestExtract(client);
        int total = 0;
        for (final String repoName : repos) {
            total += extractor.extractRepo(repoName, arguments.state(), arguments.limit());
        }
        log.info("Total pull requests extracted: " + total);
    }
}
