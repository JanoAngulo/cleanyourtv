package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.tv.TvContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads the TV's Watch Next rows (docs/watch-next.md). Needs READ_TV_LISTINGS, which build-and-install grants;
 * without it the provider returns only this app's own rows (none), so the Continue watching row just stays away.
 * The query takes no selection: without ACCESS_ALL_EPG_DATA, passing one throws a SecurityException.
 */
final class WatchNextSource {
    static final String READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS";

    private WatchNextSource() { }

    static boolean permitted(Context c) {
        return c.checkSelfPermission(READ_TV_LISTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    /** Every Watch Next row the launcher may see, unfiltered (WatchNext.select picks); empty when it can see none. */
    static List<WatchNext.Program> read(Context c) {
        List<WatchNext.Program> out = new ArrayList<>();
        try (Cursor cur = c.getContentResolver().query(TvContract.WatchNextPrograms.CONTENT_URI,
                null, null, null, null)) {
            if (cur == null) return Collections.emptyList();
            while (cur.moveToNext()) {
                out.add(new WatchNext.Program(
                        str(cur, "package_name"), str(cur, "title"), str(cur, "episode_title"),
                        str(cur, "season_display_number"), str(cur, "episode_display_number"),
                        str(cur, "poster_art_uri"), str(cur, "intent_uri"),
                        num(cur, "last_engagement_time_utc_millis"), num(cur, "last_playback_position_millis"),
                        num(cur, "duration_millis")));
            }
        } catch (RuntimeException e) {
            // SecurityException without the permission, IllegalArgumentException, or a provider/cursor failure
            // (SQLiteException, IllegalStateException): no row rather than a crash on the loader thread.
            return Collections.emptyList();
        }
        return out;
    }

    private static String str(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        return i >= 0 && !c.isNull(i) ? c.getString(i) : null;
    }

    private static long num(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        return i >= 0 && !c.isNull(i) ? c.getLong(i) : 0L;
    }
}
