package client.controllers;

import client.LanguageManager;
import client.scenes.DashboardCtrl;
import client.ui.DialogStyler;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class NoteCtrl {

    // Utilities
    private final ServerUtils server;
    private DialogStyler dialogStyler = new DialogStyler();

    // Dashboard reference
    private DashboardCtrl dashboardCtrl;
    private NotificationsCtrl notificationsCtrl;

    // References
    private ListView collectionView;
    private TreeView treeView;
    private Label noteTitle;
    private Label noteTitleMd;
    private TextArea noteBody;
    private WebView markdownView;
    private Label contentBlocker;
    private TextField searchField;
    private Label filesViewBlocker;
    private MenuButton moveNotesButton;

    // Variables
    @Getter @Setter private List<Note> createPendingNotes;
    @Getter @Setter private List<Note> updatePendingNotes;

    private Config config;
    private LanguageManager languageManager;
    private ResourceBundle bundle;

    private long tempNoteId = -1;

    @Inject
    public NoteCtrl(ServerUtils server, Config config, NotificationsCtrl notificationsCtrl) {
        this.server = server;
        createPendingNotes = new ArrayList<>();
        updatePendingNotes = new ArrayList<>();
        this.notificationsCtrl = notificationsCtrl;

        this.config = config;
        this.languageManager = LanguageManager.getInstance(this.config);
        this.bundle = this.languageManager.getBundle();
    }

    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
    }

    public void setReferences(
            ListView collectionView,
            TreeView treeView,
            Label noteTitle,
            Label noteTitleMd,
            TextArea noteBody,
            WebView markdownView,
            Label contentBlocker,
            TextField searchField,
            Label filesViewBlocker,
            MenuButton moveNotesButton
    ) {
        this.collectionView = collectionView;
        this.treeView = treeView;
        this.noteTitle = noteTitle;
        this.noteTitleMd = noteTitleMd;
        this.noteBody = noteBody;
        this.markdownView = markdownView;
        this.contentBlocker = contentBlocker;
        this.searchField = searchField;
        this.filesViewBlocker = filesViewBlocker;
        this.moveNotesButton = moveNotesButton;
    }

    public Note addNote(Collection currentCollection,
                        ObservableList<Note> allNotes,
                        ObservableList<Note> collectionNotes) {
        Collection collection = currentCollection != null ? currentCollection : dashboardCtrl.getDefaultCollection();

        Note newNote = generateNewNote(allNotes, collection);

        allNotes.add(newNote);
        if (currentCollection != null) {
            collectionView.getItems().add(newNote);
            collectionView.getSelectionModel().clearSelection();
            collectionView.getSelectionModel().select(newNote);
        } else {
            dashboardCtrl.selectNoteInTreeView(newNote);
        }

        return newNote;
    }

    private Note generateNewNote(ObservableList<Note> allNotes, Collection collection) {
        // Generate a unique title
        String baseTitle = "New Note";
        Note newNote = new Note(baseTitle, "", collection);

        String newTitle = generateUniqueTitle(allNotes, newNote, baseTitle, true);

        newNote.title = newTitle;
        newNote.id = this.tempNoteId--;

        server.send("/app/notes", newNote);

        noteTitle.setText(newTitle);
        noteTitleMd.setText(newTitle);

        noteBody.setText("");

        notificationsCtrl.pushNotification(bundle.getString("creationSuccess"), false);
        return newNote;
    }

    public void updateViewAfterAdd(Collection currentCollection, ObservableList<Note> allNotes,ObservableList<Note> collectionNotes, Note note) {
        boolean addedNoteIsSelectedTreeView = false;
        boolean addedNoteIsSelectedCollectionView = false;
        if (collectionView.getSelectionModel().getSelectedItem() != null) {
            Note selectedNote = (Note)collectionView.getSelectionModel().getSelectedItem();
            if (selectedNote.title.equals(note.title) && selectedNote.collection.equals(note.collection)) {
                addedNoteIsSelectedCollectionView = true;
            }
        }
        if (treeView.getSelectionModel().getSelectedItem() != null) {
            if(((TreeItem)treeView.getSelectionModel().getSelectedItem()).getValue() instanceof Note n && n.title.equals(note.title) && n.collection.equals(note.collection)){
                addedNoteIsSelectedTreeView = true;
            }
        }
        allNotes.removeIf(n -> n.title.equals(note.title) && n.collection.equals(note.collection));
        allNotes.add(note);
        if (currentCollection != null && currentCollection.equals(note.collection)) {
            collectionNotes.removeIf(n -> n.title.equals(note.title) && n.collection.equals(note.collection));
            collectionNotes.add(note);
        }

        if(addedNoteIsSelectedTreeView) {
            treeView.getSelectionModel().clearSelection();
            dashboardCtrl.selectNoteInTreeView(note);
            dashboardCtrl.filter();
        }
        if(addedNoteIsSelectedCollectionView) {
            collectionView.getSelectionModel().select(note);
        }
    }

    public void showCurrentNote(Note selectedNote) {
        if (selectedNote == null) return;

        Platform.runLater(() -> {
            moveNotesButton.setText(selectedNote.collection.title);

            noteTitle.setText(selectedNote.title);
            noteTitle.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            // Here, the +45 to be changed with +5 when we remove the trash icons
            noteTitle.maxWidthProperty().bind(noteBody.widthProperty() .subtract(moveNotesButton.widthProperty()) .subtract(45));

            noteTitleMd.setText(selectedNote.title);
            noteTitleMd.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            // Again, here, when the trash icons are removed, we can remove the subtract, or make it like 5px
            noteTitleMd.maxWidthProperty().bind(markdownView.widthProperty().subtract(40));

            noteBody.setText(selectedNote.body);
            contentBlocker.setVisible(false);
            filesViewBlocker.setVisible(false);
            dashboardCtrl.getFilesCtrl().showFiles(selectedNote);
            dashboardCtrl.getMarkdownCtrl().setCurrentNote(selectedNote);
            dashboardCtrl.getMarkdownCtrl().updateMarkdownView(selectedNote.getBody());

             noteBody.requestFocus();
        });
    }

    public void deleteSelectedNote(Note currentNote,
                                   ObservableList<Note> collectionNotes,
                                   ObservableList<Note> allNotes) {
        if (currentNote != null) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.CONFIRMATION,
                    bundle.getString("confirmDeletion.text"),
                    bundle.getString("confirmDeletion.text"),
                    bundle.getString("deleteNoteConfirmation.text")
            );
            Optional<ButtonType> buttonType = alert.showAndWait();

            if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
                deleteNote(currentNote, collectionNotes, allNotes);
                noteBody.clear();
                notificationsCtrl.pushNotification(bundle.getString("deleteSuccess"), false);
            }
        }

        Platform.runLater(() -> dashboardCtrl.filter());
    }

    public void deleteNote(Note currentNote,
                           ObservableList<Note> collectionNotes,
                           ObservableList<Note> allNotes) {
        updatePendingNotes.remove(currentNote);
        server.send("/app/deleteNote", currentNote);

        removeNoteFromClient(currentNote, collectionNotes, allNotes);

        Platform.runLater(() -> dashboardCtrl.filter());
    }

    public void removeNoteFromClient(Note currentNote, ObservableList<Note> collectionNotes, ObservableList<Note> allNotes) {
        allNotes.remove(currentNote);
        if (collectionView != null) {
            collectionView.getItems().remove(currentNote);
            collectionView.refresh();
        }
    }

    public void updateAfterDelete(Note currentNote,
                                  ObservableList<Note> allNotes,
                                  ObservableList<Note> collectionNotes) {
        updatePendingNotes.remove(currentNote);

        allNotes.remove(currentNote);
        collectionNotes.remove(currentNote);
    }

    public void saveAllPendingNotes() {
        try {
            for (Note note : updatePendingNotes) {
                server.updateNote(note);
            }
            updatePendingNotes.clear();
        } catch (Exception e) {
            throw e;
        }
    }

    public void onBodyChanged(Note currentNote) {
        if (currentNote != null) {
            String rawText = noteBody.getText();
            currentNote.setBody(rawText);

            // Add any edited but already existing note to the pending list
            if (!updatePendingNotes.contains(currentNote)) {
                updatePendingNotes.add(currentNote);
            }
        }
    }

    /**
     * Generates a unique title by appending "(1)", "(2)", etc., if needed.
     */
    public String generateUniqueTitle(ObservableList<Note> allNotes, Note note, String baseTitle, boolean isGlobal) {
        String uniqueTitle = baseTitle;
        int counter = 1;

        while (isTitleDuplicate(allNotes, note, uniqueTitle, isGlobal)) {
            uniqueTitle = baseTitle + " (" + counter + ")";
            counter++;
        }

        return uniqueTitle;
    }

    /**
     * Checks if the given title is already used in the collection, excluding the current note.
     */
    public boolean isTitleDuplicate(ObservableList<Note> allNotes, Note newNote, String title, boolean isGlobal) {
        return allNotes.stream()
                .filter(note -> isGlobal || (note.collection.equals(newNote.collection)))
                .anyMatch(note -> note != newNote && note.getTitle().equals(title));
    }

    /**
     * Method that delete multiple notes in the collection view
     * @param allNotes all notes
     * @param selectedItems selected notes
     * @param collectionNotes collection notes
     */
    public void deleteMultipleNotes(ObservableList<Note> allNotes, ObservableList<Note> selectedItems, ObservableList<Note> collectionNotes) {
        if (selectedItems != null) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.CONFIRMATION,
                    bundle.getString("confirmDeletion.text"),
                    bundle.getString("confirmDeletion.text"),
                    bundle.getString("deleteMultipleNotesConfirmation.text")
            );
            Optional<ButtonType> buttonType = alert.showAndWait();
            List<Note> notesToDelete = new ArrayList<>(selectedItems);
            if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
                for (Note note : notesToDelete) {
                    deleteNote(note, collectionNotes, allNotes);
                    noteBody.clear();
                }
                collectionView.getSelectionModel().clearSelection();
            }
        }
    }


    /**
     * Method that delete multiple notes in the all notes view
     * @param allNotes all notes
     * @param selectedItems selected notes
     * @param collectionNotes collection notes
     */
    public void deleteMultipleNotesInTreeView(ObservableList<Note> allNotes, ObservableList<TreeItem<Note>> selectedItems, ObservableList<Note> collectionNotes) {
        if (selectedItems != null) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.CONFIRMATION,
                    bundle.getString("confirmDeletion.text"),
                    bundle.getString("confirmDeletion.text"),
                    bundle.getString("deleteMultipleNotesConfirmation.text")
            );
            Optional<ButtonType> buttonType = alert.showAndWait();
            List<TreeItem<Note>> notesToDelete = new ArrayList<>(selectedItems);
            if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
                for (TreeItem<Note> treeItem:  notesToDelete) {
                    deleteNote(treeItem.getValue(), collectionNotes, allNotes);
                    noteBody.clear();
                }
                dashboardCtrl.allNotesView.getSelectionModel().clearSelection();

            }
        }
        dashboardCtrl.refreshTreeView();
    }
}

