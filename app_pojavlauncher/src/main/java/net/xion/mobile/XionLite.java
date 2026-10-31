package net.xion.mobile;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.MCOptionUtils;

import java.io.File;
import java.io.IOException;

/**
 * "Lite mode": phone video settings, on by default. The phone is sorted into a tier (low, mid, high) from its RAM
 * and CPU cores; the tier picks the render distance and the render resolution. Video settings are applied once per
 * game folder (marked with .xion-lite-v2), so players can still change them in game afterwards.
 */
public final class XionLite {
    public static final String PREF_KEY = "xion_lite";
    private static final String MARKER = ".xion-lite-v2";
    private static final String PREF_RES_DONE = "xion_res_v1";
    private static Context appContext;

    public enum Tier { LOW, MID, HIGH }

    /** options.txt keys for every tier (versions that don't know a key ignore it). */
    private static final String[][] COMMON = {
            {"graphicsMode", "0"},           // Fast
            {"renderClouds", "\"false\""},
            {"entityShadows", "false"},
            {"ao", "false"},                 // Smooth lighting off
            {"biomeBlendRadius", "0"},
            {"prioritizeChunkUpdates", "0"},
            {"mipmapLevels", "0"},
            {"maxFps", "120"},
            {"enableVsync", "false"},
            {"bobView", "false"},
            {"showAutosaveIndicator", "false"},
            {"menuBackgroundBlurriness", "0"}, // the 1.20.5+ menu blur is very slow on phone GPUs
            {"glintSpeed", "0.3"},
            {"darknessEffectScale", "0.5"},
    };

    private XionLite() {}

    public static boolean isEnabled() {
        return LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_KEY, true);
    }

    public static void setEnabled(boolean enabled) {
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF_KEY, enabled).apply();
    }

    /** Sorts the phone by RAM and CPU cores. */
    public static Tier tier(Context ctx) {
        int ram = Tools.getTotalDeviceMemory(ctx);
        int cores = Runtime.getRuntime().availableProcessors();
        if (ram <= 4096 || cores <= 4) return Tier.LOW;
        if (ram >= 7168 && cores >= 8) return Tier.HIGH;
        return Tier.MID;
    }

    private static String[][] forTier(Tier t) {
        switch (t) {
            case HIGH: return new String[][]{{"renderDistance", "8"}, {"simulationDistance", "6"}, {"entityDistanceScaling", "1.0"}, {"particles", "1"}};
            case MID: return new String[][]{{"renderDistance", "6"}, {"simulationDistance", "5"}, {"entityDistanceScaling", "0.75"}, {"particles", "2"}};
            default: return new String[][]{{"renderDistance", "5"}, {"simulationDistance", "5"}, {"entityDistanceScaling", "0.5"}, {"particles", "2"}};
        }
    }

    /**
     * First start with Lite mode: lowers the render resolution for the tier (the biggest FPS win on phones).
     * Runs before the launcher reads its preferences and never overrides a value the player picked.
     */
    public static void tuneLauncher(Context ctx) {
        appContext = ctx.getApplicationContext();
        if (!isEnabled() || LauncherPreferences.DEFAULT_PREF == null) return;
        if (LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_RES_DONE, false)) return;
        if (!LauncherPreferences.DEFAULT_PREF.contains("resolutionRatio")) {
            Tier t = tier(ctx);
            int target = t == Tier.HIGH ? 720 : t == Tier.MID ? 600 : 480;
            DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
            int minSide = Math.min(metrics.widthPixels, metrics.heightPixels);
            int ratio = 100;
            if (minSide > target) {
                int inc = ctx.getResources().getInteger(R.integer.resolution_seekbar_increment);
                ratio = (int) (Math.ceil(100f * target / minSide / inc) * inc);
            }
            LauncherPreferences.DEFAULT_PREF.edit().putInt("resolutionRatio", Math.max(25, ratio)).apply();
            Log.i(Tools.APP_NAME, "Xion Lite: " + t + " phone, render resolution " + ratio + "%");
        }
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF_RES_DONE, true).apply();
    }

    /** Called right before a launch: tunes a game folder the first time it is played with Lite mode on. */
    public static void applyIfNeeded(File gameDir) {
        if (!isEnabled()) return;
        if (new File(gameDir, MARKER).exists()) return;
        apply(gameDir);
    }

    /** Writes the tier's settings into the folder's options.txt, keeping every other option. */
    public static void apply(File gameDir) {
        try {
            if (!gameDir.exists() && !gameDir.mkdirs()) return;
            Tier t = appContext == null ? Tier.LOW : tier(appContext);
            MCOptionUtils.load(gameDir.getAbsolutePath());
            for (String[] kv : COMMON) MCOptionUtils.set(kv[0], kv[1]);
            for (String[] kv : forTier(t)) MCOptionUtils.set(kv[0], kv[1]);
            MCOptionUtils.save();
            //noinspection ResultOfMethodCallIgnored
            new File(gameDir, MARKER).createNewFile();
        } catch (IOException | RuntimeException e) {
            Log.w(Tools.APP_NAME, "Xion Lite: could not tune " + gameDir, e);
        }
    }
}
