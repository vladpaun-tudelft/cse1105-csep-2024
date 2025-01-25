package client.ui;

import client.LanguageManager;
import client.controllers.NoteCtrl;
import client.controllers.NotificationsCtrl;
import client.entities.Action;
import client.entities.ActionType;
import client.scenes.DashboardCtrl;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import jakarta.ws.rs.ClientErrorException;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.ResourceBundle;

public class CustomTreeCell extends TreeCell<Object> {

    private final DashboardCtrl dashboardCtrl;
    private final NotificationsCtrl notificationsCtrl;
    private final NoteCtrl noteCtrl;

    // Components for Notes
    private final Label noteTitleLabel;
    private final Region noteSpacer;
    private final Button editNoteButton;
    private final Button deleteNoteButton;
    private final HBox noteHBox;
    private TextField noteTextField;

    // Components for Collections
    private final Label collectionTitleLabel;
    private final Region collectionSpacer;
    private final Button favouriteButton;
    private final HBox collectionHBox;

    // Variables for Notes
    private String originalNoteTitle;
    private long lastClickTime = 0;
    private static final int DOUBLE_CLICK_TIMEFRAME = 400;
    private DialogStyler dialogStyler;

    @Inject
    private Config config;
    private LanguageManager manager;
    private ResourceBundle bundle;
    private final ServerUtils server;


    public CustomTreeCell(DashboardCtrl dashboardCtrl, NoteCtrl noteCtrl, DialogStyler dialogStyler , NotificationsCtrl notificationsCtrl, ServerUtils server) {
        this.dashboardCtrl = dashboardCtrl;
        this.notificationsCtrl = notificationsCtrl;
        this.noteCtrl = noteCtrl;
        this.dialogStyler = dialogStyler;
        this.manager = LanguageManager.getInstance(this.config);
        this.bundle = this.manager.getBundle();
        this.server = server;

        // Initialize Note components
        noteTitleLabel = new Label();
        noteTitleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        noteTitleLabel.setWrapText(false);
        noteTitleLabel.setMinWidth(0);

        noteSpacer = new Region();

        editNoteButton = new Button();
        editNoteButton.getStyleClass().addAll("icon", "note_list_icon", "edit_icon");
        editNoteButton.setCursor(Cursor.HAND);
        Tooltip editNoteTooltip = new Tooltip(bundle.getString("editNote.text"));
        editNoteTooltip.setShowDelay(Duration.seconds(0.2));
        editNoteButton.setTooltip(editNoteTooltip);

        deleteNoteButton = new Button();
        deleteNoteButton.getStyleClass().addAll("icon", "note_list_icon", "delete_icon");
        deleteNoteButton.setCursor(Cursor.HAND);
        Tooltip deleteNoteTooltip = new Tooltip(bundle.getString("deleteNote.text"));
        deleteNoteTooltip.setShowDelay(Duration.seconds(0.2));
        deleteNoteButton.setTooltip(deleteNoteTooltip);

        noteHBox = new HBox(0);
        noteHBox.getChildren().addAll(noteTitleLabel, noteSpacer, editNoteButton, deleteNoteButton);
        HBox.setHgrow(noteSpacer, Priority.ALWAYS);
        noteHBox.setAlignment(Pos.CENTER_LEFT);

        noteHBox.setMinHeight(30);
        noteHBox.setPrefHeight(30);
        noteHBox.setMaxHeight(30);

        configureNoteEventHandlers();

        // Initialize Collection components
        collectionTitleLabel = new Label();
        collectionTitleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        collectionTitleLabel.setWrapText(false);
        collectionTitleLabel.setMinWidth(0);

        collectionSpacer = new Region();

        favouriteButton = new Button();
        favouriteButton.getStyleClass().addAll("icon", "collection_list_icon", "set_default_collection_icon");
        favouriteButton.setCursor(Cursor.HAND);

        collectionHBox = new HBox(0);
        collectionHBox.getChildren().addAll(collectionTitleLabel, collectionSpacer, favouriteButton);
        HBox.setHgrow(collectionSpacer, Priority.ALWAYS);
        collectionHBox.setAlignment(Pos.CENTER_LEFT);

        collectionHBox.setMinHeight(15);
        collectionHBox.setPrefHeight(15);
        collectionHBox.setMaxHeight(15);

        configureCollectionEventHandlers();
    }

    private void configureNoteEventHandlers() {
        // Delete button functionality

        /*deleteNoteButton.setOnAction(event -> {
            Note note = (Note) getItem();
            if (note != null) {
                dashboardCtrl.deleteSelectedNote();
            }
        });*/
        deleteNoteButton.setOnAction(event -> {
            ObservableList<TreeItem<Note>> treeItems = dashboardCtrl.allNotesView.getSelectionModel().getSelectedItems();
            if (treeItems.size() > 1) {
                noteCtrl.deleteMultipleNotesInTreeView(dashboardCtrl.getAllNotes(), treeItems, dashboardCtrl.getCollectionNotes());
            } else {
                Note note = (Note) getItem();
                if (note != null) {
                    dashboardCtrl.deleteSelectedNote();
                }
            }
        });


        // Edit button functionality
        editNoteButton.setOnAction(event -> startEditing());

        // Double-click to start editing
        noteHBox.setOnMouseClicked(event -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime <= DOUBLE_CLICK_TIMEFRAME) {
                startEditing();
            }
            lastClickTime = currentTime;
        });
    }

    private void configureCollectionEventHandlers() {
        favouriteButton.setOnAction(event -> {
            Collection collection = (Collection) getItem();
            if (collection != null && !collection.equals(dashboardCtrl.getDefaultCollection())) {
                dashboardCtrl.setDefaultCollection(collection);
                dashboardCtrl.refreshTreeView();
            }
        });
    }

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else if (item instanceof Note note) {
            updateNoteItem(note);
        } else if (item instanceof Collection collection) {
            updateCollectionItem(collection);
        } else if (item instanceof String && item.equals(" - no notes in collection.")) {
            setText((String) item);
        }
    }

    private void updateNoteItem(Note note) {
        noteTitleLabel.setText(note.getTitle());

        // Adjust layout based on selection
        deleteNoteButton.setVisible(isSelected());
        editNoteButton.setVisible(isSelected());

        if (isSelected()) {
            noteTitleLabel.maxWidthProperty().bind(dashboardCtrl.getCollectionView().widthProperty().subtract(100));
            if (!noteHBox.getChildren().contains(editNoteButton) || !noteHBox.getChildren().contains(deleteNoteButton)) {
                if (!noteHBox.getChildren().contains(editNoteButton)) {
                    noteHBox.getChildren().add(editNoteButton);
                }
                if (!noteHBox.getChildren().contains(deleteNoteButton)) {
                    noteHBox.getChildren().add(deleteNoteButton);
                }
            }
        } else {
            noteTitleLabel.maxWidthProperty().bind(dashboardCtrl.allNotesView.widthProperty().subtract(50));
            noteHBox.getChildren().remove(editNoteButton);
            noteHBox.getChildren().remove(deleteNoteButton);
        }

        setGraphic(noteHBox);
    }

    private void updateCollectionItem(Collection collection) {
        collectionTitleLabel.setText(collection.title);

        if (collection.equals(dashboardCtrl.getDefaultCollection())) {
            collectionTitleLabel.setText(collection.title + bundle.getString("default.text"));
            favouriteButton.setDisable(true);
            favouriteButton.getStyleClass().add("default_collection_icon");
            favouriteButton.getStyleClass().remove("set_default_collection_icon");
        } else {
            favouriteButton.setDisable(false);
            favouriteButton.getStyleClass().add("set_default_collection_icon");
            favouriteButton.getStyleClass().remove("default_collection_icon");
        }

        collectionTitleLabel.maxWidthProperty().bind(dashboardCtrl.allNotesView.widthProperty().subtract(60));

        setGraphic(collectionHBox);
    }

    public void startEditing() {
        Note note = (Note) getItem();
        if (note == null) return;

        if (!server.isServerAvailable(note.collection.serverURL)) {
            String alertText = bundle.getString("noteUpdateError") + "\n" + note.title;
            dialogStyler.createStyledAlert(
                    Alert.AlertType.INFORMATION,
                    bundle.getString("serverCouldNotBeReached.text"),
                    bundle.getString("serverCouldNotBeReached.text"),
                    alertText
            ).showAndWait();
            return;
        }

        originalNoteTitle = note.getTitle();
        noteTextField = new TextField(originalNoteTitle);
        noteTextField.prefWidthProperty().bind(noteHBox.widthProperty().subtract(10));

        final boolean[] isCommitted = {false};

        noteTextField.setOnAction(event -> {
            if (!isCommitted[0]) {
                isCommitted[0] = true;
                commitTitleChange(note, noteTextField.getText());
            }
        });

        noteTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && !isCommitted[0]) {
                isCommitted[0] = true;
                commitTitleChange(note, noteTextField.getText());
            }
        });

        noteTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                isCommitted[0] = true;
                revertToNoteLabel();
            }
        });

        noteHBox.getChildren().clear();
        noteHBox.getChildren().add(noteTextField);

        noteTextField.requestFocus();
    }

    private void commitTitleChange(Note note, String newTitle) {
        if (note == null) {
            revertToNoteLabel();
            return;
        }

        String oldTitle = note.getTitle();
        String uniqueTitle = noteCtrl.generateUniqueTitle(dashboardCtrl.getAllNotes(), note, newTitle, false);

        try {
            if (uniqueTitle.equals(oldTitle)) {
                throw new IllegalArgumentException("Title cannot be the same as the current title");
            }
            note.setTitle(uniqueTitle);
            noteCtrl.getUpdatePendingNotes().add(note);
            handleReferenceTitleChange(note, oldTitle, uniqueTitle);
            noteCtrl.saveAllPendingNotes();
            noteTitleLabel.setText(uniqueTitle);

            dashboardCtrl.getNoteTitle().setText(uniqueTitle);
            dashboardCtrl.getNoteTitleMD().setText(uniqueTitle);

            originalNoteTitle = newTitle;

            dashboardCtrl.getActionHistory().push(new Action(ActionType.EDIT_TITLE, note, oldTitle, uniqueTitle));
            notificationsCtrl.pushNotification(bundle.getString("validRename"), false);
        } catch (IllegalArgumentException | ClientErrorException e) {
            if (e instanceof IllegalArgumentException) {
                notificationsCtrl.pushNotification(bundle.getString("sameName"), true);
            } else {
                notificationsCtrl.pushNotification(bundle.getString("invalidName"), true);
            }
            note.setTitle(originalNoteTitle);
        }

        revertToNoteLabel();
    }

    private void handleReferenceTitleChange(Note item, String oldTitle, String uniqueTitle) {
        dashboardCtrl.getCollectionNotes().stream()
                .filter(note -> note.collection.equals(item.collection))
                .filter(note -> note.body.contains("[[" + oldTitle + "]]"))
                .forEach(note -> {
                    note.body = note.body.replace("[[" + oldTitle + "]]", "[[" + uniqueTitle + "]]");
                    noteCtrl.getUpdatePendingNotes().add(note);
                });
    }

    private void revertToNoteLabel() {
        noteHBox.getChildren().clear();
        noteHBox.getChildren().addAll(noteTitleLabel, noteSpacer, editNoteButton, deleteNoteButton);
    }
}
