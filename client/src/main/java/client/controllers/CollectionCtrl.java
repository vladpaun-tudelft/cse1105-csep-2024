package client.controllers;

import client.LanguageManager;
import client.entities.Action;
import client.entities.ActionType;
import client.scenes.DashboardCtrl;
import client.scenes.EditCollectionsCtrl;
import client.ui.DialogStyler;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import jakarta.ws.rs.ClientErrorException;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;



public class CollectionCtrl {

    // Utilities
    private final ServerUtils server;
    private Config config;
    private LanguageManager languageManager;
    private ResourceBundle bundle;
    private NoteCtrl noteCtrl;
    private DashboardCtrl dashboardCtrl;
    private SearchCtrl searchCtrl;
    private DialogStyler dialogStyler = new DialogStyler();

    // References
    private ListView collectionView;
    private TreeView treeView;
    private MenuButton currentCollectionTitle;
    @Getter private ToggleGroup collectionSelect;
    private MenuItem allNotesButton;
    private MenuItem editCollectionTitle;
    private Button deleteCollectionButton;
    private MenuButton moveNotesButton;

    @Getter @Setter ListView moveNotesListView;

    @Inject
    public CollectionCtrl(ServerUtils server, Config config, NoteCtrl noteCtrl, SearchCtrl searchCtrl) {
        this.server = server;
        this.config = config;
        this.noteCtrl = noteCtrl;
        this.searchCtrl = searchCtrl;

        this.languageManager = LanguageManager.getInstance(this.config);
        this.bundle = this.languageManager.getBundle();
    }


    public void setReferences(ListView collectionView,
                              TreeView treeView,
                              MenuButton currentCollectionTitle,
                              MenuItem allNotesButton,
                              MenuItem editCollectionTitle,
                              Button deleteCollectionButton,
                              MenuButton moveNotesButton) {
        this.collectionView = collectionView;
        this.treeView = treeView;
        this.currentCollectionTitle = currentCollectionTitle;
        this.allNotesButton = allNotesButton;
        this.editCollectionTitle = editCollectionTitle;
        this.deleteCollectionButton = deleteCollectionButton;
        this.moveNotesButton = moveNotesButton;
        this.collectionSelect = new ToggleGroup();
        this.moveNotesListView = new ListView();
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
        listViewDisplayOnlyTitles(listView, collections, false);

        //switching to collection
        listView.setOnMouseClicked(event -> {
            Collection selectedCollection = listView.getSelectionModel().getSelectedItem();
            // Check if collectionSelect is not null
            if (collectionSelect != null) {
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
            }
            dashboardCtrl.filter();
            dashboardCtrl.updateTagList();
        });
    }

    /**
     * method that displays only collection titles in listview
     *
     * @param listView    listview
     * @param collections collections
     */
    private void listViewDisplayOnlyTitles(ListView<Collection> listView, ObservableList<Collection> collections, boolean removeSelf) {
        ObservableList<Collection> filteredCollections = FXCollections.observableArrayList(
                collections.stream()
                        .filter(collection -> !collection.equals(dashboardCtrl.getCurrentCollection()))
                        .toList()
        );
        if(dashboardCtrl.getCurrentNote() != null && removeSelf) {
            filteredCollections.remove(dashboardCtrl.getCurrentNote().collection);
        }
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

                    Label label = new Label(filteredCollection.title);
                    label.maxWidthProperty().bind(listView.widthProperty().subtract(10)); // Set maximum width in pixels
                    label.setTextOverrun(OverrunStyle.ELLIPSIS); // Set overrun to ellipsis
                    label.setStyle("-fx-text-fill: white;"); // Set text color to white
                    setGraphic(label);
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
        listViewDisplayOnlyTitles(listView, collections, true);
        //switching to collection
        listView.setOnMouseClicked(event -> {
            Note currentNote = dashboardCtrl.getCurrentNote();
            Collection selectedCollection = listView.getSelectionModel().getSelectedItem();

            //dashboardCtrl.filter();
            //dashboardCtrl.updateTagList();
            if (collectionView.getSelectionModel().getSelectedItems().size() == 1
                    || treeView.getSelectionModel().getSelectedItems().size() == 1) {
                dashboardCtrl.getActionHistory().push(new Action(ActionType.MOVE_NOTE, currentNote, currentNote.collection, selectedCollection));
               moveNoteFromCollection(currentNote, selectedCollection);
                dashboardCtrl.refreshTreeView();
               dashboardCtrl.allNotesView.getSelectionModel().clearSelection();
                dashboardCtrl.selectNoteInTreeView(currentNote);
            }
            else if(collectionView.getSelectionModel().getSelectedItems().size() > 1) {
                //dashboardCtrl.getActionHistory().push(new Action(ActionType.MOVE_MULTIPLE_NOTES, currentNote, currentNote.collection, selectedCollection));
                moveMultipleNotes(selectedCollection);

            }
            else if (dashboardCtrl.allNotesView.getSelectionModel().getSelectedItems().size() > 1) {
                //dashboardCtrl.getActionHistory().push(new Action(ActionType.MOVE_MULTIPLE_NOTES_TREE, currentNote, currentNote.collection, selectedCollection));
                moveMultipleNotesInTreeView(selectedCollection);

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
            dynamicLabel.setText(bundle.getString("current.text") + currentCollection.title);
        } else {
            dynamicLabel.setText(bundle.getString("noCollectionSelected.text"));
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
        ObservableList<Collection> collections = FXCollections.observableArrayList(dashboardCtrl.getCollections());
        listViewSetupForMovingNotes(moveNotesListView, collections);
        CustomMenuItem scrollableCollectionsItem2 = new CustomMenuItem(moveNotesListView, false);
        Collection currentCollectionForNote = null;
        if (dashboardCtrl.getCurrentNote() != null) {
            currentCollectionForNote = dashboardCtrl.getCurrentNote().collection;
        }

        Label dynamicLabel = new Label();
        Label pickNoteDestination = new Label();

        if (currentCollectionForNote != null && currentCollectionForNote.title != null) {
            dynamicLabel.setText(bundle.getString("assignedTo.text") + currentCollectionForNote.title);
        } else {
            dynamicLabel.setText(bundle.getString("noCollectionAssigned.text"));
        }
        dynamicLabel.getStyleClass().add("current-collection-label");
        pickNoteDestination.getStyleClass().add("pick-note-destination");
        CustomMenuItem dynamicLabelItem = new CustomMenuItem(dynamicLabel, false);

        pickNoteDestination.setText(bundle.getString("pickNoteDestination.text"));
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

        moveNotesButton.maxWidthProperty().bind(
                dashboardCtrl.getNoteBody().widthProperty().divide(2).subtract(40)
        );
    }


    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
    }

    public void setUp() {

        // Set up the collections menu
        ObservableList<Collection> collections = FXCollections.observableArrayList(config.readFromFile());
        dashboardCtrl.setCollections(collections);

        if (!collections.isEmpty()) {
            server.getWebSocketURL(
                    config.readDefaultCollection().serverURL
            );
            dashboardCtrl.noteAdditionSync();
            dashboardCtrl.noteDeletionSync();
        }

        Collection defaultCollection = collections.stream()
                .filter(collection -> collection.equals(config.readDefaultCollection()))
                .findFirst().orElse(null);
        dashboardCtrl.setDefaultCollection(defaultCollection);

        dashboardCtrl.getAddButton().disableProperty().bind(
                Bindings.createBooleanBinding(
                        collections::isEmpty,
                        collections
                )
        );


        for (Collection c : collections) {
            dashboardCtrl.createCollectionButton(c, currentCollectionTitle, collectionSelect);
        }

        initializeDropoutCollectionLabel();
    }


    public ObservableList<Note> viewNotes() {
        dashboardCtrl.setSearchIsActive(false);
        dashboardCtrl.clearTags(null);
        return dashboardCtrl.filter();
    }

    public void removeCollectionFromClient(boolean delete, Collection collection, List<Collection> collections, ObservableList<Note> collectionNotes, ObservableList<Note> allNotes) {
        List<Note> notesToDelete = allNotes.stream()
                .filter(note -> note.collection.equals(collection))
                .collect(Collectors.toList());
        for (Note n : notesToDelete) {
            noteCtrl.removeNoteFromClient(n,collectionNotes,allNotes);
        }

        Collection previousCollection = collections.stream().filter(c -> !c.equals(collection)).findFirst().orElse(null);
        dashboardCtrl.setDefaultCollection(previousCollection);
        config.setDefaultCollection(previousCollection);

        // delete collection from server
        if (delete) server.deleteCollection(collection);
        // delete collection from config file
        collections.remove(collection);

        config.writeAllToFile(collections);
    }

    public boolean showDeleteConfirmation() {
        return dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                bundle.getString("deleteCollection.text"),
                bundle.getString("deleteCollection.text"),
                bundle.getString("deleteCollectionConfirmation.text")
        ).showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    public boolean showForgetConfirmation() {
        return dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                bundle.getString("forgetCollection.text"),
                bundle.getString("forgetCollection.text"),
                bundle.getString("forgetCollectionConfirmation.text")
        ).showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /**
     * A method used to move note from one collection to the other
     */
    public void moveNoteFromCollection(Note currentNote, Collection selectedCollection) {
        RadioMenuItem selectedRadioMenuItem = collectionSelect.getToggles().stream()
                .filter(toggle -> toggle instanceof RadioMenuItem item && item.getText().equals(selectedCollection.title))
                .map(toggle -> (RadioMenuItem) toggle)
                .findFirst().orElse(null);
        if (selectedRadioMenuItem != null && dashboardCtrl.getCurrentNote() != null) {

            moveNote(currentNote, selectedCollection);

            dashboardCtrl.setProgrammaticChange(true);
            if(dashboardCtrl.getCurrentCollection() != null ) {
                selectedRadioMenuItem.fire();   // If not in all note view
                collectionView.getSelectionModel().select(currentNote);
            }
            dashboardCtrl.setProgrammaticChange(false);

            collectionSelect.selectToggle(selectedRadioMenuItem);
            moveNotesButton.hide();
        }
    }

    public void moveNote(Note currentNote, Collection selectedCollection) {
        currentNote.collection = selectedCollection;

        if(noteCtrl.isTitleDuplicate(dashboardCtrl.getAllNotes(), currentNote, currentNote.getTitle(), false)){
            currentNote.setTitle(noteCtrl.generateUniqueTitle(dashboardCtrl.getAllNotes(), currentNote, currentNote.getTitle(), false));
        }
        noteCtrl.getUpdatePendingNotes().add(currentNote);
        noteCtrl.saveAllPendingNotes();
    }


    public void updateCollection(Collection collection, List<Collection> collections) {
        server.updateCollection(collection);
        config.writeAllToFile(collections);
    }

    public Collection addInputtedCollection(Collection inputtedCollection, Collection currentCollection, List<Collection> collections) {
        Collection addedCollection;
        try {
            addedCollection = server.addCollection(inputtedCollection);
            if (addedCollection == null) return currentCollection;
            config.writeToFile(addedCollection);
            if (dashboardCtrl.getDefaultCollection() == null) {
                dashboardCtrl.setDefaultCollection(addedCollection);
                config.setDefaultCollection(addedCollection);
            }

            collections.add(addedCollection);
        } catch (ClientErrorException e) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    bundle.getString("error.text"),
                    bundle.getString("error.text"),
                    e.getResponse().readEntity(String.class)
            );
            alert.showAndWait();
            return currentCollection;
        }

        // add entry in collections menu
        RadioMenuItem radioMenuItem = dashboardCtrl.createCollectionButton(addedCollection, currentCollectionTitle, collectionSelect);
        collectionSelect.selectToggle(radioMenuItem);

        return currentCollection == null ? currentCollection : addedCollection;
    }

    public void addCollection(){
        EditCollectionsCtrl editCollectionsCtrl = dashboardCtrl.getMainCtrl().showEditCollections();
        editCollectionsCtrl.addCollection();
    }

    public void editCollections() {
        dashboardCtrl.getMainCtrl().showEditCollections();
    }

    /**
     * moving multiple notes in the collection view
     *
     * @param destinationCollection destination collection
     */
    public void moveMultipleNotes(Collection destinationCollection) {

        if (collectionView != null &&
                collectionView.getSelectionModel().getSelectedItems().size() > 1) {
            ObservableList<Note> selectedItems = collectionView.getSelectionModel().getSelectedItems();
            List<Note> notesToMove = new ArrayList<>(selectedItems);
            //used to reselect notes
            List<Note> previouslySelectedNotes = new ArrayList<>(selectedItems);
            for (Note note : notesToMove) {
                moveNoteFromCollection(note, destinationCollection);
            }
            dashboardCtrl.refreshTreeView();
            collectionView.getSelectionModel().clearSelection();
            //reselect items
            for (Note note : previouslySelectedNotes) {
                collectionView.getSelectionModel().select(note);
            }
        }

    }

    /**
     * moving multiple notes in the all notes view
     *
     * @param destinationCollection destination collection
     */
    public void moveMultipleNotesInTreeView(Collection destinationCollection) {

        if (dashboardCtrl.allNotesView == null) return;
        ObservableList<TreeItem<Note>> selectedItems =
                dashboardCtrl.allNotesView.getSelectionModel().getSelectedItems();
        if (selectedItems.size() < 1) return;

        // cast to list of notes
        List<Note> selectedNotes = selectedItems
                .stream()
                .map(TreeItem::getValue)
                .collect(Collectors.toList());

        for (Note note : selectedNotes) {
            moveNoteFromCollection(note, destinationCollection);
        }

        dashboardCtrl.refreshTreeView();

        // select items that were selected in another collection
        List<TreeItem<Note>> itemsToSelect = new ArrayList<>();
        for (Note note : selectedNotes) {
            TreeItem<Note> matchingItem = findTreeItem(dashboardCtrl.allNotesView.getRoot(), note);
            if (matchingItem != null) {
                itemsToSelect.add(matchingItem);
            }
        }
        dashboardCtrl.allNotesView.getSelectionModel().clearSelection();
        for (TreeItem<Note> treeItem : itemsToSelect) {
            dashboardCtrl.allNotesView.getSelectionModel().select(treeItem);
        }
    }

    /**
     * helper method for moving notes
     * @param root root
     * @param targetNote targetNote
     * @return return
     */
    public  TreeItem<Note> findTreeItem(TreeItem<Note> root, Note targetNote) {
        if (root == null) return null;
        if (root.getValue() == targetNote) {
            return root;
        }
        for (TreeItem<Note> child : root.getChildren()) {
            TreeItem<Note> result = findTreeItem(child, targetNote);
            if (result != null) return result;
        }
        return null;
    }



}

