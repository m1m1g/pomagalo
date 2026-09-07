package trainer;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.logging.Logger;

public class ComposerTab {

    private static final Logger LOG = Logger.getLogger(ComposerTab.class.getName());

    private static final String LAYOUT = "/fxml/ComposerWorkbench.fxml";

    private ComposerController controller;

    public Region build() {
        URL layout = getClass().getResource(LAYOUT);
        if (layout == null) {
            LOG.severe("[ComposerTab] Липсва файлът с подредбата: " + LAYOUT);
            return message(Lang.t("refError") + LAYOUT);
        }
        try {
            FXMLLoader loader = new FXMLLoader(layout);
            Region root = loader.load();
            controller = loader.getController();
            return root;
        } catch (Exception e) {
            LOG.severe("[ComposerTab] Подредбата не се зареди: " + e.getMessage());
            return message(Lang.t("refError") + e.getMessage());
        }
    }

    private static Region message(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("feedback");
        VBox box = new VBox(label);
        box.setPadding(new Insets(24));
        box.getStyleClass().add("panel");
        return box;
    }
}
