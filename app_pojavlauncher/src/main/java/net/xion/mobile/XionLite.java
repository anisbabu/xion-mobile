package net.xion.mobile;

import android.util.Log;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.MCOptionUtils;

import java.io.File;
import java.io.IOException;

/**
 * "Lite mode": low-end video settings for phones. On by default. Applied once per game folder
 * (marked with .xion-lite) so players can still change their settings in game afterwards.
 */
public final class XionLite {
    public static final String PREF_KEY = "xion_lite";
    private static final String MARKER = ".xion-lite";

    /** options.txt keys understood by 1.20.1 and 1.21.x (older versions ignore unknown keys). */
    private static final String[][] LOW_END = {
            {"renderDistance", "6"},
            {"simulationDistance", "5"},
            {"graphicsMode", "0"},           // Fast
            {"renderClouds", "\"false\""},
            {"particles", "2"},              // Minimal
            {"entityShadows", "false"},
            {"ao", "false"},                 // Smooth lighting off
            {"biomeBlendRadius", "0"},
            {"entityDistanceScaling", "0.6"},
            {"prioritizeChunkUpdates", "0"},
            {"mipmapLevels", "0"},
            {"maxFps", "60"},
            {"enableVsync", "false"},
            {"bobView", "false"},
            {"showAutosaveIndicator", "false"},
    };

    private XionLite() {}

    public static boolean isEnabled() {
        return LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_KEY, true);
    }

    public static void setEnabled(boolean enabled) {
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean(PREF_KEY, enabled).apply();
    }

    /** Called right before a launch: tunes a game folder the first time it is played with Lite mode on. */
    public static void applyIfNeeded(File gameDir) {
        if (!isEnabled()) return;
        if (new File(gameDir, MARKER).exists()) return;
        apply(gameDir);
    }

    /** Writes the low-end settings into the folder's options.txt, keeping every other option. */
    public static void apply(File gameDir) {
        try {
            if (!gameDir.exists() && !gameDir.mkdirs()) return;
            MCOptionUtils.load(gameDir.getAbsolutePath());
            for (String[] kv : LOW_END) MCOptionUtils.set(kv[0], kv[1]);
            MCOptionUtils.save();
            //noinspection ResultOfMethodCallIgnored
            new File(gameDir, MARKER).createNewFile();
        } catch (IOException | RuntimeException e) {
            Log.w(Tools.APP_NAME, "Xion Lite: could not tune " + gameDir, e);
        }
    }
}
