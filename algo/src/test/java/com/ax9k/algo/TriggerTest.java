package com.ax9k.algo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerTest {
    private Trigger testTrigger;
    private InvocationCounter invocationCounter;

    @BeforeEach
    void initialiseTrigger() {
        invocationCounter = new InvocationCounter();
        testTrigger = new Trigger(invocationCounter);
    }

    private static class InvocationCounter implements Runnable {
        int count;

        @Override
        public void run() {
            count++;
        }
    }

    @Nested
    class WhenNotTriggered {
        @Test
        void shouldNotBeTriggered() {
            assertFalse(testTrigger.isTriggered());
        }

        @Test
        void shouldNotHaveBeenTriggeredOnReset() {
            boolean wasTriggered = testTrigger.reset();
            assertFalse(wasTriggered);
        }

        @Test
        void shouldNotHaveInvokedRunnable() {
            assertEquals(0, invocationCounter.count);
        }
    }

    @Nested
    class WhenTriggeredOnce {
        @BeforeEach
        void triggerOnce() {
            testTrigger.trigger();
        }

        @Test
        void shouldBeTriggered() {
            assertTrue(testTrigger.isTriggered());
        }

        @Test
        void shouldHaveBeenTriggeredOnReset() {
            boolean wasTriggered = testTrigger.reset();
            assertTrue(wasTriggered);
        }

        @Test
        void shouldHaveInvokedRunnableOnce() {
            assertEquals(1, invocationCounter.count);
        }
    }

    @Nested
    class WhenTriggeredMultipleTimes {
        @BeforeEach
        void triggerTwice() {
            testTrigger.trigger();
            testTrigger.trigger();
        }

        @Test
        void shouldBeTriggered() {
            assertTrue(testTrigger.isTriggered());
        }

        @Test
        void shouldHaveBeenTriggeredOnReset() {
            boolean wasTriggered = testTrigger.reset();
            assertTrue(wasTriggered);
        }

        @Test
        void shouldHaveInvokedRunnableOnlyOnce() {
            assertEquals(1, invocationCounter.count);
        }
    }

    @Nested
    class WhenReset {
        @BeforeEach
        void triggerThenReset() {
            testTrigger.trigger();
            testTrigger.trigger();
            testTrigger.reset();
        }

        @Test
        void shouldNotBeTriggered() {
            assertFalse(testTrigger.isTriggered());
        }

        @Test
        void shouldNotHaveBeenTriggeredOnSecondReset() {
            boolean wasTriggered = testTrigger.reset();
            assertFalse(wasTriggered);
        }

        @Test
        void shouldInvokeRunnableOnNextTrigger() {
            invocationCounter.count = 0;

            testTrigger.trigger();

            assertEquals(1, invocationCounter.count);
        }
    }
}