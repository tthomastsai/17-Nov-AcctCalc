package tw.thomas.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Business-calculator logic matching the LB-5003CA function card:
 * chain calc, constant calc (K), add-on / discount %, M+ / M−, GT, overflow "E" with mantissa.
 * Pure Java (no Android imports) so it can be tested on a desktop JVM.
 *
 * Arithmetic is decimal (BigDecimal) and, as on the real calculator (and other exam-approved models
 * with the same logic, unlike Casio), every result is cut off — not rounded — to the 12 digits the
 * display holds, and that displayed value is what the next step works with.
 * So 1 ÷ 1.06 = = … (ten times) gives 0.55839477687, and 1 ÷ 3 × 3 = gives 0.99999999999.
 */
public class CalcEngine {
    public static final int MAXD = 12;
    private static final BigDecimal E12 = BigDecimal.TEN.pow(MAXD), HUNDRED = BigDecimal.valueOf(100);
    private static final MathContext WIDE = new MathContext(40, RoundingMode.DOWN);

    // entry state
    public String entry = "0";
    private BigDecimal acc = BigDecimal.ZERO;
    public String op = null;           // "add" "sub" "mul" "div"
    private boolean newEntry = true;
    private boolean typed = false;
    private String kOp = null;
    private BigDecimal kVal = BigDecimal.ZERO;
    public boolean err = false;
    public boolean on = true;
    private String lastKey = null;

    // persistent registers (doubles for storage; values never have more than 12 significant digits)
    public double mem = 0, gt = 0;

    public interface Listener { void onRegistersChanged(double mem, double gt); }
    private Listener listener;
    public void setListener(Listener l) { listener = l; }
    private void save() { if (listener != null) listener.onRegistersChanged(mem, gt); }

    private void fresh() {
        entry = "0"; acc = BigDecimal.ZERO; op = null;
        newEntry = true; typed = false; kOp = null; kVal = BigDecimal.ZERO; err = false; on = true;
    }

    // ---------------------------------------------------------------- formatting
    static final class F { final String str; final boolean err; F(String s, boolean e) { str = s; err = e; } }

    /** x cut off (towards zero) to the 12 digits the display can hold. */
    static BigDecimal cut(BigDecimal x) {
        BigDecimal a = x.abs();
        int intDigits = a.compareTo(BigDecimal.ONE) < 0 ? 1 : a.setScale(0, RoundingMode.DOWN).toPlainString().length();
        return x.setScale(Math.max(0, MAXD - intDigits), RoundingMode.DOWN);
    }

    /** null result = error (÷ 0) */
    static F fmt(BigDecimal x) {
        if (x == null) return new F("0", true);
        boolean e = false;
        if (x.abs().compareTo(E12) >= 0) {          // overflow: E with the leading 12 digits (decimal point shifted 12 places)
            e = true; x = x.divide(E12);
            if (x.abs().compareTo(E12) >= 0) return new F("0", true);
        }
        x = cut(x);
        String s = x.signum() == 0 ? "0" : x.stripTrailingZeros().toPlainString();
        return new F(s, e);
    }
    static F fmt(double x) { return Double.isNaN(x) || Double.isInfinite(x) ? new F("0", true) : fmt(BigDecimal.valueOf(x)); }

    private static BigDecimal dec(String s) {
        try { return new BigDecimal(s); } catch (NumberFormatException ex) { return BigDecimal.ZERO; }
    }
    private static BigDecimal dec(double d) { return BigDecimal.valueOf(d); }
    private BigDecimal cur() { return dec(entry); }   // the calculator works with what it shows
    /** register value after an addition: cut to 12 digits like any result (an overflow keeps the exact sum) */
    private static double reg(BigDecimal x) { return x.abs().compareTo(E12) >= 0 ? x.doubleValue() : cut(x).doubleValue(); }

    private void show(BigDecimal x) {
        F f = fmt(x);
        entry = f.str;
        if (f.err) err = true;
        newEntry = true;
    }

    private static BigDecimal calc(BigDecimal a, String op, BigDecimal b) {
        if ("add".equals(op)) return a.add(b);
        if ("sub".equals(op)) return a.subtract(b);
        if ("mul".equals(op)) return a.multiply(b);
        if ("div".equals(op)) return b.signum() == 0 ? null : a.divide(b, WIDE);
        return b;
    }

    /** √x cut to 12 digits: start from the double square root, then correct the last digit exactly */
    static BigDecimal sqrt(BigDecimal x) {
        BigDecimal r = cut(new BigDecimal(Math.sqrt(x.doubleValue()), WIDE));
        BigDecimal ulp = r.ulp();
        while (r.multiply(r).compareTo(x) > 0) r = r.subtract(ulp);
        while (r.add(ulp).multiply(r.add(ulp)).compareTo(x) <= 0) r = r.add(ulp);
        return r;
    }

    // ---------------------------------------------------------------- operations
    private void digit(char d) {
        if (newEntry) { entry = "0"; newEntry = false; }
        typed = true;
        boolean neg = entry.startsWith("-");
        String body = neg ? entry.substring(1) : entry;
        if (body.equals("0")) { entry = (neg ? "-" : "") + d; return; }
        if (body.replace(".", "").length() >= MAXD) return;
        entry += d;
    }

    private void dot() {
        if (newEntry) { entry = "0."; newEntry = false; typed = true; return; }
        typed = true;
        if (entry.indexOf('.') < 0 && entry.replace("-", "").replace(".", "").length() < MAXD) entry += ".";
    }

    private void operator(String o) {
        if (op != null && typed) {
            show(calc(acc, op, cur()));
            if (err) return;
        }
        acc = cur();
        op = o; newEntry = true; typed = false;
    }

    private void equals() { equals(true); }

    /** @param toGt false when the result is only being finished for M+ / M− (it must not go into GT) */
    private void equals(boolean toGt) {
        BigDecimal r; boolean counted = toGt;
        if (op != null) {
            BigDecimal a = acc, b = cur();
            if (op.equals("div") && !typed) r = a.signum() == 0 ? null : BigDecimal.ONE.divide(a, WIDE);   // 5 ÷ = -> 0.2
            else r = calc(a, op, b);
            // constant: × keeps the first number, + − ÷ keep the second
            if (op.equals("mul")) { kOp = "mul"; kVal = a; } else { kOp = op; kVal = op.equals("div") && !typed ? a : b; }
            op = null;
        } else if (kOp != null) {
            BigDecimal x = cur();
            r = kOp.equals("mul") ? kVal.multiply(x) : calc(x, kOp, kVal);
        } else { r = cur(); }                 // "5 =" with nothing pending still counts towards GT, as on the real calculator
        show(r);
        if (!err && counted) { gt = reg(dec(gt).add(cur())); save(); }
        acc = cur(); typed = false;
    }

    private void percent() {
        BigDecimal x = cur(), r;
        if (op != null) {
            BigDecimal a = acc, part = a.multiply(x).divide(HUNDRED);
            if (op.equals("mul")) r = part;                                      // 3000 × 20 % = 600
            else if (op.equals("div")) r = x.signum() == 0 ? null : a.multiply(HUNDRED).divide(x, WIDE);   // 20 ÷ 500 % = 4
            else if (op.equals("add")) r = a.add(part);                          // 3000 + 20 % = 3600
            else r = a.subtract(part);                                           // 3000 − 20 % = 2400
            op = null;
            show(r);
            if (!err) { gt = reg(dec(gt).add(cur())); save(); }
        } else {
            show(x.divide(HUNDRED));
        }
        acc = cur(); typed = false;
    }

    private void memAdd(int sign) {
        if (op != null && typed) equals(false);   // finish the pending calculation, but only = and % feed GT
        if (err) return;
        mem = reg(sign > 0 ? dec(mem).add(cur()) : dec(mem).subtract(cur())); save();
        newEntry = true; typed = false;
    }

    public void press(String k) {
        String prev = lastKey;
        lastKey = k;
        if (!on) { if (k.equals("onc")) fresh(); return; }
        if (k.equals("off")) { fresh(); on = false; return; }
        if (k.equals("ac")) { fresh(); gt = 0; mem = 0; save(); return; }   // AC clears GT and M too
        if (k.equals("onc")) {
            if (err) {                                  // CE on overflow: drop E, keep digits
                String keep = entry; fresh();
                entry = keep;
            } else if (!newEntry && typed) {            // CE
                entry = "0"; newEntry = true; typed = false;
            } else fresh();                             // C
            return;
        }
        if (err) return;

        if (k.length() == 1 && Character.isDigit(k.charAt(0))) { digit(k.charAt(0)); return; }
        switch (k) {
            case "00": digit('0'); digit('0'); break;
            case "dot": dot(); break;
            case "back":
                if (!newEntry) {
                    entry = entry.substring(0, entry.length() - 1);
                    if (entry.isEmpty() || entry.equals("-")) entry = "0";
                }
                break;
            case "neg":
                if (entry.startsWith("-")) entry = entry.substring(1);
                else if (cur().signum() != 0 || entry.indexOf('.') >= 0) entry = "-" + entry;
                typed = true;
                break;
            case "add": case "sub": case "mul": case "div": operator(k); break;
            case "eq": equals(); break;
            case "pct": percent(); break;
            case "sqrt": {
                BigDecimal x = cur();
                if (x.signum() < 0) { entry = "0"; err = true; }
                else show(sqrt(x));
                typed = true;
                break;
            }
            case "mplus": memAdd(1); break;
            case "mminus": memAdd(-1); break;
            case "mr": show(dec(mem)); typed = true; break;
            case "mc": mem = 0; save(); break;
            case "gt":
                if ("gt".equals(prev)) { gt = 0; save(); }   // consecutive GT presses clear the GT register; display stays
                else { show(dec(gt)); typed = true; }
                break;
        }
    }

    // ---------------------------------------------------------------- display helpers
    public boolean isNegative() { return entry.startsWith("-"); }

    /** Digits with ' separators and a trailing point on integers, e.g. 3'600.  (no sign) */
    public String displayDigits() {
        String t = isNegative() ? entry.substring(1) : entry;
        int dp = t.indexOf('.');
        String ip = dp < 0 ? t : t.substring(0, dp), fp = dp < 0 ? "" : t.substring(dp + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ip.length(); i++) {
            if (i > 0 && (ip.length() - i) % 3 == 0) sb.append('\'');
            sb.append(ip.charAt(i));
        }
        return sb.append('.').append(fp).toString();
    }

    public boolean isFull12() {
        String t = isNegative() ? entry.substring(1) : entry;
        return t.replace(".", "").length() >= MAXD;
    }
}
