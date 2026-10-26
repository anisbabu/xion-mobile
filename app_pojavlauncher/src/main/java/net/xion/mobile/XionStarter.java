package net.xion.mobile;

import android.app.Activity;
import android.util.Log;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;

/**
 * First run: creates a ready-to-play "Xion 1.21.1" Fabric profile and selects it, so Lite mode and
 * the phone performance pack apply from the very first launch. Retries on the next start if offline.
 */
public final class XionStarter {
    public static final String GAME_VERSION = "1.21.1";
    private static final String PREF_DONE = "xion_starter_v2";

    private XionStarter() {}

    public static void runIfNeeded(Activity activity) {
        if (LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_DONE, false)) return;
        new Thread(() -> {
            try {
                String key = createProfile();
                LauncherPreferences.DEFAULT_PREF.edit()
                        .putBoolean(PREF_DONE, true)
                        .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key)
                        .apply();
                activity.runOnUiThread(() -> selectInSpinner(activity, key));
            } catch (Exception e) {
                Log.w(Tools.APP_NAME, "Xion starter profile not created yet (offline?)", e);
            }
        }, "Xion starter").start();
    }

    /** @return the key of the Xion profile (existing or newly created). */
    private static String createProfile() throws Exception {
        FabriclikeUtils fabric = FabriclikeUtils.FABRIC_UTILS;
        LauncherProfiles.load();
        retuneOldDefault();
        String existing = findXionProfile();
        if (existing != null) return existing;
        // Newest stable Fabric loader for the tuned game version.
        JSONArray loaders = new JSONArray(DownloadUtils.downloadString(
                "https://meta.fabricmc.net/v2/versions/loader/" + GAME_VERSION));
        String loader = null;
        for (int i = 0; i < loaders.length() && loader == null; i++) {
            JSONObject l = loaders.getJSONObject(i).getJSONObject("loader");
            if (l.optBoolean("stable")) loader = l.getString("version");
        }
        if (loader == null) loader = loaders.getJSONObject(0).getJSONObject("loader").getString("version");

        String json = DownloadUtils.downloadString(fabric.createJsonDownloadUrl(GAME_VERSION, loader));
        String versionId = new JSONObject(json).getString("id");
        File dir = new File(Tools.DIR_HOME_VERSION, versionId);
        FileUtils.ensureDirectory(dir);
        Tools.write(new File(dir, versionId + ".json").getAbsolutePath(), json);

        LauncherProfiles.load();
        MinecraftProfile profile = new MinecraftProfile();
        profile.name = "Xion " + GAME_VERSION;
        profile.lastVersionId = versionId;
        profile.icon = fabric.getIconName();
        LauncherProfiles.insertMinecraftProfile(profile);
        LauncherProfiles.write();
        // The store may normalise keys, so look the profile up again by its contents.
        LauncherProfiles.load();
        return findXionProfile();
    }

    /** Key of a Fabric profile for GAME_VERSION created by Xion, or null. */
    private static String findXionProfile() {
        for (Map.Entry<String, MinecraftProfile> e : LauncherProfiles.mainProfileJson.profiles.entrySet()) {
            MinecraftProfile p = e.getValue();
            if (p.lastVersionId != null && p.lastVersionId.startsWith("fabric-loader-")
                    && p.lastVersionId.endsWith("-" + GAME_VERSION)) return e.getKey();
        }
        return null;
    }

    /** Upstream's starter profile points at 1.7.10; move it to the tuned version. */
    private static void retuneOldDefault() {
        boolean changed = false;
        for (MinecraftProfile p : LauncherProfiles.mainProfileJson.profiles.values()) {
            if ("1.7.10".equals(p.lastVersionId) && p.name != null && p.name.contains("Default")) {
                p.lastVersionId = GAME_VERSION;
                p.name = "Vanilla " + GAME_VERSION;
                changed = true;
            }
        }
        if (changed) LauncherProfiles.write();
    }

    private static void selectInSpinner(Activity activity, String key) {
        if (activity.isFinishing()) return;
        mcVersionSpinner spinner = activity.findViewById(R.id.mc_version_spinner);
        if (spinner == null) return;
        spinner.reloadProfiles();
        int index = spinner.getProfileAdapter().resolveProfileIndex(key);
        if (index >= 0) spinner.setProfileSelection(index);
    }
}
