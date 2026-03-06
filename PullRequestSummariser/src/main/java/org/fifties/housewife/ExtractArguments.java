package org.fifties.housewife;

import lombok.extern.log4j.Log4j2;

@Log4j2
final class ExtractArguments {

    private final String repo;
    private final String user;
    private final String csvFile;
    private final String state;
    private final int limit;

    private ExtractArguments(final String repo, final String user, final String csvFile,
                             final String state, final int limit) {
        this.repo = repo;
        this.user = user;
        this.csvFile = csvFile;
        this.state = state;
        this.limit = limit;
    }

    String repo() {
        return repo;
    }

    String user() {
        return user;
    }

    String csvFile() {
        return csvFile;
    }

    String state() {
        return state;
    }

    int limit() {
        return limit;
    }

    static ExtractArguments parse(final String[] args) {
        String repo = null;
        String user = null;
        String csvFile = null;
        String state = "all";
        int limit = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--repo":
                    repo = args[++i];
                    break;
                case "--user":
                    user = args[++i];
                    break;
                case "--csv":
                    csvFile = args[++i];
                    break;
                case "--state":
                    state = args[++i];
                    break;
                case "--limit":
                    limit = Integer.parseInt(args[++i]);
                    break;
                default:
                    log.error("Unknown argument: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }

        if (repo == null && user == null && csvFile == null) {
            printUsage();
            System.exit(1);
        }

        return new ExtractArguments(repo, user, csvFile, state, limit);
    }

    private static void printUsage() {
        log.info("Usage:\n"
                + "  extract --repo owner/repo [--state all|open|closed] [--limit N]\n"
                + "  extract --user username [--state all|open|closed] [--limit N]\n"
                + "  extract --csv file.csv");
    }
}
