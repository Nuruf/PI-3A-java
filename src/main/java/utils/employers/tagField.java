package utils.employers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class tagField extends VBox {

    private final FlowPane container;
    private final TextField inputField;
    private final List<String> tags = new ArrayList<>();
    private final String bgColor;
    private final String textColor;
    private final String borderColor;

    private Consumer<List<String>> onTagsChanged;

    public tagField(String bgColor, String textColor, String borderColor, String placeholder) {
        this.bgColor = bgColor;
        this.textColor = textColor;
        this.borderColor = borderColor;

        // Input field
        inputField = new TextField();
        inputField.setPromptText(placeholder);
        inputField.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 4 8;" +
                        "-fx-font-size: 12px;" +
                        "-fx-min-width: 150;" +
                        "-fx-pref-height: 28;"
        );

        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String text = inputField.getText().trim();
                if (!text.isEmpty() && !tags.contains(text)) {
                    addTag(text);
                    inputField.clear();
                }
            }
        });

        // Container = FlowPane qui ressemble à un TextField
        container = new FlowPane();
        container.setHgap(6);
        container.setVgap(6);
        container.setPadding(new Insets(6, 8, 6, 8));
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle(getDefaultContainerStyle());

        container.setOnMouseClicked(e -> inputField.requestFocus());

        inputField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            container.setStyle(newVal ? getFocusedContainerStyle() : getDefaultContainerStyle());
        });

        container.getChildren().add(inputField);
        this.getChildren().add(container);
    }

    private String getDefaultContainerStyle() {
        return "-fx-background-color: -color-bg-default;" +
                "-fx-border-color: -color-border-muted;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;" +
                "-fx-min-height: 38;";
    }

    private String getFocusedContainerStyle() {
        return "-fx-background-color: -color-bg-default;" +
                "-fx-border-color: -color-accent-emphasis;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;" +
                "-fx-min-height: 38;" +
                "-fx-border-width: 1.5;";
    }

    public void addTag(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String trimmed = text.trim();
        if (tags.contains(trimmed)) return;

        tags.add(trimmed);
        refreshTags();
        notifyChange();
    }

    public void removeTag(String text) {
        tags.remove(text);
        refreshTags();
        notifyChange();
    }

    public void editTag(String oldText, String newText) {
        int index = tags.indexOf(oldText);
        if (index < 0) return;

        String trimmed = newText.trim();
        if (trimmed.isEmpty()) {
            removeTag(oldText);
            return;
        }
        if (!trimmed.equals(oldText) && tags.contains(trimmed)) {
            refreshTags();
            return;
        }

        tags.set(index, trimmed);
        refreshTags();
        notifyChange();
    }

    private void refreshTags() {
        container.getChildren().clear();

        for (String tag : tags) {
            HBox tagNode = createTagNode(tag);
            tagNode.setAlignment(Pos.CENTER);
            container.getChildren().add(tagNode);
        }

        container.getChildren().add(inputField);
    }

    private HBox createTagNode(String text) {
        HBox tagBox = new HBox(4);
        tagBox.setAlignment(Pos.CENTER);
        tagBox.setPadding(new Insets(2, 6, 2, 10));
        tagBox.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-width: 1;"
        );

        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0;"
        );

        label.setOnMouseClicked(e -> {
            enterEditMode(tagBox, text);
            e.consume();
        });

        Button btnClose = new Button("✕");
        btnClose.setMinSize(16, 16);
        btnClose.setMaxSize(16, 16);
        btnClose.setPrefSize(16, 16);
        btnClose.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 9px;" +
                        "-fx-padding: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-opacity: 0.5;"
        );
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #DC2626;" +
                        "-fx-font-size: 9px;" +
                        "-fx-padding: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-opacity: 1;"
        ));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 9px;" +
                        "-fx-padding: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-opacity: 0.5;"
        ));
        btnClose.setOnAction(e -> removeTag(text));

        tagBox.getChildren().addAll(label, btnClose);
        return tagBox;
    }

    private void enterEditMode(HBox tagBox, String oldText) {
        TextField editField = new TextField(oldText);
        editField.setPrefWidth(Math.max(80, oldText.length() * 8 + 20));
        editField.setPrefHeight(24);
        editField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + textColor + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 2 8;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;"
        );

        tagBox.getChildren().clear();
        tagBox.setStyle("-fx-background-color: transparent;-fx-border-color: transparent;-fx-padding: 0;");
        tagBox.getChildren().add(editField);

        editField.requestFocus();
        editField.selectAll();

        Runnable confirmEdit = () -> {
            String newText = editField.getText().trim();
            editTag(oldText, newText);
        };

        editField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) confirmEdit.run();
            else if (e.getCode() == KeyCode.ESCAPE) refreshTags();
        });

        editField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) confirmEdit.run();
        });
    }

    public void setTags(List<String> newTags) {
        tags.clear();
        if (newTags != null) {
            for (String t : newTags) {
                if (t != null && !t.trim().isEmpty()) {
                    tags.add(t.trim());
                }
            }
        }
        refreshTags();
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    public void setOnTagsChanged(Consumer<List<String>> listener) {
        this.onTagsChanged = listener;
    }

    private void notifyChange() {
        if (onTagsChanged != null) {
            onTagsChanged.accept(getTags());
        }
    }

    public void clear() {
        tags.clear();
        refreshTags();
    }
}