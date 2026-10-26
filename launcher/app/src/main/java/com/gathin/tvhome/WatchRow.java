package com.gathin.tvhome;

/**
 * The Continue watching row is the grid row right after the dock: its posters, then empty cells up to a full row,
 * so the app grid below always starts on its own line (as DockSlots does for the dock).
 */
final class WatchRow {
    private WatchRow() { }

    /**
     * Empty cells to put after {@code items} posters, or -1 for no row at all: nothing to show, no dock row to sit
     * under, a single sideways row, or reorder mode (the row is not something the owner arranges).
     */
    static int padding(int items, int columns, boolean dockRow, boolean singleRow, boolean reordering) {
        if (items <= 0 || !dockRow || singleRow || reordering) return -1;
        return Math.max(0, columns - items);
    }

    /** A position focus can land on: an empty cell of the row sends it to the last poster. */
    static int focusable(int position, int start, int items, int padding) {
        int firstPad = start + items;
        return padding > 0 && position >= firstPad && position < firstPad + padding ? firstPad - 1 : position;
    }
}
