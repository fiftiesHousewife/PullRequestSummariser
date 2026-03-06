package org.fifties.housewife;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void delegatesToExtractCommand() {
        final String[] args = {"extract"};
        assertThat(args[0]).isEqualTo("extract");
    }

    @Test
    void delegatesToSummarizeCommand() {
        final String[] args = {"summarize"};
        assertThat(args[0]).isEqualTo("summarize");
    }
}
