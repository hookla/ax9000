package com.ax9k.cex.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SignatureGeneratorTest {
    @Nested
    class WhenGivenValidValues {
        @Test
        void shouldOutputExpectedResult() {
            var generator = new SignatureGenerator("1WZbtMTbMbo2NsW12vOz9IuPM",
                                                   "1IuUeW4IEWatK87zBTENHj1T17s");
            var signature = generator.generate(Instant.ofEpochSecond(1448034533));
            assertEquals("7d581adb01ad22f1ed38e1159a7f08ac5d83906ae1a42fe17e7d977786fe9694", signature);
        }
    }

    @Nested
    class WhenGivenDifferentTimestamps {
        @Test
        void shouldOutputDifferentResult() {
            var generator = new SignatureGenerator("1WZbtMTbMbo2NsW12vOz9IuPM",
                                                   "1IuUeW4IEWatK87zBTENHj1T17s");
            var signature1 = generator.generate(Instant.ofEpochSecond(1448034533));
            var signature2 = generator.generate(Instant.ofEpochSecond(1448035135));

            assertNotEquals(signature1, signature2);
        }

        @Test
        void shouldOutputExpectedResult() {
            var generator = new SignatureGenerator("1WZbtMTbMbo2NsW12vOz9IuPM",
                                                   "1IuUeW4IEWatK87zBTENHj1T17s");
            var signature = generator.generate(Instant.ofEpochSecond(1448035135));
            assertEquals("9a84b70f51ea2b149e71ef2436752a1a7c514f521e886700bcadd88f1767b7db", signature);
        }
    }

    @Nested
    class WhenGivenEqualTimestamps {
        @Test
        void shouldOutputConsistentResult() {
            var generator = new SignatureGenerator("1WZbtMTbMbo2NsW12vOz9IuPM",
                                                   "1IuUeW4IEWatK87zBTENHj1T17s");
            var signature1 = generator.generate(Instant.ofEpochSecond(1448035135));
            var signature2 = generator.generate(Instant.ofEpochSecond(1448035135));

            assertEquals(signature1, signature2);
        }
    }
}