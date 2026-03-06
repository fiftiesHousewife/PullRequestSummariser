package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class DiscussionMarkdownWriter {

    void append(final StringBuilder markdown, final JsonObject pullRequest) {
        final JsonArray reviews = JsonFields.arr(pullRequest, "reviews");
        final JsonArray reviewComments = JsonFields.arr(pullRequest, "review_comments");
        final JsonArray issueComments = JsonFields.arr(pullRequest, "issue_comments");

        if (reviews.isEmpty() && reviewComments.isEmpty() && issueComments.isEmpty()) {
            return;
        }

        markdown.append("## Discussion\n\n");
        appendReviews(markdown, reviews);
        appendReviewComments(markdown, reviewComments);
        appendIssueComments(markdown, issueComments);
    }

    private void appendReviews(final StringBuilder markdown, final JsonArray reviews) {
        if (reviews.isEmpty()) {
            return;
        }
        markdown.append("### Review Decisions\n\n");
        for (final JsonElement element : reviews) {
            final JsonObject review = element.getAsJsonObject();
            final String stateLabel = PullRequestMarkdownWriter.capitalize(
                    JsonFields.str(review, "state").replace("_", " "));
            final String reviewBody = JsonFields.str(review, "body");
            String bodyPreview = "";
            if (!reviewBody.isEmpty()) {
                bodyPreview = " — " + (reviewBody.length() > 200
                        ? reviewBody.substring(0, 200) + "..." : reviewBody);
            }
            markdown.append("- **").append(JsonFields.str(review, "user")).append("**: ")
                    .append(stateLabel).append(bodyPreview)
                    .append(" (").append(JsonFields.str(review, "submitted_at")).append(")\n");
        }
        markdown.append("\n");
    }

    private void appendReviewComments(final StringBuilder markdown, final JsonArray comments) {
        if (comments.isEmpty()) {
            return;
        }
        markdown.append("### Code Review Comments\n\n");
        for (final JsonElement element : comments) {
            final JsonObject comment = element.getAsJsonObject();
            final String path = JsonFields.str(comment, "path");
            final String location = path.isEmpty() ? "" : " on `" + path + "`";
            final String lineInfo = comment.has("line") && !comment.get("line").isJsonNull()
                    ? " line " + comment.get("line").getAsInt() : "";
            markdown.append("**").append(JsonFields.str(comment, "user")).append("**")
                    .append(location).append(lineInfo)
                    .append(" (").append(JsonFields.str(comment, "created_at")).append("):\n");
            markdown.append("> ").append(JsonFields.str(comment, "body")).append("\n\n");
        }
    }

    private void appendIssueComments(final StringBuilder markdown, final JsonArray comments) {
        if (comments.isEmpty()) {
            return;
        }
        markdown.append("### General Comments\n\n");
        for (final JsonElement element : comments) {
            final JsonObject comment = element.getAsJsonObject();
            markdown.append("**").append(JsonFields.str(comment, "user")).append("** (")
                    .append(JsonFields.str(comment, "created_at")).append("):\n");
            markdown.append("> ").append(JsonFields.str(comment, "body")).append("\n\n");
        }
    }
}
