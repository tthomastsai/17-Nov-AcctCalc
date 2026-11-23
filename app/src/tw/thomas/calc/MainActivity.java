package tw.thomas.calc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.FrameLayout;

public class MainActivity extends Activity implements CalcView.Host {
    static final float DEFAULT_ALIGN = 1f;   // bottom
    private static final int REQ_SETTINGS = 1;

    private SharedPreferences prefs;
    private CalcEngine eng;
    private CalcView view;
    private Typeface t500, t700;
    private FrameLayout stack;   // calculator, with the tour on top when it is showing
    private TourView tour;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("calc", MODE_PRIVATE);
        Lang.migrate(this, prefs);
        eng = new CalcEngine();
        eng.mem = parse(prefs.getString("mem", "0"));
        eng.gt = parse(prefs.getString("gt", "0"));
        eng.setListener(new CalcEngine.Listener() {
            @Override public void onRegistersChanged(double mem, double gt) {
                prefs.edit().putString("mem", Double.toString(mem)).putString("gt", Double.toString(gt)).apply();
            }
        });
        try {
            t500 = Typeface.createFromAsset(getAssets(), "fonts/GoogleSans-500.ttf");
            t700 = Typeface.createFromAsset(getAssets(), "fonts/GoogleSans-700.ttf");
        } catch (Exception e) { t500 = Typeface.DEFAULT; t700 = Typeface.DEFAULT_BOLD; }

        view = new CalcView(this, eng);
        view.setHost(this);
        stack = new FrameLayout(this);
        stack.addView(view, new FrameLayout.LayoutParams(-1, -1));
        setContentView(stack);
        applyPrefs();
        if (!prefs.getBoolean("tourDone", false)) {
            view.post(new Runnable() { @Override public void run() { startTour(); } });
        }
    }

    private static double parse(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }

    /** language, theme, key height and position from the saved settings */
    private void applyPrefs() {
        I18n.lang = Lang.effective(this, prefs);
        view.setKeyScale(prefs.getFloat("keyScale", 1f));
        view.setAlign(prefs.getFloat("align", DEFAULT_ALIGN));
        view.setSevenSegment("seven".equals(prefs.getString("lcdFont", "sans")));
        Theme t = Theme.current(this, prefs);
        view.setThemeColors(t);
        Theme.applyBars(this, t);
    }

    @Override protected void onResume() {
        super.onResume();
        applyPrefs();                                   // back from the settings page
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applyPrefs();                                   // dark mode or app language changed while open
        if (tour != null) tour.invalidate();
    }

    // ================================================================ settings page
    @Override public void openSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), REQ_SETTINGS);
        SettingsActivity.slideIn(this);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_SETTINGS && res == RESULT_OK && data != null && data.getBooleanExtra(SettingsActivity.EXTRA_TOUR, false)) {
            view.post(new Runnable() { @Override public void run() { startTour(); } });
        }
    }

    // ================================================================ first-run tour
    void startTour() {
        if (tour != null) stack.removeView(tour);
        tour = new TourView(this, view, view.getThemeColors(), t500, t700, new TourView.Done() {
            @Override public void done() {
                prefs.edit().putBoolean("tourDone", true).apply();
                stack.removeView(tour);
                tour = null;
            }
        });
        stack.addView(tour, new FrameLayout.LayoutParams(-1, -1));
    }
}
