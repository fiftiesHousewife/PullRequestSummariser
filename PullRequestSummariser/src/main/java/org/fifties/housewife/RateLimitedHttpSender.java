package org.fifties.housewife;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.log4j.Log4j2;

@Log4j2
final class RateLimitedHttpSender {
    private static final Pattern LINK_NEXT = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private static final int RATE_LIMIT_THRESHOLD = 10;

    private final HttpClient httpClient = ProxyAwareHttpClient.create();
    private final String token;

    RateLimitedHttpSender(final String token) {
        this.token = token;
    }

    HttpResponse<String> send(final String url) throws IOException, InterruptedException {
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

    String extractNextLink(final HttpResponse<String> response) {
        final String linkHeader = response.headers().firstValue("Link").orElse("");
        final Matcher matcher = LINK_NEXT.matcher(linkHeader);
        return matcher.find() ? matcher.group(1) : null;
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
}
