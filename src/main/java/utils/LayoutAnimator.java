package utils;

import javafx.animation.TranslateTransition;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class LayoutAnimator {
    private final Duration duration = Duration.millis(200);

    public LayoutAnimator(Pane pane) {
        // Listen for when items are added to the pane
        pane.getChildren().addListener((ListChangeListener<Node>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (Node node : c.getAddedSubList()) {
                        observeNode(node);
                    }
                }
            }
        });

        // Observe existing items
        for (Node node : pane.getChildren()) {
            observeNode(node);
        }
    }

    private void observeNode(Node node) {
        // Every time the layout coordinates change, animate the transition
        node.layoutXProperty().addListener((obs, oldVal, newVal) -> animate(node, oldVal.doubleValue(), node.getLayoutY(), newVal.doubleValue(), node.getLayoutY()));
        node.layoutYProperty().addListener((obs, oldVal, newVal) -> animate(node, node.getLayoutX(), oldVal.doubleValue(), node.getLayoutX(), newVal.doubleValue()));
    }

    private void animate(Node node, double oldX, double oldY, double newX, double newY) {
        double diffX = oldX - newX;
        double diffY = oldY - newY;

        if (diffX == 0 && diffY == 0) return;

        node.setTranslateX(node.getTranslateX() + diffX);
        node.setTranslateY(node.getTranslateY() + diffY);

        TranslateTransition t = new TranslateTransition(duration, node);
        t.setToX(0);
        t.setToY(0);
        t.play();
    }
}