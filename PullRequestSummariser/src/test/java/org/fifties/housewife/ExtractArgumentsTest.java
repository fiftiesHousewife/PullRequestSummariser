package org.fifties.housewife;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ExtractArgumentsTest {

    @Test
    void parsesRepoArgument() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--repo", "owner/repo"});
        assertAll(
                () -> assertThat(args.repo).isEqualTo("owner/repo"),
                () -> assertThat(args.user).isNull(),
                () -> assertThat(args.csvFile).isNull(),
                () -> assertThat(args.state).isEqualTo("all"),
                () -> assertThat(args.limit).isZero()
        );
    }

    @Test
    void parsesUserArgument() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--user", "someone"});
        assertAll(
                () -> assertThat(args.user).isEqualTo("someone"),
                () -> assertThat(args.repo).isNull()
        );
    }

    @Test
    void parsesCsvArgument() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--csv", "prs.csv"});
        assertAll(
                () -> assertThat(args.csvFile).isEqualTo("prs.csv"),
                () -> assertThat(args.repo).isNull(),
                () -> assertThat(args.user).isNull()
        );
    }

    @Test
    void parsesStateAndLimit() {
        final ExtractArguments args = ExtractArguments.parse(
                new String[]{"--repo", "o/r", "--state", "closed", "--limit", "10"});
        assertAll(
                () -> assertThat(args.state).isEqualTo("closed"),
                () -> assertThat(args.limit).isEqualTo(10)
        );
    }

    @Test
    void defaultsStateToAll() {
        final ExtractArguments args = ExtractArguments.parse(new String[]{"--repo", "o/r"});
        assertThat(args.state).isEqualTo("all");
    }
}
