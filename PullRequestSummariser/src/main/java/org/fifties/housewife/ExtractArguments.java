package org.fifties.housewife;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.extern.log4j.Log4j2;

@Log4j2
final class ExtractArguments {

    private final List<String> repos;
    private final List<String> users;
    private final String csvFile;
    private final String state;
    private final int limit;

    private ExtractArguments(final List<String> repos, final List<String> users,
                             final String csvFile, final String state, final int limit) {
        this.repos = Collections.unmodifiableList(repos);
        this.users = Collections.unmodifiableList(users);
        this.csvFile = csvFile;
        this.state = state;
        this.limit = limit;
    }

    List<String> repos() {
        return repos;
    }

    List<String> users() {
        return users;
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
        final List<String> repos = new ArrayList<>();
        final List<String> users = new ArrayList<>();
        String csvFile = null;
        String state = "all";
        int limit = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--repo":
                    repos.add(args[++i]);
                    break;
                case "--user":
                    users.add(args[++i]);
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

        if (repos.isEmpty() && users.isEmpty() && csvFile == null) {
            printUsage();
            System.exit(1);
        }

        return new ExtractArguments(repos, users, csvFile, state, limit);
    }

    private static void printUsage() {
        log.info("Usage:\n"
                + "  extract --repo owner/repo [--repo owner/repo2] [--state all|open|closed] [--limit N]\n"
                + "  extract --user username [--user username2] [--state all|open|closed] [--limit N]\n"
                + "  extract --user username --repo partial-name [--state all|open|closed] [--limit N]\n"
                + "  extract --csv file.csv");
    }
}
