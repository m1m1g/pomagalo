package trainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Composer {

    public static final List<String> SCALE =
        List.of("Νη", "Πα", "Βου", "Γα", "Δη", "Κε", "Ζω");

    private final Random rnd = new Random();

    public record Mode(int number, String greek, String label, String finalSyllable,
                       String isonSyllable, String genus, String character,
                       String ascending, String descending, String cadence, int range) {}

    public record Style(String key, String label, String description,
                        int minNotes, int maxNotes) {}

    public record Tradition(String key, String label, String description) {}

    public record Chant(Mode mode, Style style, Tradition tradition, String title,
                        List<List<String>> groups, List<String> syllables,
                        String ison, String notes) {}

    public List<Mode> modes() {
        String q = Ontology.PREFIX +
            "SELECT ?num ?greek ?final ?genus ?label ?char ?asc ?desc ?cad ?range ?isonSyl WHERE {\n" +
            "  ?m :modeNumber ?num ; :finalTone ?final ; :greekName ?greek .\n" +
            "  OPTIONAL { ?m :scaleType ?genus }\n" +
            "  OPTIONAL { ?m :character ?char }\n" +
            "  OPTIONAL { ?m :ascendingPattern ?asc }\n" +
            "  OPTIONAL { ?m :descendingPattern ?desc }\n" +
            "  OPTIONAL { ?m :cadencePattern ?cad }\n" +
            "  OPTIONAL { ?m :melodicRangeSteps ?range }\n" +
            Lang.labelBlock("?m", "?label") +
            "  OPTIONAL { ?m :hasIson ?step . ?step :paralagiasSyllable ?isonSyl }\n" +
            "} ORDER BY ?num";

        List<Mode> out = new ArrayList<>();
        for (Map<String, String> r : Ontology.get().select(q)) {
            int num = parseInt(r.get("num"), 0);
            if (num < 1) continue;
            String fin = firstWord(r.get("final"));
            out.add(new Mode(
                num,
                or(r.get("greek"), ""),
                or(r.get("label"), num + ". " + Lang.t("modeWord")),
                fin,
                or(r.get("isonSyl"), fin),
                or(r.get("genus"), "diatonic"),
                or(r.get("char"), ""),
                or(r.get("asc"), ""),
                or(r.get("desc"), ""),
                or(r.get("cad"), ""),
                parseInt(r.get("range"), 8)));
        }
        return out;
    }

    public List<Style> styles() {
        String q = Ontology.PREFIX +
            "SELECT ?key ?label ?desc ?min ?max WHERE {\n" +
            "  ?s a :ChantStyle ; :styleKey ?key ;\n" +
            "     :notesPerSyllableMin ?min ; :notesPerSyllableMax ?max .\n" +
            Lang.labelBlock("?s", "?label") +
            Lang.textBlock("?s", ":description", "?desc") +
            "} ORDER BY ?min ?key";

        List<Style> out = new ArrayList<>();
        for (Map<String, String> r : Ontology.get().select(q)) {
            String key = r.get("key");
            if (key == null || key.isBlank()) continue;
            out.add(new Style(key.trim(),
                              or(r.get("label"), key),
                              or(r.get("desc"), ""),
                              parseInt(r.get("min"), 1),
                              parseInt(r.get("max"), 1)));
        }
        return out;
    }

    public List<Tradition> traditions() {
        String q = Ontology.PREFIX +
            "SELECT ?key ?label ?desc WHERE {\n" +
            "  ?t :traditionKey ?key .\n" +
            Lang.labelBlock("?t", "?label") +
            Lang.textBlock("?t", ":description", "?desc") +
            "} ORDER BY ?key";

        List<Tradition> out = new ArrayList<>();
        for (Map<String, String> r : Ontology.get().select(q)) {
            String key = r.get("key");
            if (key == null || key.isBlank()) continue;
            out.add(new Tradition(key.trim(), or(r.get("label"), key), or(r.get("desc"), "")));
        }
        return out;
    }

    public Chant compose(Mode mode, Style style, Tradition tradition,
                         String incipit, int length) {
        int n = Math.max(6, Math.min(length, 64));
        int finalIdx = SCALE.indexOf(mode.finalSyllable());
        if (finalIdx < 0) finalIdx = 0;

        int span = Math.max(3, Math.min(mode.range(), SCALE.size()));
        int low  = Math.max(0, finalIdx - span / 2);
        int high = Math.min(SCALE.size() - 1, low + span - 1);
        low = Math.max(0, high - span + 1);

        List<String> out = new ArrayList<>();
        int pos = finalIdx;
        out.add(SCALE.get(pos));

        for (int i = 1; i < n - 3; i++) {
            int step;
            int roll = rnd.nextInt(10);
            if (roll < 5)      step = rnd.nextBoolean() ? 1 : -1;
            else if (roll < 7) step = 0;
            else               step = rnd.nextBoolean() ? 2 : -2;
            int next = pos + step;
            if (next < low || next > high) next = pos - step;
            pos = Math.max(low, Math.min(high, next));
            out.add(SCALE.get(pos));
        }

        int above = Math.min(high, finalIdx + 1);
        int below = Math.max(low, finalIdx - 1);
        out.add(SCALE.get(above != finalIdx ? above : below));
        out.add(SCALE.get(below != finalIdx ? below : above));
        out.add(SCALE.get(finalIdx));

        List<List<String>> groups = new ArrayList<>();
        List<String> flat = new ArrayList<>();
        for (String structural : out) {
            List<String> group = melisma(structural, style, low, high);
            groups.add(Collections.unmodifiableList(group));
            flat.addAll(group);
        }

        String notes = buildNotes(mode, style, tradition);
        String head  = (incipit == null || incipit.isBlank())
            ? mode.label()
            : incipit.trim() + " — " + mode.label();
        String title = head + Lang.t("cmChantOf") + out.size() + Lang.t("cmSyllables");

        return new Chant(mode, style, tradition, title,
                         Collections.unmodifiableList(groups),
                         Collections.unmodifiableList(flat),
                         mode.isonSyllable(), notes);
    }

    private List<String> melisma(String structural, Style style, int low, int high) {
        List<String> group = new ArrayList<>();
        group.add(structural);
        if (style == null) return group;

        int min = Math.max(1, style.minNotes());
        int max = Math.max(min, style.maxNotes());
        int count = (max > min) ? min + rnd.nextInt(max - min + 1) : min;
        if (count <= 1) return group;

        int base = SCALE.indexOf(structural);
        if (base < 0 || low >= high) return group;

        int pos = base;
        for (int i = 1; i < count; i++) {
            if (i == count - 1 && count > 2) pos = base;
            else                             pos = neighbourOf(base, pos, low, high);
            group.add(SCALE.get(pos));
        }
        return group;
    }

    private int neighbourOf(int base, int current, int low, int high) {
        List<Integer> near = new ArrayList<>();
        List<Integer> far  = new ArrayList<>();
        for (int distance = -2; distance <= 2; distance++) {
            if (distance == 0) continue;
            int candidate = base + distance;
            if (candidate < low || candidate > high || candidate == current) continue;
            if (Math.abs(distance) == 1) near.add(candidate);
            else                         far.add(candidate);
        }
        if (!near.isEmpty()) return near.get(rnd.nextInt(near.size()));
        if (!far.isEmpty())  return far.get(rnd.nextInt(far.size()));
        return current;
    }

    private String buildNotes(Mode m, Style style, Tradition tradition) {
        StringBuilder sb = new StringBuilder();
        sb.append(Lang.t("cmFinal")).append(m.finalSyllable())
          .append("      ").append(Lang.t("cmIson")).append(m.isonSyllable())
          .append("      ").append(Lang.t("cmGenus")).append(genusName(m.genus()));
        if (tradition != null && !tradition.label().isBlank()) {
            sb.append("\n").append(Lang.t("cmTradition")).append(": ").append(tradition.label());
            if (!tradition.description().isBlank())
                sb.append("\n").append(tradition.description());
        }
        if (style != null && !style.label().isBlank()) {
            sb.append("\n").append(Lang.t("cmStyle")).append(": ").append(style.label())
              .append("   ").append(Lang.t("cmMelisma"))
              .append(style.minNotes() == style.maxNotes()
                      ? String.valueOf(style.minNotes())
                      : style.minNotes() + "–" + style.maxNotes())
              .append(Lang.t("cmTonesPerSyllable"));
            if (!style.description().isBlank())
                sb.append("\n").append(style.description());
        }
        if (!m.ascending().isBlank())
            sb.append("\n").append(Lang.t("cmAscPattern")).append(m.ascending());
        if (!m.descending().isBlank())
            sb.append("\n").append(Lang.t("cmDescPattern")).append(m.descending());
        if (!m.cadence().isBlank())
            sb.append("\n").append(Lang.t("cmCadence")).append(m.cadence());
        if (!m.character().isBlank())
            sb.append("\n").append(Lang.t("cmCharacter")).append(m.character());
        return sb.toString();
    }

    private static String genusName(String genus) {
        if (genus == null) return "";
        return switch (genus.trim().toLowerCase()) {
            case "diatonic"        -> Lang.t("genDiatonic");
            case "soft chromatic"  -> Lang.t("genSoftChromatic");
            case "hard chromatic"  -> Lang.t("genHardChromatic");
            case "enharmonic"      -> Lang.t("genEnharmonic");
            default                -> genus;
        };
    }

    public static String encode(Chant c) {
        return c.title() + "\n" + flow(c) + "\n" + c.notes();
    }

    public static String flow(Chant c) {
        StringBuilder sb = new StringBuilder();
        for (List<String> group : c.groups()) {
            if (sb.length() > 0) sb.append("   ");
            sb.append(String.join("-", group));
        }
        return sb.toString();
    }

    private static String or(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    private static String firstWord(String v) {
        if (v == null || v.isBlank()) return "Νη";
        String s = v.trim();
        int sp = s.indexOf(' ');
        return sp > 0 ? s.substring(0, sp) : s;
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
