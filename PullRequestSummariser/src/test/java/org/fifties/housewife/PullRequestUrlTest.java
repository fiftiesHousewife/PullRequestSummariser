package org.fifties.housewife;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class PullRequestUrlTest {

    @Test
    void parsesStandardGitHubUrl() {
        final PullRequestUrl url = PullRequestUrl.parse("https://github.com/octocat/hello-world/pull/42");
        assertAll(
                () -> assertThat(url.owner()).isEqualTo("octocat"),
                () -> assertThat(url.repo()).isEqualTo("hello-world"),
                () -> assertThat(url.number()).isEqualTo(42)
        );
    }

    @Test
    void parsesUrlWithSurroundingWhitespace() {
        final PullRequestUrl url = PullRequestUrl.parse("  https://github.com/foo/bar/pull/7  ");
        assertAll(
                () -> assertThat(url.owner()).isEqualTo("foo"),
                () -> assertThat(url.repo()).isEqualTo("bar"),
                () -> assertThat(url.number()).isEqualTo(7)
        );
    }

    @Test
    void parsesHttpUrl() {
        final PullRequestUrl url = PullRequestUrl.parse("http://github.com/owner/repo/pull/1");
        assertThat(url.number()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidUrl() {
        assertThatThrownBy(() -> PullRequestUrl.parse("not-a-url"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid pull request URL");
    }

    @Test
    void fullNameCombinesOwnerAndRepo() {
        final PullRequestUrl url = PullRequestUrl.parse("https://github.com/acme/widgets/pull/99");
        assertThat(url.fullName()).isEqualTo("acme/widgets");
    }
}
