# TV Home launcher: design

Date: 2026-10-26
Owner: John Russel Angulo
Status: draft for owner approval

## 1. Intent

The owner's TCL Google TV opens on a home screen full of ads ("Sponsored" banner, "Top picks", TCL rows).
The debloat is already done (section 3); what is left is the home screen itself.

The owner wants a home screen that is **beautiful, clean, with minimal distractions**, where **the
streaming apps are shown immediately** when the TV turns on, in the spirit of Apple TV (tvOS) — the
reference the owner picked. On top of the reference launcher it should show **what they were watching**
(movie and episode posters), and **every button on the remote should keep working**.

It is also the owner's own project: a fork of the CleanYourTV reference launcher that they own and can
keep working on.

## 2. Goals, non-goals, anti-goals

Goals
- Replace Google TV's home (launcherx) with the owner's launcher, safely and reversibly.
- Front row of five apps, visible at power-on: Disney+, HBO Max, Netflix, YouTube, Spotify (this order).
- A "Continue watching" row of posters **directly below** the front row.
- Philippine landscape photos as the background, sharp on the TV.
- Every remote button that worked under Google TV still does something sensible.
- Keep everything the reference launcher already does on this model: Inputs menu (HDMI 1-3, AV, Live TV),
  clock and date, Wi-Fi and Settings buttons, app grid, D-pad behaviour, Home safety.

Non-goals
- Content recommendations, trending rows, or anything the owner did not start watching.
- Voice: the remote's mic opens Google Assistant but no audio arrives. This was tested with every debloated
  package restored and after re-pairing, so it is not this project's to fix (suspected remote battery).
  The launcher must not make it worse.
- Publishing to GitHub or the Play Store. Not now; the owner decides later.

Anti-goals (would be a failure even if it technically works)
- Any ad, sponsored tile or promo item appearing on Home — including the promo rows apps publish
  (e.g. Netflix's "Join Netflix for as low as PHP169/month" preview program).
- A busy screen: autoplaying video, many rows, flashing.
- Losing HDMI/Live TV access, Wi-Fi, ADB, or a working Home button at any point.
- Sluggish navigation on this 2 GB, 32-bit TV.
- A blank or black Home with no way back.

## 3. Current state

The TV
- TCL Smart TV Pro, device BeyondTV4, product G03_4K_GB, Realtek RTD2851M / R51MT05, software
  V8-R51MT05-LF1V620, client TCL-LA-RTD51M-S1.
- Google TV on Android 11 (SDK 30), build RP1A.200622.001, 32-bit userspace (armeabi-v7a), 2 GB RAM.
- UI drawn at 1920x1080, density 320 (panel is 4K).
- Network ADB on <tv ip>:5555, `ro.adb.secure=1`, this PC authorised. No Wireless debugging.
- Current Home: `com.google.android.apps.tv.launcherx/.home.HomeActivity`. Other HOME claimants:
  `com.google.android.tungsten.setupwraith/.RecoveryActivity` (priority 1),
  `com.android.tv.settings/.system.FallbackHome`.
- Saved Wi-Fi is owned by `android.uid.system`, **not** setupwraith (disabling setupwraith will not drop Wi-Fi).
- Remote: Bluetooth LE "TCL_RC902V_M6" (/dev/input/event3). Same family as the G03 remotes documented in
  `repo/examples/tcl-g03-google-tv/debloat.txt`.
- Live TV app `com.tcl.tv`, inputs from `com.tcl.tvinput` (tuner, HDMI 1-3, AV).

Already done
- Debloat applied from `repo/work/debloat.txt` (24 packages disabled, reboot-checked), based on
  `repo/examples/tcl-g03-google-tv`. Kept by owner's choice: Netflix, YouTube, Assistant, Chromecast,
  Miracast/T-Cast, USB player, TCL Live, Game bar, firmware updates, all owner-installed apps.
- Full backup in `repo/backup/`; one-step revert `repo/work/restore-tv.ps1` / `restore-tv.sh`
  (recovery record from before the first change, verified by dry run).
- Folder layout: `CleanYourTv/repo/` is a clean clone of github.com/Gathin23/cleanyourtv (MIT) with a
  Python venv in `repo/tools/venv`. `CleanYourTv/spikes/watchnext-probe/` is the throwaway probe below.

Spike result (Continue watching data, 2026-10-26)
- A test app holding `android.permission.READ_TV_LISTINGS` (granted with `pm grant`) read the TV
  provider without a selection: **12 Watch Next rows** (HBO Max `com.wbd.stream` 8, Netflix 2,
  iQIYI 1 without a poster, Spotify 1) and **168 preview programs** (Netflix incl. a promo item,
  discovery+, iQIYI, Spotify, TCL browser, `com.tcl.tv`). YouTube and Disney+ published nothing.
- Posters are `https://` URLs; they loaded over Wi-Fi with plain `HttpURLConnection`.
- On this build `READ_TV_LISTINGS` is `dangerous` (grantable by shell); `READ_EPG_DATA` is `normal`;
  `ACCESS_ALL_EPG_DATA` and `ACCESS_WATCHED_PROGRAMS` are `signature|privileged` (not available).
- Background reading: `repo/docs/watch-next.md`.

## 4. Decisions

Decided by the owner
- **Approach:** fork the CleanYourTV reference launcher (`repo/launcher`, package `com.gathin.tvhome`),
  keep its tested features, add a Continue watching row and the owner's own theme. (Chosen over "repo
  launcher + theme only", which cannot show posters, and over "from scratch".)
- **Front row (dock):** Disney+ `com.disney.disneyplus`, HBO Max `com.wbd.stream`, Netflix
  `com.netflix.ninja`, YouTube `com.google.android.youtube.tv`, Spotify `com.spotify.tv.android`.
- **Continue watching:** directly below the front row, not above it, and **one Down press away** rather
  than on screen at power-on ("just below the app row so the screen stays calmer until you scroll"). The
  dock keeps the reference launcher's position at the bottom of the screen; the Continue watching row
  scrolls up into view like the grid does, so this is a new row type, not a new layout mode.
- **Background:** pictures, specifically **Philippine landscapes and nature, really HD**. No personal
  photos. Coloured gradient only as a fallback.
- **Remote:** "as much as possible, everything in the remote should still work".
- **Style:** Apple TV-like: clean, minimal, beautiful.
- **Google TV's home (launcherx) stays installed and enabled, hidden behind the new launcher**, rather than
  disabled. Reason: on this firmware family disabling launcherx breaks Assistant voice results ("Google
  Assistant is having trouble processing your request", `repo/docs/TROUBLESHOOTING.md`), empties Settings >
  Accounts & profiles (#304) and the Google Home panel (#1653). The owner accepts a possible split-second
  flash of Google TV's home when pressing Home. If the flash proves bothersome on the TV, disabling launcherx
  later is a separate, reversible decision.
- **App name:** "Tahanan" (Filipino for "home").

Implementation calls (owner can override)
- Project lives in `CleanYourTv/tv-home/`: a full local clone of the CleanYourTV repo turned into the
  owner's own git project, with changes confined to `launcher/`, a new theme folder
  (`themes/philippines/`) and the owner's TV list. Keeping the whole repo means `tools/`,
  `launcher/build-and-install.sh`, restore and the safety checks keep working unchanged.
- Internal application ID stays `com.gathin.tvhome`: the repo's install, restore and recovery scripts
  key on it (`tools/recovery.py` `CUSTOM_LAUNCHER`). Only the visible app name changes.
- MIT licence and credit to the original author are kept (licence requirement).
- No greeting text above the clock (minimal). Round icon-only Wi-Fi and Settings buttons.
- Wallpapers rotate hourly, sequential order, with a dark shade over the lower part for readability.
- Place name shown small under the top-right buttons (`wallpaper_captions`); photographer credits in
  `CREDITS.md`, not on screen.
- Fallback background: dark blue-to-black gradient.
- Continue watching reads **only** the Watch Next table, never preview programs (that is where the
  promo items live).

## 5. What gets built

### 5.1 Home screen, top to bottom
1. Top bar: large thin clock and the date on the left; round frosted icons on the right: Inputs, Wi-Fi,
   Settings. Place caption of the current wallpaper under them, small and faint.
2. Open space over the landscape photo. When a front-row app is focused, its name in large light type
   with a soft glow in the app's colour (the reference launcher's behaviour). When a Continue watching
   poster is focused, that title (and season/episode when present) takes this place instead.
3. Front row: the five apps above in a frosted-glass band. Focus starts on Disney+ at power-on/Home.
   Focused tile scales up slightly with the glass highlight.
4. Continue watching row (5.2), directly below.
5. All other apps in the grid below, reached by pressing Down.

### 5.2 Continue watching row
- Source: `TvContract.WatchNextPrograms` via the TV provider, query without a selection, filter in code
  (a selection without `ACCESS_ALL_EPG_DATA` throws).
- Permission: `READ_TV_LISTINGS`, granted by the install step with `pm grant`; the launcher must also
  cope with it being absent (row hidden, no crash, no permission nag on Home).
- Order: newest `last_engagement_time_utc_millis` first. Hide items whose progress
  (`last_playback_position_millis / duration_millis`) is 95% or more, and items not engaged with in the
  last 60 days (thresholds are an implementation call). Cap the row at about 10 items.
- Hide rows from packages that are disabled or not installed, and from `hidden_apps`.
- Tile: landscape poster (16:9), title under it when focused; small app badge so the owner knows where
  it opens. Items without a poster (iQIYI today) get a gradient tile with the title.
- Press OK: open `intent_uri` (`Intent.parseUri`, `URI_INTENT_SCHEME`); if that fails, open the app.
- When the table has no usable rows, the row is not shown at all and the layout closes up.
- Refresh when Home comes to the front. No background polling or services.
- D-pad: Down from front row lands on Continue watching; Down again to the grid; Up reverses. Remember
  the last focused poster.

### 5.3 Posters and network
- Adds `android.permission.INTERNET`, used only to fetch poster images. No analytics, telemetry or
  other network calls.
- Decode at tile size (downsample), small in-memory LRU plus a disk cache in the app's cache dir, with
  a size cap. Never block the UI thread. A failed image leaves the gradient tile.

### 5.4 Wallpapers
- New theme `themes/philippines/` (`config.json`, `wallpapers/001.jpg...`, `captions.txt`,
  `CREDITS.md`, `screenshot.jpg` from the real TV).
- 10-12 photos: e.g. Palawan (El Nido / Coron), Banaue rice terraces, Chocolate Hills, Mayon, Siargao,
  Batanes, Taal, Boracay, Tubbataha/sea, Mt. Pulag sea of clouds. Variety of colour and time of day.
- Sources: Wikimedia Commons (or similar) under CC BY, CC BY-SA, CC0 or public domain only. Licence,
  author and source URL recorded per file in `CREDITS.md`.
- Quality bar: originals at least 3840 px wide (or clearly sharp at that size), well exposed, no
  watermarks, no people as the main subject. Each resized with a high-quality filter and cropped to
  exactly 1920x1080 JPEG; keep each file small enough that the launcher decodes it quickly on this TV
  (the reference theme's files are the yardstick).
- Theme keys (from `repo/themes/README.md`): `wallpaper: true`, `wallpaper_rotation_minutes: 60`,
  `wallpaper_order: sequential`, `wallpaper_captions: true`, `wallpaper_dim` tuned on the TV,
  `dock_glass: true`, `dock_opacity` tuned, `app_glow: true`, `icon_only_buttons: true`,
  `show_greeting: false`, `dock_apps` as above, `live_tv_package: com.tcl.tv`,
  `background_color` for the gradient fallback.

### 5.5 Remote buttons
- Map every button on the TCL_RC902V_M6: the owner presses each one while `getevent -lt` and logcat
  record it. Start from the G03 notes: gear is keycode 4077 (`TCL_SET`); Netflix, YouTube and Prime
  buttons are broadcasts only launcherx answered, restored with `app_keys`; Input opens TCL's source
  panel (`com.tcl.suspension`) on its own.
- Use the launcher's existing switches: `HOME_KEY_SERVICE=1` at install, `app_keys`,
  `settings_key_keycodes`, `settings_key_panel`. A Prime Video button maps to nothing useful (app
  disabled by owner) — leave it unmapped or map it to the owner's choice.
- Known trade-off (TCL G09): a key-filtering helper can stop held volume from ramping. Ask the owner to
  hold volume up before and after enabling it; if repeat breaks, report it and let the owner choose.
- Injected keys are not proof: the owner presses the real buttons.

### 5.6 Install and switch-over (safety)
Follow `repo/PROMPT.md` and `repo/AGENTS.md` rules, in this order:
1. Build the APK, push and install over ADB (`launcher/build-and-install.sh`; push+`pm install` if
   `adb install` stalls). Grant `READ_TV_LISTINGS`.
2. Push the theme (`tools/theme.sh`).
3. Make it Home (`cmd package set-home-activity`); confirm with
   `dumpsys activity activities | grep mFocusedApp` before going further.
4. **Keep launcherx enabled.** Hand Home to Tahanan with `stock_home_redirect:
   com.google.android.apps.tv.launcherx` and `HOME_KEY_SERVICE=1`, following the documented
   launcherx-stays-on setup in `repo/docs/TROUBLESHOOTING.md` (Sony VU3 case: `stock_home_redirect`,
   `key_filter: false`, HomeKeyService bound, which kept app buttons, held-volume ramp and the new Home).
   setupwraith stays enabled (its RecoveryActivity only takes Home when launcherx is off). dreamx stays
   enabled (it hosts the Google Home panel); switch the screensaver to the launcher's clock instead
   (`settings put secure screensaver_components com.gathin.tvhome/.ClockDream`) so no promo screensaver shows.
   Nothing new is disabled in this step, so the restore list does not grow; the undo for Home is
   `tools/restore.sh` (Home back to launcherx) plus resetting the screensaver component (record the
   original value first).
5. Reboot; confirm Home, Wi-Fi, ADB, HDMI/Live TV from Inputs, and the remote buttons with the real remote.
6. Check what opens at power-on (`logcat -b events -d | grep wm_set_resumed_activity`); ask Home vs Live TV.
7. At the very end: switch off USB/Network debugging, revoke authorisations, reboot, confirm port 5555 is
   closed (`tools/adb-closed.py`).

## 6. Constraints
- minSdk 26, targetSdk as the reference launcher (34); `<queries>` for LEANBACK_LAUNCHER kept;
  opaque window theme kept.
- 2 GB RAM, 32-bit: no blur, no video, no background services. Image memory bounded.
- Build on Windows 11 with the Android SDK at `%LOCALAPPDATA%\Android\Sdk` (build-tools 34/35/36,
  platforms 35/36 — platform 34 must be added with `sdkmanager`) and JDK 21 (the reference build asks for
  JDK 17; use 17 if Gradle refuses 21).
- **Windows write block:** some programs (PowerShell's New-Item, Android `d8`) get "Access is denied" writing
  under `D:\Users\Documents\...` while Git Bash and javac do not — likely Windows Controlled Folder
  Access. Gradle output may need to go outside Documents (e.g. the scratchpad or a `--project-cache-dir`
  / build dir elsewhere), or the owner allows the tools in Windows Security.
- Git Bash rewrites Unix paths: export `MSYS_NO_PATHCONV=1` for anything passing TV paths.
- Commits and docs carry no attribution trailers (owner's standing rule).
- Everything reversible: restore script must bring back Google TV Home.

## 7. Left to the builder
- Code structure of the Continue watching row inside the reference launcher (adapter, view type,
  focus handling), image cache implementation, exact tile sizes and animation values.
- Exact `wallpaper_dim`, `dock_opacity` and caption styling, tuned from screenshots on the TV.
- Photo selection within the brief in 5.4.
- Whether to add a launcher switch to turn the Continue watching row off (nice to have).

## 8. Open for the owner
- Whether to publish the fork on GitHub later.
- Asked during install, on the TV: Home vs Live TV at power-on; the volume-repeat trade-off if a key helper
  breaks it; whether the Google TV flash on Home is acceptable.
