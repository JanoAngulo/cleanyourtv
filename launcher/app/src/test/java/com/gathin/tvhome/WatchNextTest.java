package com.gathin.tvhome;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WatchNextTest {
    private static final long NOW = 1_800_000_000_000L, DAY = 86_400_000L;
    private static final Set<String> ALL = new HashSet<>(Arrays.asList("com.wbd.stream", "com.netflix.ninja"));

    private static WatchNext.Program p(String pkg, String title, long engaged, long pos, long dur) {
        return new WatchNext.Program(pkg, title, null, null, null, "https://x/" + title, "intent:#Intent;end",
                engaged, pos, dur);
    }

    private static List<WatchNext.Program> select(WatchNext.Program... in) {
        return WatchNext.select(Arrays.asList(in), NOW, ALL, Collections.emptySet(), 5);
    }

    @Test public void newestFirst() {
        List<WatchNext.Program> out = select(p("com.wbd.stream", "A", NOW - 3 * DAY, 0, 0),
                p("com.netflix.ninja", "B", NOW - DAY, 0, 0));
        assertEquals("B", out.get(0).title);
        assertEquals("A", out.get(1).title);
    }

    @Test public void dropsFinished() {
        // 95% or more watched counts as done.
        assertTrue(select(p("com.wbd.stream", "A", NOW, 950, 1000)).isEmpty());
        assertEquals(1, select(p("com.wbd.stream", "A", NOW, 949, 1000)).size());
    }

    @Test public void dropsOlderThan60Days() {
        assertTrue(select(p("com.wbd.stream", "A", NOW - 61 * DAY, 0, 0)).isEmpty());
    }

    @Test public void unknownEngagementKeptAndSortedLast() {
        List<WatchNext.Program> out = select(p("com.wbd.stream", "NoDate", 0, 0, 0),
                p("com.netflix.ninja", "Dated", NOW - 59 * DAY, 0, 0));
        assertEquals(Arrays.asList("Dated", "NoDate"), Arrays.asList(out.get(0).title, out.get(1).title));
    }

    @Test public void dropsPackagesThatCannotLaunch() {
        // Prime Video is disabled by the debloat but may still have rows.
        assertTrue(select(p("com.amazon.amazonvideo.livingroom", "A", NOW, 0, 0)).isEmpty());
    }

    @Test public void dropsHiddenApps() {
        assertTrue(WatchNext.select(Collections.singletonList(p("com.wbd.stream", "A", NOW, 0, 0)),
                NOW, ALL, Collections.singleton("com.wbd.stream"), 5).isEmpty());
    }

    @Test public void oneEntryPerShowKeepsNewest() {
        List<WatchNext.Program> out = select(p("com.wbd.stream", "MobLand", NOW - 2 * DAY, 0, 0),
                p("com.wbd.stream", "MobLand", NOW - DAY, 0, 0));
        assertEquals(1, out.size());
        assertEquals(NOW - DAY, out.get(0).engagedMs);
    }

    @Test public void capsAtMax() {
        List<WatchNext.Program> in = new ArrayList<>();
        for (int i = 0; i < 9; i++) in.add(p("com.wbd.stream", "T" + i, NOW - i * DAY, 0, 0));
        assertEquals(5, WatchNext.select(in, NOW, ALL, Collections.emptySet(), 5).size());
    }

    @Test public void dropsBlankTitles() {
        assertTrue(select(p("com.wbd.stream", " ", NOW, 0, 0)).isEmpty());
        assertTrue(select(p("com.wbd.stream", null, NOW, 0, 0)).isEmpty());
    }

    @Test public void subtitleFormats() {
        assertEquals("S2 · E5", WatchNext.subtitle(new WatchNext.Program("a", "T", null, "2", "5", null, null, 0, 0, 0)));
        assertEquals("S2 · E5 · Pilot",
                WatchNext.subtitle(new WatchNext.Program("a", "T", "Pilot", "2", "5", null, null, 0, 0, 0)));
        assertEquals("Episode 9",
                WatchNext.subtitle(new WatchNext.Program("a", "T", "Episode 9", null, null, null, null, 0, 0, 0)));
        assertEquals("", WatchNext.subtitle(new WatchNext.Program("a", "T", null, null, null, null, null, 0, 0, 0)));
    }
}
