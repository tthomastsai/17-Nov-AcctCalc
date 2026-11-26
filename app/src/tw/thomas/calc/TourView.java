package tw.thomas.calc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/** First-run tour: dims the calculator, cuts a hole around one part at a time and explains it. */
public class TourView extends View {

    public interface Done { void done(); }

    private static final String[] TARGETS = {null, "indicators", "gt", "eq", "clear", "set"};
    static final String README_URL = "https://github.com/tthomastsai/17-Nov-AcctCalc#readme";

    private final CalcView calc;
    private final Theme th;
    private final Typeface t500, t700;
    private final Done onDone;
    private int step = 0;
    private boolean downNext, downLink;   // which button is being held, for the pressed look
    private Raised pill;                  // the Next button; kept so its press animates like a key

    @Override protected boolean verifyDrawable(android.graphics.drawable.Drawable d) { return d == pill || super.verifyDrawable(d); }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint title = new TextPaint(Paint.ANTI_ALIAS_FLAG), body = new TextPaint(Paint.ANTI_ALIAS_FLAG),
            small = new TextPaint(Paint.ANTI_ALIAS_FLAG), button = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF card = new RectF(), next = new RectF(), skip = new RectF(), tmp = new RectF();

    public TourView(Context c, CalcView calc, Theme th, Typeface t500, Typeface t700, Done onDone) {
        super(c);
        this.calc = calc; this.th = th; this.t500 = t500; this.t700 = t700; this.onDone = onDone;
        ring.setStyle(Paint.Style.STROKE);
        setClickable(true);
    }

    private float dp(float v) { return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()); }
    private float sp(float v) { return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics()); }

    private RectF target() {
        String t = TARGETS[step];
        if (t == null) return null;
        RectF r;
        if (t.equals("indicators")) r = calc.indicatorRect();
        else if (t.equals("clear")) { r = calc.keyRect("ac"); r.union(calc.keyRect("onc")); }
        else r = calc.keyRect(t);
        float e = calc.unit() * 1.6f;
        r.inset(-e, -e);
        return r;
    }

    private StaticLayout layout(String s, TextPaint p, int w) {
        return StaticLayout.Builder.obtain(s, 0, s.length(), p, w).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0, 1.15f).build();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        RectF t = target();
        float rad = calc.unit() * 3.6f;

        // dim everything except the target
        path.reset();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.addRect(0, 0, w, h, Path.Direction.CW);
        if (t != null) path.addRoundRect(t, rad, rad, Path.Direction.CW);
        fill.setColor(0x99000000);
        c.drawPath(path, fill);
        if (t != null) {
            ring.setColor(0xE6FFFFFF); ring.setStrokeWidth(dp(2));
            c.drawRoundRect(t, rad, rad, ring);
        }

        // card
        int text = th.fn[3];
        title.setTypeface(t700); title.setTextSize(sp(18)); title.setColor(text);
        body.setTypeface(t500); body.setTextSize(sp(15)); body.setColor(text);
        small.setTypeface(t500); small.setTextSize(sp(13)); small.setColor(th.sub);
        button.setTypeface(t700); button.setTextSize(sp(15));

        float margin = dp(16), pad = dp(20);
        float cw = Math.min(w - 2 * margin, dp(440));
        int inner = (int) (cw - 2 * pad);
        StaticLayout lt = layout(I18n.t("tour" + (step + 1) + "_t"), title, inner);
        StaticLayout lb = layout(I18n.t("tour" + (step + 1) + "_b"), body, inner);
        float btnH = dp(44);
        float ch = pad + lt.getHeight() + dp(8) + lb.getHeight() + dp(18) + btnH + pad;
        float cx = (w - cw) / 2f, cy;
        if (t == null) cy = (h - ch) / 2f;
        else if (t.centerY() < h / 2f) cy = Math.min(t.bottom + margin, h - ch - margin);
        else cy = Math.max(t.top - margin - ch, margin);
        card.set(cx, cy, cx + cw, cy + ch);
        float d = getResources().getDisplayMetrics().density;   // the card is raised like the keys and the settings cards
        Raised cardBg = new Raised(th.dark ? 0xFF2D2F33 : th.bezelTop, th.dark ? 0xFF27292D : Raised.mix(th.bezelTop, th.bezelBot, .55f),
                th.dark ? 0xFF18191C : th.bezelLine, th.dark, d, 22, 3, 4, 6);
        cardBg.setBounds(Math.round(card.left - cardBg.padL()), Math.round(card.top), Math.round(card.right + cardBg.padL()), Math.round(card.bottom + cardBg.padB()));
        cardBg.draw(c);

        c.save();
        c.translate(cx + pad, cy + pad);
        lt.draw(c);
        c.translate(0, lt.getHeight() + dp(8));
        lb.draw(c);
        c.restore();

        // buttons: skip (text), step counter, next / get started (pill)
        float by = card.bottom - pad - btnH;
        boolean last = step == TARGETS.length - 1;
        String nextLabel = I18n.t(last ? "tour_start" : "tour_next");
        float nw = button.measureText(nextLabel) + dp(36);
        next.set(card.right - pad - nw, by, card.right - pad, by + btnH);
        if (pill == null) { pill = new Raised(th.acc[0], th.acc[1], th.acc[2], th.dark, d, -1, 2.5f, 1.5f, 2.5f); pill.setCallback(this); }
        pill.press(downNext);
        pill.setBounds(Math.round(next.left - pill.padL()), Math.round(next.top), Math.round(next.right + pill.padL()), Math.round(next.bottom + pill.padB()));
        pill.draw(c);
        button.setColor(th.acc[3]);
        button.setTextAlign(Paint.Align.CENTER);
        c.drawText(nextLabel, next.centerX(), next.centerY() - (button.descent() + button.ascent()) / 2, button);

        String counter = (step + 1) + " / " + TARGETS.length;
        small.setTextAlign(Paint.Align.RIGHT);
        c.drawText(counter, next.left - dp(14), next.centerY() - (small.descent() + small.ascent()) / 2, small);

        String sk = I18n.t(last ? "tour_more" : "tour_skip");   // last step: a link to the README for more tips
        {
            button.setColor(last ? th.label : th.sub);
            button.setAlpha(downLink ? 120 : 255);
            button.setTextAlign(Paint.Align.LEFT);
            float sw = button.measureText(sk);
            skip.set(card.left + pad - dp(8), by, card.left + pad + sw + dp(8), by + btnH);
            c.drawText(sk, card.left + pad, next.centerY() - (button.descent() + button.ascent()) / 2, button);
            button.setAlpha(255);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            tmp.set(next); tmp.inset(-dp(6), -dp(6));
            downNext = tmp.contains(e.getX(), e.getY());
            downLink = !downNext && skip.contains(e.getX(), e.getY());
            invalidate();
        } else if (a == MotionEvent.ACTION_CANCEL) { downNext = downLink = false; invalidate(); }
        if (a == MotionEvent.ACTION_UP) {
            downNext = downLink = false; invalidate();
            float x = e.getX(), y = e.getY();
            tmp.set(next); tmp.inset(-dp(6), -dp(6));
            if (tmp.contains(x, y)) {
                if (step == TARGETS.length - 1) finish(); else { step++; invalidate(); }
            } else if (skip.contains(x, y)) {
                if (step == TARGETS.length - 1) {
                    try { getContext().startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(README_URL))); } catch (Exception ex) {}
                } else finish();
            }
        }
        return true;   // the calculator underneath does not react while the tour is open
    }

    private void finish() { if (onDone != null) onDone.done(); }

    /** for tests */
    void setStep(int s) { step = s; invalidate(); }
    static int steps() { return TARGETS.length; }
}
