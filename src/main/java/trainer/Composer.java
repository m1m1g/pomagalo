package trainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Composer {

    public static final List<String> SCALE =
        List.of("Νη", "Πα", "Βου", "Γα", "Δη", "Κε", "Ζω");

    private final Random rnd = new Random();

    /**
     * Един глас, както го описва онтологията.
     *
     * <p>Първите единадесет полета са, каквито са били. Последните
     * четири са новото: апихимата на гласа и трите оборота, разложени
     * на разстояния. Разстоянията не се получават с разбор на текст, а
     * се четат от подредените стъпки в онтологията — всяка стъпка сочи
     * към индивида на невмата си, а от него се взема
     * {@code :intervalValue}. Изписаните обороти
     * ({@code ascending}, {@code descending}, {@code cadence}) остават,
     * защото те се показват на четеца, но вече не са източникът на
     * числата.</p>
     */
    public record Mode(int number, String greek, String label, String finalSyllable,
                       String isonSyllable, String genus, String character,
                       String ascending, String descending, String cadence, int range,
                       String apechema, List<Integer> cadenceSteps,
                       List<Integer> ascendingSteps, List<Integer> descendingSteps) {}

    public record Style(String key, String label, String description,
                        int minNotes, int maxNotes) {}

    public record Chant(Mode mode, Style style, String title,
                        List<List<String>> groups, List<String> syllables,
                        String ison, String notes) {}

    public List<Mode> modes() {
        String q = Ontology.PREFIX +
            "SELECT ?num ?greek ?final ?genus ?label ?char ?asc ?desc ?cad ?range ?isonSyl\n" +
            "       ?apSyl ?cadForm ?ascForm ?descForm WHERE {\n" +
            "  ?m :modeNumber ?num ; :finalTone ?final ; :greekName ?greek .\n" +
            "  OPTIONAL { ?m :scaleType ?genus }\n" +
            "  OPTIONAL { ?m :character ?char }\n" +
            "  OPTIONAL { ?m :ascendingPattern ?asc }\n" +
            "  OPTIONAL { ?m :descendingPattern ?desc }\n" +
            "  OPTIONAL { ?m :cadencePattern ?cad }\n" +
            "  OPTIONAL { ?m :melodicRangeSteps ?range }\n" +
            Lang.labelBlock("?m", "?label") +
            "  OPTIONAL { ?m :hasIson ?step . ?step :paralagiasSyllable ?isonSyl }\n" +
            "  OPTIONAL { ?m :hasApechema ?ap . ?ap :apechemaSyllables ?apSyl }\n" +
            "  OPTIONAL { ?m :hasCadentialFormula ?cadForm }\n" +
            "  OPTIONAL { ?m :hasAscendingFormula ?ascForm }\n" +
            "  OPTIONAL { ?m :hasDescendingFormula ?descForm }\n" +
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
                or(r.get("genus"), ""),
                or(r.get("char"), ""),
                or(r.get("asc"), ""),
                or(r.get("desc"), ""),
                or(r.get("cad"), ""),
                parseInt(r.get("range"), 8),
                or(r.get("apSyl"), ""),
                stapkiNaOborot(r.get("cadForm"),  r.get("cad")),
                stapkiNaOborot(r.get("ascForm"),  r.get("asc")),
                stapkiNaOborot(r.get("descForm"), r.get("desc"))));
        }
        return out;
    }

    /**
     * Разстоянията на един оборот, четени от онтологията.
     *
     * <p>Първо се опитва подредената верига от стъпки: оборотът сочи към
     * своите {@code :PatternStep}, всяка от които има място
     * ({@code :stepPosition}) и невма ({@code :stepNeume}). Това е
     * същинският запис — питането „с коя невма започва кадансът на трети
     * глас“ е заявка, а не разбор на низ.</p>
     *
     * <p>Ако глас още няма разложен оборот, се пада на изписания текст,
     * за да не остане приложението без данни. Тъй нито един глас не
     * зависи от това дали разлагането е стигнало до него.</p>
     *
     * @param oborot  името на индивида на оборота, ако го има
     * @param izpisan изписаният оборот, за резервен разбор
     */
    private List<Integer> stapkiNaOborot(String oborot, String izpisan) {
        if (Ontology.godnoIme(oborot)) {
            String q = Ontology.PREFIX +
                "SELECT ?pos ?neume WHERE {\n" +
                "  :" + oborot + " :hasStep ?stapka .\n" +
                "  ?stapka :stepPosition ?pos ; :stepNeume ?neume .\n" +
                "} ORDER BY ?pos";

            List<Integer> ot_ontologiyata = new ArrayList<>();
            boolean tsyalo = true;
            for (Map<String, String> r : Ontology.get().select(q)) {
                Integer v = nevmi().get(r.get("neume"));
                if (v == null) { tsyalo = false; break; }
                ot_ontologiyata.add(v);
            }
            if (tsyalo && !ot_ontologiyata.isEmpty())
                return Collections.unmodifiableList(ot_ontologiyata);
        }
        return Collections.unmodifiableList(stapkiOt(izpisan));
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

    public Chant compose(Mode mode, Style style,
                         String incipit, int length) {
        int n = Math.max(6, Math.min(length, 64));
        int finalIdx = SCALE.indexOf(mode.finalSyllable());
        if (finalIdx < 0) finalIdx = 0;

        int span = Math.max(3, Math.min(mode.range(), SCALE.size()));
        int low  = Math.max(0, finalIdx - span / 2);
        int high = Math.min(SCALE.size() - 1, low + span - 1);
        low = Math.max(0, high - span + 1);

        // Разстоянията идват готови от онтологията, разложени на стъпки.
        // Разборът на изписания оборот остана само за резервен случай и
        // се прави още при прочитането на гласовете.
        List<Integer> kadans  = mode.cadenceSteps();
        List<Integer> nagore  = mode.ascendingSteps();
        List<Integer> nadolu  = mode.descendingSteps();

        // Къде трябва да свърши тялото, за да падне кадансът точно на устоя.
        //
        // Всяка невма казва разстояние спрямо предната сричка, тъй че
        // последната сричка на каданса е началото му плюс сбора на
        // разстоянията. Щом тя трябва да е устоят, началото е устоят минус
        // сбора. За трети глас кадансът е Апострофос–Елафрон–Исон, сбор −3,
        // значи тялото свършва три степени над устоя.
        int sbor = 0;
        for (int s : kadans) sbor += s;
        int krai = Math.max(low, Math.min(high, finalIdx - sbor));

        int tyalo = Math.max(1, n - kadans.size());

        List<String> out = new ArrayList<>();
        int pos = finalIdx;
        out.add(SCALE.get(pos));

        int sredata = (low + high) / 2;
        int stapka = 0;
        for (int i = 1; i < tyalo; i++) {
            int ostavat = tyalo - 1 - i;
            int razstoyanie = Math.abs(krai - pos);

            if (ostavat == 0) {
                // Последната сричка на тялото е там, откъдето тръгва кадансът.
                pos = krai;
            } else if (ostavat <= razstoyanie) {
                // Прибиране: по една степен на сричка, за да се стигне точно.
                pos += Integer.signum(krai - pos);
            } else {
                // Свободният ход върви по оборотите от онтологията, а не по
                // измислена вероятност. Посоката се решава от мястото в
                // обхвата: под средата се качва по възходящия оборот, над
                // нея слиза по низходящия.
                List<Integer> oborot = (pos < sredata) ? nagore
                                     : (pos > sredata) ? nadolu
                                     : (rnd.nextBoolean() ? nagore : nadolu);
                int hod = oborot.isEmpty()
                    ? (rnd.nextBoolean() ? 1 : -1)
                    : oborot.get(stapka++ % oborot.size());
                int sled = pos + hod;
                if (sled < low || sled > high) sled = pos - hod;
                pos = Math.max(low, Math.min(high, sled));
            }
            out.add(SCALE.get(pos));
        }

        for (int s : kadans) {
            pos = Math.max(low, Math.min(high, pos + s));
            out.add(SCALE.get(pos));
        }

        List<List<String>> groups = new ArrayList<>();
        List<String> flat = new ArrayList<>();
        for (String structural : out) {
            List<String> group = melisma(structural, style, low, high);
            groups.add(Collections.unmodifiableList(group));
            flat.addAll(group);
        }

        String notes = buildNotes(mode, style);
        String head  = (incipit == null || incipit.isBlank())
            ? mode.label()
            : incipit.trim() + " — " + mode.label();
        String title = head + Lang.t("cmChantOf") + out.size() + Lang.t("cmSyllables");

        return new Chant(mode, style, title,
                         Collections.unmodifiableList(groups),
                         Collections.unmodifiableList(flat),
                         mode.isonSyllable(), notes);
    }

    private Map<String, Integer> nevmi;

    private synchronized Map<String, Integer> nevmi() {
        if (nevmi != null) return nevmi;
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map<String, String> r : Ontology.get().select(Ontology.PREFIX +
                "SELECT ?n ?value WHERE { ?n a :BodyNeume ; :intervalValue ?value }")) {
            String ime = r.get("n");
            if (ime == null || ime.isBlank()) continue;
            int v = parseInt(r.get("value"), 0);
            out.put(ime, v);
            if (ime.equals("Ison_neume")) out.put("Ison", v);
        }
        nevmi = out;
        return out;
    }

    private List<Integer> stapkiOt(String oborot) {
        List<Integer> out = new ArrayList<>();
        if (oborot == null || oborot.isBlank()) return out;
        for (String parche : oborot.replaceAll("\\(.*?\\)", "").split("[–—-]")) {
            String ime = parche.trim();
            if (ime.isEmpty()) continue;
            Integer v = nevmi().get(ime);
            if (v == null) return new ArrayList<>();
            out.add(v);
        }
        return out;
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

    private String buildNotes(Mode m, Style style) {
        StringBuilder sb = new StringBuilder();
        sb.append(Lang.t("cmFinal")).append(m.finalSyllable())
          .append("      ").append(Lang.t("cmIson")).append(m.isonSyllable());
        if (!m.genus().isBlank())
            sb.append("      ").append(Lang.t("cmGenus")).append(m.genus());
        if (!m.apechema().isBlank())
            sb.append("\n").append(Lang.t("cmApechema")).append(m.apechema());
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
