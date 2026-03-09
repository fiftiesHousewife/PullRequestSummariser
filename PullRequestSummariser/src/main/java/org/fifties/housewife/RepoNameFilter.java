package org.fifties.housewife;

import java.util.List;

final class RepoNameFilter {

    private RepoNameFilter() {
    }

    static boolean matches(final String fullName, final List<String> patterns) {
        if (patterns.isEmpty()) {
            return true;
        }
        final String repoName = fullName.contains("/") ? fullName.split("/")[1] : fullName;
        for (final String pattern : patterns) {
            if (fullName.equalsIgnoreCase(pattern)) {
                return true;
            }
            if (repoName.toLowerCase().contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    static boolean isFullName(final String repo) {
        return repo.contains("/");
    }
}
