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
        bar.setPadding(0, dp(4), 0, dp(12));
        View back = new Chevron(txt);
        Raised backKey = knob(t.fn, 40);
        back.setBackground(backKey);
        back.setContentDescription(I18n.t("back"));
        back.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });
        bar.addView(back, new LinearLayout.LayoutParams(backKey.iw, backKey.ih));
        TextView title = label(I18n.t("settings"), 24, t700, txt);
        title.setPadding(dp(10), 0, 0, 0);
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
        sb.setProgressTintList(ColorStateList.valueOf(t.acc[1]));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(sub));
        Raised thumb = knob(t.acc, 22);
        sb.setThumb(thumb);
        sb.setSplitTrack(false);
        sb.setPadding(thumb.iw / 2, dp(4), thumb.iw / 2, 0);   // room for the whole thumb at either end
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
     * Material 3 switch, drawn by hand (the app has no Material library): a 52×32 dp track with a
     * 2 dp outline when off; the thumb grows from 16 dp (off) to 24 dp (on, with a check mark) and to
     * 28 dp while pressed, and slides across in 150 ms before the setting is saved.
     */
    private final class Toggle extends View {
        boolean on; float pos, press;                       // pos: 0 off .. 1 on
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final Path tick = new Path();
        final int primary = t.acc[1], onPrimary = t.dark ? t.acc[3] : 0xFFFFFFFF, icon = t.dark ? t.acc[1] : t.acc[2];
        final int outline = t.sub, offTrack = t.dark ? 0xFF36383D : Raised.mix(t.bg, 0xFF000000, .06f);
        Runnable onChange;
        Toggle(boolean on, String desc) {
            super(SettingsActivity.this); this.on = on; pos = on ? 1 : 0;
            setContentDescription(desc); setFocusable(true); setClickable(true);
        }
        private int blend(int a, int b, float f) {
            return (Math.round(Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * f) << 24) | (Raised.mix(a, b, f) & 0xFFFFFF);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) { press = 1; invalidate(); }
            else if (e.getAction() == android.view.MotionEvent.ACTION_UP || e.getAction() == android.view.MotionEvent.ACTION_CANCEL) { press = 0; invalidate(); }
            return super.onTouchEvent(e);
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
            float tw = dp(52), th = dp(32), x0 = getWidth() - tw, y0 = (getHeight() - th) / 2, r = th / 2;
            // track: outlined surface when off, filled primary when on
            p.setStyle(Paint.Style.FILL); p.setColor(blend(offTrack, primary, pos));
            c.drawRoundRect(x0, y0, x0 + tw, y0 + th, r, r, p);
            if (pos < 1) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(blend(outline, primary, pos));
                c.drawRoundRect(x0 + dp(1), y0 + dp(1), x0 + tw - dp(1), y0 + th - dp(1), r - dp(1), r - dp(1), p);
            }
            // thumb
            float d = press > 0 ? dp(28) : dp(16) + dp(8) * pos;
            float cx = x0 + r + (tw - th) * pos, cy = y0 + r;
            p.setStyle(Paint.Style.FILL); p.setColor(blend(outline, onPrimary, pos));
            c.drawCircle(cx, cy, d / 2, p);
            if (pos > .5f) {                                     // check mark on the selected thumb
                float s = dp(16) / 2f * (pos - .5f) * 2;
                tick.reset(); tick.moveTo(cx - s * .55f, cy + s * .02f); tick.lineTo(cx - s * .15f, cy + s * .42f); tick.lineTo(cx + s * .6f, cy - s * .38f);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
                p.setColor(icon); c.drawPath(tick, p);
            }
        }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(Switch.class.getName()); info.setCheckable(true); info.setChecked(on);
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
