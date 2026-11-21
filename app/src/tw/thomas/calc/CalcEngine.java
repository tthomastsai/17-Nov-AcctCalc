package tw.thomas.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Business-calculator logic matching the LB-5003CA function card:
 * chain calc, constant calc (K), add-on / discount %, M+ / M−, GT, overflow "E" with mantissa.
 * Pure Java (no Android imports) so it can be tested on a desktop JVM.
 */
public class CalcEngine {
    public static final int MAXD = 12;

    // entry state
    public String entry = "0";
    private boolean hasFull = false;   // full-precision value of a computed result
    private double full = 0;
    private double acc = 0;
    public String op = null;           // "add" "sub" "mul" "div"
    private boolean newEntry = true;
    private boolean typed = false;
    private String kOp = null;
    private double kVal = 0;
    public boolean err = false;
    public boolean on = true;
    private String lastKey = null;

    // persistent registers
    public double mem = 0, gt = 0;

    public interface Listener { void onRegistersChanged(double mem, double gt); }
    private Listener listener;
    public void setListener(Listener l) { listener = l; }
    private void save() { if (listener != null) listener.onRegistersChanged(mem, gt); }

    private void fresh() {
        entry = "0"; hasFull = false; full = 0; acc = 0; op = null;
        newEntry = true; typed = false; kOp = null; kVal = 0; err = false; on = true;
    }

    // ---------------------------------------------------------------- formatting
    static final class F { final String str; final boolean err; F(String s, boolean e) { str = s; err = e; } }

    static F fmt(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return new F("0", true);
        boolean e = false;
        if (Math.abs(x) >= 1e12) {
            e = true; x = x / 1e12;
            if (Math.abs(x) >= 1e12) return new F("0", true);
        }
        double a = Math.abs(x);
        int intDigits = a < 1 ? 1 : String.valueOf((long) Math.floor(a)).length();
        String s = BigDecimal.valueOf(x).setScale(Math.max(0, MAXD - intDigits), RoundingMode.HALF_UP).toPlainString();
        if (s.indexOf('.') >= 0) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        if (s.replace("-", "").replace(".", "").length() > MAXD) {   // rounding carried into a 13th digit
            if (e) return new F("0", true);
            return fmt(Double.parseDouble(s));
        }
        if (s.matches("-0(\\.0*)?")) s = "0";
        return new F(s, e);
    }

    private double parse(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException ex) { return 0; }
    }
    private double cur() { return hasFull ? full : parse(entry); }
    private static double round(double x) { F f = fmt(x); return f.err ? x : Double.parseDouble(f.str); }

    private void show(double x) {
        F f = fmt(x);
        entry = f.str;
        hasFull = !f.err;
        full = x;
        if (f.err) err = true;
        newEntry = true;
    }

    private static double calc(double a, String op, double b) {
        if ("add".equals(op)) return a + b;
        if ("sub".equals(op)) return a - b;
        if ("mul".equals(op)) return a * b;
        if ("div".equals(op)) return b == 0 ? Double.NaN : a / b;
        return b;
    }

    // ---------------------------------------------------------------- operations
    private void digit(char d) {
        hasFull = false;
        if (newEntry) { entry = "0"; newEntry = false; }
        typed = true;
        boolean neg = entry.startsWith("-");
        String body = neg ? entry.substring(1) : entry;
        if (body.equals("0")) { entry = (neg ? "-" : "") + d; return; }
        if (body.replace(".", "").length() >= MAXD) return;
        entry += d;
    }

    private void dot() {
        hasFull = false;
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
        double r; boolean counted = toGt;
        if (op != null) {
            double a = acc, b = cur();
            if (op.equals("div") && !typed) r = a == 0 ? Double.NaN : 1 / a;   // 5 ÷ = -> 0.2
            else r = calc(a, op, b);
            // constant: × keeps the first number, + − ÷ keep the second
            if (op.equals("mul")) { kOp = "mul"; kVal = a; } else { kOp = op; kVal = op.equals("div") && !typed ? a : b; }
            op = null;
        } else if (kOp != null) {
            double x = cur();
            r = kOp.equals("mul") ? kVal * x : calc(x, kOp, kVal);
        } else { r = cur(); }                 // "5 =" with nothing pending still counts towards GT, as on the real calculator
        show(r);
        if (!err && counted) { gt = round(gt + cur()); save(); }
        acc = cur(); typed = false;
    }

    private void percent() {
        double x = cur(), r;
        if (op != null) {
            double a = acc;
            if (op.equals("mul")) r = a * x / 100;           // 3000 × 20 % = 600
            else if (op.equals("div")) r = x == 0 ? Double.NaN : a / x * 100;   // 20 ÷ 500 % = 4
            else if (op.equals("add")) r = a + a * x / 100;  // 3000 + 20 % = 3600
            else r = a - a * x / 100;                        // 3000 − 20 % = 2400
            op = null;
            show(r);
            if (!err) { gt = round(gt + cur()); save(); }
        } else {
            show(x / 100);
        }
        acc = cur(); typed = false;
    }

    private void memAdd(int sign) {
        if (op != null && typed) equals(false);   // finish the pending calculation, but only = and % feed GT
        if (err) return;
        mem = round(mem + sign * cur()); save();
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
                entry = keep; hasFull = true; full = parse(keep);
            } else if (!newEntry && typed) {            // CE
                entry = "0"; hasFull = false; newEntry = true; typed = false;
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
                if (hasFull) full = -full;
                if (entry.startsWith("-")) entry = entry.substring(1);
                else if (cur() != 0 || entry.indexOf('.') >= 0) entry = "-" + entry;
                typed = true;
                break;
            case "add": case "sub": case "mul": case "div": operator(k); break;
            case "eq": equals(); break;
            case "pct": percent(); break;
            case "sqrt": {
                double x = cur();
                if (x < 0) { entry = "0"; hasFull = false; err = true; }
                else show(Math.sqrt(x));
                typed = true;
                break;
            }
            case "mplus": memAdd(1); break;
            case "mminus": memAdd(-1); break;
            case "mr": show(mem); typed = true; break;
            case "mc": mem = 0; save(); break;
            case "gt":
                if ("gt".equals(prev)) { gt = 0; save(); }   // consecutive GT presses clear the GT register; display stays
                else { show(gt); typed = true; }
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
