package client.controllers;

import com.google.inject.Inject;
import commons.Note;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.stream.Collectors;

public class SearchCtrl {

    // References
    private TextField searchField;
    private ListView<Note> collectionView;
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
    public void setReferences(TextField searchField, ListView<Note> collectionView, TextArea noteBody) {
        this.searchField = searchField;
        this.collectionView = collectionView;
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
