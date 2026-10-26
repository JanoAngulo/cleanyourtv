package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PosterCacheTest {
    @Test public void sampleSizeHalvesWhileBigEnough() {
        // A 1280x720 poster for a 400x225 tile decodes at half size, not a quarter (that would be too small).
        assertEquals(2, PosterCache.sampleSize(1280, 720, 400, 225));
    }

    @Test public void sampleSizeOneForSmallSources() {
        assertEquals(1, PosterCache.sampleSize(300, 169, 400, 225));
    }

    @Test public void sampleSizeNeverZero() {
        // Bounds that failed to decode come back as 0x0.
        assertEquals(1, PosterCache.sampleSize(0, 0, 400, 225));
    }

    @Test public void diskKeyIsStableSha1Hex() {
        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", PosterCache.diskKey("test"));
    }
}
