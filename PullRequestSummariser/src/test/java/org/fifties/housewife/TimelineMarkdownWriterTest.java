package org.fifties.housewife;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class TimelineMarkdownWriterTest {

    private final TimelineMarkdownWriter writer = new TimelineMarkdownWriter();

    @Test
    void rendersTimelineInChronologicalOrder() {
        final JsonObject later = buildPullRequest(1, "Later", "open", false, "2024-03-01T00:00:00Z");
        final JsonObject earlier = buildPullRequest(2, "Earlier", "open", false, "2024-01-01T00:00:00Z");

        final StringBuilder markdown = new StringBuilder();
        writer.appendTimeline(markdown, List.of(later, earlier));

        final String result = markdown.toString();
        assertAll(
                () -> assertThat(result).contains("## Pull Request Timeline"),
                () -> assertThat(result.indexOf("Earlier")).isLessThan(result.indexOf("Later"))
        );
    }

    @Test
    void showsMergedStatusInTimeline() {
        final JsonObject pr = buildPullRequest(1, "Merged PR", "closed", true, "2024-01-01T00:00:00Z");

        final StringBuilder markdown = new StringBuilder();
        writer.appendTimeline(markdown, List.of(pr));

        assertThat(markdown.toString()).contains("[Merged]");
    }

    @Test
    void showsOpenStatusInTimeline() {
        final JsonObject pr = buildPullRequest(1, "Open PR", "open", false, "2024-01-01T00:00:00Z");

        final StringBuilder markdown = new StringBuilder();
        writer.appendTimeline(markdown, List.of(pr));

        assertThat(markdown.toString()).contains("[Open]");
    }

    @Test
    void truncatesDateToTenCharacters() {
        final JsonObject pr = buildPullRequest(1, "Title", "open", false, "2024-06-15T12:30:00Z");

        final StringBuilder markdown = new StringBuilder();
        writer.appendTimeline(markdown, List.of(pr));

        assertThat(markdown.toString()).contains("**2024-06-15**");
    }

    @Test
    void rendersIndexTable() {
        final JsonObject pr = buildPullRequest(1, "Add feature", "open", false, "2024-01-01T00:00:00Z");
        pr.addProperty("author", "alice");

        final StringBuilder markdown = new StringBuilder();
        writer.appendIndex(markdown, List.of(pr));

        assertAll(
                () -> assertThat(markdown.toString()).contains("## Pull Request Index"),
                () -> assertThat(markdown.toString()).contains("| # | Title | Author | Status | Created |"),
                () -> assertThat(markdown.toString()).contains("| #1 | Add feature | alice | Open | 2024-01-01 |")
        );
    }

    @Test
    void truncatesLongTitlesInIndex() {
        final String longTitle = "A".repeat(80);
        final JsonObject pr = buildPullRequest(1, longTitle, "open", false, "2024-01-01T00:00:00Z");

        final StringBuilder markdown = new StringBuilder();
        writer.appendIndex(markdown, List.of(pr));

        assertThat(markdown.toString()).contains("A".repeat(60));
        assertThat(markdown.toString()).doesNotContain("A".repeat(61));
    }

    private JsonObject buildPullRequest(final int number, final String title, final String state,
                                        final boolean merged, final String createdAt) {
        final JsonObject pr = new JsonObject();
        pr.addProperty("number", number);
        pr.addProperty("title", title);
        pr.addProperty("state", state);
        pr.addProperty("merged", merged);
        pr.addProperty("author", "testuser");
        pr.addProperty("created_at", createdAt);
        return pr;
    }
}
