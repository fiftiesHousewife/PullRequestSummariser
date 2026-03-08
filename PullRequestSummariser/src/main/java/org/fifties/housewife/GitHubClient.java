package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.log4j.Log4j2;

@Log4j2
final class GitHubClient {
    private static final String DEFAULT_BASE_URL = "https://api.github.com";

    private final RateLimitedHttpSender sender;
    private final String baseUrl;

    static GitHubClient fromEnvironment(final String token) {
        final String url = System.getenv("GITHUB_API_URL");
        if (url != null && !url.isEmpty()) {
            log.info("Using GitHub API URL: " + url);
            return new GitHubClient(url, token);
        }
        return new GitHubClient(token);
    }

    GitHubClient(final String token) {
        this(DEFAULT_BASE_URL, token);
    }

    GitHubClient(final String baseUrl, final String token) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.sender = new RateLimitedHttpSender(token);
    }

    JsonObject getJson(final String url) throws IOException, InterruptedException {
        final HttpResponse<String> response = sender.send(url);
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    JsonArray paginate(final String url) throws IOException, InterruptedException {
        final JsonArray results = new JsonArray();
        String currentUrl = url.contains("?") ? url + "&per_page=100" : url + "?per_page=100";

        while (currentUrl != null) {
            final HttpResponse<String> response = sender.send(currentUrl);
            results.addAll(JsonParser.parseString(response.body()).getAsJsonArray());
            currentUrl = sender.extractNextLink(response);
        }
        return results;
    }

    List<String> fetchUserRepos(final String username) throws IOException, InterruptedException {
        final JsonArray repos = paginate(baseUrl + "/users/" + username + "/repos?type=owner&sort=updated");
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
        return getJson(baseUrl + "/rate_limit").getAsJsonObject("rate");
    }

    private String repoUrl(final String owner, final String repo) {
        return baseUrl + "/repos/" + owner + "/" + repo;
    }

}
