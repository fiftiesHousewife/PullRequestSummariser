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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class CsvPullRequestExtract {

    private static final Logger LOG = Logger.getLogger(CsvPullRequestExtract.class.getName());
    private static final Path OUTPUT_DIR = Paths.get("output");

    private final GitHubClient client;
    private final PullRequestDataMapper mapper = new PullRequestDataMapper();

    CsvPullRequestExtract(final GitHubClient client) {
        this.client = client;
    }

    int extractFromCsv(final Path csvFile) throws IOException, InterruptedException {
        final List<PullRequestUrl> urls = new CsvPullRequestReader().read(csvFile);
        if (urls.isEmpty()) {
            LOG.info("No pull request URLs found in " + csvFile);
            return 0;
        }

        final Map<String, List<PullRequestUrl>> byRepo = urls.stream()
                .collect(Collectors.groupingBy(PullRequestUrl::fullName, LinkedHashMap::new, Collectors.toList()));

        LOG.info("Found " + urls.size() + " pull requests across " + byRepo.size() + " repositories");

        int totalExtracted = 0;
        for (final Map.Entry<String, List<PullRequestUrl>> entry : byRepo.entrySet()) {
            totalExtracted += extractRepoGroup(entry.getKey(), entry.getValue());
        }

        LOG.info("Total pull requests extracted: " + totalExtracted);
        return totalExtracted;
    }

    private int extractRepoGroup(final String fullName, final List<PullRequestUrl> urls)
            throws IOException, InterruptedException {
        final PullRequestUrl first = urls.get(0);
        final Path outputDirectory = OUTPUT_DIR.resolve(first.owner() + "_" + first.repo());
        Files.createDirectories(outputDirectory);

        LOG.info("Extracting " + urls.size() + " pull requests from " + fullName);

        int extracted = 0;
        for (int i = 0; i < urls.size(); i++) {
            final PullRequestUrl url = urls.get(i);
            LOG.info("[" + (i + 1) + "/" + urls.size() + "] Pull request #" + url.number());

            try {
                final JsonObject data = extractSinglePullRequest(url);
                final String json = new GsonBuilder().setPrettyPrinting().create().toJson(data);
                Files.writeString(outputDirectory.resolve("pr_" + url.number() + ".json"), json);
                extracted++;
            } catch (final IOException exception) {
                LOG.warning("Failed to extract pull request #" + url.number() + ": " + exception.getMessage());
            }
        }

        writeExtractionMeta(outputDirectory, fullName, extracted);
        return extracted;
    }

    private JsonObject extractSinglePullRequest(final PullRequestUrl url)
            throws IOException, InterruptedException {
        final String owner = url.owner();
        final String repo = url.repo();
        final int number = url.number();
        final JsonObject detail = client.fetchPullRequestDetail(owner, repo, number);
        final JsonArray commits = client.fetchPullRequestCommits(owner, repo, number);
        final JsonArray files = client.fetchPullRequestFiles(owner, repo, number);
        final JsonArray reviewComments = client.fetchReviewComments(owner, repo, number);
        final JsonArray issueComments = client.fetchIssueComments(owner, repo, number);
        final JsonArray reviews = client.fetchReviews(owner, repo, number);
        return mapper.map(detail, commits, files, reviewComments, issueComments, reviews);
    }

    private void writeExtractionMeta(final Path outputDirectory, final String fullName, final int extracted)
            throws IOException, InterruptedException {
        final JsonObject rate = client.fetchRateLimit();
        final String now = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC));

        final JsonObject meta = new JsonObject();
        meta.addProperty("repo", fullName);
        meta.addProperty("source", "csv");
        meta.addProperty("prs_extracted", extracted);
        meta.addProperty("extracted_at", now);
        meta.addProperty("rate_limit_remaining", rate.get("remaining").getAsInt());

        final String json = new GsonBuilder().setPrettyPrinting().create().toJson(meta);
        Files.writeString(outputDirectory.resolve("extraction_meta.json"), json);

        LOG.info("Done. " + extracted + " pull requests saved. Rate limit: "
                + rate.get("remaining").getAsInt() + "/" + rate.get("limit").getAsInt());
    }
}
