package trainer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class QuizTab {

    private final Label   questionLabel = new Label();
    private final VBox    answersBox    = new VBox(8);
    private final Label   noteLabel     = new Label();
    private final Label   scoreLabel    = new Label();
    private final Button  checkBtn      = new Button(Lang.t("quizCheck"));
    private final Button  nextBtn       = new Button(Lang.t("quizNext"));

    private ToggleGroup   group;
    private Quiz.Question current;
    private boolean       revealed;

    private int correct;
    private int asked;

    public Region build() {
        questionLabel.setWrapText(true);
        questionLabel.getStyleClass().add("quiz-question");

        noteLabel.setWrapText(true);
        noteLabel.getStyleClass().add("quiz-feedback");

        scoreLabel.getStyleClass().add("quiz-header-label");

        checkBtn.setOnAction(e -> check());
        nextBtn.setOnAction(e -> load());

        checkBtn.setDefaultButton(true);
        checkBtn.getStyleClass().add("toolbar-btn-primary");

        FlowPane buttons = new FlowPane(10, 8, checkBtn, nextBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(16, questionLabel, answersBox, buttons,
                            noteLabel, scoreLabel);
        box.getStyleClass().add("quiz-content");
        box.setPadding(new Insets(32, 40, 32, 40));
        box.setMaxWidth(760);

        StackPane centre = new StackPane(box);
        StackPane.setAlignment(box, Pos.TOP_CENTER);
        centre.getStyleClass().add("quiz-centre");

        ScrollPane scroll = new ScrollPane(centre);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("quiz-scroll");

        VBox outer = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        outer.getStyleClass().add("quiz-outer");

        load();
        return outer;
    }

    private void load() {
        answersBox.getChildren().clear();
        noteLabel.setText("");
        scoreLabel.setText(asked > 0
            ? Lang.t("quizScore") + correct + Lang.t("quizOf") + asked : "");
        revealed = false;
        questionLabel.setText(Lang.t("quizLoading"));
        checkBtn.setDisable(true);

        Bridge.ask(QuizAgent.SERVICE, "NEXT",
            result -> Platform.runLater(() -> show(Quiz.decode(result))),
            error  -> Platform.runLater(() -> {
                questionLabel.setText(Lang.t("quizLoadFail") + error);
            }));
    }

    private void show(Quiz.Question q) {
        current = q;
        answersBox.getChildren().clear();

        if (q == null) {
            questionLabel.setText(
                Lang.t("quizNoData"));
            return;
        }

        questionLabel.setText(q.text());
        checkBtn.setDisable(false);

        group = new ToggleGroup();
        for (String option : q.options()) {
            RadioButton rb = new RadioButton(option);
            rb.setWrapText(true);
            rb.setToggleGroup(group);
            answersBox.getChildren().add(rb);
        }
    }

    private void check() {
        if (current == null || revealed) return;

        RadioButton chosen = selected();
        if (chosen == null) {
            noteLabel.setText(Lang.t("quizPickFirst"));
            return;
        }

        boolean right = chosen.getText().equals(current.correctAnswer());
        asked++;
        if (right) correct++;

        scoreLabel.setText(Lang.t("quizScore") + correct + Lang.t("quizOf") + asked);
        reveal();
    }

    private RadioButton selected() {
        if (group == null) return null;
        return group.getSelectedToggle() instanceof RadioButton rb ? rb : null;
    }

    private void reveal() {
        if (current == null || revealed) return;
        revealed = true;

        String right = current.correctAnswer();
        RadioButton chosen = selected();

        for (var node : answersBox.getChildren()) {
            if (node instanceof RadioButton rb) {
                rb.setDisable(true);
                if (rb.getText().equals(right)) {
                    rb.getStyleClass().add("answer-correct");
                } else if (rb == chosen) {
                    rb.getStyleClass().add("answer-wrong");
                }
            }
        }

        noteLabel.setText(current.note());
        checkBtn.setDisable(true);
    }
}
