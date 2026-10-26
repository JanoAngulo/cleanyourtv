package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Posters for the Continue watching row. Most are https URLs (docs/watch-next.md), so this is the launcher's only
 * network use: fetched on two worker threads, kept small in memory and on disk, decoded at tile size.
 */
final class PosterCache {
    private static final int CONNECT_MS = 5000, READ_MS = 8000;
    /** Disk cache cap; the oldest files go first. */
    private static final long DISK_MAX_BYTES = 20L * 1024 * 1024;
    /** A poster larger than this is not a poster; stop reading. */
    private static final int MAX_DOWNLOAD_BYTES = 4 * 1024 * 1024;

    private final Context ctx;
    private final File dir;
    private final LruCache<String, Bitmap> memory;
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());

    PosterCache(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.dir = new File(this.ctx.getCacheDir(), "posters");
        int maxBytes = (int) Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory() / 16);
        this.memory = new LruCache<String, Bitmap>(maxBytes) {
            @Override protected int sizeOf(String key, Bitmap b) {
                return b.getByteCount();
            }
        };
    }

    /**
     * Shows the poster in {@code into} once loaded. The view must carry {@code uri} as its R.id.poster_uri tag;
     * a recycled view that now waits for another poster is left alone. A failed load leaves the view as it is.
     */
    void load(String uri, int reqW, int reqH, ImageView into) {
        if (uri == null || uri.isEmpty()) return;
        String key = uri + "@" + reqW + "x" + reqH;
        Bitmap hit = memory.get(key);
        if (hit != null) {
            into.setImageBitmap(hit);
            return;
        }
        pool.execute(() -> {
            Bitmap b = fetch(uri, reqW, reqH);
            if (b == null) return;
            memory.put(key, b);
            main.post(() -> {
                if (uri.equals(into.getTag(R.id.poster_uri))) into.setImageBitmap(b);
            });
        });
    }

    /** Drops the decoded posters; the disk copies stay. */
    void trim() {
        memory.evictAll();
    }

    private Bitmap fetch(String uri, int reqW, int reqH) {
        try {
            File f = new File(dir, diskKey(uri) + ".img");
            byte[] data;
            if (f.isFile()) {
                data = Files.readAllBytes(f.toPath());
                f.setLastModified(System.currentTimeMillis());
            } else {
                data = download(uri);
                if (data == null) return null;
                store(f, data);
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            o.inJustDecodeBounds = false;
            o.inSampleSize = sampleSize(o.outWidth, o.outHeight, reqW, reqH);
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(data, 0, data.length, o);
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    private byte[] download(String uri) throws IOException {
        InputStream in;
        HttpURLConnection con = null;
        if (uri.startsWith("https://") || uri.startsWith("http://")) {
            con = (HttpURLConnection) new URL(uri).openConnection();
            con.setConnectTimeout(CONNECT_MS);
            con.setReadTimeout(READ_MS);
            if (con.getResponseCode() / 100 != 2) {
                con.disconnect();
                return null;
            }
            in = con.getInputStream();
        } else if (uri.startsWith("content://") || uri.startsWith("android.resource://")) {
            in = ctx.getContentResolver().openInputStream(Uri.parse(uri));
            if (in == null) return null;
        } else {
            return null;
        }
        try (InputStream s = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = s.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_DOWNLOAD_BYTES) return null;
            }
            return out.toByteArray();
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private void store(File f, byte[] data) {
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        } catch (IOException e) {
            f.delete();
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) return;
        long total = 0;
        for (File x : files) total += x.length();
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < files.length && total > DISK_MAX_BYTES; i++) {
            total -= files[i].length();
            files[i].delete();
        }
    }

    /** Largest power of two that keeps both sides at least the requested size; never below 1. */
    static int sampleSize(int srcW, int srcH, int reqW, int reqH) {
        int sample = 1;
        if (reqW <= 0 || reqH <= 0) return sample;
        while (srcW / (sample * 2) >= reqW && srcH / (sample * 2) >= reqH) sample *= 2;
        return sample;
    }

    /** File name for a poster URI: lowercase hex SHA-1, so any URL maps to a safe name. */
    static String diskKey(String uri) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(uri.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(40);
            for (byte b : d) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
