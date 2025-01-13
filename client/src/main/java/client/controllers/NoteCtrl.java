package client.controllers;

import client.scenes.DashboardCtrl;
import client.ui.DialogStyler;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.web.WebView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteCtrl {

    // Utilities
    private final ServerUtils server;
    private DialogStyler dialogStyler = new DialogStyler();

    // Dashboard reference
    private DashboardCtrl dashboardCtrl;

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
    public List<Note> updatePendingNotes = new ArrayList<>();
    private long tempNoteId = -1;

    @Inject
    public NoteCtrl(ServerUtils server) {
        this.server = server;
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

    public void addNote(Collection currentCollection,
                        ObservableList<Note> allNotes,
                        ObservableList<Note> collectionNotes) {
        Collection collection = currentCollection != null ? currentCollection : dashboardCtrl.getDefaultCollection();

        // Generate a unique title
        String baseTitle = "New Note";
        Note newNote = new Note(baseTitle, "", collection);

        String newTitle = generateUniqueTitle(allNotes, newNote, baseTitle, true);

        newNote.title = newTitle;
        newNote.id = this.tempNoteId--;

        server.send("/app/notes", newNote);

        if (currentCollection != null) {
            collectionView.getSelectionModel().select(collectionNotes.size() - 1);
            collectionView.getFocusModel().focus(collectionNotes.size() - 1);
            collectionView.edit(collectionNotes.size() - 1);

        }else {
            dashboardCtrl.selectNoteInTreeView(newNote);
        }

        noteTitle.setText(newTitle);
        noteTitleMd.setText(newTitle);

        noteBody.setText("");

    }

    public void updateViewAfterAdd(Collection currentCollection, ObservableList<Note> allNotes, Note note) {
        if (!allNotes.contains(note)) {
            allNotes.add(note);
        }
        dashboardCtrl.getCollectionCtrl().viewNotes(currentCollection, allNotes);
    }

    public void showCurrentNote(Note selectedNote) {
        if (selectedNote == null) return;

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

        Platform.runLater(() -> noteBody.requestFocus());
    }

    public void deleteSelectedNote(Note currentNote,
                                   ObservableList<Note> collectionNotes,
                                   ObservableList<Note> allNotes) {
        if (currentNote != null) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.CONFIRMATION,
                    "Confirm deletion",
                    "Confirm deletion",
                    "Do you really want to delete this note?"
            );
            Optional<ButtonType> buttonType = alert.showAndWait();

            if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
                deleteNote(currentNote, collectionNotes, allNotes);
                collectionView.getSelectionModel().clearSelection();
                noteBody.clear();
            }
        }
    }

    public void deleteNote(Note currentNote,
                           ObservableList<Note> collectionNotes,
                           ObservableList<Note> allNotes) {
        updatePendingNotes.remove(currentNote);
        server.send("/app/deleteNote", currentNote);
        collectionNotes.remove(currentNote);
        allNotes.remove(currentNote);
        if (currentNote == null) {
            dashboardCtrl.refreshTreeView();
        }
    }

    public void updateAfterDelete(Note currentNote,
                                  Collection currentCollection,
                                  ObservableList<Note> allNotes) {
        updatePendingNotes.remove(currentNote);
        while (allNotes.contains(currentNote)) {
            allNotes.remove(currentNote);
        }
        dashboardCtrl.getCollectionCtrl().viewNotes(currentCollection, allNotes);
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

}
