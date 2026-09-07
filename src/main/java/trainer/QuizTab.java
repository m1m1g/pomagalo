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
    private final Button  showBtn       = new Button(Lang.t("quizShow"));
    private final Button  nextBtn       = new Button(Lang.t("quizNext"));

    private ToggleGroup   group;
    private Quiz.Question current;
    private boolean       revealed;

    private int correct;
    private int asked;

    public Region build() {
        boolean marks = Method.is(Method.MARKS_ERRORS);
        boolean score = Method.is(Method.SHOWS_SCORE);

        questionLabel.setWrapText(true);
        questionLabel.getStyleClass().add("quiz-question");

        noteLabel.setWrapText(true);
        noteLabel.getStyleClass().add("quiz-feedback");

        scoreLabel.getStyleClass().add("quiz-header-label");
        scoreLabel.setVisible(score);
        scoreLabel.setManaged(score);

        checkBtn.setOnAction(e -> check());
        showBtn.setOnAction(e -> reveal(false));
        nextBtn.setOnAction(e -> load());

        checkBtn.setVisible(marks);
        checkBtn.setManaged(marks);
        showBtn.setVisible(!marks);
        showBtn.setManaged(!marks);
        Button primary = marks ? checkBtn : showBtn;
        primary.setDefaultButton(true);
        primary.getStyleClass().add("toolbar-btn-primary");

        FlowPane buttons = new FlowPane(10, 8, checkBtn, showBtn, nextBtn);
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
        showBtn.setDisable(true);
        checkBtn.setDisable(true);

        Bridge.ask(QuizAgent.SERVICE, "NEXT",
            result -> Platform.runLater(() -> show(Quiz.decode(result))),
            error  -> Platform.runLater(() -> {
                questionLabel.setText(Lang.t("quizLoadFail") + error);
                showBtn.setDisable(true);
            }));
    }

    private void show(Quiz.Question q) {
        current = q;
        answersBox.getChildren().clear();

        if (q == null) {
            questionLabel.setText(
                Lang.t("quizNoData"));
            showBtn.setDisable(true);
            return;
        }

        questionLabel.setText(q.text());
        showBtn.setDisable(false);
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
        reveal(true);
    }

    private RadioButton selected() {
        if (group == null) return null;
        return group.getSelectedToggle() instanceof RadioButton rb ? rb : null;
    }

    private void reveal(boolean marked) {
        if (current == null || revealed) return;
        revealed = true;

        String right = current.correctAnswer();
        RadioButton chosen = selected();

        for (var node : answersBox.getChildren()) {
            if (node instanceof RadioButton rb) {
                rb.setDisable(true);
                if (rb.getText().equals(right)) {
                    rb.getStyleClass().add("answer-correct");
                    if (!marked) rb.setSelected(true);
                } else if (marked && rb == chosen) {
                    rb.getStyleClass().add("answer-wrong");
                }
            }
        }

        noteLabel.setText(current.note());
        showBtn.setDisable(true);
        checkBtn.setDisable(true);
    }
}
