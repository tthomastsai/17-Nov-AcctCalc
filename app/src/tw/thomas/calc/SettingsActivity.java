package tw.thomas.calc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Settings as a page of its own: language, appearance, tour, about and acknowledgements. */
public class SettingsActivity extends Activity {
    public static final String EXTRA_TOUR = "replayTour";
    static final String WEB_URL = "https://tthomastsai.github.io/17-Nov-AcctCalc/";

    private SharedPreferences prefs;
    private Typeface t500, t700, tGs, tSeven;
    private Theme t;
    private int txt, sub, card;
    private ScrollView scroll;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("calc", MODE_PRIVATE);
        try {
            t500 = Typeface.createFromAsset(getAssets(), "fonts/GoogleSans-500.ttf");
            t700 = Typeface.createFromAsset(getAssets(), "fonts/GoogleSans-700.ttf");
            tGs = Typeface.createFromAsset(getAssets(), "fonts/GoogleSans-400.ttf");
            tSeven = Typeface.createFromAsset(getAssets(), "fonts/AcctCalcSeven-Regular.ttf");
        } catch (Exception e) { t500 = Typeface.DEFAULT; t700 = Typeface.DEFAULT_BOLD; tGs = Typeface.DEFAULT; tSeven = Typeface.MONOSPACE; }
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        setContentView(scroll);
        build();
    }

    private int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics())); }

    private void build() {
        I18n.lang = Lang.effective(this, prefs);
        t = Theme.current(this, prefs);
        txt = t.fn[3]; sub = t.sub; card = t.dark ? 0xFF2A2C30 : t.bezelTop;
        Theme.applyBars(this, t);
        int y = scroll.getScrollY();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(8), dp(16), dp(24));
        page.setBackgroundColor(t.bg);

        // ---- top bar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, dp(4), 0, dp(12));
        View back = new Chevron(txt);
        back.setContentDescription(I18n.t("back"));
        back.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
        bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = label(I18n.t("settings"), 24, t700, txt);
        title.setPadding(dp(4), 0, 0, 0);
        bar.addView(title);
        page.addView(bar);

        // ---- language
        LinearLayout lang = section(page, I18n.t("language"));
        final String[] modes = {"system", "zh", "en-GB"};
        String cur = Lang.mode(this, prefs);
        int li = cur.equals("zh") ? 1 : cur.equals("en-GB") ? 2 : 0;
        String[] names = {I18n.t("lang_system"), "正體中文", "English (UK)"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            lang.addView(choiceRow(names[i], i == li, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Lang.set(SettingsActivity.this, prefs, modes[idx]);
                    if (Build.VERSION.SDK_INT < 33) build();   // on 13+ Android restarts this page in the new language
                }
            }));
        }
        if (Build.VERSION.SDK_INT >= 33) {
            TextView hint = label(I18n.t("lang_hint"), 12, t500, sub);
            hint.setPadding(0, dp(6), 0, 0);
            lang.addView(hint);
        }

        // ---- appearance
        LinearLayout look = section(page, I18n.t("display"));
        LinearLayout posCap = caption(I18n.t("position"), null);
        posCap.setPadding(0, 0, 0, dp(8));
        look.addView(posCap);
        final float[] aligns = {0f, .5f, 1f};
        float al = prefs.getFloat("align", MainActivity.DEFAULT_ALIGN);
        int ai = al < .25f ? 0 : al > .75f ? 2 : 1;
        look.addView(segmented(new String[]{I18n.t("pos_top"), I18n.t("pos_centre"), I18n.t("pos_bottom")}, ai, new Pick() {
            @Override public void pick(int i) { prefs.edit().putFloat("align", aligns[i]).apply(); build(); }
        }));

        float ks = prefs.getFloat("keyScale", 1f);
        final LinearLayout khRow = caption(I18n.t("key_height"), Math.round(ks * 100) + "%");
        look.addView(khRow);
        SeekBar sb = new SeekBar(this);
        sb.setMax(40);
        sb.setProgress(Math.round(ks * 100) - 60);
        ColorStateList tint = ColorStateList.valueOf(t.acc[1]);
        sb.setProgressTintList(tint); sb.setThumbTintList(tint);
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(sub));
        sb.setPadding(dp(4), dp(10), dp(4), dp(4));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                prefs.edit().putFloat("keyScale", (60 + p) / 100f).apply();
                ((TextView) khRow.getChildAt(1)).setText((60 + p) + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        look.addView(sb, new LinearLayout.LayoutParams(-1, -2));

        look.addView(caption(I18n.t("lcd_font"), null));
        final String[] fonts = {"sans", "seven"};
        String curFont = prefs.getString("lcdFont", "sans");
        String[] fontNames = {"Google Sans", I18n.t("font_seven")};
        Typeface[] faces = {tGs, tSeven};
        for (int i = 0; i < fonts.length; i++) {
            final int idx = i;
            LinearLayout row = choiceRow(fontNames[i], fonts[i].equals(curFont), new View.OnClickListener() {
                @Override public void onClick(View v) { prefs.edit().putString("lcdFont", fonts[idx]).apply(); build(); }
            });
            TextView sample = label("1'234.5", 20, faces[i], sub);   // preview in that font
            sample.setPadding(0, 0, dp(12), 0);
            row.addView(sample, 1);
            look.addView(row);
        }

        // with dark mode following, the choice here only applies in light mode
        LinearLayout th = caption(I18n.t(Theme.autoDark(prefs) ? "theme_light" : "theme"), null);
        th.setPadding(0, dp(18), 0, dp(10));
        look.addView(th);
        LinearLayout sw = new LinearLayout(this);
        sw.setOrientation(LinearLayout.HORIZONTAL);
        final String chosenId = Theme.chosen(prefs).id;
        for (final Theme opt : Theme.ALL) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.addView(new Swatch(opt, opt.id.equals(chosenId), txt), new LinearLayout.LayoutParams(dp(48), dp(48)));
            TextView n = label(I18n.t(opt.id), 11, t500, opt.id.equals(chosenId) ? txt : sub);
            n.setPadding(0, dp(6), 0, 0);
            n.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.addView(n);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { prefs.edit().putString("theme", opt.id).apply(); build(); }
            });
            sw.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
        }
        look.addView(sw);

        LinearLayout dr = new LinearLayout(this);
        dr.setOrientation(LinearLayout.HORIZONTAL);
        dr.setGravity(Gravity.CENTER_VERTICAL);
        dr.setPadding(0, dp(18), 0, 0);
        dr.addView(label(I18n.t("auto_dark"), 15, t500, txt), new LinearLayout.LayoutParams(0, -2, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(Theme.autoDark(prefs));
        int[][] states = {{android.R.attr.state_checked}, {}};
        toggle.setThumbTintList(new ColorStateList(states, new int[]{t.acc[1], t.dark ? 0xFFB0B3B8 : 0xFFF5F5F5}));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{t.acc[1], sub}));
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) { prefs.edit().putBoolean("autoDark", on).apply(); build(); }
        });
        dr.addView(toggle);
        look.addView(dr);
        if (Theme.autoDark(prefs) && Theme.systemDark(this)) {
            TextView note = label(I18n.t("auto_dark_on"), 12, t500, sub);
            note.setPadding(0, dp(4), 0, 0);
            look.addView(note);
        }

        // ---- tour
        LinearLayout help = section(page, null);
        help.addView(linkRow(I18n.t("tour_replay"), new View.OnClickListener() {
            @Override public void onClick(View v) { setResult(RESULT_OK, new Intent().putExtra(EXTRA_TOUR, true)); finish(); }
        }));

        // ---- about
        LinearLayout about = section(page, I18n.t("about"));
        String ver = "";
        try { ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) {}
        about.addView(label(String.valueOf(getApplicationInfo().loadLabel(getPackageManager())), 17, t700, txt));
        TextView v = label(I18n.t("version") + " " + ver, 13, t500, sub);
        v.setPadding(0, dp(2), 0, dp(12));
        about.addView(v);
        about.addView(para(I18n.t("about_basis")));
        about.addView(para(I18n.t("about_made")));
        about.addView(linkRow(I18n.t("about_web"), new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_URL))); } catch (Exception e) {}
            }
        }));

        // ---- acknowledgements: a card of its own under About
        LinearLayout ack = section(page, I18n.t("ack"));
        for (String k : new String[]{"ack_design", "ack_gs", "ack_seven", "ack_claude"}) ack.addView(para("• " + I18n.t(k)));
        ack.addView(linkRow(I18n.t("licences"), new View.OnClickListener() {
            @Override public void onClick(View v) { showLicences(); }
        }));

        scroll.removeAllViews();
        scroll.addView(page);
        final int keep = y;
        scroll.post(new Runnable() { @Override public void run() { scroll.scrollTo(0, keep); } });
    }

    /** the full text of both font licences, as the OFL asks to ship with the fonts */
    private void showLicences() {
        StringBuilder sb = new StringBuilder();
        for (String f : new String[]{"fonts/OFL.txt", "fonts/OFL-AcctCalcSeven.txt"}) {
            try (java.io.InputStream in = getAssets().open(f)) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                if (sb.length() > 0) sb.append("\n\n────────────\n\n");
                sb.append(out.toString("UTF-8").trim());
            } catch (Exception e) { /* missing file: skip */ }
        }
        TextView body = label(sb.toString(), 12, Typeface.MONOSPACE, txt);
        body.setPadding(dp(20), dp(12), dp(20), dp(12));
        body.setTextIsSelectable(true);
        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        new android.app.AlertDialog.Builder(this)
            .setTitle(I18n.t("licences"))
            .setView(sv)
            .setPositiveButton(I18n.t("done"), null)
            .show();
    }

    // ================================================================ building blocks
    private interface Pick { void pick(int i); }

    private TextView label(String s, float sp, Typeface tf, int color) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTypeface(tf); v.setTextColor(color);
        return v;
    }

    private TextView para(String s) {
        TextView v = label(s, 14, t500, txt);
        v.setLineSpacing(0, 1.2f);
        v.setPadding(0, 0, 0, dp(8));
        return v;
    }

    /** a rounded card with an optional heading above it */
    private LinearLayout section(LinearLayout page, String heading) {
        if (heading != null) {
            TextView h = label(heading, 13, t700, sub);
            h.setPadding(dp(6), dp(18), 0, dp(8));
            h.setAllCaps(false);
            page.addView(h);
        } else {
            View gap = new View(this);
            page.addView(gap, new LinearLayout.LayoutParams(-1, dp(18)));
        }
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(card); bg.setCornerRadius(dp(20));
        c.setBackground(bg);
        page.addView(c, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }

    /** a label with an optional value on the right, e.g. "Key height   100%" */
    private LinearLayout caption(String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(14), 0, dp(8));
        row.addView(label(left, 15, t500, txt), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(label(right == null ? "" : right, 15, t500, sub));
        return row;
    }

    /** one option in a list, with a check mark when selected */
    private LinearLayout choiceRow(String s, boolean selected, View.OnClickListener l) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.addView(label(s, 15, selected ? t700 : t500, txt), new LinearLayout.LayoutParams(0, -2, 1));
        Check c = new Check(t.acc[1]);
        c.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        row.addView(c, new LinearLayout.LayoutParams(dp(22), dp(22)));
        row.setOnClickListener(l);
        return row;
    }

    private final class Check extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final Path path = new Path();
        Check(int color) {
            super(SettingsActivity.this);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2.4f)); p.setColor(color);
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            path.reset(); path.moveTo(w * .18f, h * .52f); path.lineTo(w * .42f, h * .74f); path.lineTo(w * .84f, h * .28f);
            c.drawPath(path, p);
        }
    }

    private LinearLayout linkRow(String s, View.OnClickListener l) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        row.addView(label(s, 15, t500, t.label), new LinearLayout.LayoutParams(0, -2, 1));
        Chevron arrow = new Chevron(sub); arrow.right = true;
        row.addView(arrow, new LinearLayout.LayoutParams(dp(20), dp(20)));
        row.setOnClickListener(l);
        return row;
    }

    /** pill-shaped segmented control in the theme's colours */
    private LinearLayout segmented(String[] items, int selected, final Pick cb) {
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
            v.setPadding(dp(4), dp(9), dp(4), dp(9));
            v.setSingleLine(true);
            if (sel) {
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(t.acc[1]); pill.setCornerRadius(dp(17));
                v.setBackground(pill);
            }
            v.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { cb.pick(idx); } });
            seg.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return seg;
    }

    /** back arrow (left) or row arrow (right) */
    private final class Chevron extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final Path path = new Path(); boolean right;
        Chevron(int color) {
            super(SettingsActivity.this);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2.2f)); p.setColor(color);
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, s = Math.min(getWidth(), getHeight()) * (right ? .22f : .2f);
            path.reset();
            if (right) { path.moveTo(cx - s / 2, cy - s); path.lineTo(cx + s / 2, cy); path.lineTo(cx - s / 2, cy + s); }
            else { path.moveTo(cx + s / 2, cy - s); path.lineTo(cx - s / 2, cy); path.lineTo(cx + s / 2, cy + s); }
            c.drawPath(path, p);
        }
    }

    /** round preview of a theme: number key / function key / accent key colours */
    private final class Swatch extends View {
        final Theme th; final boolean sel; final int ring;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Swatch(Theme th, boolean sel, int ring) { super(SettingsActivity.this); this.th = th; this.sel = sel; this.ring = ring; }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, r = Math.min(w, h) / 2 - dp(4);
            if (sel) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(ring);
                c.drawCircle(cx, cy, r + dp(2.5f), p);
            }
            p.setStyle(Paint.Style.FILL);
            p.setColor(th.bg); c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(0x22000000);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.FILL);
            float d = r * .34f;
            p.setColor(th.num[0]); c.drawCircle(cx - d, cy - d * .6f, r * .3f, p);
            p.setColor(th.fn[0]);  c.drawCircle(cx + d, cy - d * .6f, r * .3f, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(.8f)); p.setColor(th.fn[2]);
            c.drawCircle(cx + d, cy - d * .6f, r * .3f, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(th.acc[0]); c.drawCircle(cx, cy + d * .9f, r * .3f, p);
        }
    }
}
