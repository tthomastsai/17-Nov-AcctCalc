package tw.thomas.calc;

import java.util.HashMap;
import java.util.Locale;

/** In-app strings: Traditional Chinese (Taiwan) and English (UK). */
public final class I18n {
    public static String lang = "zh";   // "zh" or "en-GB"

    private static final HashMap<String, String[]> S = new HashMap<>();
    private static void put(String k, String zh, String en) { S.put(k, new String[]{zh, en}); }
    static {
        put("settings", "設定", "Settings");
        put("language", "語言", "Language");
        put("position", "機身位置", "Position");
        put("pos_top", "偏上", "Top");
        put("pos_centre", "置中", "Centre");
        put("pos_bottom", "偏下", "Bottom");
        put("key_height", "按鍵高度", "Key height");
        put("theme", "主題配色", "Colour theme");
        put("done", "完成", "Done");
        put("auto_dark", "深色模式時自動切換為曜石黑", "Switch to Obsidian in dark mode");
        put("auto_dark_on", "目前是深色模式，正在使用曜石黑", "Dark mode is on, so Obsidian is in use");
        put("cream", "經典奶油", "Classic Cream");
        put("obsidian", "曜石黑", "Obsidian");
        put("mint", "薄荷綠", "Mint");
        put("sakura", "櫻花粉", "Sakura Pink");
        put("sky", "天空藍", "Sky Blue");
    }

    public static String t(String key) {
        String[] v = S.get(key);
        if (v == null) return key;
        return lang.equals("zh") ? v[0] : v[1];
    }

    public static String systemDefault() {
        return Locale.getDefault().getLanguage().equals("zh") ? "zh" : "en-GB";
    }
}
