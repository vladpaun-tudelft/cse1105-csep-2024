package client.ui;

import client.controllers.NoteCtrl;
import client.scenes.DashboardCtrl;
import commons.Note;
import jakarta.ws.rs.ClientErrorException;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class NoteTreeItem extends TreeCell<Note> {

    // Utillities
    private final DialogStyler dialogStyler = new DialogStyler();

    // References
    private Label overviewTitle;
    private Label markdownTitle;
    private TextArea overviewBody;
    private DashboardCtrl controller;
    private NoteCtrl noteCtrl;

    private Label noteTitle;
    private TextField textField;
    private final Button deleteButton;
    private final Button editButton;
    private final HBox hBox;
    private final Region spacer;

    private long lastClickTime = 0;
    private static final int DOUBLE_CLICK_TIMEFRAME = 400;
    private String originalTitle;

    public NoteTreeItem(Label overviewTitle, Label markdownTitle, TextArea overviewBody,
                        DashboardCtrl controller, NoteCtrl noteCtrl) {
        this.overviewTitle = overviewTitle;
        this.markdownTitle = markdownTitle;
        this.overviewBody = overviewBody;
        this.controller = controller;
        this.noteCtrl = noteCtrl;

        // Initialize the note title
        noteTitle = new Label();
        noteTitle.setTextOverrun(OverrunStyle.ELLIPSIS);
        noteTitle.setWrapText(false);
        noteTitle.setMinWidth(0);

        // Initialize the delete button
        deleteButton = new Button();
        deleteButton.getStyleClass().addAll("icon", "note_list_icon", "delete_icon");
        deleteButton.setCursor(Cursor.HAND);

        // Initialize the edit button
        editButton = new Button();
        editButton.getStyleClass().addAll("icon", "note_list_icon", "edit_icon");
        editButton.setCursor(Cursor.HAND);

        // Create layout
        hBox = new HBox(0);
        spacer = new Region();
        hBox.getChildren().addAll(noteTitle, spacer, editButton, deleteButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hBox.setAlignment(Pos.CENTER_LEFT);

        hBox.setMinHeight(30);
        hBox.setPrefHeight(30);
        hBox.setMaxHeight(30);

        configureEventHandlers();
    }

    private void configureEventHandlers() {
        deleteButton.setOnAction(event -> {
            controller.deleteSelectedNote();
        });
        editButton.setOnAction(event -> {
            startEditing();
        });

        hBox.setOnMouseClicked(event -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime <= DOUBLE_CLICK_TIMEFRAME) {
                startEditing();
            }
            lastClickTime = currentTime;
        });
    }

    @Override
    protected void updateItem(Note item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            // Handle leaf nodes and parent nodes separately
            if (getTreeItem().isLeaf()) {
                // For leaf nodes, show the custom graphic (buttons, title)
                noteTitle.setText(item.getTitle());
                deleteButton.setVisible(isSelected());
                editButton.setVisible(isSelected());

                // Adjust for selected state
                if (isSelected()) {
                    noteTitle.maxWidthProperty().bind(controller.collectionView.widthProperty().subtract(60));
                    if (!hBox.getChildren().contains(editButton) || !hBox.getChildren().contains(deleteButton)) {
                        if (!hBox.getChildren().contains(editButton)) {
                            hBox.getChildren().add(editButton);
                        }
                        if (!hBox.getChildren().contains(deleteButton)) {
                            hBox.getChildren().add(deleteButton);
                        }
                    }
                } else {
                    noteTitle.maxWidthProperty().bind(controller.collectionView.widthProperty().subtract(10));
                    hBox.getChildren().remove(editButton);
                    hBox.getChildren().remove(deleteButton);
                }

                setGraphic(hBox);
                setText(null); // Clear text to only show custom graphic
            } else {
                // For parent nodes, just show the title
                setText(item.getTitle());
                setGraphic(null); // No custom UI for parent nodes
            }
        }
    }

    private void startEditing() {
        Note item = getItem();
        if (item != null) {
            originalTitle = item.getTitle();

            textField = new TextField(originalTitle);

            // Set width dynamically to match the container minus 5px
            textField.prefWidthProperty().bind(getTreeView().widthProperty().subtract(45));  // Adjust this number to fit your layout

            final boolean isCommited[] = {false};

            // The whole isCommited logic is needed bc otherwise when you click "ENTER" both triggers are set
            // Pretty ugly, but can be improved later
            textField.setOnAction(e -> {
                if (!isCommited[0]) {
                    isCommited[0] = true;
                    commitTitleChange(item, textField.getText());
                }
            });

            textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue && !isCommited[0]) {
                    isCommited[0] = true;
                    commitTitleChange(item, textField.getText());
                }
            });

            textField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    isCommited[0] = true;
                    revertToLabel();
                }
            });

            hBox.getChildren().clear();
            hBox.getChildren().add(textField);

            textField.requestFocus();
        }

    }

    private void commitTitleChange(Note item, String newTitle) {
        if (item == null) return;
        String oldTitle = item.getTitle();

        // Ensure the title is unique in the current collection
        String uniqueTitle = noteCtrl.generateUniqueTitle(controller.getAllNotes(),item, newTitle, false);
        try {
            item.setTitle(uniqueTitle); // Update the title of the Note
            noteTitle.setText(uniqueTitle);
            noteCtrl.updatePendingNotes.add(item);// Notify NoteCtrl of the change

            noteCtrl.saveAllPendingNotes();
        } catch (ClientErrorException e) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error",
                    e.getResponse().readEntity(String.class)
            );
            alert.showAndWait();

            item.setTitle(oldTitle);
            noteTitle.setText(oldTitle);
        }

        revertToLabel();
    }

    private void revertToLabel() {
        hBox.getChildren().clear();
        hBox.getChildren().addAll(noteTitle, spacer, editButton, deleteButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        overviewTitle.setText(getItem().getTitle());
        markdownTitle.setText(getItem().getTitle());
        overviewBody.setText(getItem().getBody());
    }
}
