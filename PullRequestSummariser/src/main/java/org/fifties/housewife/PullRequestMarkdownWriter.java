package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

final class PullRequestMarkdownWriter {

    private final DiffMarkdownWriter diffWriter;
    private final DiscussionMarkdownWriter discussionWriter = new DiscussionMarkdownWriter();

    PullRequestMarkdownWriter(final int maxDiffLines) {
        this.diffWriter = new DiffMarkdownWriter(maxDiffLines);
    }

    String write(final JsonObject pr) {
        final StringBuilder markdown = new StringBuilder();
        appendOverview(markdown, pr);
        appendIntent(markdown, pr);
        appendScope(markdown, pr);
        diffWriter.append(markdown, pr);
        discussionWriter.append(markdown, pr);
        appendVerdict(markdown, pr);
        return markdown.toString();
    }

    private void appendOverview(final StringBuilder markdown, final JsonObject pr) {
        final String status = JsonFields.bool(pr, "merged") ? "Merged" : capitalize(JsonFields.str(pr, "state"));
        markdown.append("# Pull Request #").append(JsonFields.num(pr, "number")).append(": ")
                .append(JsonFields.str(pr, "title")).append("\n\n");
        markdown.append("- **Author:** ").append(JsonFields.str(pr, "author")).append("\n");
        markdown.append("- **Status:** ").append(status).append("\n");
        markdown.append("- **Created:** ").append(JsonFields.str(pr, "created_at")).append("\n");

        final String mergedAt = JsonFields.str(pr, "merged_at");
        final String closedAt = JsonFields.str(pr, "closed_at");
        if (!mergedAt.isEmpty()) {
            markdown.append("- **Merged:** ").append(mergedAt).append("\n");
        } else if (!closedAt.isEmpty()) {
            markdown.append("- **Closed:** ").append(closedAt).append("\n");
        }

        markdown.append("- **Updated:** ").append(JsonFields.str(pr, "updated_at")).append("\n");
        markdown.append("- **Branch:** `").append(JsonFields.str(pr, "head_branch"))
                .append("` -> `").append(JsonFields.str(pr, "base_branch")).append("`\n");

        final JsonArray labels = JsonFields.arr(pr, "labels");
        if (!labels.isEmpty()) {
            final StringBuilder labelText = new StringBuilder();
            for (final JsonElement label : labels) {
                if (labelText.length() > 0) {
                    labelText.append(", ");
                }
                labelText.append(label.getAsString());
            }
            markdown.append("- **Labels:** ").append(labelText).append("\n");
        }

        final String milestone = JsonFields.str(pr, "milestone");
        if (!milestone.isEmpty()) {
            markdown.append("- **Milestone:** ").append(milestone).append("\n");
        }
        markdown.append("\n");
    }

    private void appendIntent(final StringBuilder markdown, final JsonObject pr) {
        markdown.append("## Intent\n\n");
        final String body = JsonFields.str(pr, "body").trim();
        markdown.append(body.isEmpty() ? "*No pull request description provided.*" : body).append("\n\n");

        final JsonArray commits = JsonFields.arr(pr, "commits");
        if (!commits.isEmpty()) {
            markdown.append("### Commit Messages\n\n");
            for (final JsonElement element : commits) {
                final JsonObject commit = element.getAsJsonObject();
                final String message = JsonFields.str(commit, "message").split("\n")[0];
                final String sha = JsonFields.str(commit, "sha");
                final String shortSha = sha.length() >= 7 ? sha.substring(0, 7) : sha;
                markdown.append("- `").append(shortSha).append("` ").append(message)
                        .append(" (").append(JsonFields.str(commit, "author")).append(")\n");
            }
            markdown.append("\n");
        }
    }

    private void appendScope(final StringBuilder markdown, final JsonObject pr) {
        final JsonArray files = JsonFields.arr(pr, "files");
        if (files.isEmpty()) {
            return;
        }

        int totalAdditions = 0;
        int totalDeletions = 0;
        final Map<String, Integer> languageCounts = new LinkedHashMap<>();
        for (final JsonElement element : files) {
            final JsonObject file = element.getAsJsonObject();
            totalAdditions += JsonFields.num(file, "additions");
            totalDeletions += JsonFields.num(file, "deletions");
            languageCounts.merge(Languages.detect(JsonFields.str(file, "filename")), 1, Integer::sum);
        }

        markdown.append("## Scope of Changes\n\n");
        markdown.append("- **Files changed:** ").append(files.size()).append("\n");
        markdown.append("- **Lines added:** ").append(totalAdditions).append("\n");
        markdown.append("- **Lines removed:** ").append(totalDeletions).append("\n");

        final String languageSummary = languageCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
        markdown.append("- **Languages:** ").append(languageSummary).append("\n\n");

        markdown.append("### Files\n\n");
        for (final JsonElement element : files) {
            final JsonObject file = element.getAsJsonObject();
            markdown.append("- `").append(JsonFields.str(file, "filename")).append("` — ")
                    .append(JsonFields.str(file, "status"))
                    .append(" (+").append(JsonFields.num(file, "additions"))
                    .append("/-").append(JsonFields.num(file, "deletions")).append(")\n");
        }
        markdown.append("\n");
    }

    private void appendVerdict(final StringBuilder markdown, final JsonObject pr) {
        markdown.append("## Verdict\n\n");
        if (JsonFields.bool(pr, "merged")) {
            markdown.append("**Merged** on ").append(JsonFields.str(pr, "merged_at")).append("\n");
        } else if ("closed".equals(JsonFields.str(pr, "state"))) {
            final String closedAt = JsonFields.str(pr, "closed_at");
            markdown.append("**Closed without merge** on ")
                    .append(closedAt.isEmpty() ? "unknown date" : closedAt).append("\n");
        } else {
            markdown.append("**Still open**\n");
        }
        markdown.append("\n");
    }

    static String capitalize(final String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
