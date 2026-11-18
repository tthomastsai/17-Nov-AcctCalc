package tw.thomas.calc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/** Colour sets. Each key type has: face top, face bottom, side (thickness), label. */
public final class Theme {
    public final String id, name;
    public final boolean dark;
    public final int bg, label, sub, bezelTop, bezelBot, bezelLine, frame;
    public final int[] num, fn, acc;   // {faceTop, faceBot, side, text}

    private Theme(String id, String name, boolean dark, String bg, String label, String sub,
                  String bezelTop, String bezelBot, String bezelLine, String frame,
                  String[] num, String[] fn, String[] acc) {
        this.id = id; this.name = name; this.dark = dark;
        this.bg = c(bg); this.label = c(label); this.sub = c(sub);
        this.bezelTop = c(bezelTop); this.bezelBot = c(bezelBot); this.bezelLine = c(bezelLine); this.frame = c(frame);
        this.num = cs(num); this.fn = cs(fn); this.acc = cs(acc);
    }
    private static int c(String s) { return Color.parseColor(s); }
    private static int[] cs(String[] a) { int[] r = new int[a.length]; for (int i = 0; i < a.length; i++) r[i] = c(a[i]); return r; }
    private static String[] k(String... a) { return a; }

    public static final Theme[] ALL = {
        new Theme("cream", "經典奶油", false, "#F5F2ED", "#3D5A8C", "#7A8088", "#FBF9F5", "#ECE8E2", "#E0DCD5", "#5D666E",
            k("#979B9F", "#888C90", "#63676B", "#FFFFFF"),
            k("#FFFFFF", "#F2F0EC", "#C4C1BC", "#575C62"),
            k("#FCCD12", "#F2C000", "#C79C00", "#4F4210")),
        new Theme("obsidian", "曜石黑", true, "#1E1F22", "#8FA8D6", "#8A8F96", "#2C2E32", "#232528", "#36383D", "#0F1113",
            k("#4B4E53", "#404348", "#2A2C30", "#F2F2F2"),
            k("#6C7076", "#61656B", "#45484D", "#F5F5F5"),
            k("#FFA51F", "#F28F00", "#B86A00", "#2B1A00")),
        new Theme("mint", "薄荷綠", false, "#EEF4F0", "#2F6B5A", "#6F8A80", "#F7FBF8", "#E6EEE9", "#D6E2DB", "#4E6660",
            k("#82AB9D", "#719B8D", "#4E7568", "#FFFFFF"),
            k("#FFFFFF", "#F0F5F2", "#C2D1C9", "#4D6159"),
            k("#FF9178", "#F57A5E", "#C95A42", "#FFFFFF")),
        new Theme("sakura", "櫻花粉", false, "#F8EEF0", "#A2475E", "#8E7A80", "#FDF7F8", "#F1E4E7", "#E6D3D8", "#6E5A61",
            k("#CB8F9E", "#BC7E8D", "#96606D", "#FFFFFF"),
            k("#FFFFFF", "#F7EFF1", "#D8C3C9", "#6A5560"),
            k("#FAD764", "#EFC541", "#C49B22", "#5A4510")),
        new Theme("sky", "天空藍", false, "#EEF2F7", "#2F5D9E", "#77808C", "#F8FAFC", "#E6ECF3", "#D7DFE9", "#56657A",
            k("#8098B5", "#6E86A4", "#4F6580", "#FFFFFF"),
            k("#FFFFFF", "#F1F4F8", "#C3CCD8", "#4F5C6E"),
            k("#FFCC45", "#F5B921", "#C79413", "#4F3D08")),
    };

    // ---------------------------------------------------------------- shared helpers
    public static boolean autoDark(SharedPreferences p) { return p.getBoolean("autoDark", true); }

    public static boolean systemDark(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public static Theme chosen(SharedPreferences p) { return byId(p.getString("theme", "cream")); }

    /** the theme on screen: Obsidian while the system is in dark mode (if enabled), otherwise the chosen one */
    public static Theme current(Context c, SharedPreferences p) {
        return autoDark(p) && systemDark(c) ? byId("obsidian") : chosen(p);
    }

    /** colour the status and navigation bars to match */
    public static void applyBars(Activity a, Theme t) {
        Window w = a.getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(t.bg);
        w.setNavigationBarColor(t.bg);
        w.getDecorView().setBackgroundColor(t.bg);
        int flags = 0;
        if (!t.dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        w.getDecorView().setSystemUiVisibility(flags);
    }

    public static Theme byId(String id) {
        for (Theme t : ALL) if (t.id.equals(id)) return t;
        return ALL[0];
    }
}
