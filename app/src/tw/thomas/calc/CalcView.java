package tw.thomas.calc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/** The whole calculator, drawn natively on a Canvas. */
public class CalcView extends View {

    public interface Host { void openSettings(); }

    static final int NUM = 0, FN = 1, ACC = 2;

    static final class Key {
        final String k, label; final int type, row, col, span, weight; final float sizeU;
        final RectF r = new RectF();
        float p; boolean down; long holdUntil;
        Key(String k, String label, int type, int row, int col, int span, float sizeU, int weight) {
            this.k = k; this.label = label; this.type = type; this.row = row; this.col = col;
            this.span = span; this.sizeU = sizeU; this.weight = weight;
        }
    }

    private final Key[] keys = {
        new Key("back", "", FN, 0, 0, 1, 0, 0),
        new Key("sqrt", "", FN, 0, 1, 1, 0, 0),
        new Key("pct", "%", FN, 0, 2, 1, 6f, 500),
        new Key("gt", "GT", FN, 0, 3, 1, 5f, 500),
        new Key("set", "", FN, 0, 4, 1, 0, 0),
        new Key("mc", "MC", FN, 1, 0, 1, 5f, 500),
        new Key("mr", "MR", FN, 1, 1, 1, 5f, 500),
        new Key("mminus", "M−", FN, 1, 2, 1, 5f, 500),
        new Key("mplus", "M+", FN, 1, 3, 1, 5f, 500),
        new Key("div", "÷", FN, 1, 4, 1, 8f, 400),
        new Key("neg", "+/−", FN, 2, 0, 1, 5f, 500),
        new Key("7", "7", NUM, 2, 1, 1, 7.4f, 400),
        new Key("8", "8", NUM, 2, 2, 1, 7.4f, 400),
        new Key("9", "9", NUM, 2, 3, 1, 7.4f, 400),
        new Key("mul", "×", FN, 2, 4, 1, 8f, 400),
        new Key("ac", "AC", ACC, 3, 0, 1, 6.4f, 500),
        new Key("4", "4", NUM, 3, 1, 1, 7.4f, 400),
        new Key("5", "5", NUM, 3, 2, 1, 7.4f, 400),
        new Key("6", "6", NUM, 3, 3, 1, 7.4f, 400),
        new Key("sub", "−", FN, 3, 4, 1, 8f, 400),
        new Key("onc", "", ACC, 4, 0, 1, 0, 700),
        new Key("1", "1", NUM, 4, 1, 1, 7.4f, 400),
        new Key("2", "2", NUM, 4, 2, 1, 7.4f, 400),
        new Key("3", "3", NUM, 4, 3, 1, 7.4f, 400),
        new Key("add", "+", FN, 4, 4, 2, 8f, 400),
        new Key("0", "0", NUM, 5, 0, 1, 7.4f, 400),
        new Key("00", "00", NUM, 5, 1, 1, 6.4f, 400),
        new Key("dot", "·", NUM, 5, 2, 1, 12f, 400),
        new Key("eq", "=", FN, 5, 3, 1, 8f, 400),
    };

    private final CalcEngine eng;
    private Theme th = Theme.ALL[0];
    private float keyScale = 1f;
    private float align = .5f;   // 0 = top, .5 = centre, 1 = bottom
    private Host host;

    private final Typeface t400, t500, t700, tSeven;
    private boolean sevenSeg = false;   // display digits: Google Sans (false) or AcctCalc Seven (true)
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF tmp = new RectF();
    private final Rect bounds = new Rect();
    private final SparseArray<Key> pointers = new SparseArray<>();

    // layout
    private float u, contentL, contentR, headTop, fs = 1f;
    private final RectF bezel = new RectF(), frame = new RectF(), lcd = new RectF();
    private long lastFrame;

    public CalcView(Context c, CalcEngine engine) {
        super(c);
        eng = engine;
        t400 = load(c, "fonts/GoogleSans-400.ttf", Typeface.NORMAL);
        tSeven = load(c, "fonts/AcctCalcSeven-Regular.ttf", Typeface.NORMAL);
        t500 = load(c, "fonts/GoogleSans-500.ttf", Typeface.NORMAL);
        t700 = load(c, "fonts/GoogleSans-700.ttf", Typeface.BOLD);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        setHapticFeedbackEnabled(true);
    }

    /** test hook: load fonts from a folder instead of the APK assets */
    static String fontDir = null;

    private static Typeface load(Context c, String path, int fallbackStyle) {
        try {
            if (fontDir != null) return Typeface.createFromFile(fontDir + "/" + path.substring(path.indexOf('/') + 1));
            return Typeface.createFromAsset(c.getAssets(), path);
        }
        catch (Exception e) { return Typeface.create(Typeface.SANS_SERIF, fallbackStyle); }
    }

    public void setHost(Host h) { host = h; }
    public void setThemeColors(Theme t) { th = t; invalidate(); }
    public Theme getThemeColors() { return th; }
    public void setKeyScale(float s) { keyScale = Math.max(0.6f, Math.min(1f, s)); layout(getWidth(), getHeight()); invalidate(); }
    public float getKeyScale() { return keyScale; }
    public void setAlign(float a) { align = a; layout(getWidth(), getHeight()); invalidate(); }
    public void setSevenSegment(boolean on) { sevenSeg = on; invalidate(); }

    // ---- positions for the first-run tour (view coordinates)
    public RectF keyRect(String id) {
        for (Key k : keys) if (k.k.equals(id)) return new RectF(k.r);
        return null;
    }
    /** the printed indicator labels and the operator corner along the top of the LCD */
    public RectF indicatorRect() { return new RectF(lcd.left, lcd.top, lcd.right, lcd.top + 6f * u); }
    public RectF lcdRect() { return new RectF(lcd); }
    public float unit() { return u; }

    // ================================================================ layout
    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { layout(w, h); }

    private void layout(int w, int h) {
        if (w == 0 || h == 0) return;
        u = Math.min(w / 100f, h / 200f);
        float chFull = Math.min(h - 8 * u, 205 * u);
        float fixed = 6 * u + 15.3f * u + 27.9f * u + 6 * u + 7 * u;     // everything except the keys
        float hk = chFull - fixed, hkS = hk * keyScale;
        float ch = fixed + hkS;                                          // actual body height
        float left0 = (w - 100 * u) / 2f;
        float top0 = 4 * u + Math.max(0, h - ch - 8 * u) * align;
        contentL = left0 + 6 * u; contentR = left0 + 94 * u;

        float keysBottom = top0 + ch - 7 * u;

        headTop = top0 + 6 * u;
        bezel.set(contentL, headTop + 15.3f * u, contentR, headTop + 43.2f * u);
        frame.set(bezel); frame.inset(1.6f * u, 1.6f * u);
        lcd.set(frame); lcd.inset(1.1f * u, 1.1f * u);


        float keysTop = keysBottom - hkS, cg = 3.4f * u, rg = 3.6f * u;
        float colW = (contentR - contentL - 4 * cg) / 5f, rowH = (hkS - 5 * rg) / 6f;
        fs = Math.min(1f, rowH / (16f * u));
        for (Key k : keys) {
            float l = contentL + k.col * (colW + cg), t = keysTop + k.row * (rowH + rg);
            k.r.set(l, t, l + colW, t + k.span * rowH + (k.span - 1) * rg);
        }
    }

    // ================================================================ drawing
    @Override protected void onDraw(Canvas c) {
        if (u == 0) layout(getWidth(), getHeight());
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 16 : Math.max(0, Math.min(50, now - lastFrame));
        lastFrame = now;
        boolean animating = false;
        for (Key k : keys) {
            float target = (k.down || now < k.holdUntil) ? 1f : 0f;
            float tau = target > k.p ? 18f : 75f;               // quick press, soft creamy release
            k.p += (target - k.p) * (1f - (float) Math.exp(-dt / tau));
            if (Math.abs(target - k.p) < 0.003f) k.p = target; else animating = true;
            if (now < k.holdUntil) animating = true;
        }

        c.drawColor(th.bg);
        drawHead(c);
        drawLcd(c);
        for (Key k : keys) drawKey(c, k);

        if (animating) postInvalidateOnAnimation(); else lastFrame = 0;
    }

    private void setText(Typeface tf, float size, int color, float spacingEm) {
        text.setTypeface(tf); text.setTextSize(size); text.setColor(color);
        text.setLetterSpacing(spacingEm); text.setTextSkewX(0); text.setFontFeatureSettings(null);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHead(Canvas c) {
        // the three elements share one centre line, and the outer two line up with the LCD glass edges
        float L = lcd.left, R = lcd.right, yc = headTop + 3.6f * u;

        // label block: cap top of CALCULATOR to baseline of ELECTRONIC is centred on yc
        setText(t700, 3.4f * u, th.label, 0.06f);
        float labW = text.measureText("CALCULATOR");
        c.drawText("CALCULATOR", L, yc - .75f * u, text);
        setText(t500, 2.5f * u, th.sub, 0.14f);
        c.drawText("ELECTRONIC", L, yc + 3.15f * u, text);

        // "12 digits", right-aligned to the LCD
        setText(t700, 2.8f * u, th.label, 0);
        text.setTextSkewX(-0.22f);
        float digW = text.measureText("12 digits");
        text.setTextAlign(Paint.Align.RIGHT);
        c.drawText("12 digits", R, yc + 1f * u, text);

        // solar panel, centred in the space between the two
        float sw = 26 * u;
        float sl = (L + labW + R - digW) / 2f - sw / 2f, sr = sl + sw, st = yc - 3.6f * u, sb = yc + 3.6f * u;
        fill.setColor(Color.BLACK);   // opaque: otherwise the panel inherits the last key's 55% alpha after the first frame
        fill.setShader(new LinearGradient(0, st, 0, sb, new int[]{0xFF2A2423, 0xFF171313, 0xFF241E1D}, new float[]{0, .6f, 1}, Shader.TileMode.CLAMP));
        tmp.set(sl, st, sr, sb);
        c.drawRoundRect(tmp, .6f * u, .6f * u, fill);
        fill.setShader(null);
        stroke.setStrokeWidth(.35f * u); stroke.setColor(0xFF0D0A0A);
        tmp.inset(.175f * u, .175f * u);
        c.drawRoundRect(tmp, .5f * u, .5f * u, stroke);
        fill.setColor(0x0DFFFFFF);
        for (float lx = sl + 7.2f * u; lx < sr - u; lx += 7.5f * u) c.drawRect(lx, st + .35f * u, lx + .3f * u, sb - .35f * u, fill);
        fill.setColor(th.dark ? 0x22FFFFFF : 0xFFE2DED8);
        c.drawRect(sl + .6f * u, sb, sr - .6f * u, sb + .3f * u, fill);

        setText(t500, 1.9f * u, th.sub, 0.12f);
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText("TWO POWER", (sl + sr) / 2f, sb + 3.1f * u, text);
    }

    private final Path gear = new Path(), gearTmp = new Path();
    private final android.graphics.Matrix gearMatrix = new android.graphics.Matrix();   // reused every frame

    private void drawGear(Canvas c, float cx, float cy, float r, int color) {
        gear.reset();
        gear.addCircle(cx, cy, .74f * r, Path.Direction.CW);
        for (int i = 0; i < 8; i++) {
            gearTmp.reset();
            tmp.set(cx - .2f * r, cy - r, cx + .2f * r, cy - .5f * r);
            gearTmp.addRoundRect(tmp, .07f * r, .07f * r, Path.Direction.CW);
            gearMatrix.setRotate(i * 45f, cx, cy);
            gearTmp.transform(gearMatrix);
            gear.op(gearTmp, Path.Op.UNION);
        }
        gearTmp.reset();
        gearTmp.addCircle(cx, cy, .32f * r, Path.Direction.CW);
        gear.op(gearTmp, Path.Op.DIFFERENCE);
        fill.setShader(null); fill.setColor(color);
        c.drawPath(gear, fill);
    }

    private void drawLcd(Canvas c) {
        // bezel
        fill.setShader(null); fill.setColor(th.bezelLine);
        tmp.set(bezel); tmp.inset(-.35f * u, -.35f * u);
        c.drawRoundRect(tmp, 2.5f * u, 2.5f * u, fill);
        fill.setShader(new LinearGradient(0, bezel.top, 0, bezel.bottom, th.bezelTop, th.bezelBot, Shader.TileMode.CLAMP));
        c.drawRoundRect(bezel, 2.2f * u, 2.2f * u, fill);
        fill.setShader(null); fill.setColor(th.frame);
        c.drawRoundRect(frame, 1.4f * u, 1.4f * u, fill);

        // glass
        fill.setShader(new LinearGradient(lcd.left, lcd.top, lcd.left + lcd.width() * .17f, lcd.bottom,
            new int[]{0xFFB9BFB6, 0xFFA9AFA6, 0xFFA2A89F}, new float[]{0, .55f, 1}, Shader.TileMode.CLAMP));
        c.drawRoundRect(lcd, .6f * u, .6f * u, fill);
        c.save();
        path.reset(); path.addRoundRect(lcd, .6f * u, .6f * u, Path.Direction.CW);
        c.clipPath(path);
        fill.setShader(new LinearGradient(0, lcd.top, 0, lcd.top + 2.2f * u, 0x47000000, 0x00000000, Shader.TileMode.CLAMP));
        c.drawRect(lcd.left, lcd.top, lcd.right, lcd.top + 2.2f * u, fill);
        fill.setShader(new LinearGradient(lcd.left, 0, lcd.left + 1.2f * u, 0, 0x22000000, 0x00000000, Shader.TileMode.CLAMP));
        c.drawRect(lcd.left, lcd.top, lcd.left + 1.2f * u, lcd.bottom, fill);
        float gx = lcd.width() * .45f, gy = lcd.height() * .42f;
        fill.setShader(new LinearGradient(lcd.left, lcd.top, lcd.left + gx, lcd.top + gy, 0x2EFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRect(lcd, fill);
        fill.setShader(null);
        c.restore();

        final int ink = 0xFF1D2320;
        boolean on = eng.on;
        boolean neg = eng.isNegative(), full12 = eng.isFull12();

        // printed indicator labels + segment bars
        String[] labs = {"MINUS", "MEMORY", "ERROR", "GRAND TOTAL"};
        boolean[] st = {neg && full12, eng.mem != 0, eng.err, eng.gt != 0};
        float x = lcd.left + 2.4f * u, base = lcd.top + 1.3f * u + 1.95f * u;
        setText(t500, 2f * u, ink, 0.05f);
        for (int i = 0; i < labs.length; i++) {
            float w = text.measureText(labs[i]);
            text.setAlpha(158);
            c.drawText(labs[i], x, base, text);
            fill.setColor(ink); fill.setAlpha(on && st[i] ? 230 : 13);
            float cx = x + w / 2f, bt = lcd.top + 1.3f * u + 3.1f * u;
            tmp.set(cx - 2.3f * u, bt, cx + 2.3f * u, bt + u);
            c.drawRoundRect(tmp, .2f * u, .2f * u, fill);
            x += w + 2.2f * u;
        }
        fill.setAlpha(255);

        if (!on) return;

        // pending operator, top-right
        if (eng.op != null && !eng.err) {
            String o = eng.op.equals("add") ? "+" : eng.op.equals("sub") ? "−" : eng.op.equals("mul") ? "×" : "÷";
            setText(t500, 5f * u, ink, 0);
            text.setAlpha(230);
            text.setTextAlign(Paint.Align.RIGHT);
            text.getTextBounds(o, 0, o.length(), bounds);
            c.drawText(o, lcd.right - 2.4f * u, lcd.top + 3.3f * u - bounds.exactCenterY(), text);
        }

        // number
        String s = (neg && !full12 ? "−" : "") + eng.displayDigits();
        float size = 10.5f * u;
        if (sevenSeg) {
            size *= 0.748f / 0.76f;            // same digit height as Google Sans
            setText(tSeven, size, ink, 0);
        } else {
            setText(t400, size, ink, -0.01f);
            text.setFontFeatureSettings("tnum");
        }
        text.setAlpha(230);
        float avail = lcd.width() - 4.8f * u, w = text.measureText(s);
        if (w > avail) { size *= avail / w; text.setTextSize(size); }
        text.setTextAlign(Paint.Align.RIGHT);
        c.drawText(s, lcd.right - 2.4f * u, lcd.bottom - 2.4f * u - 0.2f * 10.5f * u, text);
    }

    private static int alpha(int color, float a) { return (color & 0x00FFFFFF) | (Math.round(a * 255) << 24); }

    private void drawKey(Canvas c, Key k) {
        int[] col = k.type == NUM ? th.num : k.type == ACC ? th.acc : th.fn;
        float pr = k.p, rad = 3f * u;
        RectF r = k.r;
        float dy = pr * .9f * u, side = (1.1f - .85f * pr) * u;
        float shOff = (1.6f - 1.1f * pr) * u, shBlur = (1.6f - .8f * pr) * u, shA = (th.dark ? .45f : .22f) - .04f * pr;

        // soft shadow: stacked translucent layers
        fill.setShader(null);
        int n = 6; float a = 1f - (float) Math.pow(1f - shA, 1f / n);
        fill.setColor(alpha(Color.BLACK, a));
        for (int i = 0; i < n; i++) {
            float e = shBlur * (1f - i / (float) (n - 1));
            tmp.set(r.left - e * .6f, r.top + dy + shOff - e * .3f, r.right + e * .6f, r.bottom + dy + shOff + e);
            c.drawRoundRect(tmp, rad + e, rad + e, fill);
        }
        // key thickness
        fill.setColor(col[2]);
        tmp.set(r); tmp.offset(0, dy + side);
        c.drawRoundRect(tmp, rad, rad, fill);

        // face
        c.save();
        c.translate(0, dy);
        path.reset(); path.addRoundRect(r, rad, rad, Path.Direction.CW);
        c.save();
        c.clipPath(path);
        fill.setColor(alpha(Color.WHITE, th.dark ? .14f : .55f));
        c.drawRect(r, fill);                                         // top highlight edge
        fill.setShader(new LinearGradient(0, r.top, 0, r.bottom, col[0], col[1], Shader.TileMode.CLAMP));
        tmp.set(r); tmp.offset(0, .4f * u);
        c.drawRoundRect(tmp, rad, rad, fill);
        fill.setShader(new LinearGradient(0, r.bottom - 1.2f * u, 0, r.bottom, 0x00000000, 0x12000000, Shader.TileMode.CLAMP));
        c.drawRect(r.left, r.bottom - 1.2f * u, r.right, r.bottom, fill);
        fill.setShader(null);
        c.restore();

        drawLabel(c, k, col[3]);
        c.restore();
    }

    private void drawLabel(Canvas c, Key k, int color) {
        RectF r = k.r;
        float cx = r.centerX(), cy = r.centerY();
        if (k.k.equals("back")) {
            float s = Math.min(r.width(), r.height()) * .42f / 20f;
            path.reset();
            path.moveTo(cx + (5 - 10) * s, cy + (3 - 10) * s);
            path.lineTo(cx + (16 - 10) * s, cy);
            path.lineTo(cx + (5 - 10) * s, cy + (17 - 10) * s);
            path.close();
            fill.setColor(color); c.drawPath(path, fill);
            return;
        }
        if (k.k.equals("sqrt")) {
            float s = Math.min(r.width() * .42f / 24f, r.height() * .42f / 20f);
            float[][] pts = {{2, 11}, {5.5f, 9.5f}, {9.5f, 17.5f}, {14.5f, 2.5f}, {23, 2.5f}};
            path.reset();
            for (int i = 0; i < pts.length; i++) {
                float px = cx + (pts[i][0] - 12) * s, py = cy + (pts[i][1] - 10) * s;
                if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
            }
            stroke.setColor(color); stroke.setStrokeWidth(2.2f * s);
            c.drawPath(path, stroke);
            return;
        }
        if (k.k.equals("set")) {
            drawGear(c, cx, cy, Math.min(r.width(), r.height()) * .22f, color);
            return;
        }
        if (k.k.equals("onc")) {
            float s1 = 3.6f * u * fs, s2 = 3.3f * u * fs;
            setText(t700, s1, color, 0);
            text.setTextAlign(Paint.Align.CENTER);
            text.getTextBounds("ON", 0, 2, bounds);
            float h1 = bounds.height();
            setText(t700, s2, color, 0);
            text.setTextAlign(Paint.Align.CENTER);
            text.getTextBounds("C·CE", 0, 4, bounds);
            float h2 = bounds.height(), w2 = text.measureText("C·CE");
            float gap = .55f * u * fs, lh = .45f * u * fs;
            float top = cy - (h1 + gap + lh + gap + h2) / 2f;
            c.drawText("C·CE", cx, top + h1 + gap + lh + gap + h2, text);
            setText(t700, s1, color, 0);
            text.setTextAlign(Paint.Align.CENTER);
            c.drawText("ON", cx, top + h1, text);
            fill.setColor(color);
            c.drawRect(cx - w2 / 2f, top + h1 + gap, cx + w2 / 2f, top + h1 + gap + lh, fill);
            return;
        }
        Typeface tf = k.weight >= 700 ? t700 : k.weight >= 500 ? t500 : t400;
        setText(tf, k.sizeU * u * fs, color, k.weight >= 500 ? -0.02f : 0);
        text.setTextAlign(Paint.Align.CENTER);
        text.getTextBounds(k.label, 0, k.label.length(), bounds);
        float drop = k.k.equals("dot") ? .5f * u : 0;   // a centred dot reads as sitting high; nudge it down a little
        c.drawText(k.label, cx, cy - bounds.exactCenterY() + drop, text);
    }

    // ================================================================ touch
    private Key hit(float x, float y) {
        float g = 1.6f * u;
        for (Key k : keys) if (x >= k.r.left - g && x <= k.r.right + g && y >= k.r.top - g && y <= k.r.bottom + g) return k;
        return null;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
            int i = e.getActionIndex();
            float x = e.getX(i), y = e.getY(i);
            Key k = hit(x, y);
            if (k != null) {
                k.down = true;
                k.holdUntil = SystemClock.uptimeMillis() + 70;
                pointers.put(e.getPointerId(i), k);
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (k.k.equals("set")) { if (host != null) host.openSettings(); }
                else eng.press(k.k);
                invalidate();
            }
        } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_POINTER_UP) {
            int id = e.getPointerId(e.getActionIndex());
            Key k = pointers.get(id);
            if (k != null) { k.down = false; pointers.remove(id); invalidate(); }
        } else if (a == MotionEvent.ACTION_CANCEL) {
            for (Key k : keys) k.down = false;
            pointers.clear(); invalidate();
        }
        return true;
    }
}
