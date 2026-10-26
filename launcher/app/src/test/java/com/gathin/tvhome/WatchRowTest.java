package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WatchRowTest {
    @Test public void padsShortRowToColumns() {
        // Two posters in a five-column grid: three empty cells keep the next app row on its own line.
        assertEquals(3, WatchRow.padding(2, 5, true, false, false));
    }

    @Test public void fullRowNeedsNoPadding() {
        assertEquals(0, WatchRow.padding(5, 5, true, false, false));
    }

    @Test public void noRowWhenEmpty() {
        assertEquals(-1, WatchRow.padding(0, 5, true, false, false));
    }

    @Test public void noRowWhileReordering() {
        assertEquals(-1, WatchRow.padding(3, 5, true, false, true));
    }

    @Test public void noRowWithoutDockRowOrInSingleRow() {
        assertEquals(-1, WatchRow.padding(3, 5, false, false, false));
        assertEquals(-1, WatchRow.padding(3, 5, true, true, false));
    }

    @Test public void paddingFocusGoesToLastPoster() {
        // Dock at 0..4, posters at 5..6, padding at 7..9.
        assertEquals(6, WatchRow.focusable(8, 5, 2, 3));
        assertEquals(5, WatchRow.focusable(5, 5, 2, 3));
        assertEquals(12, WatchRow.focusable(12, 5, 2, 3));
    }
}
