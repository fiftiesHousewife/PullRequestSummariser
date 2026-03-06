package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class DiffMarkdownWriter {

    private final int maxDiffLines;

    DiffMarkdownWriter(final int maxDiffLines) {
        this.maxDiffLines = maxDiffLines;
    }

    void append(final StringBuilder markdown, final JsonObject pr) {
        final JsonArray files = JsonFields.arr(pr, "files");
        if (files.isEmpty()) {
            return;
        }

        markdown.append("## Key Diffs\n\n");
        int totalDiffLines = 0;
        boolean truncated = false;

        for (final JsonElement element : files) {
            final JsonObject file = element.getAsJsonObject();
            final String patch = JsonFields.str(file, "patch");
            if (patch.isEmpty()) {
                continue;
            }
            final String[] patchLines = patch.split("\n");

            if (totalDiffLines + patchLines.length > maxDiffLines) {
                final int remaining = maxDiffLines - totalDiffLines;
                if (remaining > 0) {
                    markdown.append("### `").append(JsonFields.str(file, "filename")).append("`\n");
                    markdown.append("```diff\n");
                    for (int i = 0; i < remaining; i++) {
                        markdown.append(patchLines[i]).append("\n");
                    }
                    markdown.append("```\n");
                    markdown.append("*... truncated (").append(patchLines.length - remaining)
                            .append(" more lines)*\n\n");
                }
                truncated = true;
                break;
            }

            markdown.append("### `").append(JsonFields.str(file, "filename")).append("`\n");
            markdown.append("```diff\n").append(patch).append("\n```\n\n");
            totalDiffLines += patchLines.length;
        }

        if (truncated) {
            markdown.append("*Diff output truncated at ").append(maxDiffLines)
                    .append(" lines. Remaining files omitted for brevity.*\n\n");
        }
    }
}
