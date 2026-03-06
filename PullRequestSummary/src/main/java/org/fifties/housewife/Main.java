package org.fifties.housewife;

import java.util.Arrays;
import java.util.logging.Logger;

public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

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
                LOG.severe("Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }

    private static void printUsage() {
        LOG.info("Usage:\n"
                + "  extract   --user <username>\n"
                + "  extract   --repo <owner/repo> [--state all|open|closed] [--limit N]\n"
                + "  summarize --input <dirs...> [--max-diff-lines N]");
    }
}
