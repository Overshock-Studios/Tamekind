package com.tamekind.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreedingSeasonTest {

    private static final int DAY = 24_000;
    private static final int SEASON_LEN = 30;

    @Test
    void firstDayIsSpring() {
        assertEquals(BreedingSeason.Season.SPRING, BreedingSeason.fromGameTime(0L, SEASON_LEN));
        assertEquals(BreedingSeason.Season.SPRING, BreedingSeason.fromGameTime(DAY * 29L, SEASON_LEN));
    }

    @Test
    void rollsToSummerOnDay30() {
        assertEquals(BreedingSeason.Season.SUMMER, BreedingSeason.fromGameTime(DAY * 30L, SEASON_LEN));
    }

    @Test
    void cyclesThroughAllFour() {
        assertEquals(BreedingSeason.Season.AUTUMN, BreedingSeason.fromGameTime(DAY * 60L, SEASON_LEN));
        assertEquals(BreedingSeason.Season.WINTER, BreedingSeason.fromGameTime(DAY * 90L, SEASON_LEN));
        assertEquals(BreedingSeason.Season.SPRING, BreedingSeason.fromGameTime(DAY * 120L, SEASON_LEN));
    }

    @Test
    void negativeGameTimeClampsToSpring() {
        assertEquals(BreedingSeason.Season.SPRING, BreedingSeason.fromGameTime(-DAY * 50L, SEASON_LEN));
    }

    @Test
    void zeroSeasonLengthDefaultsToOne() {
        assertEquals(BreedingSeason.Season.SUMMER, BreedingSeason.fromGameTime(DAY, 0));
    }
}
