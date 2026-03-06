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
import java.util.logging.Logger;

public final class PullRequestExtract {

    private static final Logger LOG = Logger.getLogger(PullRequestExtract.class.getName());
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
        final Path outputDirectory = OUTPUT_DIR.resolve(owner + "_" + repo);
        Files.createDirectories(outputDirectory);

        LOG.info("Extracting pull requests from " + fullName + " (state=" + state + ")");

        JsonArray pullRequests = client.fetchPullRequestList(owner, repo, state);
        if (limit > 0 && pullRequests.size() > limit) {
            final JsonArray trimmed = new JsonArray();
            for (int i = 0; i < limit; i++) {
                trimmed.add(pullRequests.get(i));
            }
            pullRequests = trimmed;
        }

        if (pullRequests.isEmpty()) {
            LOG.info("No pull requests found for " + fullName);
            return 0;
        }

        LOG.info("Found " + pullRequests.size() + " pull requests, extracting details...");

        int extracted = 0;
        for (int i = 0; i < pullRequests.size(); i++) {
            final JsonObject pullRequest = pullRequests.get(i).getAsJsonObject();
            final int number = pullRequest.get("number").getAsInt();
            final String title = pullRequest.get("title").getAsString();
            LOG.info("[" + (i + 1) + "/" + pullRequests.size() + "] Pull request #" + number + ": " + title);

            try {
                final JsonObject data = extractSinglePr(owner, repo, number);
                final String json = new GsonBuilder().setPrettyPrinting().create().toJson(data);
                Files.writeString(outputDirectory.resolve("pr_" + number + ".json"), json);
                extracted++;
            } catch (final IOException exception) {
                LOG.warning("Failed to extract pull request #" + number + ": " + exception.getMessage());
            }
        }

        writeExtractionMeta(outputDirectory, fullName, state, extracted);
        return extracted;
    }

    private JsonObject extractSinglePr(final String owner, final String repo, final int number)
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

        LOG.info("Done. " + extracted + " pull requests saved. Rate limit: "
                + rate.get("remaining").getAsInt() + "/" + rate.get("limit").getAsInt());
    }

    public static void main(final String[] args) throws Exception {
        String repo = null;
        String user = null;
        String state = "all";
        int limit = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--repo":
                    repo = args[++i];
                    break;
                case "--user":
                    user = args[++i];
                    break;
                case "--state":
                    state = args[++i];
                    break;
                case "--limit":
                    limit = Integer.parseInt(args[++i]);
                    break;
                default:
                    LOG.severe("Unknown argument: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }

        if (repo == null && user == null) {
            printUsage();
            System.exit(1);
        }

        final String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isEmpty()) {
            LOG.severe("GITHUB_TOKEN environment variable is required.\n"
                    + "Create a Personal Access Token at https://github.com/settings/tokens\n"
                    + "Then: export GITHUB_TOKEN=ghp_your_token_here");
            System.exit(1);
        }

        final GitHubClient client = new GitHubClient(token);
        final PullRequestExtract extractor = new PullRequestExtract(client);

        if (user != null) {
            final List<String> repos = client.fetchUserRepos(user);
            if (repos.isEmpty()) {
                LOG.info("No repos found for user '" + user + "'");
                System.exit(1);
            }
            LOG.info("Found " + repos.size() + " repos for " + user);
            int total = 0;
            for (final String repoName : repos) {
                total += extractor.extractRepo(repoName, state, limit);
            }
            LOG.info("Total pull requests extracted: " + total);
        } else {
            extractor.extractRepo(repo, state, limit);
        }
    }

    private static void printUsage() {
        LOG.info("Usage:\n"
                + "  PullRequestExtract --repo owner/repo [--state all|open|closed] [--limit N]\n"
                + "  PullRequestExtract --user username [--state all|open|closed] [--limit N]");
    }
}
