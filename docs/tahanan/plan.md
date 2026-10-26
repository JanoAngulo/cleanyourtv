# Tahanan Launcher Implementation Plan

**Goal:** Fork the CleanYourTV reference launcher into "Tahanan": the owner's TCL G03 home screen with a five-app dock, a Continue watching poster row one Down press below it, and rotating HD Philippine landscape wallpapers, installed with Google TV kept enabled behind it.

**Architecture:** The launcher's Home is one vertical `RecyclerView` grid (`MainActivity.grid`, `AppAdapter`) whose first row is the dock. Continue watching becomes one more grid row right after the dock: up to `columns` entries of a new `AppAdapter.Entry.WATCH` kind, padded with non-focusable `EMPTY` entries so the next app row starts on its own line. Data comes from the TV provider's Watch Next table (pure selection logic in `WatchNext`, Android read in `WatchNextSource`), posters through a small `PosterCache`. The look is a theme folder (`themes/philippines/`), not code.

**Tech Stack:** Java 17 source level, Android SDK 34 (minSdk 26), AndroidX RecyclerView 1.3.2 + DynamicAnimation (no new dependencies), JUnit 4 JVM tests, Gradle wrapper in `launcher/`, Python venv tools (`tools/tv.py`, `tools/theme.sh`), Pillow for photo resizing (outside the repo).

**Spec:** `docs/tahanan/design.md`

## Global Constraints

- Project root: `D:\Users\Documents\.Website Portfolio\CleanYourTv\tv-home` (a full clone of the CleanYourTV repo). All paths below are relative to it unless absolute.
- Application ID stays `com.gathin.tvhome`; visible name is "Tahanan".
- minSdk 26, targetSdk 34, compileSdk 34; keep `<queries>` for LEANBACK_LAUNCHER and the opaque window theme.
- No new library dependencies. No background services, polling, blur or video. Image memory bounded (TV: 2 GB RAM, 32-bit).
- `android.permission.INTERNET` is used only to fetch poster images. No analytics or other network calls.
- Continue watching reads only `TvContract.WatchNextPrograms`, never preview programs; query without a selection.
- Hide items at ≥ 95% progress, items engaged more than 60 days ago; newest engagement first; at most `columns` (5) items.
- Dock: `com.disney.disneyplus`, `com.wbd.stream`, `com.netflix.ninja`, `com.google.android.youtube.tv`, `com.spotify.tv.android`, in that order; `columns` 5.
- Google TV (`com.google.android.apps.tv.launcherx`), setupwraith and dreamx stay enabled. Install with `STOCK_LAUNCHER=none`.
- Wallpapers: 10–12 Philippine landscapes, CC BY / CC BY-SA / CC0 / PD only, 1920×1080 JPEG each, rotating every 60 min, sequential, place caption on, credits in `CREDITS.md`.
- Keep MIT `LICENSE` and credit to the original project.
- Commit messages, README and code comments carry no attribution trailers.
- TV: `TV_HOST=<tv ip>`. In Git Bash always `export MSYS_NO_PATHCONV=1` before passing TV paths.
- Windows write block: if a tool fails with "Access is denied" under `D:\Users\Documents`, move that tool's output outside Documents (Task 1 decides how) — do not disable Windows security yourself.
- Owner-facing questions during install (Home vs Live TV at power-on, volume-repeat trade-off, Google TV flash) are asked, not assumed.

## Review Focus

1. **Watch Next rows from a disabled or uninstalled app** (Prime Video is disabled on this TV and may still have rows) → not shown. Test: `WatchNextTest.dropsPackagesThatCannotLaunch` (Task 2).
2. **Poster missing, 404, slow or offline** → tile shows a dark gradient with the title, UI never blocks, no crash. Test: `PosterCacheTest.sampleSizeNeverZero` plus the offline check in Task 6 step "Wi-Fi off" (Task 6).
3. **`READ_TV_LISTINGS` not granted** (fresh install without the script, or revoked) → row absent, no crash, no prompt on Home. Check: Task 6 step "revoke permission".
4. **Stale or malformed `intent_uri`** (content removed, unparseable) → opens the app's start screen instead; never an uncaught `ActivityNotFoundException`/`URISyntaxException`. Check: Task 5 launch fallback + Task 6 "bad intent" step.
5. **Reorder mode and module cards** → the Continue watching row never takes part in reorder (not movable, not saved to `Order`), and fewer than 5 posters never pull app tiles up into its row. Test: `WatchRowTest.padsShortRowToColumns`, `WatchRowTest.noRowWhileReordering` (Task 3).

---

### Task 1: Project setup, rename to Tahanan, baseline build

**Files:**
- Create: `tv-home/` (clone of `CleanYourTv/repo`)
- Modify: `launcher/app/src/main/res/values*/strings.xml` (every `name="app_name"`, 27 files)
- Modify: `README.md` (top section only)

- [ ] **Step 1: Clone and detach from upstream**

```bash
cd "/d/Users/Documents/.Website Portfolio/CleanYourTv"
git clone repo tv-home
cd tv-home && git remote rename origin local-clean && git remote add upstream https://github.com/Gathin23/cleanyourtv.git
git checkout -b tahanan
```
Expected: `git status` clean on branch `tahanan`. `repo/work`, `repo/backup`, `repo/shots` are git-ignored and therefore **not** copied; leave them in `repo/` (restore keeps running from there).

- [ ] **Step 2: Tools venv**

```bash
py -3.14 -m venv tools/venv && tools/venv/Scripts/pip install -q -r tools/requirements.txt
TV_HOST=<tv ip> tools/venv/Scripts/python.exe -I tools/tv.py --probe
```
Expected: prints `ro.product.model = Smart TV Pro`.

- [ ] **Step 3: SDK platform 34 and JDK**

Run `"$LOCALAPPDATA/Android/Sdk/cmdline-tools/latest/bin/sdkmanager.bat" "platforms;android-34"` (accept licence). Use JDK 21 at `C:\Program Files\Java\jdk-21`; if Gradle 8.9 rejects it, install Temurin 17 with `winget install EclipseAdoptium.Temurin.17.JDK` and use that `JAVA_HOME`.

- [ ] **Step 4: Baseline build and tests**

```bash
cd launcher && export JAVA_HOME="/c/Program Files/Java/jdk-21"
./gradlew --no-daemon testReleaseUnitTest assembleRelease
```
Expected: `BUILD SUCCESSFUL`, `app/build/outputs/apk/release/app-release.apk` exists.
If it fails with "Access is denied" writing under `app/build`: create `launcher/init-builddir.gradle` with `allprojects { layout.buildDirectory = file("C:/tv-home-build/${project.name}") }`, run with `--init-script init-builddir.gradle`, and point `APK=` in `build-and-install.sh` at `${TAHANAN_BUILD_DIR:-app/build}/outputs/apk/release/app-release.apk` (default unchanged). Re-run; expected PASS.

- [ ] **Step 5: Rename to Tahanan**

Set every `<string name="app_name">…</string>` under `launcher/app/src/main/res/` to `Tahanan`. At the top of `README.md` add a short section: "Tahanan — John Russel Angulo's home screen for a TCL Smart TV Pro (G03, Google TV 11), forked from CleanYourTV by Gathin23 (MIT)." Keep `LICENSE` untouched.

- [ ] **Step 6: Build again, commit**

Run: `./gradlew --no-daemon testReleaseUnitTest assembleRelease` → `BUILD SUCCESSFUL`.

```bash
git add -A launcher/app/src/main/res README.md launcher/build-and-install.sh launcher/init-builddir.gradle 2>/dev/null
git commit -m "Launcher: rename to Tahanan"
```

---

### Task 2: Watch Next selection logic (pure)

**Files:**
- Create: `launcher/app/src/main/java/com/gathin/tvhome/WatchNext.java`
- Test: `launcher/app/src/test/java/com/gathin/tvhome/WatchNextTest.java`

**Interfaces:**
- Produces:
  - `final class WatchNext.Program { final String pkg, title, episodeTitle, season, episode, posterUri, intentUri; final long engagedMs, positionMs, durationMs; Program(String pkg, String title, String episodeTitle, String season, String episode, String posterUri, String intentUri, long engagedMs, long positionMs, long durationMs) }`
  - `static final long MAX_AGE_MS = 60L * 24 * 60 * 60 * 1000;`
  - `static final float DONE_FRACTION = 0.95f;`
  - `static List<Program> select(List<Program> all, long nowMs, Set<String> launchable, Set<String> hidden, int max)`
  - `static String subtitle(Program p)` → `"S2 · E5"`, `"S2 · E5 · Title"`, `"E5"`, the episode title alone, or `""`.

- [ ] **Step 1: Write the failing tests**

```java
public class WatchNextTest {
    static final long NOW = 1_800_000_000_000L, DAY = 86_400_000L;
    static WatchNext.Program p(String pkg, String title, long engaged, long pos, long dur) {
        return new WatchNext.Program(pkg, title, null, null, null, "https://x/" + title, "intent:#Intent;end", engaged, pos, dur);
    }
    static final Set<String> ALL = new HashSet<>(Arrays.asList("com.wbd.stream", "com.netflix.ninja"));

    @Test public void newestFirst() {
        List<WatchNext.Program> out = WatchNext.select(Arrays.asList(
                p("com.wbd.stream", "A", NOW - 3 * DAY, 0, 0), p("com.netflix.ninja", "B", NOW - DAY, 0, 0)),
                NOW, ALL, Collections.emptySet(), 5);
        assertEquals("B", out.get(0).title);
        assertEquals("A", out.get(1).title);
    }
    @Test public void dropsFinished() {   // 95% or more watched
        assertTrue(WatchNext.select(Collections.singletonList(p("com.wbd.stream", "A", NOW, 950, 1000)),
                NOW, ALL, Collections.emptySet(), 5).isEmpty());
        assertEquals(1, WatchNext.select(Collections.singletonList(p("com.wbd.stream", "A", NOW, 949, 1000)),
                NOW, ALL, Collections.emptySet(), 5).size());
    }
    @Test public void dropsOlderThan60Days() {
        assertTrue(WatchNext.select(Collections.singletonList(p("com.wbd.stream", "A", NOW - 61 * DAY, 0, 0)),
                NOW, ALL, Collections.emptySet(), 5).isEmpty());
    }
    @Test public void unknownEngagementKeptAndSortedLast() {
        List<WatchNext.Program> out = WatchNext.select(Arrays.asList(
                p("com.wbd.stream", "NoDate", 0, 0, 0), p("com.netflix.ninja", "Dated", NOW - 59 * DAY, 0, 0)),
                NOW, ALL, Collections.emptySet(), 5);
        assertEquals(Arrays.asList("Dated", "NoDate"), Arrays.asList(out.get(0).title, out.get(1).title));
    }
    @Test public void dropsPackagesThatCannotLaunch() {   // e.g. Prime Video disabled by the debloat
        assertTrue(WatchNext.select(Collections.singletonList(p("com.amazon.amazonvideo.livingroom", "A", NOW, 0, 0)),
                NOW, ALL, Collections.emptySet(), 5).isEmpty());
    }
    @Test public void dropsHiddenApps() {
        assertTrue(WatchNext.select(Collections.singletonList(p("com.wbd.stream", "A", NOW, 0, 0)),
                NOW, ALL, Collections.singleton("com.wbd.stream"), 5).isEmpty());
    }
    @Test public void oneEntryPerShowKeepsNewest() {
        List<WatchNext.Program> out = WatchNext.select(Arrays.asList(
                p("com.wbd.stream", "MobLand", NOW - 2 * DAY, 0, 0), p("com.wbd.stream", "MobLand", NOW - DAY, 0, 0)),
                NOW, ALL, Collections.emptySet(), 5);
        assertEquals(1, out.size());
        assertEquals(NOW - DAY, out.get(0).engagedMs);
    }
    @Test public void capsAtMax() {
        List<WatchNext.Program> in = new ArrayList<>();
        for (int i = 0; i < 9; i++) in.add(p("com.wbd.stream", "T" + i, NOW - i * DAY, 0, 0));
        assertEquals(5, WatchNext.select(in, NOW, ALL, Collections.emptySet(), 5).size());
    }
    @Test public void dropsBlankTitles() {
        assertTrue(WatchNext.select(Collections.singletonList(p("com.wbd.stream", " ", NOW, 0, 0)),
                NOW, ALL, Collections.emptySet(), 5).isEmpty());
    }
    @Test public void subtitleFormats() {
        assertEquals("S2 · E5", WatchNext.subtitle(new WatchNext.Program("a", "T", null, "2", "5", null, null, 0, 0, 0)));
        assertEquals("S2 · E5 · Pilot", WatchNext.subtitle(new WatchNext.Program("a", "T", "Pilot", "2", "5", null, null, 0, 0, 0)));
        assertEquals("Episode 9", WatchNext.subtitle(new WatchNext.Program("a", "T", "Episode 9", null, null, null, null, 0, 0, 0)));
        assertEquals("", WatchNext.subtitle(new WatchNext.Program("a", "T", null, null, null, null, null, 0, 0, 0)));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew --no-daemon testReleaseUnitTest --tests com.gathin.tvhome.WatchNextTest`
Expected: FAIL, `cannot find symbol: class WatchNext`.

- [ ] **Step 3: Implement `WatchNext`**

Rules in order: drop blank title; drop `pkg` not in `launchable` or in `hidden`; drop `durationMs > 0 && positionMs >= DONE_FRACTION * durationMs`; drop `engagedMs > 0 && nowMs - engagedMs > MAX_AGE_MS`; keep one per `pkg + title` (newest `engagedMs`); sort by `engagedMs` descending with 0 last; cap at `max`. `subtitle` joins non-blank `"S"+season`, `"E"+episode`, `episodeTitle` with `" · "`.

- [ ] **Step 4: Run tests** → PASS (all 10).

- [ ] **Step 5: Commit** `git commit -m "Launcher: Watch Next selection for the Continue watching row"`

---

### Task 3: Continue watching row geometry (pure)

**Files:**
- Create: `launcher/app/src/main/java/com/gathin/tvhome/WatchRow.java`
- Test: `launcher/app/src/test/java/com/gathin/tvhome/WatchRowTest.java`

**Interfaces:**
- Produces:
  - `static int padding(int items, int columns, boolean dockRow, boolean singleRow, boolean reordering)` — empty entries to add after the posters; `-1` means "no row at all".
  - `static int focusable(int position, int start, int items, int padding)` — a position in the padding gives the last poster; any other position is returned unchanged.

- [ ] **Step 1: Failing tests**

```java
public class WatchRowTest {
    @Test public void padsShortRowToColumns() { assertEquals(3, WatchRow.padding(2, 5, true, false, false)); }
    @Test public void fullRowNeedsNoPadding() { assertEquals(0, WatchRow.padding(5, 5, true, false, false)); }
    @Test public void noRowWhenEmpty() { assertEquals(-1, WatchRow.padding(0, 5, true, false, false)); }
    @Test public void noRowWhileReordering() { assertEquals(-1, WatchRow.padding(3, 5, true, false, true)); }
    @Test public void noRowWithoutDockRowOrInSingleRow() {
        assertEquals(-1, WatchRow.padding(3, 5, false, false, false));
        assertEquals(-1, WatchRow.padding(3, 5, true, true, false));
    }
    @Test public void paddingFocusGoesToLastPoster() {
        // dock at 0..4, posters at 5..6, padding at 7..9
        assertEquals(6, WatchRow.focusable(8, 5, 2, 3));
        assertEquals(5, WatchRow.focusable(5, 5, 2, 3));
        assertEquals(12, WatchRow.focusable(12, 5, 2, 3));
    }
}
```

- [ ] **Step 2: Run** `--tests com.gathin.tvhome.WatchRowTest` → FAIL (missing class).
- [ ] **Step 3: Implement `WatchRow`** (two static methods, private constructor, like `DockSlots`).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "Launcher: Continue watching row geometry"`

---

### Task 4: Poster cache

**Files:**
- Create: `launcher/app/src/main/java/com/gathin/tvhome/PosterCache.java`
- Test: `launcher/app/src/test/java/com/gathin/tvhome/PosterCacheTest.java`

**Interfaces:**
- Produces:
  - `static int sampleSize(int srcW, int srcH, int reqW, int reqH)` — largest power of two keeping both sides ≥ requested; never < 1.
  - `static String diskKey(String uri)` — lowercase hex SHA-1 of the URI (UTF-8), 40 chars.
  - `PosterCache(Context ctx)`; `void load(String uri, int reqW, int reqH, ImageView into)` — sets the bitmap on the main thread only if `into.getTag(R.id.poster_uri)` still equals `uri`; on failure leaves the view untouched.
  - `void trim()` — clears memory cache (call from `onTrimMemory`, see `MemoryTrimPolicy`).

- [ ] **Step 1: Failing tests**

```java
public class PosterCacheTest {
    @Test public void sampleSizeHalvesWhileBigEnough() { assertEquals(2, PosterCache.sampleSize(1280, 720, 400, 225)); }
    @Test public void sampleSizeOneForSmallSources() { assertEquals(1, PosterCache.sampleSize(300, 169, 400, 225)); }
    @Test public void sampleSizeNeverZero() { assertEquals(1, PosterCache.sampleSize(0, 0, 400, 225)); }
    @Test public void diskKeyIsStableSha1Hex() {
        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", PosterCache.diskKey("test"));
    }
}
```

- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.** Memory: `android.util.LruCache<String, Bitmap>` sized by bytes, `Runtime.maxMemory() / 16`. Disk: `cacheDir/posters/<diskKey>.img`, total capped at 20 MB (delete oldest by `lastModified` after each write). Fetch: `HttpURLConnection`, connect 5 s / read 8 s, `https`/`http` only; `content://` and `android.resource://` through `ContentResolver.openInputStream`. Decode bounds first, then `inSampleSize = sampleSize(...)`, `Bitmap.Config.RGB_565`. Executor: `Executors.newFixedThreadPool(2)`. Add `<item name="poster_uri" type="id"/>` to `res/values/ids.xml` (create if absent).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `git commit -m "Launcher: poster cache for Continue watching"`

---

### Task 5: Read the TV provider and show the row in the adapter

**Files:**
- Create: `launcher/app/src/main/java/com/gathin/tvhome/WatchNextSource.java`
- Create: `launcher/app/src/main/res/layout/item_watch.xml` (only if the existing tile layout `item_app.xml` cannot show a poster + title overlay; prefer reusing it)
- Modify: `launcher/app/src/main/AndroidManifest.xml` (permissions, `<queries>`)
- Modify: `launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java` (`Entry`, `reload`, bind, click, `movableKeys`, `focusablePosition`)
- Modify: `launcher/build-and-install.sh` and `launcher/build-and-install.ps1` (grant)

**Interfaces:**
- Consumes: `WatchNext.Program`, `WatchNext.select`, `WatchNext.subtitle` (Task 2); `WatchRow.padding`, `WatchRow.focusable` (Task 3); `PosterCache` (Task 4).
- Produces:
  - `final class WatchNextSource { static boolean permitted(Context c); static List<WatchNext.Program> read(Context c) }` — `read` returns an empty list when not permitted, on `SecurityException`, or a null cursor; queries `TvContract.WatchNextPrograms.CONTENT_URI` with null projection/selection; maps columns `package_name, title, episode_title, season_display_number, episode_display_number, poster_art_uri, intent_uri, last_engagement_time_utc_millis, last_playback_position_millis, duration_millis` (missing column → null/0).
  - `AppAdapter.Entry.WATCH = 4` and field `final WatchNext.Program watch` (null for other kinds).
  - `void AppAdapter.setWatchNext(List<WatchNext.Program> selected)` — stores the list; takes effect at the next `reload(...)`.
  - `boolean AppAdapter.isWatchAt(int position)`.

- [ ] **Step 1: Manifest**

Add `<uses-permission android:name="android.permission.INTERNET" />`, `<uses-permission android:name="android.permission.READ_TV_LISTINGS" />`, `<uses-permission android:name="com.android.providers.tv.permission.READ_EPG_DATA" />`, and inside `<queries>` `<provider android:authorities="android.media.tv" />`. Comment each like the neighbouring entries (why, who grants it).

- [ ] **Step 2: `WatchNextSource`** as specified above.

- [ ] **Step 3: Adapter row**

In `reload`, after the dock entries and dock empty slots (i.e. at index `dockSize` when `dockRow`), when `WatchRow.padding(n, dockSize, dockRow, singleRow, reordering) >= 0` insert the `n` WATCH entries then that many `EMPTY` entries (component `empty-watch-slot-i`). WATCH entry: `label` = title, `name` = `WatchNext.subtitle` (or title when blank), component `new ComponentName(pkg, "watch:" + i)`, banner/icon null. `movableKeys` and `move` skip/refuse WATCH entries; `focusablePosition` applies `WatchRow.focusable` after `DockSlots.focusable`. Bind: show the poster through `PosterCache.load(posterUri, tileW, tileH, view)` with tag `R.id.poster_uri`; until it loads (or without a poster) a vertical gradient `#1C2A44 → #0B1324` with the title in the tile. Every WATCH tile carries the source app's icon as a small badge (about 28 dp, bottom-left, from `pm.getApplicationIcon(pkg)`) so the owner sees where it opens. Click: `Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)` + `FLAG_ACTIVITY_NEW_TASK`; on `URISyntaxException`, `ActivityNotFoundException` or `SecurityException` fall back to `pm.getLeanbackLaunchIntentForPackage(pkg)` (then `getLaunchIntentForPackage`). Long-press on a WATCH tile does nothing.

- [ ] **Step 4: Install grant**

In `build-and-install.sh`, in the batch at the `pm grant … ACCESS_FINE_LOCATION` line, add `"pm grant com.gathin.tvhome android.permission.READ_TV_LISTINGS 2>/dev/null; echo tv-listings-granted"`. Same in `build-and-install.ps1`.

- [ ] **Step 5: Build, tests**

Run: `./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease` → `BUILD SUCCESSFUL`, lint 0 errors. (The repo's 576 KB APK size gate does not bind this fork; report the size anyway.)

- [ ] **Step 6: Commit** `git commit -m "Launcher: Continue watching row from the TV's Watch Next"`

---

### Task 6: Wire into MainActivity and verify on the TV (Google TV still Home)

**Files:**
- Modify: `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` (`onResume`, `showHero`, tile click, `onTrimMemory`)

**Interfaces:**
- Consumes: `WatchNextSource.read/permitted`, `WatchNext.select`, `AppAdapter.setWatchNext`, `AppAdapter.isWatchAt`, `PosterCache.trim`.

- [ ] **Step 1: Load on resume.** In `onResume`, on a background thread: `WatchNext.select(WatchNextSource.read(this), System.currentTimeMillis(), <packages with a launch intent and enabled>, hiddenApps, COLUMNS)`; post back, `adapter.setWatchNext(...)`, then the existing reload path. No polling. Keep focus on the previously focused key (`keyAt`/`positionOf`).
- [ ] **Step 2: Hero.** When the focused position `isWatchAt`, the hero shows the title (existing `labelAt`) and the subtitle in the smaller line the cinematic shelf uses for "Last opened"; glow colour from the poster when sampled, else the theme's accent.
- [ ] **Step 3: Trim.** Call `PosterCache.trim()` where `MemoryTrimPolicy` releases memory.
- [ ] **Step 4: Build and side-install without touching Home**

```bash
export MSYS_NO_PATHCONV=1 TV_HOST=<tv ip>
tools/venv/Scripts/python.exe -I tools/b64push.py launcher/app/build/outputs/apk/release/app-release.apk /data/local/tmp/tvhome.apk
tools/venv/Scripts/python.exe -I tools/tv.py 'pm install -r /data/local/tmp/tvhome.apk; pm grant com.gathin.tvhome android.permission.READ_TV_LISTINGS; am start -n com.gathin.tvhome/.MainActivity'
tools/venv/Scripts/python.exe -I tools/tv.py --screenshot shots/t6-home.png DPAD_DOWN --wait 3
```
Expected: screenshot shows the dock and, one row down, posters (HBO Max, Netflix, Spotify at least; iQIYI as a gradient tile with title). Google TV is still Home (press Home returns to it).

- [ ] **Step 5: Navigation.** Inject `DPAD_DOWN`, `DPAD_RIGHT` ×5, `DPAD_UP`, `DPAD_DOWN` ×2, screenshot after each: focus moves dock → posters → grid and back; right edge of a short poster row does not jump into the grid. OK on a poster opens that title (check `dumpsys activity activities | grep -i ResumedActivity` shows the app).
- [ ] **Step 6: Failure modes (Review Focus 2–4).** (a) `pm revoke com.gathin.tvhome android.permission.READ_TV_LISTINGS`, restart → no row, no crash (`logcat -b crash -d` empty); grant again. (b) Ask the owner to switch Wi-Fi off for a minute → row shows gradient tiles, D-pad stays smooth; Wi-Fi back. (c) Bad `intent_uri`: no shell path can write Watch Next rows, so this is a reviewer check — the click handler catches `URISyntaxException`, `ActivityNotFoundException` and `SecurityException` and each falls back to the leanback launch intent; then open every poster on the row once on the TV and confirm each lands in its app.
- [ ] **Step 7: Commit** `git commit -m "Launcher: load Continue watching on resume, hero title for posters"`

---

### Task 7: Philippine landscapes theme

**Files:**
- Create: `themes/philippines/config.json`, `themes/philippines/wallpapers/001.jpg … 012.jpg`, `themes/philippines/wallpapers/captions.txt`, `themes/philippines/CREDITS.md`, `themes/philippines/screenshot.jpg`
- Work dir (not committed): a scratch folder outside the repo (originals, each download in that new empty dir; resize script kept elsewhere in the scratchpad)

- [ ] **Step 1: Pick photos.** From Wikimedia Commons (prefer Featured/Quality/Valued images), 10–12 Philippine landscapes across: El Nido or Coron (Palawan), Banaue/Batad rice terraces, Chocolate Hills, Mayon, Siargao, Batanes, Taal, a beach (Boracay or similar), Mt. Pulag or another sea of clouds, one underwater/reef or waterfall. Check via the Commons API `extmetadata`: licence is CC BY, CC BY-SA, CC0 or PD; original width ≥ 3840 px (or visibly sharp at that size); no watermark; people not the main subject. Show the owner the shortlist (thumbnails or titles) before resizing.
- [ ] **Step 2: Resize.** Python with Pillow in a scratchpad venv (`python -I`): centre-crop to 16:9, resize to exactly 1920×1080 with `Image.LANCZOS`, save JPEG quality 88, progressive, sRGB, metadata stripped; aim near the size of `themes/nature-calm/wallpapers/*.jpg`. Name `001.jpg`…, in an order that alternates colour/time of day.
- [ ] **Step 3: Captions and credits.** `captions.txt`: `001.jpg<TAB>El Nido, Palawan` (place only). `CREDITS.md`: per file — place, author, licence, source URL, "resized and cropped".
- [ ] **Step 4: `config.json`**

```json
{
  "dock_apps": ["com.disney.disneyplus", "com.wbd.stream", "com.netflix.ninja",
                "com.google.android.youtube.tv", "com.spotify.tv.android"],
  "columns": 5,
  "live_tv_package": "com.tcl.tv",
  "show_greeting": false,
  "icon_only_buttons": true,
  "wallpaper": true,
  "wallpaper_rotation_minutes": 60,
  "wallpaper_order": "sequential",
  "wallpaper_captions": true,
  "wallpaper_dim": 25,
  "dock_glass": true,
  "dock_opacity": 70,
  "app_glow": true,
  "focus_overshoot": false,
  "background_color": "#0B1324",
  "stock_home_redirect": "com.google.android.apps.tv.launcherx",
  "key_filter": false
}
```
`wallpaper_dim` and `dock_opacity` are starting values, tuned in Step 5. Remote keys are added in Task 8.

- [ ] **Step 5: Push and tune.** `TV_HOST=<tv ip> tools/theme.sh themes/philippines`, open Tahanan, screenshot each wallpaper (temporarily `wallpaper_rotation_minutes: 1` if needed, restored to 60 after). Clock, buttons, dock names and posters readable on every photo; replace any photo that fights the text. Save the final Home as `themes/philippines/screenshot.jpg`.
- [ ] **Step 6: Commit** `git commit -m "themes: philippines, Philippine landscapes for Tahanan"`

---

### Task 8: Make Tahanan Home, remote buttons, reboot check

**Files:**
- Modify: `themes/philippines/config.json` (key mapping)
- Modify: `CleanYourTv/repo/work/debloat.txt` header only (notes; nothing new disabled)

- [ ] **Step 1: Record what to undo.** `tools/tv.py 'settings get secure screensaver_components; settings get secure enabled_accessibility_services'` → save output to `CleanYourTv/repo/backup/before-tahanan.txt`.
- [ ] **Step 2: Install as Home, Google TV kept.**

```bash
cd launcher && TV_HOST=<tv ip> STOCK_LAUNCHER=none HOME_KEY_SERVICE=1 ./build-and-install.sh
```
Expected: `Home now resolves to com.gathin.tvhome/...`; `launcherx` still enabled (`pm list packages -d | grep launcherx` empty). Confirm with `dumpsys activity activities | grep mFocusedApp` after pressing Home.
- [ ] **Step 3: Screensaver.** `settings put secure screensaver_components com.gathin.tvhome/.ClockDream`.
- [ ] **Step 4: Map the remote.** Run `getevent -lt /dev/input/event3` (and logcat for key broadcasts) while the owner presses every button once and names it. Add to the theme: `settings_key_keycodes: [4077]` (gear, confirm), `settings_key_panel` per owner's preference, `app_keys` for each app button found (G03 references: `KEYCODE_TCL_NETFLIX:com.netflix.ninja`, `KEYCODE_TCL_YOUTUBE:com.google.android.youtube.tv`; Prime button — owner decides, app is disabled). Push theme, then the owner presses each real button; each opens the right thing. Input still opens TCL's source panel.
- [ ] **Step 5: Volume and flash.** Owner holds volume up: it ramps. If it steps one notch at a time, report the G09 trade-off and let the owner choose. Owner presses Home from an app ~5 times: report whether Google TV flashes; if it bothers them, note it as a follow-up (disabling launcherx is a separate decision).
- [ ] **Step 6: Reboot check.** `tools/tv.py --reboot`, wait for `sys.boot_completed`, then: Home is Tahanan (`mFocusedApp`), Wi-Fi up, HDMI 1–3 / AV / Live TV from Inputs, Continue watching present, `logcat -b crash -d` empty, `pm list packages -d` unchanged (27). Check power-on target (`logcat -b events -d | grep wm_set_resumed_activity`) and ask the owner Home or Live TV; set `power_on_home` or `boot_to_live_tv` accordingly (never both).
- [ ] **Step 7: Undo path documented.** Append to `repo/work/debloat.txt` header: Tahanan installed; undo Home with `repo/work/restore-tv.ps1`, screensaver back with the value in `backup/before-tahanan.txt`, `pm uninstall com.gathin.tvhome` optional. Dry-run `recovery.py --commands` still shows the 18:49 record.
- [ ] **Step 8: Commit** `git commit -m "themes: philippines remote buttons for the TCL RC902V remote"`

---

### Task 9: Close network ADB (only when the owner says they are done)

- [ ] **Step 1:** Ask the owner to confirm no more changes today.
- [ ] **Step 2:** Owner turns off USB debugging (and Network debugging if listed) and taps "Revoke USB debugging authorisations"; reboot.
- [ ] **Step 3:** `tools/venv/Scripts/python.exe -I tools/adb-closed.py <tv ip>` → reports the port closed ("Connection refused").
