package org.fifties.housewife;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CsvPullRequestReaderTest {

    private final CsvPullRequestReader reader = new CsvPullRequestReader();

    @TempDir
    Path tempDir;

    @Test
    void readsOneUrlPerLine() throws IOException {
        final Path csv = writeCsv(
                "https://github.com/owner/repo/pull/1",
                "https://github.com/owner/repo/pull/2"
        );
        final List<PullRequestUrl> urls = reader.read(csv);
        assertAll(
                () -> assertThat(urls).hasSize(2),
                () -> assertThat(urls.get(0).number()).isEqualTo(1),
                () -> assertThat(urls.get(1).number()).isEqualTo(2)
        );
    }

    @Test
    void takesFirstColumnFromCsv() throws IOException {
        final Path csv = writeCsv(
                "https://github.com/owner/repo/pull/5, some description, extra"
        );
        final List<PullRequestUrl> urls = reader.read(csv);
        assertAll(
                () -> assertThat(urls).hasSize(1),
                () -> assertThat(urls.get(0).number()).isEqualTo(5)
        );
    }

    @Test
    void skipsBlankLinesAndComments() throws IOException {
        final Path csv = writeCsv(
                "# This is a header comment",
                "",
                "https://github.com/owner/repo/pull/3",
                "  ",
                "# Another comment"
        );
        final List<PullRequestUrl> urls = reader.read(csv);
        assertThat(urls).hasSize(1);
    }

    @Test
    void skipsInvalidUrlsWithWarning() throws IOException {
        final Path csv = writeCsv(
                "https://github.com/owner/repo/pull/1",
                "not-a-valid-url",
                "https://github.com/owner/repo/pull/3"
        );
        final List<PullRequestUrl> urls = reader.read(csv);
        assertAll(
                () -> assertThat(urls).hasSize(2),
                () -> assertThat(urls.get(0).number()).isEqualTo(1),
                () -> assertThat(urls.get(1).number()).isEqualTo(3)
        );
    }

    @Test
    void returnsEmptyListForEmptyFile() throws IOException {
        final Path csv = writeCsv();
        final List<PullRequestUrl> urls = reader.read(csv);
        assertThat(urls).isEmpty();
    }

    @Test
    void handlesMultipleRepos() throws IOException {
        final Path csv = writeCsv(
                "https://github.com/alice/alpha/pull/1",
                "https://github.com/bob/beta/pull/2",
                "https://github.com/alice/alpha/pull/3"
        );
        final List<PullRequestUrl> urls = reader.read(csv);
        assertAll(
                () -> assertThat(urls).hasSize(3),
                () -> assertThat(urls.get(0).fullName()).isEqualTo("alice/alpha"),
                () -> assertThat(urls.get(1).fullName()).isEqualTo("bob/beta"),
                () -> assertThat(urls.get(2).fullName()).isEqualTo("alice/alpha")
        );
    }

    private Path writeCsv(final String... lines) throws IOException {
        final Path csv = tempDir.resolve("prs.csv");
        Files.writeString(csv, String.join("\n", lines));
        return csv;
    }
}
