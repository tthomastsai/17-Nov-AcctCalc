package tw.thomas.calc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
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

    /** id of one of the slide animations in res/anim (looked up by name so the render tests need no R class) */
    static int anim(Activity a, String name) { return a.getResources().getIdentifier(name, "anim", a.getPackageName()); }

    /** Settings slides in from the right and back out to the right; the calculator moves a little to the left underneath. */
    static void slideIn(Activity from) {
        if (Build.VERSION.SDK_INT < 34) from.overridePendingTransition(anim(from, "slide_in_right"), anim(from, "slide_out_left"));
    }

    @Override public void finish() {
        super.finish();
        if (Build.VERSION.SDK_INT < 34) overridePendingTransition(anim(this, "slide_in_left"), anim(this, "slide_out_right"));
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 34) {   // also covers the predictive back gesture
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, anim(this, "slide_in_right"), anim(this, "slide_out_left"));
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, anim(this, "slide_in_left"), anim(this, "slide_out_right"));
        }
        prefs = getSharedPreferences("calc", MODE_PRIVATE);
        t500 = CalcView.load(this, "fonts/GoogleSans-500.ttf", Typeface.NORMAL);
        t700 = CalcView.load(this, "fonts/GoogleSans-700.ttf", Typeface.BOLD);
        tGs = CalcView.load(this, "fonts/GoogleSans-400.ttf", Typeface.NORMAL);
        tSeven = CalcView.load(this, "fonts/AcctCalcSeven-Regular.ttf", Typeface.NORMAL);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        setContentView(scroll);
        build();
    }

    private float den() { return getResources().getDisplayMetrics().density; }

    /** a card in the keys' style: raised, with a soft shadow and a little thickness */
    private Raised cardBg() {
        int top = t.dark ? 0xFF2D2F33 : t.bezelTop;
        int bot = t.dark ? 0xFF27292D : Raised.mix(t.bezelTop, t.bezelBot, .55f);
        int side = t.dark ? 0xFF18191C : t.bezelLine;
        return new Raised(top, bot, side, t.dark, den(), 20, 3, 4, 6);
    }

    /** a small round key: back button, switch and slider thumbs */
    private Raised knob(int[] col, float sizeDp) {
        Raised k = new Raised(col[0], col[1], col[2], t.dark, den(), -1, 2.5f, 2, 3);
        k.centred = true;
        k.iw = dp(sizeDp) + 2 * k.padL(); k.ih = dp(sizeDp) + k.padT() + k.padB();
        return k;
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
        page.setPadding(dp(12), dp(8), dp(12), dp(24));
        page.setBackgroundColor(t.bg);

        // ---- top bar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        Raised barBg = cardBg();                                   // back arrow and title share one raised block, sized to them
        bar.setBackground(barBg);
        bar.setPadding(dp(4) + barBg.padL(), dp(4), dp(16) + barBg.padL(), dp(4) + barBg.padB());
        View back = new Chevron(txt);
        back.setContentDescription(I18n.t("back"));
        back.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
        bar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = label(I18n.t("settings"), 22, t700, txt);
        title.setPadding(dp(2), 0, 0, 0);
        bar.addView(title);
        bar.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });   // the whole block goes back
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-2, -2);   // only as wide as the arrow and title
        barLp.setMargins(0, dp(4), 0, dp(4));
        page.addView(bar, barLp);

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
        Slider sb = new Slider(Math.round(ks * 100), I18n.t("key_height"));
        sb.onChange = new Pick() {
            @Override public void pick(int v) {
                prefs.edit().putFloat("keyScale", v / 100f).apply();
                ((TextView) khRow.getChildAt(1)).setText(v + "%");
            }
        };
        look.addView(sb, new LinearLayout.LayoutParams(-1, dp(40)));

        look.addView(caption(I18n.t("lcd_font"), null));
        final String[] fonts = {"sans", "seven"};
        String curFont = prefs.getString("lcdFont", "sans");
        String[] fontNames = {"Google Sans", I18n.t("font_seven")};
        Typeface[] faces = {tGs, tSeven};
        // two tiles side by side in a groove, the chosen one raised: a large sample with the font's name under it
        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        tiles.setBackground(new Well(t.dark, den(), 18));
        tiles.setPadding(dp(4), dp(4), dp(4), dp(3));
        for (int i = 0; i < fonts.length; i++) {
            final int idx = i;
            boolean sel = fonts[i].equals(curFont);
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_HORIZONTAL);
            Raised face = new Raised(t.acc[0], t.acc[1], t.acc[2], t.dark, den(), 14, 2.5f, 1.5f, 2.5f);
            if (sel) tile.setBackground(face);
            tile.setPadding(face.padL(), dp(10), face.padL(), dp(8) + face.padB());
            int fg = sel ? t.acc[3] : t.fn[3];
            TextView sample = label("1'234.5", 26, faces[i], fg);
            sample.setGravity(Gravity.CENTER);
            sample.setIncludeFontPadding(false);          // the two fonts have different line heights: give both samples
            tile.addView(sample, new LinearLayout.LayoutParams(-1, dp(36)));   // the same box so the names line up
            TextView nm = label(fontNames[i], 12, t500, fg);
            nm.setGravity(Gravity.CENTER); nm.setPadding(0, dp(2), 0, 0);
            tile.addView(nm);
            tile.setContentDescription(fontNames[i]);
            tile.setSelected(sel);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { prefs.edit().putString("lcdFont", fonts[idx]).apply(); build(); }
            });
            tiles.addView(tile, new LinearLayout.LayoutParams(0, -2, 1));
        }
        look.addView(tiles);

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
        final Toggle toggle = new Toggle(Theme.autoDark(prefs), I18n.t("auto_dark"));
        toggle.onChange = new Runnable() {
            @Override public void run() { prefs.edit().putBoolean("autoDark", toggle.on).apply(); build(); }
        };
        dr.addView(toggle, new LinearLayout.LayoutParams(dp(56), dp(48)));
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
        help.addView(linkRow(I18n.t("tour_more"), new View.OnClickListener() {   // same link as the tour's last step
            @Override public void onClick(View v) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(TourView.README_URL))); } catch (Exception e) {}
            }
        }));

        // ---- other devices: a card of its own before About (as the web version has "Use it on your phone")
        LinearLayout other = section(page, I18n.t("other_devices"));
        other.addView(linkRow(I18n.t("about_web"), new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_URL))); } catch (Exception e) {}
            }
        }));
        TextView on = para(I18n.t("other_devices_note"));
        on.setTextColor(sub); on.setTextSize(13);
        other.addView(on);

        // ---- about
        LinearLayout about = section(page, I18n.t("about"));
        String ver = "";
        try { ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) {}
        about.addView(label(String.valueOf(getApplicationInfo().loadLabel(getPackageManager())), 17, t700, txt));
        TextView v = label(I18n.t("version") + " " + ver, 13, t500, sub);
        v.setPadding(0, dp(2), 0, dp(12));
        about.addView(v);
        about.addView(para(I18n.t("about_basis")));
        TextView made = para(I18n.t("about_made"));
        made.setText(bold(I18n.t("about_made"), AUTHOR));
        about.addView(made);

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
        int dlgTheme = t.dark ? android.R.style.Theme_DeviceDefault_Dialog_Alert : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        new android.app.AlertDialog.Builder(this, dlgTheme)   // follow the app's theme, not the system's
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

    private static final String AUTHOR = "tthomastsai";

    /** s with the first occurrence of part set in Google Sans Bold. */
    private CharSequence bold(String s, String part) {
        android.text.SpannableString sp = new android.text.SpannableString(s);
        int i = s.indexOf(part);
        Object span = android.os.Build.VERSION.SDK_INT >= 28 ? new android.text.style.TypefaceSpan(t700)
                : new android.text.style.StyleSpan(Typeface.BOLD);   // Android 6–8: synthetic bold
        if (i >= 0) sp.setSpan(span, i, i + part.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sp;
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
            h.setPadding(dp(10), dp(14), 0, dp(6));
            h.setAllCaps(false);
            page.addView(h);
        } else {
            View gap = new View(this);
            page.addView(gap, new LinearLayout.LayoutParams(-1, dp(12)));
        }
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        Raised bg = cardBg();
        c.setBackground(bg);
        c.setPadding(dp(18) + bg.padL(), dp(16), dp(18) + bg.padL(), dp(16) + bg.padB());
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
        seg.setBackground(new Well(t.dark, den(), -1));
        seg.setPadding(dp(3), dp(3), dp(3), dp(2));
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean sel = i == selected;
            TextView v = label(items[i], 14, t500, sel ? t.acc[3] : t.fn[3]);
            v.setGravity(Gravity.CENTER);
            v.setSingleLine(true);
            Raised pill = new Raised(t.acc[0], t.acc[1], t.acc[2], t.dark, den(), -1, 2.5f, 1.5f, 2.5f);
            if (sel) v.setBackground(pill);
            v.setPadding(dp(4) + pill.padL(), dp(8), dp(4) + pill.padL(), dp(8) + pill.padB());
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

    /**
     * On/off switch: the Material 3 shape (a 52×32 dp pill holding a round thumb) in the calculator's
     * materials: the pill is a recessed groove, tinted with the accent when on, and the thumb is a
     * raised round key that slides across in 150 ms before the setting is saved.
     */
    private final class Toggle extends View {
        boolean on; float pos;                               // pos: 0 off .. 1 on
        final Well groove = new Well(t.dark, den(), -1);
        final Raised thumb = knob(t.fn, 24);
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final Path tick = new Path();
        Runnable onChange;
        Toggle(boolean on, String desc) {
            super(SettingsActivity.this); this.on = on; pos = on ? 1 : 0;
            setContentDescription(desc); setFocusable(true); setClickable(true);
        }
        @Override public boolean performClick() {
            super.performClick();
            on = !on;
            android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(pos, on ? 1 : 0);
            va.setDuration(150);
            va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                boolean done;
                @Override public void onAnimationUpdate(android.animation.ValueAnimator a) {
                    pos = (Float) a.getAnimatedValue(); invalidate();
                    if (!done && a.getAnimatedFraction() >= 1f) { done = true; if (onChange != null) onChange.run(); }
                }
            });
            va.start();
            return true;
        }
        @Override protected void onDraw(Canvas c) {
            float tw = dp(52), th = dp(32), x0 = getWidth() - tw, y0 = (getHeight() - th) / 2;
            groove.setBounds(Math.round(x0), Math.round(y0), Math.round(x0 + tw), Math.round(y0 + th));
            groove.draw(c);
            if (pos > 0) {                                      // accent fill inside the groove
                p.setStyle(Paint.Style.FILL);
                p.setColor((Math.round(pos * 0xE6) << 24) | (t.acc[1] & 0xFFFFFF));
                float in = dp(2.5f);
                c.drawRoundRect(x0 + in, y0 + in, x0 + tw - in, y0 + th - in - dp(1), th / 2 - in, th / 2 - in, p);
            }
            float d = dp(24), cx = x0 + dp(4) + d / 2 + (tw - d - dp(8)) * pos, cy = y0 + th / 2 - dp(1.5f);
            int x = Math.round(cx - thumb.iw / 2f), y = Math.round(cy - thumb.ih / 2f);
            thumb.setBounds(x, y, x + thumb.iw, y + thumb.ih);
            thumb.draw(c);
            if (pos > .5f) {                                     // check mark on the thumb when on
                float s = d / 2 * .55f * (pos - .5f) * 2;
                tick.reset(); tick.moveTo(cx - s * .55f, cy + s * .02f); tick.lineTo(cx - s * .15f, cy + s * .42f); tick.lineTo(cx + s * .6f, cy - s * .38f);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
                p.setColor(t.acc[2]); c.drawPath(tick, p);
            }
        }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(Switch.class.getName()); info.setCheckable(true); info.setChecked(on);
        }
    }

    /**
     * Key height slider: a groove as thick as its knob, filled with the accent up to the knob, which is
     * a small raised round key. 60–100 %, in steps of 1; TalkBack reads it as a slider.
     */
    private final class Slider extends View {
        int value; Pick onChange;
        final int min = 60, max = 100;
        final Well groove = new Well(t.dark, den(), -1);
        final Raised knob = knob(t.acc, 16);
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Slider(int v, String desc) {
            super(SettingsActivity.this); value = Math.max(min, Math.min(max, v));
            setContentDescription(desc); setFocusable(true);
        }
        private float left() { return knob.iw / 2f; }
        private float right() { return getWidth() - knob.iw / 2f; }
        private float xOf(int v) { return left() + (right() - left()) * (v - min) / (float) (max - min); }
        private void set(int v) {
            v = Math.max(min, Math.min(max, v));
            if (v != value) { value = v; invalidate(); if (onChange != null) onChange.pick(v); }
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            int a = e.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_DOWN) getParent().requestDisallowInterceptTouchEvent(true);
            if (a == android.view.MotionEvent.ACTION_DOWN || a == android.view.MotionEvent.ACTION_MOVE || a == android.view.MotionEvent.ACTION_UP) {
                set(Math.round(min + (e.getX() - left()) / (right() - left()) * (max - min)));
                return true;
            }
            return a == android.view.MotionEvent.ACTION_CANCEL || super.onTouchEvent(e);
        }
        @Override public boolean onKeyDown(int code, android.view.KeyEvent e) {
            if (code == android.view.KeyEvent.KEYCODE_DPAD_LEFT) { set(value - 1); return true; }
            if (code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) { set(value + 1); return true; }
            return super.onKeyDown(code, e);
        }
        @Override protected void onDraw(Canvas c) {
            float gh = dp(16), cy = getHeight() / 2f, x = xOf(value);
            groove.setBounds(Math.round(left() - gh / 2), Math.round(cy - gh / 2), Math.round(right() + gh / 2), Math.round(cy + gh / 2));
            groove.draw(c);
            p.setColor((0xE6 << 24) | (t.acc[1] & 0xFFFFFF));
            float in = dp(2.5f);
            c.drawRoundRect(left() - gh / 2 + in, cy - gh / 2 + in, x, cy + gh / 2 - in - dp(1), gh / 2 - in, gh / 2 - in, p);
            int kx = Math.round(x - knob.iw / 2f), ky = Math.round(cy - dp(1.5f) - knob.ih / 2f);
            knob.setBounds(kx, ky, kx + knob.iw, ky + knob.ih);
            knob.draw(c);
        }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(SeekBar.class.getName());
            info.setRangeInfo(android.view.accessibility.AccessibilityNodeInfo.RangeInfo.obtain(
                    android.view.accessibility.AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, min, max, value));
            info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
            info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        }
        @Override public boolean performAccessibilityAction(int action, android.os.Bundle args) {
            if (action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) { set(value + 5); return true; }
            if (action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) { set(value - 5); return true; }
            return super.performAccessibilityAction(action, args);
        }
    }

    /** round preview of a theme: number key / function key / accent key colours */
    private final class Swatch extends View {
        final Theme th; final boolean sel; final int ring;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Raised disc;
        Swatch(Theme th, boolean sel, int ring) {
            super(SettingsActivity.this); this.th = th; this.sel = sel; this.ring = ring;
            disc = new Raised(th.bg, Raised.mix(th.bg, th.dark ? 0xFF000000 : th.bezelBot, .5f), th.dark ? Raised.mix(th.bg, 0xFF000000, .4f) : th.bezelLine, th.dark, den(), -1, 2, 1.5f, 2);
            disc.centred = true;
        }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, r = Math.min(w, h) / 2 - dp(4);
            if (sel) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(ring);
                c.drawCircle(cx, cy, r + dp(2.5f), p);
            }
            disc.setBounds(Math.round(cx - r - disc.padL()), Math.round(cy - r - disc.padT()), Math.round(cx + r + disc.padL()), Math.round(cy + r + disc.padB()));
            disc.draw(c);                                      // the theme's body colour as a raised disc
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
