package org.fifties.housewife;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ExtractArgumentsTest {

    @Test
    void parsesSingleRepoArgument() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--repo", "owner/repo"});
        assertAll(
                () -> assertThat(args.repos()).containsExactly("owner/repo"),
                () -> assertThat(args.users()).isEmpty(),
                () -> assertThat(args.csvFile()).isNull(),
                () -> assertThat(args.state()).isEqualTo("all"),
                () -> assertThat(args.limit()).isZero()
        );
    }

    @Test
    void parsesMultipleRepoArguments() {
        final ExtractArguments args = ExtractArguments.parse(
                new String[]{"--repo", "owner/repo1", "--repo", "owner/repo2"});
        assertThat(args.repos()).containsExactly("owner/repo1", "owner/repo2");
    }

    @Test
    void parsesSingleUserArgument() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--user", "someone"});
        assertAll(
                () -> assertThat(args.users()).containsExactly("someone"),
                () -> assertThat(args.repos()).isEmpty()
        );
    }

    @Test
    void parsesMultipleUserArguments() {
        final ExtractArguments args = ExtractArguments.parse(
                new String[]{"--user", "alice", "--user", "bob"});
        assertThat(args.users()).containsExactly("alice", "bob");
    }

    @Test
    void parsesUserAndRepoTogether() {
        final ExtractArguments args = ExtractArguments.parse(
                new String[]{"--user", "alice", "--repo", "my-service"});
        assertAll(
                () -> assertThat(args.users()).containsExactly("alice"),
                () -> assertThat(args.repos()).containsExactly("my-service")
        );
    }

    @Test
    void parsesCsvArgument() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--csv", "prs.csv"});
        assertAll(
                () -> assertThat(args.csvFile()).isEqualTo("prs.csv"),
                () -> assertThat(args.repos()).isEmpty(),
                () -> assertThat(args.users()).isEmpty()
        );
    }

    @Test
    void parsesStateAndLimit() {
        final ExtractArguments args = ExtractArguments.parse(
                new String[]{"--repo", "o/r", "--state", "closed", "--limit", "10"});
        assertAll(
                () -> assertThat(args.state()).isEqualTo("closed"),
                () -> assertThat(args.limit()).isEqualTo(10)
        );
    }

    @Test
    void defaultsStateToAll() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--repo", "o/r"});
        assertThat(args.state()).isEqualTo("all");
    }

    @Test
    void repoListIsUnmodifiable() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--repo", "o/r"});
        assertThat(args.repos()).isUnmodifiable();
    }

    @Test
    void userListIsUnmodifiable() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--user", "someone"});
        assertThat(args.users()).isUnmodifiable();
    }
}
