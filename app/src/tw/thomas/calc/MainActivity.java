package tw.thomas.calc;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity implements CalcView.Host {
    private SharedPreferences prefs;
    private CalcEngine eng;
    private CalcView view;
    private Typeface t500, t700;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("calc", MODE_PRIVATE);
        eng = new CalcEngine();
        eng.mem = parse(prefs.getString("mem", "0"));
        eng.gt = parse(prefs.getString("gt", "0"));
        eng.setListener(new CalcEngine.Listener() {
            @Override public void onRegistersChanged(double mem, double gt) {
                prefs.edit().putString("mem", Double.toString(mem)).putString("gt", Double.toString(gt)).apply();
            }
        });
        try {
            t500 = Typeface.createFromAsset(getAssets(), "fonts/GoogleSans-500.ttf");
            t700 = Typeface.createFromAsset(getAssets(), "fonts/GoogleSans-700.ttf");
        } catch (Exception e) { t500 = Typeface.DEFAULT; t700 = Typeface.DEFAULT_BOLD; }

        view = new CalcView(this, eng);
        view.setHost(this);
        view.setKeyScale(prefs.getFloat("keyScale", 1f));
        view.setAlign(prefs.getFloat("align", DEFAULT_ALIGN));
        I18n.lang = prefs.getString("lang", I18n.systemDefault());
        setContentView(view);
        applyTheme();
    }

    static final float DEFAULT_ALIGN = 1f;   // bottom

    // ================================================================ theme / dark mode
    private boolean autoDark() { return prefs.getBoolean("autoDark", true); }

    private boolean systemDark() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /** the theme on screen: Obsidian while the system is in dark mode (if enabled), otherwise the chosen one */
    private void applyTheme() {
        Theme chosen = Theme.byId(prefs.getString("theme", "cream"));
        Theme t = autoDark() && systemDark() ? Theme.byId("obsidian") : chosen;
        view.setThemeColors(t);
        applySystemBars(t);
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applyTheme();                                       // dark mode switched while the app is open
        if (dialog != null && dialog.isShowing()) buildSettings();
    }

    private static double parse(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }

    private void applySystemBars(Theme t) {
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(t.bg);
        w.setNavigationBarColor(t.bg);
        w.getDecorView().setBackgroundColor(t.bg);
        int flags = 0;
        if (!t.dark) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= 0x00000010; // LIGHT_NAVIGATION_BAR
        }
        w.getDecorView().setSystemUiVisibility(flags);
    }

    private int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics())); }

    // ================================================================ settings sheet
    private Dialog dialog;

    @Override public void openSettings() {
        if (dialog != null && dialog.isShowing()) return;
        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        buildSettings();
        Window w = dialog.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setGravity(Gravity.TOP);
        w.setDimAmount(0.12f);
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.y = dp(12);
        w.setAttributes(lp);
        dialog.show();
    }

    private void buildSettings() {
        final Theme t = view.getThemeColors();
        final int txt = t.fn[3], sub = t.sub, card = t.dark ? 0xFF2A2C30 : t.bezelTop;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(card); bg.setCornerRadius(dp(24));
        root.setBackground(bg);
        root.setElevation(dp(8));

        root.addView(label(I18n.t("settings"), 20, t700, txt));

        // ---- language
        root.addView(section(I18n.t("language"), txt));
        final String[] langs = {"zh", "en-GB"};
        root.addView(segmented(new String[]{"繁體中文", "English (UK)"}, I18n.lang.equals("zh") ? 0 : 1, t, new Pick() {
            @Override public void pick(int i) {
                I18n.lang = langs[i];
                prefs.edit().putString("lang", langs[i]).apply();
                buildSettings();
            }
        }));

        // ---- body position
        root.addView(section(I18n.t("position"), txt));
        final float[] aligns = {0f, .5f, 1f};
        float cur = prefs.getFloat("align", DEFAULT_ALIGN);
        int ai = cur < .25f ? 0 : cur > .75f ? 2 : 1;
        root.addView(segmented(new String[]{I18n.t("pos_top"), I18n.t("pos_centre"), I18n.t("pos_bottom")}, ai, t, new Pick() {
            @Override public void pick(int i) {
                view.setAlign(aligns[i]);
                prefs.edit().putFloat("align", aligns[i]).apply();
                buildSettings();
            }
        }));

        // ---- key height
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(18), 0, 0);
        row.addView(label(I18n.t("key_height"), 15, t500, txt), new LinearLayout.LayoutParams(0, -2, 1));
        final TextView val = label(Math.round(view.getKeyScale() * 100) + "%", 15, t500, sub);
        row.addView(val);
        root.addView(row);

        SeekBar sb = new SeekBar(this);
        sb.setMax(40);
        sb.setProgress(Math.round(view.getKeyScale() * 100) - 60);
        ColorStateList tint = ColorStateList.valueOf(t.acc[1]);
        sb.setProgressTintList(tint); sb.setThumbTintList(tint);
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(sub));
        sb.setPadding(dp(4), dp(10), dp(4), dp(4));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                float k = (60 + p) / 100f;
                view.setKeyScale(k);
                val.setText((60 + p) + "%");
                prefs.edit().putFloat("keyScale", k).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(sb, new LinearLayout.LayoutParams(-1, -2));

        // ---- themes
        TextView th = section(I18n.t("theme"), txt);
        th.setPadding(0, dp(18), 0, dp(10));
        root.addView(th);
        LinearLayout sw = new LinearLayout(this);
        sw.setOrientation(LinearLayout.HORIZONTAL);
        final String chosenId = prefs.getString("theme", "cream");
        for (final Theme opt : Theme.ALL) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            Swatch s = new Swatch(opt, opt.id.equals(chosenId), txt);
            cell.addView(s, new LinearLayout.LayoutParams(dp(48), dp(48)));
            TextView n = label(I18n.t(opt.id), 11, t500, opt.id.equals(chosenId) ? txt : sub);
            n.setPadding(0, dp(6), 0, 0);
            n.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.addView(n);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    prefs.edit().putString("theme", opt.id).apply();
                    applyTheme();
                    buildSettings();
                }
            });
            sw.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
        }
        root.addView(sw);

        // ---- follow dark mode
        LinearLayout dr = new LinearLayout(this);
        dr.setOrientation(LinearLayout.HORIZONTAL);
        dr.setGravity(Gravity.CENTER_VERTICAL);
        dr.setPadding(0, dp(18), 0, 0);
        dr.addView(label(I18n.t("auto_dark"), 15, t500, txt), new LinearLayout.LayoutParams(0, -2, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(autoDark());
        int[][] states = {{android.R.attr.state_checked}, {}};
        toggle.setThumbTintList(new ColorStateList(states, new int[]{t.acc[1], t.dark ? 0xFFB0B3B8 : 0xFFF5F5F5}));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{t.acc[1], sub}));
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) {
                prefs.edit().putBoolean("autoDark", on).apply();
                applyTheme();
                buildSettings();
            }
        });
        dr.addView(toggle);
        root.addView(dr);
        if (autoDark() && systemDark()) {
            TextView note = label(I18n.t("auto_dark_on"), 12, t500, sub);
            note.setPadding(0, dp(4), 0, 0);
            root.addView(note);
        }

        // ---- done
        TextView done = label(I18n.t("done"), 15, t700, t.acc[3]);
        done.setGravity(Gravity.CENTER);
        GradientDrawable dbg = new GradientDrawable();
        dbg.setColor(t.acc[1]); dbg.setCornerRadius(dp(22));
        done.setBackground(dbg);
        done.setPadding(0, dp(12), 0, dp(12));
        done.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-1, -2);
        dl.topMargin = dp(22);
        root.addView(done, dl);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dp(14), 0, dp(14), dp(14));
        wrap.addView(root, new LinearLayout.LayoutParams(-1, -2));
        dialog.setContentView(wrap);
    }

    private interface Pick { void pick(int i); }

    private TextView section(String s, int color) {
        TextView v = label(s, 15, t500, color);
        v.setPadding(0, dp(18), 0, dp(8));
        return v;
    }

    /** Pill-shaped segmented control in the theme's colours. */
    private LinearLayout segmented(String[] items, int selected, Theme t, final Pick cb) {
        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setPadding(dp(3), dp(3), dp(3), dp(3));
        GradientDrawable track = new GradientDrawable();
        track.setColor(t.dark ? 0x1FFFFFFF : 0x12000000);
        track.setCornerRadius(dp(20));
        seg.setBackground(track);
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean sel = i == selected;
            TextView v = label(items[i], 14, t500, sel ? t.acc[3] : t.fn[3]);
            v.setGravity(Gravity.CENTER);
            v.setPadding(dp(6), dp(9), dp(6), dp(9));
            if (sel) {
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(t.acc[1]); pill.setCornerRadius(dp(17));
                v.setBackground(pill);
            }
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { cb.pick(idx); }
            });
            seg.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return seg;
    }

    private TextView label(String s, float sp, Typeface tf, int color) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTypeface(tf); v.setTextColor(color);
        return v;
    }

    /** Round preview of a theme: number key / function key / accent key colours. */
    private final class Swatch extends View {
        final Theme t; final boolean sel; final int ring;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Swatch(Theme t, boolean sel, int ring) { super(MainActivity.this); this.t = t; this.sel = sel; this.ring = ring; }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, r = Math.min(w, h) / 2 - dp(4);
            if (sel) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(ring);
                c.drawCircle(cx, cy, r + dp(2.5f), p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(t.bg); c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(0x22000000);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.FILL);
            float d = r * .34f;
            p.setColor(t.num[0]); c.drawCircle(cx - d, cy - d * .6f, r * .3f, p);
            p.setColor(t.fn[0]);  c.drawCircle(cx + d, cy - d * .6f, r * .3f, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(.8f)); p.setColor(t.fn[2]);
            c.drawCircle(cx + d, cy - d * .6f, r * .3f, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(t.acc[0]); c.drawCircle(cx, cy + d * .9f, r * .3f, p);
        }
    }
}
