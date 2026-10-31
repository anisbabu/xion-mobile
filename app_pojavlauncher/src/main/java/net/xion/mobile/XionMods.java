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
 * Installs Xion's phone pack into Fabric profiles of supported versions (with Lite mode on).
 * Only mods that run on mobile renderers: no Sodium (unsupported by the GL layers, it crashes) and nothing that
 * needs a keyboard key to be useful. Runs on the JVM launch thread, once per game folder and Minecraft version.
 * Only release builds are used, so an alpha or beta mod never reaches a phone.
 */
public final class XionMods {
    /** Modrinth slugs. Mods without a release for the game version are skipped. */
    private static final String[] PACK = {
            "fabric-api",           // library most Fabric mods need
            // Performance
            "ferrite-core",         // much lower RAM use
            "modernfix",            // faster start, less memory
            "lithium",              // game logic / physics
            "entityculling",        // skip hidden entities
            "moreculling",          // skip hidden block faces, leaves and item frames
            "entity-view-distance", // don't draw far-away mobs and items
            "krypton",              // lighter networking
            "dynamic-fps",          // throttle when backgrounded, saves battery
            "badoptimizations",     // many small CPU savings
            "scalablelux",          // lighting on spare CPU cores
            "clumps",               // merges XP orbs, fewer lag spikes
            "noisium",              // faster world generation
            "ksyxis",               // worlds load faster
            "fastquit",             // leave worlds instantly, save in background
            "lazy-language-loader", // faster start
            "fast-ip-ping",         // server list pings instantly
            // Features
            "appleskin",            // hunger and saturation on the HUD
            "xaeros-minimap",       // minimap with waypoints
            "modmenu",              // browse and configure mods
    };
    private static final Pattern MODDED = Pattern.compile("^(fabric|quilt)-loader-[^-]+-(.+)$");

    private XionMods() {}

    public static void ensure(String versionId, File gameDir) {
        if (!XionLite.isEnabled() || versionId == null) return;
        Matcher m = MODDED.matcher(versionId);
        if (!m.matches()) return;
        String loader = m.group(1), game = m.group(2);
        if (!XionVersions.supported(game)) {
            Logger.appendToLog("[Xion] " + game + " is not a Xion Mobile version (1.21.x, 26.1 to 26.3); no phone pack installed");
            return;
        }
        File marker = new File(gameDir, ".xion-mods-v3-" + game);
        if (marker.exists()) return;

        File mods = new File(gameDir, "mods");
        if (!mods.exists() && !mods.mkdirs()) return;
        Logger.appendToLog("[Xion] Installing phone performance pack for " + game + " (" + loader + ")");
        int ok = 0;
        java.util.Set<String> done = new java.util.HashSet<>();
        for (String slug : PACK) {
            try {
                if (install(slug, loader, game, mods, done)) ok++;
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
    private static boolean install(String slug, String loader, String game, File mods, java.util.Set<String> done) throws Exception {
        if (!done.add(slug)) return !done.contains("!" + slug);
        boolean ok = false;
        try {
            ok = installOnce(slug, loader, game, mods, done);
            return ok;
        } finally {
            if (!ok) done.add("!" + slug);
        }
    }

    private static boolean installOnce(String slug, String loader, String game, File mods, java.util.Set<String> done) throws Exception {
        String loaders = "quilt".equals(loader) ? "[\"quilt\",\"fabric\"]" : "[\"fabric\"]";
        String url = "https://api.modrinth.com/v2/project/" + slug + "/version"
                + "?loaders=" + Uri.encode(loaders)
                + "&game_versions=" + Uri.encode("[\"" + game + "\"]");
        JSONArray versions = new JSONArray(DownloadUtils.downloadString(url));
        if (versions.length() == 0) {
            Logger.appendToLog("[Xion] " + slug + " has no build for " + game);
            return false;
        }
        JSONObject version = null;
        for (int i = 0; i < versions.length() && version == null; i++) {
            if ("release".equals(versions.getJSONObject(i).optString("version_type"))) version = versions.getJSONObject(i);
        }
        if (version == null) {
            Logger.appendToLog("[Xion] " + slug + " has no stable build for " + game + " yet");
            return false;
        }
        // Pull in required libraries first (by Modrinth project id), so the game never starts with missing dependencies.
        JSONArray deps = version.optJSONArray("dependencies");
        if (deps != null) {
            for (int i = 0; i < deps.length(); i++) {
                JSONObject d = deps.getJSONObject(i);
                String pid = d.optString("project_id", null);
                if ("required".equals(d.optString("dependency_type")) && pid != null && !pid.equals("null")) {
                    boolean depOk;
                    try { depOk = install(pid, loader, game, mods, done); }
                    catch (Exception e) { depOk = false; Logger.appendToLog("[Xion] Could not add dependency " + pid + ": " + e.getMessage()); }
                    // A mod without its library would crash the game, so leave it out.
                    if (!depOk) {
                        Logger.appendToLog("[Xion] Skipped " + slug + ": a library it needs isn't available");
                        return false;
                    }
                }
            }
        }
        JSONArray files = version.getJSONArray("files");
        JSONObject file = files.getJSONObject(0);
        for (int i = 0; i < files.length(); i++) {
            if (files.getJSONObject(i).optBoolean("primary")) file = files.getJSONObject(i);
        }
        String name = file.getString("filename");
        File target = new File(mods, name);
        if (target.exists()) return true;
        DownloadUtils.downloadFile(file.getString("url"), target);
        Logger.appendToLog("[Xion] Installed " + name);
        return true;
    }
}
