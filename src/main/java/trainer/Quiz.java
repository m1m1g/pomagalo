package trainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class Quiz {

    public record Question(String text, List<String> options, int correct, String note) {
        public String correctAnswer() { return options.get(correct); }
    }

    private static final Random RND = new Random();

    private Quiz() {}

    public static Question next() {
        return question(glasove(), stapki());
    }

    public static Question question(List<Map<String, String>> glasove,
                                    List<Map<String, String>> stapki) {
        if (glasove == null) glasove = List.of();
        if (stapki == null)  stapki  = List.of();

        List<Map<String, String>> withChar = new ArrayList<>();
        for (Map<String, String> r : glasove) {
            String c = r.get("char");
            if (c != null && !c.isBlank()) withChar.add(r);
        }

        List<Integer> kinds = new ArrayList<>();
        if (glasove.size() >= 4)  { kinds.add(0); kinds.add(1); }
        if (withChar.size() >= 4) { kinds.add(2); }
        if (stapki.size()  >= 4)  { kinds.add(3); }
        if (kinds.isEmpty()) return null;

        int kind = kinds.get(RND.nextInt(kinds.size()));
        switch (kind) {
            case 0:  return finalTone(glasove);
            case 1:  return greekName(glasove);
            case 2:  return character(withChar);
            default: return syllable(stapki);
        }
    }

    public static List<Map<String, String>> glasove() {
        return Ontology.get().select(
            "SELECT ?id ?num ?final ?greek ?char ?label ?ison ?range ?asc ?desc ?cad "
          + "?apih ?apihNote WHERE {\n" +
            "  ?id :modeNumber ?num ;\n" +
            "      :finalTone ?final ;\n" +
            "      :greekName ?greek .\n" +
            "  OPTIONAL { ?id :character ?char }\n" +
            "  OPTIONAL { ?id :melodicRangeSteps ?range }\n" +
            "  OPTIONAL { ?id :ascendingPattern ?asc }\n" +
            "  OPTIONAL { ?id :descendingPattern ?desc }\n" +
            "  OPTIONAL { ?id :cadencePattern ?cad }\n" +
            "  OPTIONAL { ?id :apechemaSyllables ?apih }\n" +
            "  OPTIONAL { ?id :apechemaNote ?apihNote }\n" +
            "  OPTIONAL { ?id :hasIson ?step . ?step :paralagiasSyllable ?ison }\n" +
            Lang.labelBlock("?id", "?label") +
            "} ORDER BY ?num");
    }

    public static List<Map<String, String>> stapki() {
        return Ontology.get().select(
            "SELECT ?id ?syl ?label WHERE {\n" +
            "  ?id a :ParalagiaStep ;\n" +
            "      :paralagiasSyllable ?syl .\n" +
            Lang.labelBlock("?id", "?label") +
            "} ORDER BY ?id");
    }

    private static Question finalTone(List<Map<String, String>> g) {
        Map<String, String> row = g.get(RND.nextInt(g.size()));
        String correct = row.get("final");
        List<String> pool = values(g, "final");
        return build(Lang.t("qFinalTone") + name(row) + "?",
                     correct, pool,
                     Lang.t("qNoteFinal"));
    }

    private static Question greekName(List<Map<String, String>> g) {
        Map<String, String> row = g.get(RND.nextInt(g.size()));
        String correct = row.get("greek");
        List<String> pool = values(g, "greek");
        return build(Lang.t("qGreekName") + name(row) + "?",
                     correct, pool,
                     Lang.t("qNoteGreek"));
    }

    private static Question character(List<Map<String, String>> g) {
        Map<String, String> row = g.get(RND.nextInt(g.size()));
        String correct = name(row);
        List<String> pool = new ArrayList<>();
        for (Map<String, String> r : g) pool.add(name(r));
        return build(Lang.t("qCharacter") + row.get("char") + "“?",
                     correct, pool,
                     Lang.t("qNoteCharacter"));
    }

    private static Question syllable(List<Map<String, String>> s) {
        Map<String, String> row = s.get(RND.nextInt(s.size()));
        String correct = row.get("syl");
        List<String> pool = values(s, "syl");
        String label = row.get("label");
        if (label == null || label.isBlank()) label = row.get("id");
        return build(Lang.t("qSyllable") + withoutSyllable(label, correct) + "?",
                     correct, pool,
                     Lang.t("qNoteSyllable"));
    }

    private static String withoutSyllable(String label, String syllable) {
        if (label == null) return "";
        String rest = label;

        int dash = rest.indexOf('–');
        if (dash < 0) dash = rest.indexOf('—');
        if (dash >= 0) rest = rest.substring(dash + 1);
        else if (syllable != null && rest.startsWith(syllable))
            rest = rest.substring(syllable.length());

        rest = withoutHz(rest).trim();
        return rest.isBlank() ? label : rest;
    }

    private static String withoutHz(String text) {
        if (text == null) return "";
        String rest = text.replaceAll(",?\\s*[0-9]+(?:[.,][0-9]+)?\\s*Hz", "");
        rest = rest.replaceAll("\\(\\s*\\)", "");
        return rest.trim();
    }

    private static String name(Map<String, String> row) {
        String label = row.get("label");
        if (label != null && !label.isBlank()) return label;
        return row.get("id");
    }

    private static List<String> values(List<Map<String, String>> rows, String key) {
        List<String> out = new ArrayList<>();
        for (Map<String, String> r : rows) {
            String v = r.get(key);
            if (v != null && !v.isBlank()) out.add(v);
        }
        return out;
    }

    public static String encode(Question q) {
        StringBuilder sb = new StringBuilder();
        sb.append("Q\t").append(clean(q.text())).append("\n");
        sb.append("N\t").append(clean(q.note())).append("\n");
        sb.append("C\t").append(q.correct());
        for (String o : q.options()) sb.append("\nO\t").append(clean(o));
        return sb.toString();
    }

    public static Question decode(String text) {
        if (text == null || text.isBlank()) return null;
        String question = "", note = "";
        int correct = 0;
        List<String> options = new ArrayList<>();

        for (String line : text.split("\n")) {
            String[] parts = line.split("\t", 2);
            if (parts.length < 2) continue;
            switch (parts[0]) {
                case "Q": question = parts[1]; break;
                case "N": note     = parts[1]; break;
                case "C": try { correct = Integer.parseInt(parts[1].trim()); }
                          catch (NumberFormatException ignored) {} break;
                case "O": options.add(parts[1]); break;
                default: break;
            }
        }
        if (question.isBlank() || options.isEmpty()) return null;
        if (correct < 0 || correct >= options.size()) correct = 0;
        return new Question(question, options, correct, note);
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace("\t", " ").replace("\n", " ");
    }

    private static Question build(String text, String correct, List<String> pool, String note) {
        Set<String> opts = new LinkedHashSet<>();
        opts.add(correct);

        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RND);
        for (String c : shuffled) {
            if (opts.size() >= 4) break;
            if (!c.equals(correct)) opts.add(c);
        }

        List<String> options = new ArrayList<>(opts);
        Collections.shuffle(options, RND);
        return new Question(text, options, options.indexOf(correct), note);
    }
}
