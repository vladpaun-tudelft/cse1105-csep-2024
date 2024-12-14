package client.ui;

import client.controllers.NoteCtrl;
import client.scenes.DashboardCtrl;
import commons.Note;
import jakarta.ws.rs.ClientErrorException;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class NoteListItem extends ListCell<Note> {

    // Utillities
    private final DialogStyler dialogStyler = new DialogStyler();

    // References
    private Label overviewTitle;
    private Label markdownTitle;
    private TextArea overviewBody;
    private DashboardCtrl controller;
    private NoteCtrl noteCtrl;

    // List cell content
    private final Label noteTitle;
    private final Button deleteButton;
    private final HBox hBox;
    private TextField textField;

    // Variables
    private String originalTitle;
    private long lastClickTime = 0;
    private static final int DOUBLE_CLICK_TIMEFRAME = 400;

    public NoteListItem(Label overviewTitle,Label markdownTitle, TextArea overviewBody, DashboardCtrl controller, NoteCtrl noteCtrl) {

        this.overviewTitle = overviewTitle;
        this.markdownTitle = markdownTitle;
        this.overviewBody = overviewBody;
        this.controller = controller;
        this.noteCtrl = noteCtrl;

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
            if (item == null) return;

            if (newValue) {
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

            handleReferenceTitleChange(item, oldTitle, uniqueTitle);

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

    // This could be done more smart by having a db table with references?
    private void handleReferenceTitleChange(Note item, String oldTitle, String uniqueTitle) {
        controller.getCollectionNotes().stream()
                .filter(note -> note.collection.equals(item.collection))
                .filter(note -> note.body.contains("[[" + oldTitle + "]]"))
                .forEach(note -> {
                    note.body = note.body.replace("[[" + oldTitle + "]]", "[[" + uniqueTitle + "]]");
                    noteCtrl.updatePendingNotes.add(note);
                });
    }


    private void revertToLabel() {
        hBox.getChildren().clear();
        Region spacer = new Region();
        hBox.getChildren().addAll(noteTitle, spacer, deleteButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        overviewTitle.setText(getItem().getTitle());
        markdownTitle.setText(getItem().getTitle());
        overviewBody.setText(getItem().getBody());
    }
}
