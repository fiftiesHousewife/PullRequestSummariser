package org.fifties.housewife;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class RepoMarkdownWriter {

    private final TimelineMarkdownWriter timelineWriter = new TimelineMarkdownWriter();

    String write(final List<JsonObject> pullRequests, final JsonObject meta) {
        final StringBuilder markdown = new StringBuilder();
        appendHeader(markdown, pullRequests, meta);
        appendStatistics(markdown, pullRequests);
        appendContributors(markdown, pullRequests);
        appendDirectories(markdown, pullRequests);
        appendLanguages(markdown, pullRequests);
        timelineWriter.appendTimeline(markdown, pullRequests);
        timelineWriter.appendIndex(markdown, pullRequests);
        return markdown.toString();
    }

    private void appendHeader(final StringBuilder markdown, final List<JsonObject> pullRequests,
                              final JsonObject meta) {
        final String repoName = JsonFields.str(meta, "repo").isEmpty()
                ? "Unknown Repo" : JsonFields.str(meta, "repo");
        markdown.append("# Repository Summary: ").append(repoName).append("\n\n");
        markdown.append("- **Extraction date:** ").append(JsonFields.str(meta, "extracted_at")).append("\n");
        markdown.append("- **Total pull requests extracted:** ").append(pullRequests.size()).append("\n\n");
    }

    private void appendStatistics(final StringBuilder markdown, final List<JsonObject> pullRequests) {
        if (pullRequests.isEmpty()) {
            markdown.append("No pull requests to summarize.\n");
            return;
        }

        int merged = 0;
        int closed = 0;
        int open = 0;
        for (final JsonObject pr : pullRequests) {
            if (JsonFields.bool(pr, "merged")) {
                merged++;
            } else if ("closed".equals(JsonFields.str(pr, "state"))) {
                closed++;
            } else {
                open++;
            }
        }

        markdown.append("## Pull Request Statistics\n\n");
        markdown.append("- **Merged:** ").append(merged).append("\n");
        markdown.append("- **Closed (not merged):** ").append(closed).append("\n");
        markdown.append("- **Open:** ").append(open).append("\n");
        if (merged + closed > 0) {
            final int mergeRate = (int) Math.round(100.0 * merged / (merged + closed));
            markdown.append("- **Merge rate:** ").append(mergeRate).append("%\n");
        }
        markdown.append("\n");
    }

    private void appendContributors(final StringBuilder markdown, final List<JsonObject> pullRequests) {
        final Map<String, Integer> authors = new LinkedHashMap<>();
        for (final JsonObject pr : pullRequests) {
            authors.merge(JsonFields.str(pr, "author"), 1, Integer::sum);
        }

        markdown.append("## Contributors\n\n");
        authors.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> markdown.append("- **").append(entry.getKey())
                        .append("**: ").append(entry.getValue()).append(" pull requests\n"));
        markdown.append("\n");
    }

    private void appendDirectories(final StringBuilder markdown, final List<JsonObject> pullRequests) {
        final Map<String, Integer> directories = new LinkedHashMap<>();
        for (final JsonObject pr : pullRequests) {
            for (final JsonElement element : JsonFields.arr(pr, "files")) {
                final String filename = JsonFields.str(element.getAsJsonObject(), "filename");
                final String[] parts = filename.split("/");
                directories.merge(parts.length > 1 ? parts[0] : "(root)", 1, Integer::sum);
            }
        }

        if (directories.isEmpty()) {
            return;
        }

        markdown.append("## Areas of Work (by top-level directory)\n\n");
        directories.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
                .forEach(entry -> markdown.append("- `").append(entry.getKey())
                        .append("/`: ").append(entry.getValue()).append(" file changes\n"));
        markdown.append("\n");
    }

    private void appendLanguages(final StringBuilder markdown, final List<JsonObject> pullRequests) {
        final Map<String, Integer> languageCounts = new LinkedHashMap<>();
        for (final JsonObject pr : pullRequests) {
            for (final JsonElement element : JsonFields.arr(pr, "files")) {
                final String language = Languages.detect(JsonFields.str(element.getAsJsonObject(), "filename"));
                languageCounts.merge(language, 1, Integer::sum);
            }
        }

        if (languageCounts.isEmpty()) {
            return;
        }

        markdown.append("## Languages\n\n");
        languageCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> markdown.append("- **").append(entry.getKey())
                        .append("**: ").append(entry.getValue()).append(" files changed\n"));
        markdown.append("\n");
    }

}
