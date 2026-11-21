import tw.thomas.calc.CalcEngine;
import java.util.Random;

/**
 * Random key sequences (fixed seed, so failures can be reproduced): after every key the engine must
 * show a plain number of at most 12 digits, never NaN or Infinity, and must not throw.
 * Runs [count] short sequences (1–40 keys) and count/20 long ones (100–500 keys).
 * Usage: java EngineFuzz [count]
 */
public class EngineFuzz {
    static final String[] KEYS = {"0","1","2","3","4","5","6","7","8","9","00","dot","add","sub","mul","div","eq",
        "pct","sqrt","neg","back","mplus","mminus","mr","mc","gt","onc","ac"};

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 20000, bad = 0, nLong = n / 20;
        Random rnd = new Random(20261122L);
        for (int i = 0; i < n + nLong; i++) {
            CalcEngine e = new CalcEngine();
            StringBuilder seq = new StringBuilder();
            int len = i < n ? 1 + rnd.nextInt(40) : 100 + rnd.nextInt(401);
            try {
                for (int j = 0; j < len; j++) {
                    String k = KEYS[rnd.nextInt(KEYS.length)];
                    seq.append(k).append(' ');
                    e.press(k);
                    String problem = check(e);
                    if (problem != null) {
                        if (bad++ < 10) System.out.println("FAIL  " + seq + "->  " + problem);
                        break;
                    }
                }
            } catch (RuntimeException ex) {
                if (bad++ < 10) System.out.println("FAIL  " + seq + "->  " + ex);
            }
        }
        System.out.println(n + " short and " + nLong + " long random sequences, " + bad + " failed");
        if (bad > 0) System.exit(1);
    }

    static String check(CalcEngine e) {
        String s = e.entry;
        if (!s.matches("-?\\d+(\\.\\d*)?")) return "entry not a plain number: " + s;
        if (s.replace("-", "").replace(".", "").length() > CalcEngine.MAXD) return "more than 12 digits: " + s;
        for (double v : new double[]{e.mem, e.gt})
            if (Double.isNaN(v) || Double.isInfinite(v)) return "M or GT is " + v;
        String d = e.displayDigits();
        if (d.contains("NaN") || d.contains("Infinity")) return "display shows " + d;
        return null;
    }
}
