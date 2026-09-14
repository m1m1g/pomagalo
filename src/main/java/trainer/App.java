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
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class App extends Application {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private AgentContainer container;
    private ComposerTab    composerTab;
    private BorderPane     root;

    @Override
    public void start(Stage stage) {
        Thread preload = new Thread(() -> {
            Ontology o = Ontology.get();
            LOG.info("[App] Онтология: " + o.size() + " тройки.");
        }, "ontology-preload");
        preload.setDaemon(true);
        preload.start();

        Db.get();
        startJade();

        root = new BorderPane();
        buildTabs();

        Scene scene = new Scene(root, 1180, 760);
        Theme.apply(scene);

        Theme.onChange(() -> Platform.runLater(() -> root.setBottom(statusBar())));

        stage.setTitle(Lang.t("appTitle"));
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.show();

        stage.setOnCloseRequest(e -> {
            zatvori();
            stopJade();
            Platform.exit();
        });
    }

    @Override
    public void stop() {
        zatvori();
        stopJade();
    }

    private void zatvori() {
        try { Images.close(); } catch (Exception ignored) {}
    }

    private void buildTabs() {
        composerTab = new ComposerTab();

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("main-tabs");

        tabs.getTabs().addAll(
            new Tab(Lang.t("tabGuide"),     Ui.keepLabelsReadable(new GuideTab().build())),
            new Tab(Lang.t("tabQuiz"),      Ui.keepLabelsReadable(new QuizTab().build())),
            new Tab(Lang.t("tabComposer"),  Ui.keepLabelsReadable(composerTab.build())),
            new Tab(Lang.t("tabLibrary"),   Ui.keepLabelsReadable(new LibraryTab().build())),
            new Tab(Lang.t("tabSettings"),  Ui.keepLabelsReadable(new SettingsTab().build()))
        );

        root.setTop(Ui.keepLabelsReadable(toolBar()));
        root.setCenter(tabs);
        root.setBottom(Ui.keepLabelsReadable(statusBar()));
    }

    private Region toolBar() {
        Label mark = new Label();
        mark.getStyleClass().add("logo-mark");

        Label title = new Label(Lang.t("appTitle"));
        title.getStyleClass().add("app-title");

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        Label dot = new Label();
        dot.getStyleClass().addAll("status-dot", container != null ? "dot-online" : "dot-offline");

        Label state = new Label(container != null ? Lang.t("stAgents") : Lang.t("stStandalone"));
        state.getStyleClass().add("status-label");

        HBox bar = new HBox(mark, title, gap, dot, state);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("toolbar");
        return bar;
    }

    private Region statusBar() {
        boolean live = container != null;

        Label dot = new Label();
        dot.getStyleClass().addAll("status-dot", live ? "dot-online" : "dot-offline");

        Label state = new Label(Lang.t(live ? "stAgents" : "stStandalone"));
        state.getStyleClass().add("status-label");

        HBox bar = new HBox(10, dot, state);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setAlignment(Pos.CENTER_LEFT);
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
            p.setParameter(Profile.PLATFORM_ID, "ByzantineTrainer");
            container = rt.createMainContainer(p);

            container.createNewAgent("onto",     OntologyAgent.class.getName(), new Object[]{}).start();
            container.createNewAgent("bridge",   Bridge.class.getName(),        new Object[]{}).start();
            container.createNewAgent("quiz",     QuizAgent.class.getName(),     new Object[]{}).start();
            container.createNewAgent("librarian", LibrarianAgent.class.getName(), new Object[]{}).start();
            container.createNewAgent("composer", ComposerAgent.class.getName(),  new Object[]{}).start();

            LOG.info("[App] JADE стартира с 5 агента.");
        } catch (Exception e) {
            LOG.info("[App] JADE не е наличен, работим офлайн: " + e.getMessage());
            container = null;
        }
    }

    private void stopJade() {
        try { if (container != null) container.kill(); } catch (Exception ignored) {}
    }

    public static void main(String[] args) { launch(args); }
}
