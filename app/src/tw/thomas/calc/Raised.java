package tw.thomas.calc;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;

/**
 * The calculator keys' look as a Drawable, so the settings page can share it: a soft stacked shadow,
 * a thickness strip under the face, a vertical gradient face with a lighter lip along the top and,
 * in light themes, a hairline rim that fades out downwards.
 *
 * The face sits inside the bounds; room for the thickness and shadow is kept below and to the sides
 * (see {@link #padL()} etc.), so the drawable never paints outside its view.
 */
final class Raised extends ColorDrawable {   // ColorDrawable only as a concrete base; it draws nothing of its own
    final int top, bot, side; final boolean dark;
    final float radius, thick, lip, shOff, shBlur, shA, rim;
    float pressed;                               // 0..1, how far the face has sunk (animated)
    private boolean down;                        // held right now
    private long downAt, last;                   // press start, last frame (ms)
    boolean pressable = true;                    // false for a chosen option, which is a position, not a key
    boolean centred;                             // keep as much room above the face as below, so it centres on its view
    int iw = -1, ih = -1;                        // intrinsic size, for thumbs

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF f = new RectF(), tmp = new RectF();
    private final Path clip = new Path();

    /** @param d screen density (px per dp); radius &lt; 0 means fully round */
    Raised(int top, int bot, int side, boolean dark, float d, float radiusDp, float thickDp, float shOffDp, float shBlurDp) {
        this.top = top; this.bot = bot; this.side = side; this.dark = dark;
        radius = radiusDp < 0 ? -1 : radiusDp * d; thick = thickDp * d; lip = 1.6f * d;
        shOff = shOffDp * d; shBlur = shBlurDp * d; shA = dark ? .42f : .18f; rim = .9f * d;
        stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(rim);
    }

    int padL() { return Math.round(shBlur * .6f); }
    int padT() { return centred ? padB() : 0; }
    int padB() { return Math.round(thick + shOff + shBlur); }

    static int mix(int a, int b, float t) {
        return Color.rgb(Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
    private static int alpha(int c, float a) { return (Math.round(a * 255) << 24) | (c & 0xFFFFFF); }

    @Override public boolean getPadding(Rect p) { p.set(padL(), padT(), padL(), padB()); return true; }

    /** press or release, animated exactly like a calculator key (CalcView): a quick press, a soft creamy release */
    void press(boolean on) {
        if (on == down) return;
        down = on;
        if (on) downAt = android.os.SystemClock.uptimeMillis();
        invalidateSelf();
    }

    /** advance the press animation; true while it is still moving */
    private boolean step() {
        long now = android.os.SystemClock.uptimeMillis();
        float dt = last == 0 ? 16 : Math.max(0, Math.min(50, now - last));
        last = now;
        float target = (down || now < downAt + 70) ? 1f : 0f;      // a quick tap still shows, as on the keys
        float tau = target > pressed ? 18f : 75f;
        pressed += (target - pressed) * (1f - (float) Math.exp(-dt / tau));
        if (Math.abs(target - pressed) < 0.003f) pressed = target;
        boolean moving = pressed != target || now < downAt + 70;
        if (!moving) last = 0;
        return moving;
    }

    @Override public void draw(Canvas c) {
        boolean moving = step();
        Rect b = getBounds();
        // the same proportions as a pressed key: the face sinks 9/11 of the thickness, the side shrinks to about a fifth,
        // the shadow comes in and tightens
        float pr = pressed, dy = pr * thick * (.9f / 1.1f), th = thick * (1f - (.85f / 1.1f) * pr);
        float so = shOff * (1f - (1.1f / 1.6f) * pr), sb = shBlur * (1f - .5f * pr);
        f.set(b.left + padL(), b.top + padT(), b.right - padL(), b.bottom - padB());
        float r = radius < 0 ? Math.min(f.width(), f.height()) / 2 : Math.min(radius, Math.min(f.width(), f.height()) / 2);

        // soft shadow: stacked translucent layers, as on the keys
        fill.setShader(null);
        int n = 5; float a = 1f - (float) Math.pow(1f - shA * (1 - .18f * pr), 1f / n);
        fill.setColor(alpha(Color.BLACK, a));
        for (int i = 0; i < n; i++) {
            float e = sb * (1f - i / (float) (n - 1));
            tmp.set(f.left - e * .6f, f.top + dy + so - e * .3f, f.right + e * .6f, f.bottom + dy + th + so + e * .6f);
            c.drawRoundRect(tmp, r + e, r + e, fill);
        }
        // thickness, under the sunken face
        fill.setColor(side);
        tmp.set(f); tmp.offset(0, dy + th);
        c.drawRoundRect(tmp, r, r, fill);

        // face (sinks by dy when pressed)
        c.save();
        c.translate(0, dy);
        clip.reset(); clip.addRoundRect(f, r, r, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        fill.setColor(mix(top, Color.WHITE, dark ? .14f : .45f));   // lip: a lighter shade of the face
        c.drawRect(f, fill);
        fill.setShader(new LinearGradient(0, f.top, 0, f.bottom, top, bot, Shader.TileMode.CLAMP));
        tmp.set(f); tmp.offset(0, lip);
        c.drawRoundRect(tmp, r, r, fill);
        fill.setShader(null);
        c.restore();
        if (!dark) {                                                // hairline rim fading out downwards
            stroke.setShader(new LinearGradient(0, f.top, 0, f.top + Math.min(f.height(), 48 * rim), alpha(Color.BLACK, .10f), 0, Shader.TileMode.CLAMP));
            tmp.set(f.left, f.top, f.right, f.bottom + th); tmp.inset(rim / 2, rim / 2);
            c.drawRoundRect(tmp, r, r, stroke);
            stroke.setShader(null);
        }
        c.restore();
        if (moving) invalidateSelf();
    }

    /** a view that is pressed sinks its face, like a key */
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] state) {
        if (!pressable) return false;
        boolean p = false;
        for (int st : state) if (st == android.R.attr.state_pressed) p = true;
        if (p == down) return false;
        press(p); return true;
    }

    @Override public int getIntrinsicWidth() { return iw; }
    @Override public int getIntrinsicHeight() { return ih; }
    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter cf) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

/** A recessed groove (the opposite of {@link Raised}): for the tracks of the segmented control, switch and slider. */
final class Well extends ColorDrawable {
    final boolean dark; final float radius, d;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF f = new RectF();

    /** radius &lt; 0 means fully round */
    Well(boolean dark, float d, float radiusDp) { this.dark = dark; this.d = d; radius = radiusDp < 0 ? -1 : radiusDp * d; }

    @Override public void draw(Canvas c) {
        f.set(getBounds());
        float r = radius < 0 ? Math.min(f.width(), f.height()) / 2 : radius;
        p.setShader(null);
        if (!dark) { p.setColor(0xB3FFFFFF); c.drawRoundRect(f, r, r, p); }          // light catch along the lower lip
        f.bottom -= dark ? 0 : d;
        p.setColor(dark ? 0x40000000 : 0x14000000); c.drawRoundRect(f, r, r, p);
        p.setShader(new LinearGradient(0, f.top, 0, f.top + 5 * d, dark ? 0x59000000 : 0x24000000, 0, Shader.TileMode.CLAMP));
        c.drawRoundRect(f, r, r, p);                                                 // inner shadow under the top edge
        p.setShader(null);
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter cf) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
