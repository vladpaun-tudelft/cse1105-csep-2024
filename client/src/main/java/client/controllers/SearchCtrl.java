package client.controllers;

import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

public class SearchCtrl {

    // References
    private TextField searchField;
    private ListView<Note> collectionView;
    private TreeView<Note> treeView;
    private TextArea noteBody;

    // Variables
    private boolean searchIsActive = false;

    @Inject
    public SearchCtrl() {}

    /**
     * Set up references for the search bar and collection view.
     *
     * @param searchField    TextField for search input
     * @param collectionView ListView for displaying notes
     */
    public void setReferences(TextField searchField, ListView<Note> collectionView, TreeView<Note> treeView, TextArea noteBody) {
        this.searchField = searchField;
        this.collectionView = collectionView;
        this.treeView = treeView;
        this.noteBody = noteBody;
    }

    /**
     * Activates or deactivates the search mode.
     *
     * @param isActive        Whether search is active
     * @param collectionNotes Original collection of notes
     */
    public void setSearchIsActive(boolean isActive, ObservableList<Note> collectionNotes) {
        this.searchIsActive = isActive;
        if (!isActive) {
            resetSearch(collectionNotes);
        }
    }

    /**
     * Perform a search on the provided collection of notes.
     *
     * @param collectionNotes Original collection of notes to search within
     */
    public void search(ObservableList<Note> collectionNotes) {
        String searchText = searchField.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            resetSearch(collectionNotes);
            return;
        }

        searchIsActive = true;
        List<Note> filteredNotes = collectionNotes.stream()
                .filter(note -> containsText(note, searchText))
                .collect(Collectors.toList());

        updateCollectionView(FXCollections.observableArrayList(filteredNotes));
    }

    public void searchInTreeView(TreeView<Note> allNotesView, ObservableList<Note> allNotes, List<Collection> collections) {
        String searchText = searchField.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            resetSearch(allNotes);
            return;
        }

        searchIsActive = true;

        // Create a list to store filtered TreeItems
        TreeItem<Note> virtualRoot = new TreeItem<>(null);
        virtualRoot.setExpanded(true); // Optional: if you want the root to be expanded by default

        // Filter TreeView notes based on searchText
        for (Collection collection : collections) {
            TreeItem<Note> collectionItem = new TreeItem<>(new Note(collection.title, null, collection));
            List<Note> collectionNotes = allNotes.stream()
                    .filter(n -> n.collection.equals(collection) && containsText(n, searchText))
                    .collect(Collectors.toList());

            if (!collectionNotes.isEmpty()) {
                for (Note note : collectionNotes) {
                    TreeItem<Note> noteItem = new TreeItem<>(note);
                    collectionItem.getChildren().add(noteItem);
                }
                virtualRoot.getChildren().add(collectionItem);
                collectionItem.setExpanded(true);
            }
        }

        // Update TreeView with filtered items
        allNotesView.setRoot(virtualRoot);
        allNotesView.setShowRoot(false); // Hide the virtual root if you don't want it to be visible
    }


    /**
     * Reset the search field and display the original collection of notes.
     *
     * @param collectionNotes Original collection of notes
     */
    public void resetSearch(ObservableList<Note> collectionNotes) {
        searchField.clear();
        updateCollectionView(collectionNotes);
    }

    /**
     * Updates the collection view with the given list of notes.
     *
     * @param notes List of notes to display
     */
    private void updateCollectionView(ObservableList<Note> notes) {
        collectionView.setItems(notes);
        collectionView.getSelectionModel().clearSelection();
        treeView.getSelectionModel().clearSelection();
    }

    /**
     * Checks if the note's title or body contains the given search text.
     *
     * @param note       Note to check
     * @param searchText Text to search for
     * @return true if the note contains the text, false otherwise
     */
    private boolean containsText(Note note, String searchText) {
        return note.title.toLowerCase().contains(searchText) ||
                note.body.toLowerCase().contains(searchText);
    }
}
