package trainer;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class AdminApp extends Application {

    private static final Logger LOG = Logger.getLogger(AdminApp.class.getName());

    private static final String JADE_PORT = "1100";

    private AgentContainer container;
    private BorderPane     root;

    @Override
    public void start(Stage stage) {
        startJade();

        root = new BorderPane();
        build();

        Scene scene = new Scene(root, 1120, 740);
        Theme.apply(scene);
        Theme.onChange(() -> Platform.runLater(this::build));

        stage.setTitle(title());
        stage.setScene(scene);
        stage.setMinWidth(880);
        stage.setMinHeight(600);
        stage.show();

        stage.setOnCloseRequest(e -> {
            stopJade();
            Platform.exit();
        });
    }

    @Override
    public void stop() { stopJade(); }

    private static String title() {
        return Lang.t("appTitle") + " · " + Lang.t("tabOntology");
    }

    private void build() {
        root.setTop(Ui.keepLabelsReadable(toolBar()));
        root.setCenter(Ui.keepLabelsReadable(new ReferenceTab().build()));
        root.setBottom(Ui.keepLabelsReadable(statusBar()));
    }

    private Region toolBar() {
        Label mark = new Label();
        mark.getStyleClass().add("logo-mark");

        Label name = new Label(title());
        name.getStyleClass().add("app-title");

        HBox heading = new HBox(12, mark, name);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label themeLabel = new Label(bare("stTheme"));
        themeLabel.getStyleClass().add("bar-control-label");

        ComboBox<String> theme = new ComboBox<>();
        theme.getStyleClass().addAll("field-choice", "field-choice-narrow");
        theme.getItems().addAll(Theme.supported().values());
        theme.setValue(Theme.title());
        theme.setOnAction(e -> Theme.set(Theme.keyOf(theme.getValue())));

        HBox controls = new HBox(12, themeLabel, theme);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox bar = new VBox(10, heading, controls);
        bar.getStyleClass().addAll("toolbar", "toolbar-stacked");
        return bar;
    }

    private static String bare(String key) {
        String t = Lang.t(key);
        return t.endsWith(":") ? t.substring(0, t.length() - 1) : t;
    }

    private Region statusBar() {
        Label dot = new Label();
        dot.getStyleClass().addAll("status-dot",
            container != null ? "dot-online" : "dot-offline");

        Label state = new Label(container != null
            ? Lang.t("stAgents") : Lang.t("stStandalone"));
        state.getStyleClass().add("status-label");

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        Label triples = new Label(Ontology.get().size() + Lang.t("stTriples"));
        triples.getStyleClass().add("status-label");

        HBox bar = new HBox(10, dot, state, gap, triples);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private void startJade() {
        try {
            Runtime rt = Runtime.instance();
            rt.setCloseVM(false);

            Profile p = new ProfileImpl();
            p.setParameter(Profile.MAIN, "true");
            p.setParameter(Profile.GUI, "false");
            p.setParameter(Profile.PLATFORM_ID, "ByzantineOntologyAdmin");
            p.setParameter(Profile.LOCAL_PORT, JADE_PORT);
            container = rt.createMainContainer(p);

            container.createNewAgent("onto",   OntologyAgent.class.getName(), new Object[]{}).start();
            container.createNewAgent("bridge", Bridge.class.getName(),        new Object[]{}).start();

            LOG.info("[AdminApp] JADE стартира с 2 агента на пристанище " + JADE_PORT + ".");
        } catch (Exception e) {
            LOG.warning("[AdminApp] JADE не е наличен — записът няма да работи: "
                      + e.getMessage());
            container = null;
        }
    }

    private void stopJade() {
        try { if (container != null) container.kill(); } catch (Exception ignored) {}
    }

    public static void main(String[] args) { launch(args); }
}
