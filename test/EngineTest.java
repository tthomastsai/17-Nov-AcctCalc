import tw.thomas.calc.CalcEngine;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/** Runs every line of cases.txt against CalcEngine (pure Java, no Android needed). */
public class EngineTest {
    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(args.length > 0 ? args[0] : "test/cases.txt"), StandardCharsets.UTF_8);
        int n = 0, fail = 0;
        for (String raw : lines) {
            String line = raw.contains("#") ? raw.substring(0, raw.indexOf('#')) : raw;
            if (!line.contains("|")) continue;
            String[] p = line.split("\\|");
            String keys = p[0].trim(), want = p[1].trim();
            CalcEngine e = new CalcEngine();
            for (String k : keys.split("\\s+")) e.press(k);
            n++;
            if (!e.entry.equals(want)) { fail++; System.out.println("FAIL  " + keys + "  ->  " + e.entry + "  (want " + want + ")"); }
        }
        System.out.println(n + " cases, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
