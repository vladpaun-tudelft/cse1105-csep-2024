package client.controllers;

import client.utils.Config;
import client.utils.ServerUtils;
import commons.Collection;
import commons.Note;
import jakarta.ws.rs.ClientErrorException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


public class CollectionCtrl {

    private final ServerUtils server;
    private Config config;


    public CollectionCtrl(ServerUtils server, Config config) {
        this.server = server;
        this.config = config;
    }

    public void viewNotes(Collection currentCollection,
                          ListView collectionView,
                          ObservableList<Note> collectionNotes,
                          Label currentCollectionTitle,
                          Button deleteCollectionButton,
                          MenuItem editCollectionTitle) {
        if (currentCollection == null) {
            collectionNotes = FXCollections.observableArrayList(server.getAllNotes());
            currentCollectionTitle.setText("All Notes");
        } else {
            collectionNotes = FXCollections.observableArrayList(server.getNotesByCollection(currentCollection));
            currentCollectionTitle.setText(currentCollection.title);
        }

        collectionView.setItems(collectionNotes);

        boolean deleteDisabled = currentCollection == null || currentCollectionTitle.equals("Default");
        deleteCollectionButton.setDisable(deleteDisabled);
        editCollectionTitle.setDisable(deleteDisabled);

        collectionView.getSelectionModel().clearSelection();
    }

    public void deleteCollection(Collection currentCollection,
                                 List<Collection> collections,
                                 Menu collectionMenu,
                                 ToggleGroup collectionSelect,
                                 RadioMenuItem allNotesButton) throws IOException {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete collection");
        alert.setContentText("Are you sure you want to delete this collection? All notes in the collection will be deleted as well.");
        Optional<ButtonType> buttonType = alert.showAndWait();

        if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
            List<Note> notesInCollection = server.getNotesByCollection(currentCollection);
            //TODO check if we need to do this
            for (Note n : notesInCollection) {
                // noteCtrl.deleteNote(n);
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
        }
    }

    /**
     * Method that will change the title in collection
     *
     * @throws IOException exception
     */
    public void changeTitleInCollection(Collection currentCollection,
                                        List<Collection> collections,
                                        ToggleGroup collectionSelect,
                                        Label currentCollectionTitle) throws IOException {

        // Get the collection title
        String currentCollectionString = currentCollection.title;

        // Ask for a new title with a dialog
        TextInputDialog dialog = new TextInputDialog(currentCollectionString);
        dialog.setTitle("Change Collection Title");
        dialog.setContentText("Please enter the new title for the collection:");

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
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText(e.getResponse().readEntity(String.class));
                alert.showAndWait();
                return;
            }

            config.writeAllToFile(collections);

            // update the menu item
            ((RadioMenuItem) collectionSelect.getSelectedToggle()).setText(newTitle);
            currentCollectionTitle.setText(newTitle);
        }
    }

    /**
     * A method used to move note from one collection to the other
     *
     * @throws IOException exception
     */
    public Collection moveNoteFromCollection(Collection currentCollection,
                                             ListView collectionView,
                                             List<Collection> collections) throws IOException {
        // Get the currently selected note
        Note currentNote = (Note) collectionView.getSelectionModel().getSelectedItem();
        if (currentNote == null) {
            return null;
        }

        // Ask for a new title with a dialog
        TextInputDialog dialog = new TextInputDialog(currentCollection.title);
        dialog.setTitle("Move Note");
        dialog.setContentText("Please enter the title of destination collection:");
        Optional<String> destinationCollectionTitle = dialog.showAndWait();

        // If user provided a title of destination collection
        if (destinationCollectionTitle.isPresent()) {
            String destinationTitle = destinationCollectionTitle.get().trim();

            // If user provided a title that is empty
            if (destinationTitle.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("A destination collection needs a title");
                alert.showAndWait();
                return currentCollection;
            }

            // If user will choose the same collection
            if (destinationTitle.equals(currentCollection.title)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setContentText("Cannot move the note to the same collection");
                alert.showAndWait();
                return currentCollection;
            }

            Collection destinationCollection;
            try {
                destinationCollection = server.getCollectionByTitle(destinationTitle);
            } catch (ClientErrorException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText(e.getResponse().readEntity(String.class));
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

    public Collection addCollection(List<Collection> collections,
                                    ToggleGroup collectionSelect,
                                    Menu collectionMenu) throws IOException {
        Collection addedCollection;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New collection");
        dialog.setContentText("Please enter the title for your new collection");
        Optional<String> collectionTitle = dialog.showAndWait();
        if (collectionTitle.isPresent()) {
            String s = collectionTitle.get();

            try {
                addedCollection = server.addCollection(new Collection(s));
                config.writeToFile(addedCollection);
                collections.add(addedCollection);
            } catch (ClientErrorException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText(e.getResponse().readEntity(String.class));
                alert.showAndWait();
                return null;
            }

            // add entry in collections menu
            RadioMenuItem radioMenuItem = new RadioMenuItem(s);
            radioMenuItem.setToggleGroup(collectionSelect);
            radioMenuItem.setStyle("-fx-text-fill: #000000");
            radioMenuItem.setOnAction(Event -> {
                //viewNotes();
            });
            collectionMenu.getItems().addFirst(radioMenuItem);
            collectionSelect.selectToggle(radioMenuItem);

            // Something to view the new collection
            return addedCollection;
        }
        return null;
    }
}
