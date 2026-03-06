package org.fifties.housewife;

import lombok.extern.log4j.Log4j2;

import java.util.Arrays;

@Log4j2
public final class Main {

    private Main() {
    }

    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        final String command = args[0];
        final String[] remaining = Arrays.copyOfRange(args, 1, args.length);

        switch (command) {
            case "extract":
                PullRequestExtract.main(remaining);
                break;
            case "summarize":
                PullRequestSummarize.main(remaining);
                break;
            default:
                log.error("Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }

    private static void printUsage() {
        log.info("Usage:\n"
                + "  extract   --user <username>\n"
                + "  extract   --repo <owner/repo> [--state all|open|closed] [--limit N]\n"
                + "  extract   --csv <file.csv>\n"
                + "  summarize --input <dirs...> [--max-diff-lines N]");
    }
}
