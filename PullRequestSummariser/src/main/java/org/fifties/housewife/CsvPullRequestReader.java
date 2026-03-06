package org.fifties.housewife;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

final class CsvPullRequestReader {

    private static final Logger LOG = Logger.getLogger(CsvPullRequestReader.class.getName());

    List<PullRequestUrl> read(final Path csvFile) throws IOException {
        final List<String> lines = Files.readAllLines(csvFile);
        final List<PullRequestUrl> urls = new ArrayList<>();

        for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
            final String line = lines.get(lineNumber - 1).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            final String cell = line.contains(",") ? line.split(",")[0].trim() : line;
            try {
                urls.add(PullRequestUrl.parse(cell));
            } catch (final IllegalArgumentException exception) {
                LOG.warning("Skipping line " + lineNumber + ": " + exception.getMessage());
            }
        }

        return urls;
    }
}
