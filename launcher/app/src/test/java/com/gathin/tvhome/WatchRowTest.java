package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test public void containsPostersAndPaddingOnly() {
        // Dock at 0..4, posters at 5..6, padding at 7..9: reorder and Edit dock must leave all of 5..9 alone.
        assertTrue(WatchRow.contains(5, 5, 2, 3));
        assertTrue(WatchRow.contains(9, 5, 2, 3));
        assertFalse(WatchRow.contains(4, 5, 2, 3));
        assertFalse(WatchRow.contains(10, 5, 2, 3));
        assertFalse(WatchRow.contains(5, -1, 0, 0));   // no row
    }

    @Test public void paddingFocusGoesToLastPoster() {
        // Dock at 0..4, posters at 5..6, padding at 7..9.
        assertEquals(6, WatchRow.focusable(8, 5, 2, 3));
        assertEquals(5, WatchRow.focusable(5, 5, 2, 3));
        assertEquals(12, WatchRow.focusable(12, 5, 2, 3));
    }
}
