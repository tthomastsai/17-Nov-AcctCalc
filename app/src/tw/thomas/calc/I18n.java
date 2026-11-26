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
        put("theme_light", "淺色模式下的主題配色", "Colour theme in light mode");
        put("lang_system", "跟隨系統", "System default");
        put("lang_hint", "也可以在手機的「設定 › 應用程式 › 會計計算機 › 語言」更改。", "You can also change this in the phone's Settings › Apps › Accounting Calculator › Language.");
        put("display", "外觀", "Appearance");
        put("about", "關於", "About");
        put("version", "版本", "Version");
        put("about_basis", "介面與按鍵配置參考利百代 LB-5003CA 桌上型計算機。本 App 為個人作品，非官方產品，與利百代公司無關。",
            "The layout and keys are based on the LIBERTY LB-5003CA desk calculator. This is a personal project, not an official product, and is not affiliated with LIBERTY (利百代).");
        put("about_made", "由 tthomastsai 開發", "Made by tthomastsai");
        put("ack", "致謝", "Acknowledgements");
        put("ack_design", "設計參考：利百代 LB-5003CA 桌上型計算機及其功能說明卡", "Design reference: the LIBERTY LB-5003CA desk calculator and its function card");
        put("ack_gs", "Google Sans 字型：Copyright 2017 Google, Inc.，SIL Open Font License 1.1", "Google Sans font: Copyright 2017 Google, Inc., SIL Open Font License 1.1");
        put("ack_seven", "AcctCalc Seven 字型：本 App 自製，SIL Open Font License 1.1", "AcctCalc Seven font: made for this app, SIL Open Font License 1.1");
        put("ack_claude", "開發協作：Claude（Anthropic）", "Developed with Claude (Anthropic)");
        put("licences", "字型授權全文", "Font licences (full text)");
        put("about_font", "字型：Google Sans、AcctCalc Seven（本專案自製），皆為 SIL Open Font License 1.1", "Fonts: Google Sans and AcctCalc Seven (made for this app), both under the SIL Open Font License 1.1");
        put("lcd_font", "螢幕數字字體", "Display digits");
        put("font_seven", "七段顯示", "Seven-segment");
        put("about_web", "網頁版：iPhone、iPad 及電腦", "Web version: iPhone, iPad and computers");
        put("other_devices", "在其他裝置上使用", "Use it on other devices");
        put("other_devices_note", "用瀏覽器開啟後，可以加到主畫面，像 App 一樣全螢幕使用，離線也能用。", "Open it in a browser and add it to the Home Screen to use it full screen like an app, offline too.");
        put("back", "返回", "Back");
        put("tour_replay", "重看導覽", "Replay the tour");
        put("tour_skip", "跳過", "Skip");
        put("tour_next", "下一步", "Next");
        put("tour_start", "開始使用", "Get started");
        put("tour_more", "更多操作技巧", "More tips");
        put("tour1_t", "歡迎使用會計計算機", "Welcome to Accounting Calculator");
        put("tour1_b", "按鍵配置和運算規則都照 12 位數的桌上型商用計算機設計，以下帶你看幾個重點。",
            "The keys and calculation rules follow a real 12-digit desk calculator. Here are a few things worth knowing.");
        put("tour2_t", "螢幕指示", "Display indicators");
        put("tour2_b", "MINUS、MEMORY、ERROR、GRAND TOTAL 作用中時，下方的橫條會亮起；右上角的 + − × ÷ 會亮起目前的運算。",
            "A bar lights up under MINUS, MEMORY, ERROR or GRAND TOTAL when it is active; in the top-right corner, the pending one of + − × ÷ lights up.");
        put("tour3_t", "GT 總計", "GT (grand total)");
        put("tour3_b", "每次按 = 或 % 算出的結果都會累加。按一下叫出，連按第二下清空。",
            "Every result from = or % is added up. Press once to recall it; press again to clear it.");
        put("tour4_t", "常數計算", "Constant calculation");
        put("tour4_b", "算完一次後，輸入新數字再按 =，會重複同樣的運算。例如 54 × 5 = 之後按 2 =，會得到 108。",
            "After a calculation, enter a new number and press = to repeat it. For example, after 54 × 5 =, pressing 2 = gives 108.");
        put("tour5_t", "清除", "Clearing");
        put("tour5_b", "ON/C·CE 按一下清除剛輸入的數，按第二下清除算式；AC 會連 M 和 GT 一起清空。",
            "ON/C·CE clears the number just entered, and the calculation on a second press. AC clears everything, including M and GT.");
        put("tour6_t", "設定", "Settings");
        put("tour6_b", "主題配色、深色模式、按鍵高度、機身位置和語言都在這裡，也可以重看這份導覽。",
            "Colour theme, dark mode, key height, position and language are all here, and you can replay this tour.");
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
