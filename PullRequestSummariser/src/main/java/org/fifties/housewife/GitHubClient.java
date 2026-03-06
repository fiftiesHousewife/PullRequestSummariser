package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.log4j.Log4j2;

@Log4j2
final class GitHubClient {
    private static final String BASE_URL = "https://api.github.com";
    private static final Pattern LINK_NEXT = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private static final int RATE_LIMIT_THRESHOLD = 10;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .proxy(ProxySelector.getDefault())
            .build();
    private final String token;

    GitHubClient(final String token) {
        this.token = token;
    }

    JsonObject getJson(final String url) throws IOException, InterruptedException {
        final HttpResponse<String> response = send(url);
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    JsonArray paginate(final String url) throws IOException, InterruptedException {
        final JsonArray results = new JsonArray();
        String currentUrl = url.contains("?") ? url + "&per_page=100" : url + "?per_page=100";

        while (currentUrl != null) {
            final HttpResponse<String> response = send(currentUrl);
            results.addAll(JsonParser.parseString(response.body()).getAsJsonArray());
            currentUrl = extractNextLink(response);
        }
        return results;
    }

    List<String> fetchUserRepos(final String username) throws IOException, InterruptedException {
        final JsonArray repos = paginate(BASE_URL + "/users/" + username + "/repos?type=owner&sort=updated");
        final List<String> result = new ArrayList<>();
        for (final JsonElement repo : repos) {
            result.add(repo.getAsJsonObject().get("full_name").getAsString());
        }
        return result;
    }

    JsonObject fetchPullRequestDetail(final String owner, final String repo, final int number)
            throws IOException, InterruptedException {
        return getJson(repoUrl(owner, repo) + "/pulls/" + number);
    }

    JsonArray fetchPullRequestCommits(final String owner, final String repo, final int number)
            throws IOException, InterruptedException {
        return paginate(repoUrl(owner, repo) + "/pulls/" + number + "/commits");
    }

    JsonArray fetchPullRequestFiles(final String owner, final String repo, final int number)
            throws IOException, InterruptedException {
        return paginate(repoUrl(owner, repo) + "/pulls/" + number + "/files");
    }

    JsonArray fetchReviewComments(final String owner, final String repo, final int number)
            throws IOException, InterruptedException {
        return paginate(repoUrl(owner, repo) + "/pulls/" + number + "/comments");
    }

    JsonArray fetchIssueComments(final String owner, final String repo, final int number)
            throws IOException, InterruptedException {
        return paginate(repoUrl(owner, repo) + "/issues/" + number + "/comments");
    }

    JsonArray fetchReviews(final String owner, final String repo, final int number)
            throws IOException, InterruptedException {
        return paginate(repoUrl(owner, repo) + "/pulls/" + number + "/reviews");
    }

    JsonArray fetchPullRequestList(final String owner, final String repo, final String state)
            throws IOException, InterruptedException {
        return paginate(repoUrl(owner, repo) + "/pulls?state=" + state + "&sort=updated&direction=desc");
    }

    JsonObject fetchRateLimit() throws IOException, InterruptedException {
        return getJson(BASE_URL + "/rate_limit").getAsJsonObject("rate");
    }

    private String repoUrl(final String owner, final String repo) {
        return BASE_URL + "/repos/" + owner + "/" + repo;
    }

    private HttpResponse<String> send(final String url) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url + ": " + response.body());
        }
        waitIfRateLimited(response);
        return response;
    }

    private void waitIfRateLimited(final HttpResponse<String> response) throws InterruptedException {
        final int remaining = response.headers().firstValue("X-RateLimit-Remaining")
                .map(Integer::parseInt).orElse(Integer.MAX_VALUE);
        if (remaining < RATE_LIMIT_THRESHOLD) {
            final long resetEpoch = response.headers().firstValue("X-RateLimit-Reset")
                    .map(Long::parseLong).orElse(0L);
            final long waitSeconds = resetEpoch - Instant.now().getEpochSecond() + 1;
            if (waitSeconds > 0) {
                log.warn("Rate limit low (" + remaining + " remaining). Waiting " + waitSeconds + " seconds...");
                Thread.sleep(waitSeconds * 1000);
            }
        }
    }

    private String extractNextLink(final HttpResponse<String> response) {
        final String linkHeader = response.headers().firstValue("Link").orElse("");
        final Matcher matcher = LINK_NEXT.matcher(linkHeader);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
