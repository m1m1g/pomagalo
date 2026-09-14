package trainer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ComposerController {

    private static final Logger LOG = Logger.getLogger(ComposerController.class.getName());

    @FXML private Label cardVoiceTitle, cardParamsTitle;
    @FXML private Label lblMode, lblStyle, lblTitle, lblLength;
    @FXML private Label lengthLabel, outputMeta, statusLabel;

    @FXML private ComboBox<String> modeChoice, styleChoice;
    @FXML private TextField        incipitField;
    @FXML private Slider           lengthSlider;
    @FXML private HBox             paralagePane;
    @FXML private TextArea         outputArea;

    @FXML private Button generateBtn, copyBtn, saveBtn;
    @FXML private ProgressIndicator spinner;

    private final Composer composer = new Composer();

    private List<Composer.Mode>  modes  = List.of();
    private List<Composer.Style> styles = List.of();

    private ComposerAgent.Composed current;


    @FXML
    public void initialize() {
        applyTexts();
        loadChoices();
        wireActions();

        spinner.setVisible(false);
        spinner.setManaged(false);
        copyBtn.setDisable(true);
        saveBtn.setDisable(true);

        updateScaleTiles();
    }

    private void applyTexts() {
        cardVoiceTitle.setText(Lang.t("cmCardVoice"));
        cardParamsTitle.setText(Lang.t("cmCardParams"));

        lblMode.setText(Lang.t("kbMode"));
        lblStyle.setText(Lang.t("cmStyle"));
        lblTitle.setText(Lang.t("cmTitle"));
        lblLength.setText(Lang.t("cmLength"));

        incipitField.setPromptText(Lang.t("cmTitlePrompt"));
        outputArea.setPromptText(Lang.t("cmOutputPrompt"));

        generateBtn.setText(Lang.t("cmCompose"));
        copyBtn.setText(Lang.t("cmCopy"));
        saveBtn.setText(Lang.t("btnSaveLibrary"));

        lengthLabel.setText(length() + Lang.t("cmSyllables"));
    }

    private void loadChoices() {
        modes  = composer.modes();
        styles = composer.styles();

        List<String> modeNames = new ArrayList<>();
        for (Composer.Mode m : modes) modeNames.add(m.number() + ". " + m.label());
        if (modeNames.isEmpty()) modeNames.add(Lang.t("quizNoData"));
        modeChoice.setItems(FXCollections.observableArrayList(modeNames));
        modeChoice.getSelectionModel().selectFirst();

        List<String> styleNames = new ArrayList<>();
        for (Composer.Style s : styles) styleNames.add(s.label());
        styleChoice.setItems(FXCollections.observableArrayList(styleNames));
        if (!styleNames.isEmpty())
            styleChoice.getSelectionModel().select(Math.min(1, styleNames.size() - 1));
        pokazhi(lblStyle, styleChoice, !styles.isEmpty());

        LOG.info("[Composer] Прочетени: " + modes.size() + " гласа, "
               + styles.size() + " стила.");
    }

    private void wireActions() {
        lengthSlider.valueProperty().addListener((o, was, now) ->
            lengthLabel.setText(length() + Lang.t("cmSyllables")));

        modeChoice.setOnAction(e -> updateScaleTiles());

        generateBtn.setOnAction(e -> compose());
        copyBtn.setOnAction(e -> copy());
        saveBtn.setOnAction(e -> save());
    }

    private void updateScaleTiles() {
        paralagePane.getChildren().clear();
        Composer.Mode m = selectedMode();
        if (m == null) return;

        for (String syllable : Composer.SCALE) {
            Label tile = new Label(syllable);
            tile.getStyleClass().add("syl-tile");
            if (syllable.equals(m.finalSyllable()))      tile.getStyleClass().add("syl-finalis");
            else if (syllable.equals(m.isonSyllable()))  tile.getStyleClass().add("syl-ison");

            tile.setMinWidth(Region.USE_PREF_SIZE);
            tile.setMaxWidth(Double.MAX_VALUE);
            tile.setAlignment(Pos.CENTER);
            HBox.setHgrow(tile, Priority.ALWAYS);

            paralagePane.getChildren().add(tile);
        }
    }

    private void compose() {
        Composer.Mode m = selectedMode();
        if (m == null) {
            statusLabel.setText(Lang.t("cmNoModes"));
            return;
        }
        Composer.Style style = selectedStyle();

        busy(true);
        outputArea.setText("");
        outputMeta.setText(Lang.t("cmGenerating"));
        statusLabel.setText("");

        String content = "COMPOSE:"
            + m.number()                                                  + "|"
            + (style != null ? LibrarianAgent.pole(style.key()) : "")      + "|"
            + LibrarianAgent.pole(incipitField.getText())                  + "|"
            + length();

        Bridge.ask(ComposerAgent.SERVICE, content,
            result -> Platform.runLater(() -> showChant(ComposerAgent.decode(result))),
            error  -> Platform.runLater(() -> {
                busy(false);
                outputMeta.setText("");
                statusLabel.setText(Lang.t("refError") + error);
            }));
    }

    private void showChant(ComposerAgent.Composed chant) {
        busy(false);
        if (chant == null) {
            outputMeta.setText("");
            statusLabel.setText(Lang.t("cmNoModes"));
            return;
        }
        current = chant;

        outputArea.setText(chant.title() + "\n\n"
                         + chant.flow() + "\n\n"
                         + chant.notes());

        StringBuilder meta = new StringBuilder(chant.mode());
        if (!chant.style().isBlank())     meta.append("  ·  ").append(chant.style());
        outputMeta.setText(meta.toString());

        statusLabel.setText(Lang.t("cmComposed") + chant.ison() + ".");
        copyBtn.setDisable(false);
        saveBtn.setDisable(false);
    }

    private void busy(boolean working) {
        spinner.setVisible(working);
        spinner.setManaged(working);
        generateBtn.setDisable(working);
    }

    private void copy() {
        if (outputArea.getText().isBlank()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(outputArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText(Lang.t("cmCopied"));
    }

    private void save() {
        if (current == null) return;

        String content = "SAVE:chant:"
            + LibrarianAgent.pole(current.title()) + "|"
            + LibrarianAgent.pole(current.mode())  + "|"
            + LibrarianAgent.pole(current.flow())  + "|"
            + LibrarianAgent.pole(current.notes());

        Bridge.ask(LibrarianAgent.SERVICE, content,
            ok -> Platform.runLater(() -> {
                statusLabel.setText(Lang.t("cmSavedLibrary"));
                saveBtn.setDisable(true);
            }),
            err -> Platform.runLater(() ->
                statusLabel.setText(Lang.t("cmSaveFailed") + err)));
    }

    private int length() { return (int) Math.round(lengthSlider.getValue()); }

    private Composer.Mode selectedMode() {
        int i = modeChoice.getSelectionModel().getSelectedIndex();
        return (i >= 0 && i < modes.size()) ? modes.get(i) : null;
    }

    private static void pokazhi(javafx.scene.Node nadpis,
                                javafx.scene.Node upravlenie, boolean da) {
        for (javafx.scene.Node n : new javafx.scene.Node[]{nadpis, upravlenie}) {
            if (n == null) continue;
            n.setVisible(da);
            n.setManaged(da);
        }
    }

    private Composer.Style selectedStyle() {
        int i = styleChoice.getSelectionModel().getSelectedIndex();
        return (i >= 0 && i < styles.size()) ? styles.get(i) : null;
    }
}
