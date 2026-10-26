package net.xion.mobile;

import android.net.Uri;
import android.util.Log;

import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Installs Xion's phone performance pack into Fabric/Quilt profiles (with Lite mode on).
 * Only mods that run on mobile renderers: no Sodium, which the engine's GL layers don't support.
 * Runs on the JVM launch thread, once per game folder and Minecraft version.
 */
public final class XionMods {
    /** Modrinth slugs: memory, CPU, culling and networking optimisations. */
    private static final String[] PACK = {
            "ferrite-core",     // much lower RAM use
            "modernfix",        // faster start, less memory
            "lithium",          // game logic / physics
            "entityculling",    // skip hidden entities
            "krypton",          // lighter networking
            "dynamic-fps",      // throttle when backgrounded, saves battery
            "badoptimizations", // many small CPU savings
    };
    private static final Pattern MODDED = Pattern.compile("^(fabric|quilt)-loader-[^-]+-(.+)$");

    private XionMods() {}

    public static void ensure(String versionId, File gameDir) {
        if (!XionLite.isEnabled() || versionId == null) return;
        Matcher m = MODDED.matcher(versionId);
        if (!m.matches()) return;
        String loader = m.group(1), game = m.group(2);
        File marker = new File(gameDir, ".xion-mods-" + game);
        if (marker.exists()) return;

        File mods = new File(gameDir, "mods");
        if (!mods.exists() && !mods.mkdirs()) return;
        Logger.appendToLog("[Xion] Installing phone performance pack for " + game + " (" + loader + ")");
        int ok = 0;
        for (String slug : PACK) {
            try {
                if (install(slug, loader, game, mods)) ok++;
            } catch (Exception e) {
                Logger.appendToLog("[Xion] Skipped " + slug + ": " + e.getMessage());
            }
        }
        Logger.appendToLog("[Xion] Performance pack: " + ok + "/" + PACK.length + " mods ready");
        try {
            //noinspection ResultOfMethodCallIgnored
            marker.createNewFile();
        } catch (Exception e) {
            Log.w(Tools.APP_NAME, "Xion: could not write mod marker", e);
        }
    }

    /** @return true when the mod is present after this call (already there or freshly downloaded). */
    private static boolean install(String slug, String loader, String game, File mods) throws Exception {
        String loaders = "quilt".equals(loader) ? "[\"quilt\",\"fabric\"]" : "[\"fabric\"]";
        String url = "https://api.modrinth.com/v2/project/" + slug + "/version"
                + "?loaders=" + Uri.encode(loaders)
                + "&game_versions=" + Uri.encode("[\"" + game + "\"]");
        JSONArray versions = new JSONArray(DownloadUtils.downloadString(url));
        if (versions.length() == 0) {
            Logger.appendToLog("[Xion] " + slug + " has no build for " + game);
            return false;
        }
        JSONArray files = versions.getJSONObject(0).getJSONArray("files");
        JSONObject file = files.getJSONObject(0);
        for (int i = 0; i < files.length(); i++) {
            if (files.getJSONObject(i).optBoolean("primary")) file = files.getJSONObject(i);
        }
        String name = file.getString("filename");
        File target = new File(mods, name);
        if (target.exists()) return true;
        // Replace an older copy of the same mod instead of loading two versions.
        File[] existing = mods.listFiles((dir, n) -> n.toLowerCase().startsWith(slug.split("-")[0]) && n.endsWith(".jar"));
        if (existing != null && existing.length > 0) return true;
        DownloadUtils.downloadFile(file.getString("url"), target);
        Logger.appendToLog("[Xion] Installed " + name);
        return true;
    }
}
