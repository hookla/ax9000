package com.ax9k.cex.provider.orderbook;

import com.ax9k.core.marketmodel.orderbook.OrderBookLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.ArrayUtils.isSorted;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBookLevelsTest {
    private static final OrderBookLevel[] SAMPLE_LEVELS = {
            new OrderBookLevel(1, 1),
            new OrderBookLevel(2, 1),
            new OrderBookLevel(3, 1),
            new OrderBookLevel(4, 1),
            new OrderBookLevel(5, 1),
            };
    private static final Comparator<OrderBookLevel> PRICE_ASCENDING = BookLevelComparators.ASKS;
    private static final int TEST_DEPTH = 5;

    private OrderBookLevels testLevels;

    @BeforeEach
    void initialiseTestLevels() {
        testLevels = new OrderBookLevels(PRICE_ASCENDING, TEST_DEPTH);
        testLevels.set(SAMPLE_LEVELS);
    }

    @Nested
    class WhenNew {
        @BeforeEach
        void initialiseEmptyLevels() {
            testLevels = new OrderBookLevels(PRICE_ASCENDING, TEST_DEPTH);
        }

        @Test
        void shouldHaveCorrectDepth() {
            var array = testLevels.toArray();

            assertEquals(TEST_DEPTH, array.length);
        }

        @Test
        void shouldNotContainNullLevels() {
            var array = testLevels.toArray();

            assertFalse(contains(array, null));
        }

        @Test
        void shouldContainOnlyEmptyLevels() {
            var array = testLevels.toArray();

            OrderBookLevel[] emptyElements = new OrderBookLevel[TEST_DEPTH];
            Arrays.fill(emptyElements, OrderBookLevel.EMPTY);

            assertArrayEquals(emptyElements, array);
        }
    }

    @Nested
    class WhenNewLevelAdded {
        private final OrderBookLevel TEST_NEW_LEVEL = new OrderBookLevel(1.5, 2);

        @BeforeEach
        void addLevel() {
            testLevels.add(TEST_NEW_LEVEL);
        }

        @Test
        void shouldFailOnNullLevel() {
            assertThrows(NullPointerException.class, () -> testLevels.add(null));
        }

        @Test
        void shouldMaintainCorrectOrderingOfLevels() {
            assertTrue(isSorted(testLevels.toArray(), PRICE_ASCENDING));
        }

        @Test
        void shouldMaintainCorrectDepth() {
            assertEquals(TEST_DEPTH, testLevels.toArray().length);
        }

        @Test
        void shouldRetainNewRelevantLevel() {
            assertTrue(contains(testLevels.toArray(), TEST_NEW_LEVEL));
        }

        @Test
        void shouldShiftOutOnlyLastLevel() {
            OrderBookLevel[] expectedLevels = {
                    new OrderBookLevel(1, 1),
                    TEST_NEW_LEVEL,
                    new OrderBookLevel(2, 1),
                    new OrderBookLevel(3, 1),
                    new OrderBookLevel(4, 1),
                    };

            assertArrayEquals(expectedLevels, testLevels.toArray());
        }

        @Test
        void shouldIgnoreIrrelevantNewLevels() {
            testLevels = new OrderBookLevels(PRICE_ASCENDING, TEST_DEPTH);
            testLevels.set(SAMPLE_LEVELS);

            testLevels.add(new OrderBookLevel(6, 1));

            assertArrayEquals(SAMPLE_LEVELS, testLevels.toArray());
        }
    }

    @Nested
    class WhenLevelQuantityUpdated {
        private final double PRICE_LEVEL_TO_UPDATE = 1, UPDATED_QUANTITY = 10;

        @BeforeEach
        void updateLevel() {
            testLevels.add(new OrderBookLevel(PRICE_LEVEL_TO_UPDATE, UPDATED_QUANTITY));
        }

        @Test
        void levelShouldBeUpdatedCorrectly() {
            double quantityForUpdatedLevel = Stream.of(testLevels.toArray())
                                                   .filter(level -> level.getPrice() == PRICE_LEVEL_TO_UPDATE)
                                                   .findFirst()
                                                   .orElseThrow()
                                                   .getQuantity();

            assertEquals(UPDATED_QUANTITY, quantityForUpdatedLevel);
        }

        @Test
        void shouldOnlyContainOneElementPerPriceLevel() {
            long levelsForUpdatedQuantity = Stream.of(testLevels.toArray())
                                                  .filter(level -> level.getPrice() == PRICE_LEVEL_TO_UPDATE)
                                                  .count();

            assertEquals(1, levelsForUpdatedQuantity);
        }

        @Test
        void shouldMaintainProperOrderingOfLevels() {
            assertTrue(isSorted(testLevels.toArray(), PRICE_ASCENDING));
        }
    }

    @Nested
    class WhenPriceLevelRemoved {
        private final double PRICE_LEVEL_TO_REMOVE = 3;

        @Test
        void shouldNotContainLevelForPrice() {
            testLevels.add(new OrderBookLevel(PRICE_LEVEL_TO_REMOVE, 0));
            Optional<OrderBookLevel> levelForRemovedPrice = Stream.of(testLevels.toArray())
                                                                  .filter(level -> level.getPrice() ==
                                                                                   PRICE_LEVEL_TO_REMOVE)
                                                                  .findFirst();

            assertFalse(levelForRemovedPrice.isPresent());
        }

        @Test
        void shouldRetainReferenceToPreviouslyIrrelevantLevel() {
            var irrelevantLevel = new OrderBookLevel(6, 1);
            testLevels.add(irrelevantLevel);
            assertFalse(contains(testLevels.toArray(), irrelevantLevel));

            testLevels.add(new OrderBookLevel(PRICE_LEVEL_TO_REMOVE, 0));

            assertTrue(contains(testLevels.toArray(), irrelevantLevel));
        }

        @Test
        void shouldMaintainProperOrderingOfLevels() {
            testLevels.add(new OrderBookLevel(PRICE_LEVEL_TO_REMOVE, 0));

            assertTrue(isSorted(testLevels.toArray(), PRICE_ASCENDING));
        }
    }
}