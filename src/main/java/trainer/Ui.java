package trainer;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.Region;

final class Ui {

    private Ui() {}

    static <T extends Node> T keepLabelsReadable(T node) {
        apply(node);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) keepLabelsReadable(child);
            parent.getChildrenUnmodifiable().addListener(
                (ListChangeListener<Node>) change -> {
                    while (change.next())
                        for (Node added : change.getAddedSubList()) keepLabelsReadable(added);
                });
        }
        return node;
    }

    private static void apply(Node node) {
        if (!(node instanceof ButtonBase button)) return;
        if (button.isWrapText()) return;
        if (button.getPrefWidth() != Region.USE_COMPUTED_SIZE) return;
        button.setMinWidth(Region.USE_PREF_SIZE);
    }
}
