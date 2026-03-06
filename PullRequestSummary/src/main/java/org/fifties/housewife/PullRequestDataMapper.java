package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

final class PullRequestDataMapper {

    JsonObject map(final JsonObject prDetail, final JsonArray commits, final JsonArray files,
                   final JsonArray reviewComments, final JsonArray issueComments, final JsonArray reviews) {
        final JsonObject result = new JsonObject();
        result.addProperty("number", prDetail.get("number").getAsInt());
        result.addProperty("title", prDetail.get("title").getAsString());
        result.addProperty("state", prDetail.get("state").getAsString());
        result.addProperty("merged", JsonFields.bool(prDetail, "merged"));
        result.addProperty("author", prDetail.getAsJsonObject("user").get("login").getAsString());
        result.addProperty("created_at", JsonFields.str(prDetail, "created_at"));
        result.addProperty("updated_at", JsonFields.str(prDetail, "updated_at"));
        result.addProperty("merged_at", JsonFields.str(prDetail, "merged_at"));
        result.addProperty("closed_at", JsonFields.str(prDetail, "closed_at"));
        result.add("labels", mapLabels(prDetail));
        result.add("milestone", mapMilestone(prDetail));
        result.addProperty("body", JsonFields.str(prDetail, "body"));
        result.addProperty("base_branch", prDetail.getAsJsonObject("base").get("ref").getAsString());
        result.addProperty("head_branch", prDetail.getAsJsonObject("head").get("ref").getAsString());
        result.add("commits", mapCommits(commits));
        result.add("files", mapFiles(files));
        result.add("review_comments", mapReviewComments(reviewComments));
        result.add("issue_comments", mapIssueComments(issueComments));
        result.add("reviews", mapReviews(reviews));
        return result;
    }

    private JsonArray mapLabels(final JsonObject prDetail) {
        final JsonArray labels = new JsonArray();
        for (final JsonElement label : prDetail.getAsJsonArray("labels")) {
            labels.add(label.getAsJsonObject().get("name").getAsString());
        }
        return labels;
    }

    private JsonElement mapMilestone(final JsonObject prDetail) {
        if (prDetail.has("milestone") && !prDetail.get("milestone").isJsonNull()) {
            return new com.google.gson.JsonPrimitive(
                    prDetail.getAsJsonObject("milestone").get("title").getAsString());
        }
        return JsonNull.INSTANCE;
    }

    private JsonArray mapCommits(final JsonArray commits) {
        final JsonArray result = new JsonArray();
        for (final JsonElement element : commits) {
            final JsonObject commit = element.getAsJsonObject();
            final JsonObject inner = commit.getAsJsonObject("commit");
            final JsonObject entry = new JsonObject();
            entry.addProperty("sha", commit.get("sha").getAsString());
            entry.addProperty("message", inner.get("message").getAsString());
            entry.addProperty("author", inner.getAsJsonObject("author").get("name").getAsString());
            entry.addProperty("date", inner.getAsJsonObject("author").get("date").getAsString());
            result.add(entry);
        }
        return result;
    }

    private JsonArray mapFiles(final JsonArray files) {
        final JsonArray result = new JsonArray();
        for (final JsonElement element : files) {
            final JsonObject file = element.getAsJsonObject();
            final JsonObject entry = new JsonObject();
            entry.addProperty("filename", file.get("filename").getAsString());
            entry.addProperty("status", file.get("status").getAsString());
            entry.addProperty("additions", file.get("additions").getAsInt());
            entry.addProperty("deletions", file.get("deletions").getAsInt());
            entry.addProperty("changes", file.get("changes").getAsInt());
            entry.addProperty("patch", JsonFields.str(file, "patch"));
            result.add(entry);
        }
        return result;
    }

    private JsonArray mapReviewComments(final JsonArray comments) {
        final JsonArray result = new JsonArray();
        for (final JsonElement element : comments) {
            final JsonObject comment = element.getAsJsonObject();
            final JsonObject entry = new JsonObject();
            entry.addProperty("user", comment.getAsJsonObject("user").get("login").getAsString());
            entry.addProperty("body", comment.get("body").getAsString());
            entry.addProperty("path", JsonFields.str(comment, "path"));
            entry.add("line", comment.has("line") && !comment.get("line").isJsonNull()
                    ? comment.get("line") : JsonNull.INSTANCE);
            entry.addProperty("created_at", comment.get("created_at").getAsString());
            result.add(entry);
        }
        return result;
    }

    private JsonArray mapIssueComments(final JsonArray comments) {
        final JsonArray result = new JsonArray();
        for (final JsonElement element : comments) {
            final JsonObject comment = element.getAsJsonObject();
            final JsonObject entry = new JsonObject();
            entry.addProperty("user", comment.getAsJsonObject("user").get("login").getAsString());
            entry.addProperty("body", comment.get("body").getAsString());
            entry.addProperty("created_at", comment.get("created_at").getAsString());
            result.add(entry);
        }
        return result;
    }

    private JsonArray mapReviews(final JsonArray reviews) {
        final JsonArray result = new JsonArray();
        for (final JsonElement element : reviews) {
            final JsonObject review = element.getAsJsonObject();
            final JsonObject entry = new JsonObject();
            entry.addProperty("user", review.getAsJsonObject("user").get("login").getAsString());
            entry.addProperty("state", review.get("state").getAsString());
            entry.addProperty("body", JsonFields.str(review, "body"));
            entry.addProperty("submitted_at", JsonFields.str(review, "submitted_at"));
            result.add(entry);
        }
        return result;
    }
}
