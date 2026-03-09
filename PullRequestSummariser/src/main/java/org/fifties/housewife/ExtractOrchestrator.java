package org.fifties.housewife;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.log4j.Log4j2;

@Log4j2
final class ExtractOrchestrator {

    private final GitHubClient client;
    private final PullRequestExtract extractor;

    ExtractOrchestrator(final GitHubClient client) {
        this.client = client;
        this.extractor = new PullRequestExtract(client);
    }

    void run(final ExtractArguments arguments) throws IOException, InterruptedException {
        final List<String> resolvedRepos = resolveRepos(arguments);
        if (resolvedRepos.isEmpty()) {
            log.info("No repositories matched the given criteria");
            return;
        }

        int total = 0;
        for (final String repoName : resolvedRepos) {
            total += extractor.extractRepo(repoName, arguments.state(), arguments.limit());
        }
        log.info("Total pull requests extracted: " + total);
    }

    private List<String> resolveRepos(final ExtractArguments arguments)
            throws IOException, InterruptedException {
        final boolean hasUsers = !arguments.users().isEmpty();
        final boolean hasRepos = !arguments.repos().isEmpty();
        final boolean hasPartialRepos = hasRepos && arguments.repos().stream()
                .anyMatch(repo -> !RepoNameFilter.isFullName(repo));

        if (hasPartialRepos && !hasUsers) {
            log.error("Partial repo names require --user to know where to search");
            return List.of();
        }

        if (hasUsers) {
            return resolveFromUsers(arguments);
        }
        return arguments.repos();
    }

    private List<String> resolveFromUsers(final ExtractArguments arguments)
            throws IOException, InterruptedException {
        final Set<String> result = new LinkedHashSet<>();
        for (final String username : arguments.users()) {
            final List<String> userRepos = client.fetchUserRepos(username);
            if (userRepos.isEmpty()) {
                log.info("No repos found for user '" + username + "'");
                continue;
            }
            log.info("Found " + userRepos.size() + " repos for " + username);
            for (final String repoName : userRepos) {
                if (RepoNameFilter.matches(repoName, arguments.repos())) {
                    result.add(repoName);
                }
            }
        }
        return new ArrayList<>(result);
    }
}
