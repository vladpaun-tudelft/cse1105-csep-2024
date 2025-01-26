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
import commons.EmbeddedFile;
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
    private final NotificationsCtrl notificationsCtrl;
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

    @Getter @Setter private ListView moveNotesListView;
    @Getter @Setter private List<EmbeddedFile> embeddedFilesCache = new ArrayList<>();

    @Inject
    public CollectionCtrl(ServerUtils server, Config config, NoteCtrl noteCtrl, SearchCtrl searchCtrl, NotificationsCtrl notificationsCtrl) {
        this.server = server;
        this.config = config;
        this.noteCtrl = noteCtrl;
        this.searchCtrl = searchCtrl;
        this.notificationsCtrl = notificationsCtrl;

        this.languageManager = LanguageManager.getInstance(this.config);
        this.bundle = this.languageManager.getBundle();
    }

    // for testing purposes
    public CollectionCtrl(ServerUtils server, Config config, NoteCtrl noteCtrl) {
        this.server = server;
        this.config = config;
        this.noteCtrl = noteCtrl;
        this.notificationsCtrl = null;
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
                        if (selectedCollection == null) {
                            return;
                        }
                        if (item.getText().equals(selectedCollection.title)) {
                            collectionSelect.selectToggle(item);
                            item.fire();
                            currentCollectionTitle.hide();
                            break;
                        }
                    }
                }
                dashboardCtrl.showBlockers();
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
                    label.setStyle("-fx-text-fill: -main-text;"); // Set text color to white
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
                        dashboardCtrl.getActionHistory().push(new Action(ActionType.MOVE_NOTE, currentNote, currentNote.collection, null, selectedCollection));
                        dashboardCtrl.setProgrammaticChange(true);
                        moveNoteFromCollection(currentNote, selectedCollection);
                        dashboardCtrl.refreshTreeView();
                        dashboardCtrl.allNotesView.getSelectionModel().clearSelection();
                        dashboardCtrl.selectNoteInTreeView(currentNote);
                        dashboardCtrl.setProgrammaticChange(false);
                        if (dashboardCtrl.getCurrentCollection() == null) {
                            dashboardCtrl.allNotesView.scrollTo(dashboardCtrl.allNotesView.getSelectionModel().getSelectedIndex());
                        } else {
                            dashboardCtrl.collectionView.scrollTo(dashboardCtrl.collectionView.getSelectionModel().getSelectedIndex());
                        }


                    } else if (collectionView.getSelectionModel().getSelectedItems().size() > 1) {
                        //dashboardCtrl.setProgrammaticChange(true);
                        ObservableList<Note> selectedNotes = collectionView.getSelectionModel().getSelectedItems();
                        Collection currentCollection = selectedNotes.get(0).collection;
                        moveMultipleNotes(selectedCollection);
                        dashboardCtrl.getActionHistory().push(new Action
                                (ActionType.MOVE_MULTIPLE_NOTES, null, currentCollection, null, selectedCollection));
                        // dashboardCtrl.setProgrammaticChange(false);

                    } else if (dashboardCtrl.allNotesView.getSelectionModel().getSelectedItems().size() > 1) {
                        ObservableList<TreeItem<Note>> selectedNotes
                                = (ObservableList<TreeItem<Note>>) dashboardCtrl.allNotesView.getSelectionModel().getSelectedItems();
                        if (selectedCollection != null) {
                            ObservableList<TreeItem<Note>> copiedList = FXCollections.observableArrayList(
                                    selectedNotes.stream()
                                            .map(originalTreeItem -> {
                                                Note originalNote = originalTreeItem.getValue();
                                                Note copiedNote = new Note(
                                                        originalNote.getTitle(),
                                                        originalNote.getBody(),
                                                        originalNote.collection
                                                );
                                                copiedNote.id=originalNote.id;
                                                return new TreeItem<>(copiedNote);
                                            })
                                            .collect(Collectors.toList())
                            );
                            moveMultipleNotesInTreeView(selectedCollection, false, copiedList);
                            dashboardCtrl.getActionHistory().push(new Action(ActionType.MOVE_MULTIPLE_NOTES_TREE, null, copiedList, null, null));
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

        // Set the default collection if it exists
        Collection defaultCollection = collections.stream()
                .filter(collection -> collection.equals(config.readDefaultCollection()))
                .findFirst()
                .orElse(null);
        dashboardCtrl.setDefaultCollection(defaultCollection);

        // Iterate over all collections to connect to their servers
        for (Collection collection : collections) {
            String serverURL = collection.serverURL;

            // Check if the server is available
            if (server.isServerAvailable(serverURL)) {
                ServerUtils.getUnavailableCollections().remove(collection);

                // Establish WebSocket connection for the server
                server.getWebSocketURL(serverURL);

                // Register for updates on this server
                dashboardCtrl.noteAdditionSync(serverURL);
                dashboardCtrl.noteTitleSync(serverURL);
                dashboardCtrl.noteDeletionSync(serverURL);

            } else {
                // Handle unavailable servers
                if (!ServerUtils.getUnavailableCollections().contains(collection)) {
                    ServerUtils.getUnavailableCollections().add(collection);
                }

                if (collection.equals(defaultCollection)) {
                    // Special message for unavailable default collection
                    dialogStyler.createStyledAlert(
                            Alert.AlertType.INFORMATION,
                            bundle.getString("error.text"),
                            bundle.getString("error.text"),
                            bundle.getString("unavailableDefaultCollectionError")
                    ).showAndWait();
                } else {
                    // General message for unavailable collections
                    dialogStyler.createStyledAlert(
                            Alert.AlertType.INFORMATION,
                            bundle.getString("error.text"),
                            bundle.getString("error.text"),
                            bundle.getString("unavailableCollectionsError") + collection.title
                    ).showAndWait();
                }
            }
        }

        // Disable the "Add Note" button if there are no collections
        dashboardCtrl.getAddButton().disableProperty().bind(
                Bindings.createBooleanBinding(
                        collections::isEmpty,
                        collections
                )
        );

        // Populate the collections menu with buttons
        for (Collection c : collections) {
            dashboardCtrl.createCollectionButton(c, currentCollectionTitle, collectionSelect);
        }

        // Initialize dropout collection label
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

        // delete collection from server
        if (delete) {
            server.deleteCollection(collection);
            if(notificationsCtrl != null)notificationsCtrl.pushNotification(bundle.getString("deleteCollection"), false);
        }
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
    /**
     * A method used to move note from one collection to the other
     */
    public void moveNoteFromCollection(Note currentNote, Collection selectedCollection) {
        if(selectedCollection.title==null){
            return;
        }
        if (!server.isServerAvailable(currentNote.collection.serverURL) || !server.isServerAvailable(selectedCollection.serverURL)) {
            String alertText = bundle.getString("noteUpdateError") + "\n" + currentNote.title;
            dialogStyler.createStyledAlert(
                    Alert.AlertType.INFORMATION,
                    bundle.getString("serverCouldNotBeReached.text"),
                    bundle.getString("serverCouldNotBeReached.text"),
                    alertText
            ).showAndWait();
            return;
        }



        RadioMenuItem selectedRadioMenuItem = collectionSelect.getToggles().stream()
                .filter(toggle -> toggle instanceof RadioMenuItem item && item.getText().equals(selectedCollection.title))
                .map(toggle -> (RadioMenuItem) toggle)
                .findFirst().orElse(null);
        if (selectedRadioMenuItem != null && dashboardCtrl.getCurrentNote() != null) {
            moveNote(currentNote, selectedCollection);

            if(dashboardCtrl.getCurrentCollection() != null ) {
                selectedRadioMenuItem.fire();   // If not in all note view
                collectionView.getSelectionModel().select(currentNote);
            }

            collectionSelect.selectToggle(selectedRadioMenuItem);
            moveNotesButton.hide();
        }
    }


    public void moveNote(Note currentNote, Collection selectedCollection) {
        if (!server.isServerAvailable(selectedCollection.serverURL)) {
            dialogStyler.createStyledAlert(
                    Alert.AlertType.INFORMATION,
                    bundle.getString("serverError.text"),
                    bundle.getString("serverError.text"),
                    bundle.getString("serverUnreachable.text")
            ).showAndWait();
            return;
        }

        embeddedFilesCache.addAll(currentNote.embeddedFiles);

        // DELETE NOTE
        noteCtrl.deleteNote(currentNote, dashboardCtrl.getCollectionNotes(), dashboardCtrl.getAllNotes());

        // MOVE NOTE
        currentNote.collection = selectedCollection;
        

        if(noteCtrl.isTitleDuplicate(dashboardCtrl.getAllNotes(), currentNote, currentNote.getTitle(), false)){
            currentNote.setTitle(noteCtrl.generateUniqueTitle(dashboardCtrl.getAllNotes(), currentNote, currentNote.getTitle(), false));
        }

        // ADD IT BACK
        server.send("/app/notes", currentNote,currentNote.collection.serverURL);

        dashboardCtrl.getAllNotes().add(currentNote);
        if (dashboardCtrl.getCurrentCollection() != null) {
            collectionView.getItems().add(currentNote);
        }

        if(notificationsCtrl != null) notificationsCtrl.pushNotification(bundle.getString("movedNote") + selectedCollection.title, false);
    }

    public void addFilesBack(Note note) {
        for (EmbeddedFile file : embeddedFilesCache) {
            if (!note.embeddedFiles.contains(file)) {
                note.embeddedFiles.add(file);
                dashboardCtrl.getFilesCtrl().addDeletedFile(note, file);
            }
        }
    }

    public void updateCollection(Collection collection, List<Collection> collections) {
        server.updateCollection(collection);
        config.writeAllToFile(collections);
        if(notificationsCtrl != null) notificationsCtrl.pushNotification(bundle.getString("updatedCollection") + collection.title, false);
    }

    public Collection addInputtedCollection(Collection inputtedCollection, Collection currentCollection, List<Collection> collections) {
        Collection addedCollection;
        try {
            addedCollection = server.addCollection(inputtedCollection);
            if (addedCollection == null) return currentCollection;
            config.writeToFile(addedCollection);
            if (dashboardCtrl.getDefaultCollection() == null) {
                dashboardCtrl.setDefaultCollection(addedCollection);
            }

            collections.add(addedCollection);
            if(notificationsCtrl != null) notificationsCtrl.pushNotification(bundle.getString("addedCollection"), false);
        } catch (ClientErrorException e) {
            if(notificationsCtrl != null) notificationsCtrl.pushNotification(e.getResponse().readEntity(String.class), true);
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
            dashboardCtrl.setProgrammaticChange(true);
            ObservableList<Note> selectedItems = collectionView.getSelectionModel().getSelectedItems();
            List<Note> notesToMove = new ArrayList<>(selectedItems);
            //used to reselect notes
            List<Note> previouslySelectedNotes = new ArrayList<>(selectedItems);
            //Collection currentCollection = previouslySelectedNotes.get(0).collection;

            for (Note note : notesToMove) {
                moveNoteFromCollection(note, destinationCollection);
            }

           //dashboardCtrl.refreshTreeView();
            collectionView.getSelectionModel().clearSelection();
            //reselect items
            for (Note note : previouslySelectedNotes) {
                collectionView.getSelectionModel().select(note);
            }

            dashboardCtrl.setProgrammaticChange(false);
            if(notificationsCtrl != null) notificationsCtrl.pushNotification(bundle.getString("movedNotesMultiple") + destinationCollection.title, false);
        }

    }

    /**
     * moving multiple notes in the all notes view
     *
     * @param destinationCollection destination collection
     */
    public void moveMultipleNotesInTreeView(Collection destinationCollection, boolean isUndo, ObservableList<TreeItem<Note>> previousNotes) {

        if (dashboardCtrl.allNotesView == null) return;
        ObservableList<TreeItem<Note>> selectedItems =
                dashboardCtrl.allNotesView.getSelectionModel().getSelectedItems();
        if (selectedItems.size() < 1) return;
        dashboardCtrl.setProgrammaticChange(true);

        // cast to list of notes
        List<Note> selectedNotes = selectedItems
                .stream()
                .map(TreeItem::getValue)
                .collect(Collectors.toList());

        if(!isUndo){
            for (Note note : selectedNotes) {
                moveNoteFromCollection(note, destinationCollection);
            }
        } else {
            for( Note note : selectedNotes ) {

                TreeItem<Note> previousNote = previousNotes
                        .stream()
                        .filter(n -> n.getValue().id == note.getId())
                        .findFirst()
                        .orElse(null);

                if (previousNote != null) {
                    moveNoteFromCollection(note, previousNote.getValue().collection);
                }

            }

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

        dashboardCtrl.allNotesView.scrollTo(dashboardCtrl.allNotesView.getSelectionModel().getSelectedIndex()
                - dashboardCtrl.allNotesView.getSelectionModel().getSelectedItems().size()/2);

        dashboardCtrl.setProgrammaticChange(false);
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

