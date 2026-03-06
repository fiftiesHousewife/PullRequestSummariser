package org.fifties.housewife;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

record PullRequestUrl(String owner, String repo, int number) {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://github\\.com/([^/]+)/([^/]+)/pull/(\\d+)");

    static PullRequestUrl parse(final String text) {
        final Matcher matcher = URL_PATTERN.matcher(text.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid pull request URL: " + text);
        }
        return new PullRequestUrl(matcher.group(1), matcher.group(2), Integer.parseInt(matcher.group(3)));
    }

    String fullName() {
        return owner + "/" + repo;
    }
}
