package net.xion.mobile;

import net.kdt.pojavlaunch.modloaders.FabricVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Xion Mobile only offers the versions it is tuned and tested for: Fabric on 1.21 to 1.21.x and 26.1 to 26.3.
 * Older versions run worse on phones and newer ones haven't been checked with the mobile renderers yet.
 */
public final class XionVersions {
    private static final Pattern RELEASE = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?$");

    private XionVersions() {}

    /** True for a release Xion Mobile supports (1.21, 1.21.x, 26.1 to 26.3.x); snapshots never count. */
    public static boolean supported(String gameVersion) {
        if (gameVersion == null) return false;
        Matcher m = RELEASE.matcher(gameVersion);
        if (!m.matches()) return false;
        int major = Integer.parseInt(m.group(1)), minor = Integer.parseInt(m.group(2));
        if (major == 1) return minor == 21;
        return major == 26 && minor >= 1 && minor <= 3;
    }

    /** Keeps only supported game versions from Fabric's list (null stays null). */
    public static FabricVersion[] filter(FabricVersion[] versions) {
        if (versions == null) return null;
        List<FabricVersion> out = new ArrayList<>();
        for (FabricVersion v : versions) if (supported(v.version)) out.add(v);
        return out.toArray(new FabricVersion[0]);
    }
}
