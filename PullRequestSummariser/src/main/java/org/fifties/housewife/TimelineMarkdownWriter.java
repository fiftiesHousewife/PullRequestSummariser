package org.fifties.housewife;

import com.google.gson.JsonObject;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class TimelineMarkdownWriter {

    void appendTimeline(final StringBuilder markdown, final List<JsonObject> pullRequests) {
        final List<JsonObject> sorted = sortByCreatedDate(pullRequests);

        markdown.append("## Pull Request Timeline\n\n");
        for (final JsonObject pr : sorted) {
            final String status = JsonFields.bool(pr, "merged") ? "Merged"
                    : PullRequestMarkdownWriter.capitalize(JsonFields.str(pr, "state"));
            final String date = truncateDate(JsonFields.str(pr, "created_at"));
            markdown.append("- **").append(date).append("** Pull Request #")
                    .append(JsonFields.num(pr, "number")).append(": ")
                    .append(JsonFields.str(pr, "title")).append(" [").append(status).append("]\n");
        }
        markdown.append("\n");
    }

    void appendIndex(final StringBuilder markdown, final List<JsonObject> pullRequests) {
        final List<JsonObject> sorted = sortByCreatedDate(pullRequests);

        markdown.append("## Pull Request Index\n\n");
        markdown.append("| # | Title | Author | Status | Created |\n");
        markdown.append("|---|-------|--------|--------|--------|\n");
        for (final JsonObject pr : sorted) {
            final String status = JsonFields.bool(pr, "merged") ? "Merged"
                    : PullRequestMarkdownWriter.capitalize(JsonFields.str(pr, "state"));
            String title = JsonFields.str(pr, "title");
            if (title.length() > 60) {
                title = title.substring(0, 60);
            }
            final String date = truncateDate(JsonFields.str(pr, "created_at"));
            markdown.append("| #").append(JsonFields.num(pr, "number")).append(" | ")
                    .append(title).append(" | ").append(JsonFields.str(pr, "author"))
                    .append(" | ").append(status).append(" | ").append(date).append(" |\n");
        }
        markdown.append("\n");
    }

    private List<JsonObject> sortByCreatedDate(final List<JsonObject> pullRequests) {
        return pullRequests.stream()
                .sorted(Comparator.comparing(pr -> JsonFields.str(pr, "created_at")))
                .collect(Collectors.toList());
    }

    private String truncateDate(final String dateTime) {
        if (dateTime.length() >= 10) {
            return dateTime.substring(0, 10);
        }
        return dateTime;
    }
}
