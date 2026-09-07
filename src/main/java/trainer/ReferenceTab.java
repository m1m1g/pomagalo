package trainer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReferenceTab {

    private final ComboBox<String> classChoice = new ComboBox<>();
    private final ListView<String> list        = new ListView<>();
    private final TextArea         detail      = new TextArea();
    private final TextField        search      = new TextField();

    private final TextField idField    = new TextField();
    private final TextField labelField = new TextField();
    private final TextArea  descField  = new TextArea();
    private final Label     status     = new Label();

    private List<String> ids = new ArrayList<>();

    private List<String> classIds = new ArrayList<>();

    public Region build() {
        detail.setEditable(false);
        detail.setWrapText(true);

        descField.setPrefRowCount(4);
        descField.setWrapText(true);

        detail.getStyleClass().add("library-detail");
        descField.getStyleClass().add("field-input");
        idField.getStyleClass().add("field-input");
        labelField.getStyleClass().add("field-input");
        list.getStyleClass().add("library-list");
        status.getStyleClass().add("hint-label");
        classChoice.getStyleClass().add("field-choice");

        search.setPromptText(Lang.t("refSearchHint"));
        search.setOnAction(e -> doSearch());
        search.getStyleClass().add("ref-search-field");

        Button searchBtn = new Button(Lang.t("btnSearch"));
        searchBtn.setOnAction(e -> doSearch());

        HBox searchRow = new HBox(8, search, searchBtn);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchRow.getStyleClass().add("ref-filter-bar");

        classChoice.setOnAction(e -> loadIndividuals());
        classChoice.setMaxWidth(Double.MAX_VALUE);
        list.getSelectionModel().selectedIndexProperty()
            .addListener((o, a, b) -> showDetail(b.intValue()));

        VBox left = new VBox(10,
            fieldLabel("refClass"), classChoice,
            fieldLabel("refEntries"), list);
        left.setMinWidth(220);
        left.setPrefWidth(320);
        left.setMaxWidth(440);
        left.setPadding(new Insets(0, 4, 0, 0));
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setMinHeight(120);

        Button addBtn = new Button(Lang.t("btnAdd"));
        addBtn.setOnAction(e -> add());

        Button saveBtn = new Button(Lang.t("btnSaveChanges"));
        saveBtn.setOnAction(e -> save());

        Button deleteBtn = new Button(Lang.t("btnRemove"));
        deleteBtn.setOnAction(e -> delete());

        FlowPane crudButtons = new FlowPane(10, 8, addBtn, saveBtn, deleteBtn);
        crudButtons.setAlignment(Pos.CENTER_LEFT);
        for (Button b : new Button[]{addBtn, saveBtn, deleteBtn}) {
            b.setWrapText(true);
            b.setMinWidth(Region.USE_PREF_SIZE);
        }

        detail.setPrefRowCount(8);
        detail.setMinHeight(90);
        status.setWrapText(true);
        status.setMaxWidth(Double.MAX_VALUE);
        for (TextField f : new TextField[]{idField, labelField})
            f.setMaxWidth(Double.MAX_VALUE);
        descField.setMaxWidth(Double.MAX_VALUE);

        VBox right = new VBox(10,
            fieldLabel("refProperties"),  detail,
            fieldLabel("refIdentifier"), idField,
            fieldLabel("refLabel"),      labelField,
            fieldLabel("refDescription"), descField,
            crudButtons, status);
        right.setMinWidth(0);
        right.setFillWidth(true);
        VBox.setVgrow(detail, Priority.ALWAYS);

        SplitPane columns = new SplitPane(left, right);
        columns.setOrientation(Orientation.HORIZONTAL);
        columns.setDividerPositions(0.32);
        SplitPane.setResizableWithParent(left, Boolean.FALSE);
        columns.getStyleClass().add("ref-panel");

        VBox box = new VBox(12, searchRow, columns);
        box.setPadding(new Insets(16));
        box.getStyleClass().add("ref-outer");
        VBox.setVgrow(columns, Priority.ALWAYS);

        loadClasses();
        return box;
    }

    private static Label fieldLabel(String key) {
        Label l = new Label(Lang.t(key));
        l.getStyleClass().add("field-label");
        return l;
    }

    private void loadClasses() {
        String ns = Ontology.get().ns();

        List<Map<String, String>> rows = Ontology.get().select(
            "SELECT DISTINCT ?cls ?label WHERE {\n" +
            "  ?i a ?cls .\n" +
            "  FILTER(STRSTARTS(STR(?cls), \"" + ns + "\"))\n" +
            Lang.labelBlock("?cls", "?label") +
            "} ORDER BY ?cls");

        Map<String, String> labels = new LinkedHashMap<>();
        for (Map<String, String> r : rows) {
            String c = r.get("cls");
            if (c != null && !c.isBlank()) labels.putIfAbsent(c, or(r.get("label")));
        }

        Map<String, List<String>> children = new LinkedHashMap<>();
        Map<String, String>       parentOf = new LinkedHashMap<>();
        for (Map<String, String> r : Ontology.get().select(
                "SELECT DISTINCT ?cls ?parent WHERE {\n" +
                "  ?cls rdfs:subClassOf ?parent .\n" +
                "  FILTER(?cls != ?parent)\n" +
                "  FILTER(STRSTARTS(STR(?cls), \"" + ns + "\"))\n" +
                "  FILTER(STRSTARTS(STR(?parent), \"" + ns + "\"))\n" +
                "  FILTER NOT EXISTS {\n" +
                "    ?cls rdfs:subClassOf ?mid . ?mid rdfs:subClassOf ?parent .\n" +
                "    FILTER(?mid != ?cls && ?mid != ?parent)\n" +
                "  }\n" +
                "} ORDER BY ?cls ?parent")) {
            String c = r.get("cls"), p = r.get("parent");
            if (c == null || p == null || c.isBlank() || p.isBlank()) continue;
            if (!labels.containsKey(c) || !labels.containsKey(p)) continue;
            if (parentOf.putIfAbsent(c, p) == null)
                children.computeIfAbsent(p, k -> new ArrayList<>()).add(c);
        }

        classIds = new ArrayList<>();
        List<String> shown = new ArrayList<>();
        for (String c : labels.keySet())
            if (!parentOf.containsKey(c))
                addBranch(c, 0, labels, children, shown);
        for (String c : labels.keySet())
            if (!classIds.contains(c)) addBranch(c, 0, labels, children, shown);

        classChoice.setItems(FXCollections.observableArrayList(shown));
        if (!shown.isEmpty()) {
            classChoice.getSelectionModel().select(0);
            loadIndividuals();
        }
    }

    private void addBranch(String cls, int depth, Map<String, String> labels,
                           Map<String, List<String>> children, List<String> shown) {
        if (classIds.contains(cls)) return;
        classIds.add(cls);

        String label = labels.get(cls);
        shown.add("    ".repeat(depth)
                + (depth == 0 ? "" : "↳ ") + cls
                + (label == null || label.isBlank() ? "" : "  ·  " + label));

        for (String child : children.getOrDefault(cls, List.of()))
            addBranch(child, depth + 1, labels, children, shown);
    }

    private void loadIndividuals() {
        String cls = selectedClass();
        if (cls == null) return;
        String ns = Ontology.get().ns();

        List<Map<String, String>> rows = Ontology.get().select(
            "SELECT DISTINCT ?id ?label WHERE {\n" +
            "  ?id a ?sub .\n" +
            "  ?sub rdfs:subClassOf* <" + ns + cls + "> .\n" +
            "  FILTER(STRSTARTS(STR(?id), \"" + ns + "\"))\n" +
            Lang.labelBlock("?id", "?label") +
            "} ORDER BY ?id");

        fill(rows);
        status.setText(Lang.t("refShown") + ids.size()
                     + Lang.t("refRecordsFromClass") + cls + ".");
    }

    private String selectedClass() {
        int i = classChoice.getSelectionModel().getSelectedIndex();
        return (i >= 0 && i < classIds.size()) ? classIds.get(i) : null;
    }

    private static String or(String value) { return value == null ? "" : value; }

    private void doSearch() {
        String term = search.getText();
        if (term == null || term.isBlank()) { loadIndividuals(); return; }

        String ns = Ontology.get().ns();
        List<Map<String, String>> rows = Ontology.get().select(
            "SELECT DISTINCT ?id ?label WHERE {\n" +
            "  ?id ?p ?text .\n" +
            "  FILTER(isLiteral(?text) && CONTAINS(LCASE(STR(?text)), LCASE(\"" +
                 Ontology.esc(term) + "\")))\n" +
            "  FILTER(!isBlank(?id) && STRSTARTS(STR(?id), \"" + ns + "\"))\n" +
            Lang.labelBlock("?id", "?label") +
            "} ORDER BY ?id LIMIT 200");

        fill(rows);
        status.setText(Lang.t("refFound") + ids.size()
                     + Lang.t("refRecordsFor") + "„" + term + "“.");
    }

    private void fill(List<Map<String, String>> rows) {
        ids = new ArrayList<>();
        List<String> shown = new ArrayList<>();
        for (Map<String, String> r : rows) {
            String id = r.get("id");
            if (id == null || id.isBlank()) continue;
            String label = r.get("label");
            ids.add(id);
            shown.add((label != null && !label.isBlank()) ? label + "  (" + id + ")" : id);
        }
        list.setItems(FXCollections.observableArrayList(shown));
        detail.clear();
    }

    private void showDetail(int index) {
        if (index < 0 || index >= ids.size()) return;
        String id = ids.get(index);
        String ns = Ontology.get().ns();
        String iri = "<" + ns + id + ">";

        List<Map<String, String>> rows = Ontology.get().select(
            "SELECT ?p ?v ?tag WHERE {\n" +
            "  " + iri + " ?p ?v .\n" +
            "  BIND(IF(isLiteral(?v), LANG(?v), \"\") AS ?tag)\n" +
            "} ORDER BY ?p ?tag");

        StringBuilder sb = new StringBuilder();
        for (Map<String, String> r : rows) {
            String tag = r.get("tag");
            sb.append(r.get("p")).append(": ").append(r.get("v"));
            if (tag != null && !tag.isBlank()) sb.append("  [").append(tag).append("]");
            sb.append("\n");
        }
        detail.setText(sb.toString());

        List<Map<String, String>> texts = Ontology.get().select(
            "SELECT ?label ?desc WHERE {\n" +
            Lang.labelBlock(iri, "?label") +
            Lang.textBlock(iri, ":description", "?desc") +
            "} LIMIT 1");

        idField.setText(id);
        labelField.setText(firstValue(texts, "label"));
        descField.setText(firstValue(texts, "desc"));
    }

    private static String firstValue(List<Map<String, String>> rows, String variable) {
        if (rows.isEmpty()) return "";
        String v = rows.get(0).get(variable);
        return (v == null) ? "" : v;
    }

    private void add() {
        String cls = selectedClass();
        String id  = idField.getText();
        if (cls == null || id == null || id.isBlank()) {
            status.setText(Lang.t("refPickClass"));
            return;
        }
        String ns = Ontology.get().ns();
        String sparql =
            "INSERT DATA {\n" +
            "  <" + ns + id.trim() + "> a <" + ns + cls + "> ;\n" +
            "     rdfs:label \"" + Ontology.esc(labelField.getText()) + "\"@" + Lang.code() + " ;\n" +
            "     :description \"" + Ontology.esc(descField.getText()) + "\"@" + Lang.code() + " .\n" +
            "}";
        send(sparql, Lang.t("refAdded"));
    }

    private void save() {
        String id = idField.getText();
        if (id == null || id.isBlank()) { status.setText(Lang.t("refPickEntry")); return; }

        String ns  = Ontology.get().ns();
        String iri = "<" + ns + id.trim() + ">";
        String lg  = Lang.code();

        String sparql =
            "DELETE { " + iri + " rdfs:label ?l }\n" +
            "WHERE  { " + iri + " rdfs:label ?l . FILTER(lang(?l) = \"" + lg + "\") } ;\n" +
            "DELETE { " + iri + " :description ?o }\n" +
            "WHERE  { " + iri + " :description ?o . FILTER(lang(?o) = \"" + lg + "\") } ;\n" +
            "INSERT DATA {\n" +
            "  " + iri + " rdfs:label \"" + Ontology.esc(labelField.getText())
                + "\"@" + lg + " ;\n" +
            "     :description \"" + Ontology.esc(descField.getText()) + "\"@" + lg + " .\n" +
            "}";
        send(sparql, Lang.t("refSavedChanges"));
    }

    private void delete() {
        String id = idField.getText();
        if (id == null || id.isBlank()) { status.setText(Lang.t("refPickEntry")); return; }

        String ns = Ontology.get().ns();
        String sparql = "DELETE WHERE { <" + ns + id.trim() + "> ?p ?o }";
        send(sparql, Lang.t("refDeleted"));
    }

    private void send(String sparql, String okMessage) {
        Bridge.ask(OntologyAgent.SERVICE, "UPDATE:" + sparql,
            result -> Platform.runLater(() -> {
                status.setText(okMessage);
                loadIndividuals();
            }),
            error -> Platform.runLater(() -> status.setText(Lang.t("refError") + error)));
    }
}
