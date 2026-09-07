package trainer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class GuideTab {

    private static final Logger LOG = Logger.getLogger(GuideTab.class.getName());

    private static final String CAT_ECHOS    = "echos";
    private static final String CAT_NEUMES   = "neumes";
    private static final String CAT_STEPS    = "steps";
    private static final String CAT_HISTORY  = "history";
    private static final String CAT_PERSONS  = "persons";
    private static final String CAT_STYLES   = "styles";

    private final ComboBox<String> category = new ComboBox<>();
    private final ComboBox<String> detail   = new ComboBox<>();
    private final TextField        search   = new TextField();
    private final VBox             content  = new VBox(10);

    private final Map<String, String> categoryKeys = new LinkedHashMap<>();
    private List<Map<String, String>> modes  = List.of();
    private List<Map<String, String>> neumes = List.of();

    public Region build() {
        categoryKeys.clear();
        categoryKeys.put(Lang.t("gEchos"),    CAT_ECHOS);
        categoryKeys.put(Lang.t("gNeumes"),   CAT_NEUMES);
        categoryKeys.put(Lang.t("gSteps"),    CAT_STEPS);
        categoryKeys.put(Lang.t("gHistory"),  CAT_HISTORY);
        categoryKeys.put(Lang.t("gPersons"),  CAT_PERSONS);
        categoryKeys.put(Lang.t("gStyles"),   CAT_STYLES);

        category.setItems(FXCollections.observableArrayList(categoryKeys.keySet()));
        category.getStyleClass().addAll("field-choice", "field-choice-filter");
        category.setOnAction(e -> refresh());

        detail.getStyleClass().addAll("field-choice", "field-choice-filter");
        detail.setOnAction(e -> show());

        Label categoryLabel = new Label(Lang.t("gCategory"));
        categoryLabel.getStyleClass().add("ref-filter-label");

        search.setPromptText(Lang.t("refSearchHint"));
        search.getStyleClass().addAll("ref-search-field", "ref-search-short");
        search.setOnAction(e -> doSearch());
        HBox.setHgrow(search, Priority.ALWAYS);

        Button searchBtn = new Button(Lang.t("btnSearch"));
        searchBtn.setOnAction(e -> doSearch());
        searchBtn.setMinWidth(Region.USE_PREF_SIZE);

        HBox bar = new HBox(12, categoryLabel, category, detail, search, searchBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("ref-filter-bar");

        content.getStyleClass().addAll("ref-panel", "guide-body");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("ref-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox outer = new VBox(bar, scroll);
        outer.getStyleClass().add("ref-outer");

        modes  = Quiz.glasove();
        neumes = loadNeumes();
        category.getSelectionModel().selectFirst();
        refresh();
        return outer;
    }

    private void refresh() {
        String key = categoryKeys.getOrDefault(category.getValue(), CAT_ECHOS);
        List<String> items = new ArrayList<>();

        switch (key) {
            case CAT_ECHOS -> {
                for (Map<String, String> m : modes) items.add(modeName(m));
            }
            case CAT_NEUMES -> {
                items.add(Lang.t("gOverview"));
                for (Map<String, String> n : neumes) items.add(text(n.get("label")));
            }
            default -> { }
        }

        boolean hasDetail = !items.isEmpty();
        detail.setVisible(hasDetail);
        detail.setManaged(hasDetail);

        if (hasDetail) {
            detail.setItems(FXCollections.observableArrayList(items));
            detail.getSelectionModel().selectFirst();
        } else {
            detail.setItems(FXCollections.observableArrayList());
        }
        show();
    }

    private void show() {
        content.getChildren().clear();
        String key = categoryKeys.getOrDefault(category.getValue(), CAT_ECHOS);
        switch (key) {
            case CAT_ECHOS    -> showEchos();
            case CAT_NEUMES   -> showNeumes();
            case CAT_STEPS    -> showSteps();
            case CAT_HISTORY  -> showHistory();
            case CAT_PERSONS  -> showPersons();
            case CAT_STYLES   -> showStyles();
            default           -> showEchos();
        }
    }

    private void showEchos() {
        Map<String, String> mode = selectedRow(modes, this::modeName);
        if (mode == null) { body(Lang.t("quizNoData")); return; }

        title(modeName(mode));

        String finalis = firstWord(mode.get("final"));
        section(Lang.t("cmFinal"));
        body(text(mode.get("final")));

        if (has(mode.get("ison"))) {
            section(Lang.t("cmIson"));
            body(text(mode.get("ison")));
        }
        if (has(mode.get("char"))) {
            section(Lang.t("cmCharacter"));
            body(text(mode.get("char")));
        }
        if (has(mode.get("greek"))) {
            section(Lang.t("gGreekName"));
            body(text(mode.get("greek")));
        }

        if (has(mode.get("apih"))) {
            section(Lang.t("gApechema"));
            body(text(mode.get("apih")));
            if (has(mode.get("apihNote"))) body(text(mode.get("apihNote")));
        }

        section(Lang.t("cmCardScale"));
        tiles(finalis, text(mode.get("ison")));
    }

    private void tiles(String finalis, String ison) {
        FlowPane pane = new FlowPane(6, 6);
        for (String syllable : Composer.SCALE) {
            Label tile = new Label(syllable);
            tile.getStyleClass().add("syl-tile");
            if (syllable.equals(finalis))    tile.getStyleClass().add("syl-finalis");
            else if (syllable.equals(ison))  tile.getStyleClass().add("syl-ison");
            pane.getChildren().add(tile);
        }
        content.getChildren().add(pane);
    }

    private void showStyles() {
        List<Map<String, String>> stilove = Ontology.get().select(
            Ontology.PREFIX +
            "SELECT ?key ?label ?desc ?min ?max WHERE {\n" +
            "  ?s a :ChantStyle ; :styleKey ?key ;\n" +
            "     :notesPerSyllableMin ?min ; :notesPerSyllableMax ?max .\n" +
            Lang.labelBlock("?s", "?label") +
            Lang.textBlock("?s", ":description", "?desc") +
            "} ORDER BY ?min ?key");

        if (stilove.isEmpty()) { body(Lang.t("quizNoData")); return; }

        title(Lang.t("gStyles"));
        for (Map<String, String> st : stilove) {
            section(text(st.get("label")));
            String min = text(st.get("min")), max = text(st.get("max"));
            body(Lang.t("gNotesPerSyllable")
                 + (min.equals(max) ? min : min + "–" + max));
            if (has(st.get("desc"))) body(text(st.get("desc")));
        }
    }

    private void showNeumes() {
        String chosen = detail.getValue();

        if (chosen == null || chosen.equals(Lang.t("gOverview"))) {
            title(Lang.t("gNeumes"));
            group(Lang.t("gBody"),       "BodyNeume");
            group(Lang.t("gOrnamental"), "OrnamentalNeume");
            group(Lang.t("gRhythmic"),   "RhythmicNeume");
            return;
        }

        Map<String, String> neume = selectedRow(neumes, n -> text(n.get("label")));
        if (neume == null) { body(Lang.t("gNoResults")); return; }

        title(text(neume.get("label")));

        section(Lang.t("gKind"));
        body(kindName(neume.get("kind")));

        if (has(neume.get("greek"))) {
            section(Lang.t("gGreekName"));
            body(text(neume.get("greek")));
        }
        if (has(neume.get("value"))) {
            section(Lang.t("gDegrees"));
            body(describeSteps(neume.get("value")));
        }
        if (has(neume.get("desc"))) {
            section(Lang.t("refDescription"));
            body(text(neume.get("desc")));
        }
        picture(neume.get("img"));
    }

    private static Region sign(String path) {
        if (path != null && !path.isBlank()) {
            Image image = Images.get(path);
            if (image != null) {
                ImageView view = new ImageView(image);
                view.setPreserveRatio(true);
                view.setFitHeight(28);
                view.getStyleClass().add("neume-sign");
                StackPane box = new StackPane(view);
                box.setPrefWidth(64);
                return box;
            }
        }
        Region empty = new Region();
        empty.setPrefWidth(64);
        return empty;
    }

    private void picture(String path) {
        if (path == null || path.isBlank()) return;
        Image image = Images.get(path);
        if (image == null) return;
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setFitWidth(Math.min(image.getWidth(), 260));
        view.getStyleClass().add("guide-image");
        content.getChildren().add(view);
    }

    private void group(String heading, String kind) {
        section(heading);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);

        String[] headers = {"", Lang.t("gSign"), Lang.t("gDegrees"), Lang.t("refDescription")};
        for (int c = 0; c < headers.length; c++) {
            Label h = new Label(headers[c]);
            h.getStyleClass().add("hint-label");
            grid.add(h, c, 0);
        }

        int row = 1;
        for (Map<String, String> n : neumes) {
            if (!kind.equals(n.get("kind"))) continue;
            final String label = text(n.get("label"));

            Label name = new Label(label);
            name.getStyleClass().add("ref-neume-name");
            name.setPrefWidth(220);
            name.setStyle("-fx-cursor: hand;");
            name.setOnMouseClicked(e -> {
                detail.getSelectionModel().select(label);
                show();
            });

            Label steps = new Label(has(n.get("value")) ? describeSteps(n.get("value")) : "—");
            steps.getStyleClass().add("ref-cell");
            steps.setPrefWidth(150);

            Label desc = new Label(text(n.get("desc")));
            desc.getStyleClass().add("ref-cell");
            desc.setWrapText(true);
            desc.setMaxWidth(460);

            grid.add(sign(n.get("img")), 0, row);
            grid.add(name,  1, row);
            grid.add(steps, 2, row);
            grid.add(desc,  3, row);
            row++;
        }
        content.getChildren().add(grid);
    }

    private String describeSteps(String raw) {
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return raw;
        }
        if (value == 0) return Lang.t("exIson");
        String direction = value > 0 ? Lang.t("exAscending") : Lang.t("exDescending");
        return direction + " (" + (value > 0 ? "+" : "") + value + ")";
    }

    private String kindName(String kind) {
        if (kind == null) return "";
        return switch (kind) {
            case "BodyNeume"       -> Lang.t("gBody");
            case "OrnamentalNeume" -> Lang.t("gOrnamental");
            case "RhythmicNeume"   -> Lang.t("gRhythmic");
            default                -> kind;
        };
    }

    private void showSteps() {
        title(Lang.t("gSteps"));
        body(Lang.t("gStepsIntro"));

        List<Map<String, String>> steps = Ontology.get().select(Ontology.PREFIX +
            "SELECT ?syl ?label ?order WHERE {\n" +
            "  ?s a :ParalagiaStep ; :paralagiasSyllable ?syl .\n" +
            "  OPTIONAL { ?s :stepOrder ?order }\n" +
            Lang.labelBlock("?s", "?label") +
            "} ORDER BY ?order ?syl");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(6);
        int row = 0;
        for (Map<String, String> s : steps) {
            Label syllable = new Label(text(s.get("syl")));
            syllable.getStyleClass().add("ref-neume-name");
            syllable.setPrefWidth(120);
            Label name = new Label(text(s.get("label")));
            name.getStyleClass().add("ref-cell");
            grid.add(syllable, 0, row);
            grid.add(name,     1, row);
            row++;
        }
        content.getChildren().add(grid);

        for (Map<String, String> mode : modes) {
            section(modeName(mode));
            tiles(firstWord(mode.get("final")), text(mode.get("ison")));
        }
    }

    private void showHistory() {
        title(Lang.t("gHistory"));

        List<Map<String, String>> periods = Ontology.get().select(Ontology.PREFIX +
            "SELECT ?label ?desc ?start WHERE {\n" +
            "  ?p a :HistoricalPeriod .\n" +
            "  OPTIONAL { ?p :periodStart ?start }\n" +
            Lang.labelBlock("?p", "?label") +
            Lang.textBlock("?p", ":description", "?desc") +
            "} ORDER BY ?start");

        if (periods.isEmpty()) { body(Lang.t("gNoResults")); return; }
        for (Map<String, String> p : periods) {
            section(text(p.get("label")));
            body(text(p.get("desc")));
        }
    }

    private void showPersons() {
        title(Lang.t("gPersons"));

        List<Map<String, String>> people = Ontology.get().select(Ontology.PREFIX +
            "SELECT DISTINCT ?label ?desc ?born ?died ?img WHERE {\n" +
            "  ?p a ?kind .\n" +
            "  ?kind rdfs:subClassOf* :Person .\n" +
            "  OPTIONAL { ?p :birthYear ?born }\n" +
            "  OPTIONAL { ?p :deathYear ?died }\n" +
            "  OPTIONAL { ?p :hasImageData ?img }\n" +
            Lang.labelBlock("?p", "?label") +
            Lang.textBlock("?p", ":description", "?desc") +
            "} ORDER BY DESC(BOUND(?born)) ?born ?label");

        if (people.isEmpty()) { body(Lang.t("gNoResults")); return; }
        for (Map<String, String> p : people) {
            section(text(p.get("label")));
            String years = godini(p.get("born"), p.get("died"));
            if (has(years)) body(years);
            picture(p.get("img"));
            body(text(p.get("desc")));
        }
    }

    private static String godini(String born, String died) {
        boolean b = has(born), d = has(died);
        if (!b && !d) return "";
        if (b && d)   return born.trim() + " – " + died.trim();
        return b ? born.trim() + " – …" : "… – " + died.trim();
    }

    private void doSearch() {
        String term = search.getText();
        if (term == null || term.isBlank()) { refresh(); return; }

        content.getChildren().clear();
        title(Lang.t("refFound") + "„" + term.trim() + "“");
        Label waiting = new Label(Lang.t("quizLoading"));
        waiting.getStyleClass().add("hint-label");
        content.getChildren().add(waiting);

        final String needle = term.trim();
        Thread worker = new Thread(() -> {
            List<Map<String, String>> rows = searchRows(needle);
            Platform.runLater(() -> showSearchResults(needle, rows));
        }, "guide-search");
        worker.setDaemon(true);
        worker.start();
    }

    private List<Map<String, String>> searchRows(String term) {
        String ns = Ontology.get().ns();
        return Ontology.get().select(Ontology.PREFIX +
            "SELECT DISTINCT ?id ?label ?kind WHERE {\n" +
            "  ?id ?p ?text .\n" +
            "  FILTER(isLiteral(?text) && CONTAINS(LCASE(STR(?text)), LCASE(\"" +
                 Ontology.esc(term) + "\")))\n" +
            "  FILTER(!isBlank(?id) && STRSTARTS(STR(?id), \"" + ns + "\"))\n" +
            "  OPTIONAL { ?id a ?kind . FILTER(STRSTARTS(STR(?kind), \"" + ns + "\")) }\n" +
            Lang.labelBlock("?id", "?label") +
            "} ORDER BY ?kind ?id LIMIT 200");
    }

    private void showSearchResults(String term, List<Map<String, String>> rows) {
        content.getChildren().clear();
        title(Lang.t("refFound") + "„" + term + "“");

        if (rows.isEmpty()) { body(Lang.t("gNoResults")); return; }

        Map<String, List<Map<String, String>>> byKind = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String kind = text(row.get("kind"));
            if (kind.isBlank()) kind = "—";
            byKind.computeIfAbsent(kind, k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<Map<String, String>>> entry : byKind.entrySet()) {
            section(entry.getKey());
            for (Map<String, String> row : entry.getValue()) {
                String label = text(row.get("label"));
                Label item = new Label(label.isBlank() ? text(row.get("id")) : label);
                item.getStyleClass().add("ref-cell");
                item.setWrapText(true);
                content.getChildren().add(item);
            }
        }
        LOG.fine("[Guide] Намерени записи: " + rows.size());
    }

    private List<Map<String, String>> loadNeumes() {
        return Ontology.get().select(Ontology.PREFIX +
            "SELECT ?id ?label ?greek ?value ?desc ?kind ?img WHERE {\n" +
            "  VALUES ?type { :BodyNeume :OrnamentalNeume :RhythmicNeume }\n" +
            "  ?id a ?type .\n" +
            "  BIND(REPLACE(STR(?type), \"^.*#\", \"\") AS ?kind)\n" +
            "  OPTIONAL { ?id :greekName ?greek }\n" +
            "  OPTIONAL { ?id :intervalValue ?value }\n" +
            "  OPTIONAL { ?id :hasImageData ?img }\n" +
            Lang.labelBlock("?id", "?label") +
            Lang.textBlock("?id", ":description", "?desc") +
            "} ORDER BY ?kind ?value ?id");
    }

    private Map<String, String> selectedRow(List<Map<String, String>> rows,
                                            java.util.function.Function<Map<String, String>, String> naming) {
        String chosen = detail.getValue();
        if (rows.isEmpty()) return null;
        if (chosen == null) return rows.get(0);
        for (Map<String, String> row : rows) {
            if (chosen.equals(naming.apply(row))) return row;
        }
        return rows.get(0);
    }

    private String modeName(Map<String, String> row) {
        String label = text(row.get("label"));
        String num   = text(row.get("num"));
        if (label.isBlank()) label = Lang.t("modeWord");
        return num.isBlank() ? label : num + ". " + label;
    }

    private void title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("ref-title");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(label);
    }

    private void section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("ref-section");
        content.getChildren().add(label);
    }

    private void body(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("ref-body");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().add(label);
    }

    private static boolean has(String v)  { return v != null && !v.isBlank(); }
    private static String  text(String v) { return v == null ? "" : v.trim(); }

    private static String firstWord(String v) {
        if (!has(v)) return "";
        String s = v.trim();
        int space = s.indexOf(' ');
        return space > 0 ? s.substring(0, space) : s;
    }
}
