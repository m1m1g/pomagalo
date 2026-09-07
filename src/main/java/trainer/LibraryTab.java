package trainer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.geometry.Orientation;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class LibraryTab {

    private static final Logger LOG = Logger.getLogger(LibraryTab.class.getName());

    private final ListView<String> chList     = new ListView<>();
    private final TextArea         chDetail   = new TextArea();
    private final Label            chStats    = new Label();
    private List<String[]>         chRows;

    public Region build() {
        Region chants = chantsPane();
        VBox box = new VBox(chants);
        box.getStyleClass().add("library-panel");
        VBox.setVgrow(chants, Priority.ALWAYS);
        refreshChants();
        return box;
    }

    private void ask(String command, Consumer<List<String[]>> then) {
        Bridge.ask(LibrarianAgent.SERVICE, command,
            result -> Platform.runLater(() -> then.accept(LibrarianAgent.decode(result))),
            error  -> LOG.warning("[LibraryTab] " + command + ": " + error));
    }

    private Region chantsPane() {
        chDetail.setEditable(false);
        chDetail.setWrapText(true);
        chDetail.setPrefRowCount(10);
        chDetail.getStyleClass().add("library-detail");
        chList.getStyleClass().add("library-list");
        chStats.getStyleClass().add("lib-sub");

        chList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        chList.getSelectionModel().selectedIndexProperty().addListener(
            (o, a, b) -> showChant(b.intValue()));

        Button refresh = new Button(Lang.t("btnRefresh"));
        refresh.setOnAction(e -> refreshChants());
        Button delete = new Button(Lang.t("btnDelete"));
        delete.setOnAction(e -> {
            int i = chList.getSelectionModel().getSelectedIndex();
            if (chRows != null && i >= 0 && i < chRows.size()) {
                ask("DELETE:chant:" + chRows.get(i)[0], r -> refreshChants());
            }
        });

        return pane(chList, chStats, chDetail, refresh, delete);
    }

    private static Region pane(ListView<String> list, Label stats,
                               TextArea detail, Button refresh, Button delete) {
        FlowPane buttons = new FlowPane(10, 8, refresh, delete);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(12, list, buttons, stats);
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setMinHeight(100);
        top.setMinHeight(150);

        Label caption = new Label(Lang.t("libDetails"));
        caption.getStyleClass().add("field-label");

        VBox bottom = new VBox(8, caption, detail);
        VBox.setVgrow(detail, Priority.ALWAYS);
        detail.setMinHeight(70);
        bottom.setMinHeight(110);

        SplitPane split = new SplitPane(top, bottom);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.58);
        split.getStyleClass().add("library-split");

        VBox column = new VBox(split);
        VBox.setVgrow(split, Priority.ALWAYS);
        column.setMaxWidth(920);
        column.setPadding(new Insets(16));

        StackPane centre = new StackPane(column);
        StackPane.setAlignment(column, Pos.TOP_CENTER);
        centre.getStyleClass().add("library-centre");
        return centre;
    }

    private void refreshChants() {
        ask("CHANTS", rows -> {
            chRows = rows;
            var items = FXCollections.<String>observableArrayList();
            for (String[] r : chRows) items.add(r[1] + "   (" + r[5] + ")");
            chList.setItems(items);
            chStats.setText(Lang.t("libSavedCh") + chRows.size());
            chDetail.clear();
        });
    }

    private void showChant(int index) {
        if (chRows == null || index < 0 || index >= chRows.size()) return;
        String[] r = chRows.get(index);
        chDetail.setText(r[1]
                       + "\n" + Lang.t("kbMode") + " " + r[2] + "\n\n"
                       + r[3] + "\n\n" + r[4]
                       + "\n\n" + Lang.t("libRecorded") + r[5]);
    }
}
