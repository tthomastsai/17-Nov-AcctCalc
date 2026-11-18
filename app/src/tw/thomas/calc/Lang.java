package tw.thomas.calc;

import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * App language. On Android 13+ it is the per-app language, so it can also be changed in
 * Settings › Apps › Accounting Calculator › Language; older Android keeps it in our own prefs.
 * Modes: "system", "zh", "en-GB".
 */
public final class Lang {
    private Lang() {}

    private static final String PREF = "lang";
    private static boolean perApp() { return Build.VERSION.SDK_INT >= 33; }

    private static String fromLocale(Locale l) { return l != null && "zh".equals(l.getLanguage()) ? "zh" : "en-GB"; }

    /** the selected mode, including "system" */
    public static String mode(Context c, SharedPreferences p) {
        if (perApp()) {
            LocaleList l = c.getSystemService(LocaleManager.class).getApplicationLocales();
            return l.isEmpty() ? "system" : fromLocale(l.get(0));
        }
        return p.getString(PREF, "system");
    }

    /** the language actually in use: "zh" or "en-GB" */
    public static String effective(Context c, SharedPreferences p) {
        String m = mode(c, p);
        if (!m.equals("system")) return m;
        return fromLocale(c.getResources().getConfiguration().getLocales().get(0));
    }

    public static void set(Context c, SharedPreferences p, String mode) {
        if (perApp()) {
            LocaleList l = mode.equals("system") ? LocaleList.getEmptyLocaleList()
                    : LocaleList.forLanguageTags(mode.equals("zh") ? "zh-TW" : "en-GB");
            c.getSystemService(LocaleManager.class).setApplicationLocales(l);   // Android re-applies the configuration
        } else {
            p.edit().putString(PREF, mode).apply();
        }
    }

    /** v2.12 and earlier kept an explicit choice in prefs; hand it over to the system once */
    public static void migrate(Context c, SharedPreferences p) {
        if (!perApp() || !p.contains(PREF)) return;
        String old = p.getString(PREF, "system");
        p.edit().remove(PREF).apply();
        if (!old.equals("system") && mode(c, p).equals("system")) set(c, p, old);
    }
}
