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
    private TreeView treeView;
    private MenuButton currentCollectionTitle;
    private Menu collectionMenu;
    private ToggleGroup collectionSelect;
    private MenuItem allNotesButton;
    private MenuItem editCollectionTitle;
    private Button deleteCollectionButton;
    private MenuButton moveNotesButton;




    @Inject
    public CollectionCtrl(ServerUtils server, Config config, NoteCtrl noteCtrl, SearchCtrl searchCtrl) {
        this.server = server;
        this.config = config;
        this.noteCtrl = noteCtrl;
        this.searchCtrl = searchCtrl;
    }


    public void setReferences(ListView collectionView,
                              TreeView treeView,
                              MenuButton currentCollectionTitle,
                              Menu collectionMenu,
                              ToggleGroup collectionSelect,
                              MenuItem allNotesButton,
                              MenuItem editCollectionTitle,
                              Button deleteCollectionButton,
                              MenuButton moveNotesButton) {
        this.collectionView = collectionView;
        this.treeView = treeView;
        this.currentCollectionTitle = currentCollectionTitle;
        this.collectionMenu = collectionMenu;
        this.collectionSelect = collectionSelect;
        this.allNotesButton = allNotesButton;
        this.editCollectionTitle = editCollectionTitle;
        this.deleteCollectionButton = deleteCollectionButton;
        this.moveNotesButton = moveNotesButton;
    }
    /**
     * method used for selecting and switching the collection
     *
     * @param listView    listview
     * @param collections collections
     */

    private void listViewSetupForCollections(
            ListView<Collection> listView,
            ObservableList<Collection> collections
    ) {
        listViewDisplayOnlyTitles(listView, collections);

        //switching to collection
        listView.setOnMouseClicked(event -> {
            Collection selectedCollection = listView.getSelectionModel().getSelectedItem();
            for (Toggle toggle : collectionSelect.getToggles()) {
                if (toggle instanceof RadioMenuItem) {
                    RadioMenuItem item = (RadioMenuItem) toggle;
                    if (item.getText().equals(selectedCollection.title)) {
                        collectionSelect.selectToggle(item);
                        item.fire();
                        currentCollectionTitle.hide();
                        break;
                    }
                }
            }
        });
    }

    /**
     * method that displays only collection titles in listview
     *
     * @param listView    listview
     * @param collections collections
     */
    private void listViewDisplayOnlyTitles(ListView<Collection> listView, ObservableList<Collection> collections) {
        ObservableList<Collection> filteredCollections = collections.filtered(collection -> !collection.equals(dashboardCtrl.getCurrentCollection()));

        listView.setItems(filteredCollections);
        listView.setFixedCellSize(35);
        listView.getStyleClass().add("collection-list-view");
        listView.setFocusTraversable(false);
        int maxVisibleItems = 6;
        listView.setPrefHeight(Math.min(maxVisibleItems, filteredCollections.size()) * listView.getFixedCellSize() + 2);
        listView.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(Collection filteredCollection, boolean empty) {
                super.updateItem(filteredCollection, empty);
                if (empty || filteredCollection == null) {
                    setText(null);
                } else {
                    setText(filteredCollection.title);
                }
            }
        });
    }

    /**
     * a method used to move notes from listview
     *
     * @param listView    listview
     * @param collections collections
     */
    private void listViewSetupForMovingNotes(
            ListView<Collection> listView,
            ObservableList<Collection> collections
    ) {
        listViewDisplayOnlyTitles(listView, collections);
        //switching to collection
        listView.setOnMouseClicked(event -> {
            Collection selectedCollection = listView.getSelectionModel().getSelectedItem();
            for (Toggle toggle : collectionSelect.getToggles()) {
                if (toggle instanceof RadioMenuItem) {
                    RadioMenuItem item = (RadioMenuItem) toggle;
                    if (item.getText().equals(selectedCollection.title)) {
                        Note currentNote = dashboardCtrl.getCurrentNote();
                        try {
                            moveNoteFromCollection(currentNote, dashboardCtrl.getCurrentCollection(), selectedCollection);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        collectionSelect.selectToggle(item);
                        if(dashboardCtrl.getCurrentCollection() != null )item.fire();   // If not in all note view
                        else {
                            dashboardCtrl.treeViewSetup();                             // else update all note view
                            dashboardCtrl.selectNoteInTreeView(currentNote);
                        }
                        moveNotesButton.hide();
                        break;
                    }
                }
            }
        });
    }
    /**
     * dropout menu for collections + for moving notes
     */
    public void initializeDropoutCollectionLabel() {


        ListView<Collection> collectionListView = new ListView<>();
        ObservableList<Collection> collections = FXCollections.observableArrayList(dashboardCtrl.getCollections());
        listViewSetupForCollections(collectionListView, collections);

        CustomMenuItem scrollableCollectionsItem = new CustomMenuItem(collectionListView, false);


        Collection currentCollection = dashboardCtrl.getCurrentCollection();

        Label dynamicLabel = new Label();
        if (currentCollection != null && currentCollection.title != null) {
            dynamicLabel.setText("Current: " + currentCollection.title);
        } else {
            dynamicLabel.setText("No collection selected");
        }
        dynamicLabel.getStyleClass().add("current-collection-label");

        CustomMenuItem dynamicLabelItem = new CustomMenuItem(dynamicLabel, false);
        currentCollectionTitle.getItems().set(4, dynamicLabelItem);
        currentCollectionTitle.getItems().set(6, scrollableCollectionsItem);
        currentCollectionTitle.setOnHidden(event -> {
            initializeDropoutCollectionLabel();
        });
        currentCollectionTitle.setOnShowing(event -> {
            initializeDropoutCollectionLabel();
        });

    }

    public void moveNotesInitialization() {


        ListView<Collection> collectionListView2 = new ListView<>();
        ObservableList<Collection> collections = FXCollections.observableArrayList(dashboardCtrl.getCollections());
        listViewSetupForMovingNotes(collectionListView2, collections);
        CustomMenuItem scrollableCollectionsItem2 = new CustomMenuItem(collectionListView2, false);
        Collection currentCollectionForNote = null;
        if (dashboardCtrl.getCurrentNote() != null) {
            currentCollectionForNote = dashboardCtrl.getCurrentNote().collection;
        }

        Label dynamicLabel = new Label();
        Label pickNoteDestination = new Label();

        if (currentCollectionForNote != null && currentCollectionForNote.title != null) {
            dynamicLabel.setText("Assigned to: " + currentCollectionForNote.title);
        } else {
            dynamicLabel.setText("No collection assigned");
        }
        dynamicLabel.getStyleClass().add("current-collection-label");
        pickNoteDestination.getStyleClass().add("pick-note-destination");
        CustomMenuItem dynamicLabelItem = new CustomMenuItem(dynamicLabel, false);

        pickNoteDestination.setText("Pick Note Destination");
        CustomMenuItem pickNoteDestinationItem = new CustomMenuItem(pickNoteDestination, false);
        moveNotesButton.getItems().set(0, dynamicLabelItem);
        moveNotesButton.getItems().set(2, pickNoteDestinationItem);
        moveNotesButton.getItems().set(3, scrollableCollectionsItem2);
        moveNotesButton.setOnHidden(event -> {
            moveNotesInitialization();
        });
        moveNotesButton.setOnShowing(event -> {
            moveNotesInitialization();
        });


    }


    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
    }
    public List<Collection> setUp() {

        //is this necessary if allNotesButton is menu Item is menu Item?
        //collectionSelect.selectToggle( allNotesButton);

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
            collectionView.setVisible(false);
            treeView.setVisible(true);
            dashboardCtrl.treeViewSetup();
            collectionView.getSelectionModel().clearSelection();
        } else {
            collectionNotes = FXCollections.observableArrayList(
                    allNotes.stream()
                            .filter(note -> note.collection.equals(currentCollection))
                            .collect(Collectors.toList())
            );
            currentCollectionTitle.setText(currentCollection.title);
            collectionView.setVisible(true);
            treeView.setVisible(false);
            treeView.getSelectionModel().clearSelection();
        }

        collectionView.setItems(collectionNotes);
        collectionView.getSelectionModel().clearSelection();

        boolean deleteDisabled = (currentCollection == null || currentCollectionTitle.getText().equals("Default"));
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

        if (currentCollection == null || currentCollectionTitle.getText().equals("Default")) {
            return null;
        }
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
    public Collection moveNoteFromCollection(Note currentNote, Collection currentCollection,
                                             Collection destinationCollection) throws IOException {
        if (currentNote == null) {
            return currentCollection;
        }

        currentNote.collection = destinationCollection;
        noteCtrl.updatePendingNotes.add(currentNote);
        noteCtrl.saveAllPendingNotes();

        return destinationCollection;
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
