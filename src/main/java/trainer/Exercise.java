package trainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Exercise {

    private static final java.util.logging.Logger LOG =
        java.util.logging.Logger.getLogger(Exercise.class.getName());

    public static final String FULL_MARKER = "FULL|";

    public static final List<String> SCALE =
        Arrays.asList("Νη", "Πα", "Βου", "Γα", "Δη", "Κε", "Ζω");

    private static final int TETRACHORD = 3;

    private static final Random RND = new Random();

    public record Task(String title, String glas, List<String> steps, String hint) {
        public String stepsAsText() { return String.join(" ", steps); }
    }

    public record Mode(String label, String finalSyllable, String isonSyllable,
                       int range, String ascending, String descending, String cadence,
                       String apechema) {}

    private Exercise() {}

    public static Task next(List<Map<String, String>> glasove) {
        List<Mode> modes = modes(glasove);
        Mode mode = modes.isEmpty() ? fallbackMode() : modes.get(RND.nextInt(modes.size()));

        return switch (RND.nextInt(6)) {
            case 0  -> anabasis(mode);
            case 1  -> katabasis(mode);
            case 2  -> trochos(mode);
            case 3  -> thirds(mode);
            case 4  -> withIson(mode);
            default -> fromNeumes(mode);
        };
    }

    public static List<Mode> modes(List<Map<String, String>> rows) {
        List<Mode> out = new ArrayList<>();
        if (rows == null) return out;
        for (Map<String, String> row : rows) {
            String fin = firstWord(row.get("final"));
            if (fin.isBlank()) continue;
            String label = value(row.get("label"), value(row.get("id"), Lang.t("modeWord")));
            out.add(new Mode(
                label,
                fin,
                value(row.get("ison"), fin),
                parseInt(row.get("range"), 8),
                value(row.get("asc"), ""),
                value(row.get("desc"), ""),
                value(row.get("cad"), ""),
                value(row.get("apih"), "")));
        }
        return out;
    }

    private static Mode fallbackMode() {
        return new Mode(Lang.t("exModeFallback"), "Πα", "Πα", 8, "", "", "", "");
    }

    private static String withApechema(Mode mode, String hint) {
        if (mode.apechema() == null || mode.apechema().isBlank()) return hint;
        return Lang.t("exApechema") + mode.apechema() + ". " + hint;
    }

    private static Task anabasis(Mode mode) {
        int start = index(mode.finalSyllable());
        int len   = Math.max(5, Math.min(mode.range(), 9));
        List<String> steps = new ArrayList<>();
        for (int i = 0; i < len; i++) steps.add(step(start + i));
        return new Task(Lang.t("exAnabasis"), mode.label(), steps,
                        withApechema(mode, Lang.t("exAnabasisHint")));
    }

    private static Task katabasis(Mode mode) {
        int start = index(mode.finalSyllable());
        int len   = Math.max(5, Math.min(mode.range(), 9));
        List<String> steps = new ArrayList<>();
        for (int i = len - 1; i >= 0; i--) steps.add(step(start + i));
        return new Task(Lang.t("exKatabasis"), mode.label(), steps,
                        withApechema(mode, Lang.t("exKatabasisHint")));
    }

    private static Task trochos(Mode mode) {
        int start  = index(mode.finalSyllable());
        int chains = 2 + RND.nextInt(2);
        List<String> steps = new ArrayList<>();
        steps.add(step(start));
        for (int chain = 0; chain < chains; chain++) {
            int base = start + chain * TETRACHORD;
            for (int i = 1; i <= TETRACHORD; i++) steps.add(step(base + i));
        }
        return new Task(Lang.t("exTrochos"), mode.label(), steps,
                        withApechema(mode, Lang.t("exTrochosHint")));
    }

    private static Task thirds(Mode mode) {
        int start = index(mode.finalSyllable());
        int pairs = 3 + RND.nextInt(2);
        List<String> steps = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            steps.add(step(start + i));
            steps.add(step(start + i + 2));
        }
        return new Task(Lang.t("exThirds"), mode.label(), steps,
                        withApechema(mode, Lang.t("exThirdsHint")));
    }

    private static Task withIson(Mode mode) {
        int ison = index(mode.isonSyllable());
        int len  = 3 + RND.nextInt(2);
        List<String> steps = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            steps.add(step(ison));
            steps.add(step(ison + 1 + RND.nextInt(Math.max(1, Math.min(mode.range() - 1, 5)))));
        }
        steps.add(step(ison));
        return new Task(Lang.t("exIson"), mode.label(), steps,
                        withApechema(mode, Lang.t("exHoldIson") + mode.isonSyllable()
                                         + Lang.t("exAlternate") + Lang.t("exEnterAll")));
    }

    private static Task fromNeumes(Mode mode) {
        boolean cadence = !mode.cadence().isBlank() && RND.nextBoolean();
        String pattern  = cadence ? mode.cadence() : mode.ascending();
        if (pattern.isBlank()) pattern = mode.cadence();
        if (pattern.isBlank()) return trochos(mode);

        List<String> names = neumeNames(pattern);
        Map<String, Integer> values = neumeValues();
        if (names.isEmpty() || values.isEmpty()) return trochos(mode);

        List<String> read = new ArrayList<>();
        int total = 0;
        for (String name : names) {
            Integer move = values.get(name);
            if (move == null) {
                LOG.warning("[Exercise] Знакът „" + name + "“ няма записано "
                          + "разстояние; оборотът на " + mode.label()
                          + " не се разчита. Виж :intervalValue в онтологията.");
                return trochos(mode);
            }
            read.add(name);
            total += move;
        }

        int position = cadence
            ? index(mode.finalSyllable()) - total
            : index(mode.finalSyllable());

        List<String> steps = new ArrayList<>();
        steps.add(step(position));
        for (String name : read) {
            position += values.get(name);
            steps.add(step(position));
        }

        String hint = withApechema(mode,
            (cadence ? Lang.t("exCadenceHint") : Lang.t("exMetrophonyHint"))
            + String.join(" – ", read));
        return new Task(cadence ? Lang.t("exCadence") : Lang.t("exMetrophony"),
                        mode.label(), steps, hint);
    }

    static List<String> neumeNames(String pattern) {
        List<String> out = new ArrayList<>();
        if (pattern == null) return out;
        String body = pattern;
        int bracket = body.indexOf('(');
        if (bracket >= 0) body = body.substring(0, bracket);
        for (String part : body.split("[–—-]")) {
            String name = part.trim();
            if (!name.isEmpty()) out.add(name);
        }
        return out;
    }

    static synchronized Map<String, Integer> neumeValues() {
        if (neumeCache != null) return neumeCache;

        Map<String, Integer> out = new LinkedHashMap<>();
        String q = Ontology.PREFIX +
            "SELECT ?n ?value WHERE { ?n a :BodyNeume ; :intervalValue ?value }";
        for (Map<String, String> r : Ontology.get().select(q)) {
            String iri = r.get("n"), value = r.get("value");
            if (iri == null || value == null) continue;
            String name = localName(iri);
            try {
                int steps = Integer.parseInt(value.trim());
                out.put(name, steps);
                if (name.equals("Ison_neume")) out.put("Ison", steps);
            } catch (NumberFormatException ignored) {
            }
        }
        neumeCache = out;
        return out;
    }

    private static Map<String, Integer> neumeCache;

    private static String localName(String iri) {
        int hash = iri.lastIndexOf('#');
        if (hash >= 0) return iri.substring(hash + 1);
        int slash = iri.lastIndexOf('/');
        return slash >= 0 ? iri.substring(slash + 1) : iri;
    }

    public static String check(String expectedRaw, String givenRaw) {
        List<String> expected = parse(expectedRaw);
        List<String> given    = parse(givenRaw);

        if (expected.isEmpty()) return "0/0;ERROR: " + Lang.t("exNoExpected");
        if (given.isEmpty())    return "0/" + expected.size() + ";" + Lang.t("exNothing");

        int hits = 0;
        int firstWrong = -1;
        for (int i = 0; i < expected.size(); i++) {
            if (i < given.size() && expected.get(i).trim().equalsIgnoreCase(given.get(i).trim())) {
                hits++;
            } else if (firstWrong < 0) {
                firstWrong = i;
            }
        }

        String message;
        if (hits == expected.size() && given.size() == expected.size()) {
            message = FULL_MARKER + Lang.t("exMatched");
        } else if (given.size() != expected.size()) {
            message = Lang.t("exWereExpected") + expected.size() + Lang.t("exCountMismatch")
                    + given.size() + ". " + Lang.t("exFirstDiff")
                    + (firstWrong < 0 ? expected.size() + 1 : firstWrong + 1) + ".";
        } else {
            message = Lang.t("exFirstDiff") + (firstWrong + 1)
                    + Lang.t("exExpectedWord") + expected.get(firstWrong)
                    + Lang.t("exButEntered") + given.get(firstWrong) + ".";
        }
        return hits + "/" + expected.size() + ";" + message;
    }

    public static String encode(Task task) {
        return task.title() + "|" + task.glas() + "|"
             + task.stepsAsText() + "|" + task.hint();
    }

    public static Task decode(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("\\|", -1);
        if (parts.length < 4) return null;
        return new Task(parts[0], parts[1], parse(parts[2]), parts[3]);
    }

    private static String step(int index) {
        return SCALE.get(Math.floorMod(index, SCALE.size()));
    }

    private static int index(String syllable) {
        int i = SCALE.indexOf(syllable);
        return i >= 0 ? i : 0;
    }

    public static List<String> parse(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String part : raw.trim().split("[\\s,;.\\-–—]+")) {
            String s = part.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static String value(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    private static String firstWord(String v) {
        if (v == null || v.isBlank()) return "";
        String s = v.trim();
        int space = s.indexOf(' ');
        return space > 0 ? s.substring(0, space) : s;
    }

    private static int parseInt(String v, int fallback) {
        if (v == null) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
