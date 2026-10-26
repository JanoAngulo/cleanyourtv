package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the Continue watching row shows, picked from the TV's Watch Next rows (WatchNextSource): only titles the
 * owner started in an app Home can open, not finished and not months old, one per show, newest first.
 */
final class WatchNext {
    /** Rows engaged with longer ago than this are left out: the TV keeps them for months (docs/watch-next.md). */
    static final long MAX_AGE_MS = 60L * 24 * 60 * 60 * 1000;
    /** Watched this far, a title counts as finished; some apps leave a "continue" row at 99%. */
    static final float DONE_FRACTION = 0.95f;

    private WatchNext() { }

    /** One Watch Next row as the TV provider holds it; strings may be null, times 0 when unknown. */
    static final class Program {
        final String pkg, title, episodeTitle, season, episode, posterUri, intentUri;
        final long engagedMs, positionMs, durationMs;

        Program(String pkg, String title, String episodeTitle, String season, String episode, String posterUri,
                String intentUri, long engagedMs, long positionMs, long durationMs) {
            this.pkg = pkg;
            this.title = title;
            this.episodeTitle = episodeTitle;
            this.season = season;
            this.episode = episode;
            this.posterUri = posterUri;
            this.intentUri = intentUri;
            this.engagedMs = engagedMs;
            this.positionMs = positionMs;
            this.durationMs = durationMs;
        }
    }

    /**
     * The rows to show, at most {@code max}. Rows from packages Home cannot launch (a disabled app keeps its rows)
     * or that the owner hid are dropped. A row without an engagement time is kept, after the dated ones.
     */
    static List<Program> select(List<Program> all, long nowMs, Set<String> launchable, Set<String> hidden, int max) {
        Map<String, Program> newest = new LinkedHashMap<>();
        for (Program p : all) {
            if (blank(p.title) || p.pkg == null || !launchable.contains(p.pkg) || hidden.contains(p.pkg)) continue;
            if (p.durationMs > 0 && p.positionMs >= DONE_FRACTION * p.durationMs) continue;
            if (p.engagedMs > 0 && nowMs - p.engagedMs > MAX_AGE_MS) continue;
            String show = p.pkg + "\n" + p.title.trim();
            Program seen = newest.get(show);
            if (seen == null || p.engagedMs > seen.engagedMs) newest.put(show, p);
        }
        List<Program> out = new ArrayList<>(newest.values());
        out.sort((a, b) -> {
            if ((a.engagedMs == 0) != (b.engagedMs == 0)) return a.engagedMs == 0 ? 1 : -1;
            return Long.compare(b.engagedMs, a.engagedMs);
        });
        return out.size() > max ? new ArrayList<>(out.subList(0, Math.max(0, max))) : out;
    }

    /** The line under the title: "S2 · E5 · Pilot", parts left out when the app did not fill them. */
    static String subtitle(Program p) {
        StringBuilder sb = new StringBuilder();
        if (!blank(p.season)) sb.append('S').append(p.season.trim());
        if (!blank(p.episode)) append(sb, "E" + p.episode.trim());
        if (!blank(p.episodeTitle)) append(sb, p.episodeTitle.trim());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (sb.length() > 0) sb.append(" · ");
        sb.append(part);
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
