package client.controllers;

import client.scenes.DashboardCtrl;
import client.ui.DialogStyler;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import jakarta.ws.rs.ClientErrorException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class CollectionCtrl {

    // Utilities
    private final ServerUtils server;
    private Config config;
    private NoteCtrl noteCtrl;
    private DashboardCtrl dashboardCtrl;
    private SearchCtrl searchCtrl;
    private DialogStyler dialogStyler = new DialogStyler();

    // References
    private ListView collectionView;
    private Label currentCollectionTitle;
    private Menu collectionMenu;
    private ToggleGroup collectionSelect;
    private RadioMenuItem allNotesButton;
    private MenuItem editCollectionTitle;
    private Button deleteCollectionButton;



    @Inject
    public CollectionCtrl(ServerUtils server, Config config, NoteCtrl noteCtrl, SearchCtrl searchCtrl) {
        this.server = server;
        this.config = config;
        this.noteCtrl = noteCtrl;
        this.searchCtrl = searchCtrl;
    }

    public void setReferences(ListView collectionView,
                              Label currentCollectionTitle,
                              Menu collectionMenu,
                              ToggleGroup collectionSelect,
                              RadioMenuItem allNotesButton,
                              MenuItem editCollectionTitle,
                              Button deleteCollectionButton) {
        this.collectionView = collectionView;
        this.currentCollectionTitle = currentCollectionTitle;
        this.collectionMenu = collectionMenu;
        this.collectionSelect = collectionSelect;
        this.allNotesButton = allNotesButton;
        this.editCollectionTitle = editCollectionTitle;
        this.deleteCollectionButton = deleteCollectionButton;
    }
    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
    }
    public List<Collection> setUp() {
        collectionSelect.selectToggle(allNotesButton);

        List<Collection> collections;
        // If the default collection doesn't exist, create it
        try {
            if (config.readFromFile().isEmpty()) {
                Collection defaultCollection = server.addCollection(new Collection("Default"));
                config.writeToFile(defaultCollection);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Set up the collections menu
        try {
            collections = config.readFromFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Collection c : collections) {
            dashboardCtrl.createCollectionButton(c,collectionMenu, collectionSelect);
        }
        return collections;
    }


    public ObservableList<Note> viewNotes(Collection currentCollection, ObservableList<Note> allNotes) {
        dashboardCtrl.setSearchIsActive(false);
        ObservableList<Note> collectionNotes;
        if (currentCollection == null) {
            collectionNotes = allNotes;
            currentCollectionTitle.setText("All Notes");
        } else {
            collectionNotes = FXCollections.observableArrayList(
                    allNotes.stream()
                            .filter(note -> note.collection.equals(currentCollection))
                            .collect(Collectors.toList())
            );
            currentCollectionTitle.setText(currentCollection.title);
        }

        collectionView.setItems(collectionNotes);
        collectionView.getSelectionModel().clearSelection();

        boolean deleteDisabled = currentCollection == null || currentCollectionTitle.getText().equals("Default");
        deleteCollectionButton.setDisable(deleteDisabled);
        editCollectionTitle.setDisable(deleteDisabled);

        collectionView.getSelectionModel().clearSelection();
        return collectionNotes!=null?collectionNotes : FXCollections.observableArrayList();
    }

    public Collection deleteCollection(Collection currentCollection,
                                       List<Collection> collections,
                                       ObservableList<Note> collectionNotes,
                                       ObservableList<Note> allNotes) throws IOException {
        Alert alert = dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                "Delete collection",
                "Delete collection",
                "Are you sure you want to delete this collection? All notes in the collection will be deleted as well."
        );
        Optional<ButtonType> buttonType = alert.showAndWait();

        if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
            List<Note> notesToDelete = collectionNotes.stream().toList();
            for (Note n : notesToDelete) {
                noteCtrl.deleteNote(n,collectionNotes,allNotes);
            }
            // delete collection from server
            server.deleteCollection(currentCollection.id);
            // delete collection from config file
            collections.remove(currentCollection);

            config.writeAllToFile(collections);
            // delete collection from collections menu
            collectionMenu.getItems().remove(
                    collectionSelect.getSelectedToggle()
            );
            collectionSelect.selectToggle(allNotesButton);
            return null;
        }
        return currentCollection;
    }

    /**
     * Method that will change the title in collection
     *
     * @throws IOException exception
     */
    public Collection changeTitleInCollection(Collection currentCollection, List<Collection> collections) throws IOException {

        String oldTitle = currentCollection.title;

        // Ask for a new title with a dialog
        TextInputDialog dialog = dialogStyler.createStyledTextInputDialog(
                "Change Collection Title",
                "Change Collection Title",
                "Please enter the new title for the collection:"
        );

        Optional<String> newTitleOptional = dialog.showAndWait();

        // If we get a title
        if (newTitleOptional.isPresent()) {
            String newTitle = newTitleOptional.get().trim();

            try {
                // Update the collection's title
                currentCollection.title = newTitle;
                // Update the collection on the server
                server.updateCollection(currentCollection);

            } catch (ClientErrorException e) {
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.ERROR,
                        "Error",
                        "Error",
                        e.getResponse().readEntity(String.class)
                );
                alert.showAndWait();

                currentCollection.title = oldTitle;
                return currentCollection;
            }

            config.writeAllToFile(collections);

            // update the menu item
            ((RadioMenuItem) collectionSelect.getSelectedToggle()).setText(newTitle);
            currentCollectionTitle.setText(newTitle);
        }
        return currentCollection;
    }

    /**
     * A method used to move note from one collection to the other
     *
     * @throws IOException exception
     */
    public Collection moveNoteFromCollection(Note currentNote, Collection currentCollection, List<Collection> collections) throws IOException {
        if (currentNote == null) {
            return currentCollection;
        }

        // Ask for a new title with a dialog
        TextInputDialog dialog = dialogStyler.createStyledTextInputDialog(
                "Move Note",
                "Move Note",
                "Please enter the title of destination collection:"
        );
        Optional<String> destinationCollectionTitle = dialog.showAndWait();

        // If user provided a title of destination collection
        if (destinationCollectionTitle.isPresent()) {
            String destinationTitle = destinationCollectionTitle.get().trim();

            // If user provided a title that is empty
            if (destinationTitle.isEmpty()) {
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.ERROR,
                        "Error",
                        "Error",
                        "A destination collection needs a title"
                );
                alert.showAndWait();
                return currentCollection;
            }

            // If user will choose the same collection
            if (destinationTitle.equals(currentCollection.title)) {
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.WARNING,
                        "Warning",
                        "Warning",
                        "Cannot move the note to the same collection"
                );
                alert.showAndWait();
                return currentCollection;
            }

            Collection destinationCollection;
            try {
                destinationCollection = server.getCollectionByTitle(destinationTitle);
            } catch (ClientErrorException e) {
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.ERROR,
                        "Error",
                        "Error",
                        e.getResponse().readEntity(String.class)
                );
                alert.showAndWait();
                return currentCollection;
            }

            //Move the note from previous collection to the new collection
            currentCollection.notes.remove(currentNote);
            destinationCollection.notes.add(currentNote);

            // Update the note in server (we changed its collection)
            currentNote.collection = destinationCollection;
            server.updateNote(currentNote);

            // Save collections locally
            config.writeAllToFile(collections);

            return destinationCollection;
        }
        return currentCollection;
    }

    public Collection addCollection(Collection currentCollection, List<Collection> collections) throws IOException {
        Collection addedCollection;

        TextInputDialog dialog = dialogStyler.createStyledTextInputDialog(
                "New collection",
                "New collection",
                "Please enter the title for your new collection:"
        );
        Optional<String> collectionTitle = dialog.showAndWait();
        if (collectionTitle.isPresent()) {
            String s = collectionTitle.get();

            try {
                addedCollection = server.addCollection(new Collection(s));
                config.writeToFile(addedCollection);
                collections.add(addedCollection);
            } catch (ClientErrorException e) {
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.ERROR,
                        "Error",
                        "Error",
                        e.getResponse().readEntity(String.class)
                );
                alert.showAndWait();
                return currentCollection;
            }

            // add entry in collections menu
            RadioMenuItem radioMenuItem = dashboardCtrl.createCollectionButton(addedCollection, collectionMenu, collectionSelect);
            collectionSelect.selectToggle(radioMenuItem);

            return addedCollection;
        }
        return currentCollection;
    }


}
