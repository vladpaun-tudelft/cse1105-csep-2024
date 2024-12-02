package client.ui;

import client.scenes.DashboardCtrl;
import commons.Note;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class NoteListItem extends ListCell<Note> {

    // References
    private Label overviewTitle;
    private TextArea overviewBody;
    private DashboardCtrl controller;

    // List cell content
    private final Label noteTitle;
    private final Button deleteButton;
    private final HBox hBox;
    private TextField textField;

    // Variables
    private String originalTitle;
    private long lastClickTime = 0;
    private static final int DOUBLE_CLICK_TIMEFRAME = 400;

    public NoteListItem(Label overviewTitle, TextArea overviewBody, DashboardCtrl controller) {

        this.overviewTitle = overviewTitle;
        this.overviewBody = overviewBody;
        this.controller = controller;

        // Initialize the note title
        noteTitle = new Label();
        noteTitle.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        noteTitle.maxWidthProperty().bind(controller.collectionView.widthProperty().subtract(40));
        noteTitle.setWrapText(false);
        noteTitle.setMinWidth(0);

        // Initialize the delete button
        deleteButton = new Button();
        deleteButton.getStyleClass().add("delete_icon");
        deleteButton.setVisible(false);

        // Create layout
        hBox = new HBox(0);
        Region spacer = new Region();
        hBox.getChildren().addAll(noteTitle, spacer, deleteButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setHgrow(deleteButton, Priority.NEVER);
        hBox.setAlignment(Pos.CENTER_LEFT);

        configureEventHandlers();
    }

    private void configureEventHandlers() {
        deleteButton.setOnAction(event -> {
            controller.deleteSelectedNote();  // getItem()
        });

        hBox.setOnMouseClicked(event -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime <= DOUBLE_CLICK_TIMEFRAME) {
                startEditing();
            }
            lastClickTime = currentTime;
        });

        focusedProperty().addListener((observable, oldValue, newValue) -> {
            Note item = getItem();
            if (item == null || overviewTitle == null || overviewBody == null) return;

            if (newValue) {
                overviewTitle.setText(item.getTitle());
                overviewBody.setText(item.getBody());
                controller.handleContentBlocker(false);
                deleteButton.setVisible(true);
            }else {
                deleteButton.setVisible(false);
            }
        });
    }

    @Override
    protected void updateItem(Note item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            noteTitle.setText(item.getTitle());
            setGraphic(hBox);
        }
    }

    private void startEditing() {
        Note item = getItem();
        if (item != null) {
            originalTitle = item.getTitle();
            textField = new TextField(originalTitle);
            HBox.setHgrow(textField, Priority.ALWAYS);

            textField.setOnAction(e -> {
                item.setTitle(textField.getText());
                noteTitle.setText(item.getTitle());
                revertToLabel();
            });

            textField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    item.setTitle(originalTitle);
                    noteTitle.setText(originalTitle);
                    revertToLabel();
                }
            });

            textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    item.setTitle(textField.getText());
                    noteTitle.setText(item.getTitle());
                    revertToLabel();
                }
            });

            hBox.getChildren().clear();
            hBox.getChildren().add(textField);

            textField.requestFocus();
        }
    }

    private void revertToLabel() {
        hBox.getChildren().clear();
        Region spacer = new Region();
        hBox.getChildren().addAll(noteTitle, spacer, deleteButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        overviewTitle.setText(getItem().getTitle());
        overviewBody.setText(getItem().getBody());
    }
}
