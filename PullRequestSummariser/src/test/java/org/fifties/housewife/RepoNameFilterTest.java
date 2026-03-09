package org.fifties.housewife;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class RepoNameFilterTest {

    @Test
    void matchesAllWhenNoPatternsProvided() {
        assertThat(RepoNameFilter.matches("owner/my-repo", List.of())).isTrue();
    }

    @Test
    void matchesExactFullName() {
        assertThat(RepoNameFilter.matches("owner/my-repo", List.of("owner/my-repo"))).isTrue();
    }

    @Test
    void matchesExactFullNameCaseInsensitively() {
        assertThat(RepoNameFilter.matches("Owner/My-Repo", List.of("owner/my-repo"))).isTrue();
    }

    @Test
    void matchesPartialRepoName() {
        assertThat(RepoNameFilter.matches("owner/my-service-api", List.of("service"))).isTrue();
    }

    @Test
    void matchesPartialRepoNameCaseInsensitively() {
        assertThat(RepoNameFilter.matches("owner/MyService", List.of("myservice"))).isTrue();
    }

    @Test
    void rejectsNonMatchingPattern() {
        assertThat(RepoNameFilter.matches("owner/my-repo", List.of("other"))).isFalse();
    }

    @Test
    void matchesIfAnyPatternMatches() {
        assertThat(RepoNameFilter.matches("owner/my-repo", List.of("other", "my-repo"))).isTrue();
    }

    @Test
    void detectsFullName() {
        assertAll(
                () -> assertThat(RepoNameFilter.isFullName("owner/repo")).isTrue(),
                () -> assertThat(RepoNameFilter.isFullName("repo")).isFalse()
        );
    }
}
