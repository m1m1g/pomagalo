package trainer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class SettingsTab {

    public Region build() {
        VBox column = new VBox(26);
        column.setMaxWidth(720);
        column.setPadding(new Insets(34, 40, 34, 40));
        column.getStyleClass().add("set-column");

        Label lead = new Label(Lang.t("setLead"));
        lead.setWrapText(true);
        lead.getStyleClass().add("set-lead");

        column.getChildren().addAll(lead, look());

        StackPane centre = new StackPane(column);
        StackPane.setAlignment(column, Pos.TOP_CENTER);
        centre.getStyleClass().add("set-centre");

        ScrollPane scroll = new ScrollPane(centre);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("set-scroll");

        VBox outer = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        outer.getStyleClass().add("set-outer");
        return outer;
    }

    private Region look() {
        ComboBox<String> theme = choice();
        theme.getItems().addAll(Theme.supported().values());
        theme.setValue(Theme.title());
        theme.setOnAction(e -> Theme.set(Theme.keyOf(theme.getValue())));

        GridPane g = grid();
        row(g, 0, Lang.t("stTheme"), theme, Lang.t("setHintTheme"));
        return section(Lang.t("setLook"), g);
    }

    private static ComboBox<String> choice() {
        ComboBox<String> c = new ComboBox<>();
        c.getStyleClass().add("field-choice");
        c.setMaxWidth(Double.MAX_VALUE);
        c.setMinWidth(150);
        return c;
    }

    private static GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(16);

        ColumnConstraints left = new ColumnConstraints();
        left.setMinWidth(110);
        left.setPrefWidth(170);
        left.setMaxWidth(210);
        left.setHalignment(javafx.geometry.HPos.LEFT);

        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        right.setMinWidth(150);
        right.setFillWidth(true);

        g.getColumnConstraints().addAll(left, right);
        return g;
    }

    private static void row(GridPane g, int r, String name,
                            Region control, String hint) {
        Label n = new Label(name);
        n.setWrapText(true);
        n.getStyleClass().add("set-name");

        Label h = new Label(hint);
        h.setWrapText(true);
        h.setMaxWidth(Double.MAX_VALUE);
        h.getStyleClass().add("set-hint");

        VBox cell = new VBox(6, control, h);
        cell.setFillWidth(true);

        g.add(n, 0, r);
        g.add(cell, 1, r);
    }

    private static Region section(String title, Region content) {
        Label t = new Label(title);
        t.getStyleClass().add("set-section-title");

        Region line = new Region();
        line.getStyleClass().add("set-section-rule");
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(12, t, line, content);
        box.getStyleClass().add("set-section");
        return box;
    }
}
