package trainer;

import javafx.scene.Scene;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class Theme {

    private static final Logger LOG = Logger.getLogger(Theme.class.getName());

    public static final String DARK  = "dark";
    public static final String LIGHT = "light";

    private static final String BASE = "/css/base.css";

    private static String   current = DARK;
    private static Scene    scene;
    private static Runnable onChange;

    private Theme() {}

    public static Map<String, String> supported() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(DARK,  Lang.t("themeDark"));
        m.put(LIGHT, Lang.t("themeLight"));
        return m;
    }

    public static synchronized String code() { return current; }

    public static String title() {
        String n = supported().get(code());
        return n != null ? n : code();
    }

    public static String keyOf(String displayName) {
        for (Map.Entry<String, String> e : supported().entrySet())
            if (e.getValue().equals(displayName)) return e.getKey();
        return DARK;
    }

    public static synchronized void apply(Scene target) {
        scene = target;
        reapply();
    }

    public static synchronized void set(String themeCode) {
        String c = (LIGHT.equals(themeCode) || DARK.equals(themeCode)) ? themeCode : DARK;
        if (c.equals(current)) return;
        current = c;
        reapply();
        LOG.info("[Theme] Темата е сменена на: " + c);
        if (onChange != null) onChange.run();
    }

    public static synchronized void onChange(Runnable action) { onChange = action; }

    private static void reapply() {
        if (scene == null) return;
        List<String> sheets = stylesheets();
        if (!sheets.isEmpty()) scene.getStylesheets().setAll(sheets);
    }

    private static synchronized List<String> stylesheets() {
        String base = url(BASE);
        String tint = url("/css/" + current + ".css");
        if (base == null || tint == null) {
            LOG.warning("[Theme] Файловете с оформлението не са намерени — "
                      + "приложението ще се покаже с вида по подразбиране на JavaFX.");
            return List.of();
        }
        return List.of(base, tint);
    }

    private static String url(String resource) {
        var u = Theme.class.getResource(resource);
        return u != null ? u.toExternalForm() : null;
    }
}
