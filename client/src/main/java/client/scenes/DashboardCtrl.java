package client.scenes;

import client.controllers.*;
import client.entities.Action;
import client.entities.ActionType;
import client.ui.CustomTreeCell;
import client.ui.DialogStyler;
import client.ui.NoteListItem;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Controls all logic for the main dashboard.
 */
@SuppressWarnings("rawtypes")
public class DashboardCtrl implements Initializable {

    // Utilities
    //TODO: This is just a temporary solution, to be changed with something smarter
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @Getter private final ServerUtils server;
    @Getter private final MainCtrl mainCtrl;
    @Getter private final Config config;
    @Getter private final DialogStyler dialogStyler;

    // Controllers
    @Inject
    @Getter private final MarkdownCtrl markdownCtrl;
    @Getter private final CollectionCtrl collectionCtrl;
    @Getter private final NoteCtrl noteCtrl;
    @Getter private final SearchCtrl searchCtrl;
    @Getter private final FilesCtrl filesCtrl;

    // FXML Components
    @FXML private Label contentBlocker;
    @FXML @Getter private TextArea noteBody;
    @FXML private WebView markdownView;
    @FXML private Label markdownViewBlocker;
    @FXML @Getter private Label noteTitle;
    @FXML public ListView collectionView;
    @FXML public TreeView allNotesView;
    @FXML @Getter Button addButton;
    @FXML @Getter private Label noteTitleMD;
    @FXML private Button deleteButton;
    @FXML private Button clearSearchButton;
    @FXML @Getter private TextField searchField;
    @FXML private MenuButton currentCollectionTitle;
    @FXML private MenuItem allNotesButton;
    @FXML private ToggleGroup collectionSelect;
    @FXML private Button deleteCollectionButton;
    @FXML private MenuItem editCollectionTitle;
    @FXML private MenuButton moveNotesButton;
    @FXML private Button addFileButton;
    @FXML private HBox filesView;
    @FXML private Label filesViewBlocker;


    // Variables
    @Getter @Setter private Note currentNote = null;
    @Getter @Setter private Collection currentCollection = null;
    @Getter @Setter private Collection defaultCollection = null;
    @Getter @Setter private Collection destinationCollection = null;
    @Getter @Setter public ObservableList<Collection> collections;
    @Getter @Setter private ObservableList<Note> allNotes;
    @Getter @Setter public ObservableList<Note> collectionNotes;

    private TreeItem<Object> noNotesItem = new TreeItem<>(" - no notes in collection.");
    @Getter @Setter private boolean isProgrammaticChange = false;
    @Getter @Setter private Deque<Action> actionHistory = new ArrayDeque<>();
    private boolean isUndoBodyChange = false; // Flag for undo-triggered body changes

    @Inject
    public DashboardCtrl(ServerUtils server,
                         MainCtrl mainCtrl,
                         Config config,
                         MarkdownCtrl markdownCtrl,
                         CollectionCtrl collectionCtrl,
                         NoteCtrl noteCtrl,
                         SearchCtrl searchCtrl,
                         FilesCtrl filesCtrl,
                         DialogStyler dialogStyler) {
        this.mainCtrl = mainCtrl;
        this.server = server;
        this.config = config;
        this.markdownCtrl = markdownCtrl;
        this.collectionCtrl = collectionCtrl;
        this.noteCtrl = noteCtrl;
        this.searchCtrl = searchCtrl;
        this.filesCtrl = filesCtrl;
        this.dialogStyler = dialogStyler;
    }

    @SneakyThrows
    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
        allNotes = FXCollections.observableArrayList(server.getAllNotes());
        collectionNotes = allNotes;
        markdownCtrl.setReferences(collectionView, allNotesView, markdownView, markdownViewBlocker, noteBody);
        markdownCtrl.setDashboardCtrl(this);
        searchCtrl.setReferences(searchField, collectionView, allNotesView, noteBody);
        searchField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    search();
                }
                default -> {
                }
            }
        });

        collectionCtrl.setReferences(collectionView,
                allNotesView,
                currentCollectionTitle,
                allNotesButton,
                editCollectionTitle,
                deleteCollectionButton,
                moveNotesButton
        );
        collectionSelect = collectionCtrl.getCollectionSelect();

        collectionCtrl.setDashboardCtrl(this);

        noteCtrl.setReferences(
                collectionView,
                allNotesView,
                noteTitle,
                noteTitleMD,
                noteBody,
                markdownView,
                contentBlocker,
                searchField,
                filesViewBlocker,
                moveNotesButton
        );
        noteCtrl.setDashboardCtrl(this);

        collectionCtrl.setUp();


        BooleanBinding isNoteSelected = collectionView.getSelectionModel().selectedItemProperty().isNull()
                .and(allNotesView.getSelectionModel().selectedItemProperty().isNull()
                        .or(Bindings.createBooleanBinding(() -> {
                            TreeItem<Object> selectedItem = (TreeItem<Object>) allNotesView.getSelectionModel().getSelectedItem();
                            return selectedItem == null || !(selectedItem.getValue() instanceof Note); // Disable if no selection OR not a leaf
                        }, allNotesView.getSelectionModel().selectedItemProperty())));

        moveNotesButton.disableProperty().bind(isNoteSelected);

        deleteButton.disableProperty().bind(isNoteSelected);

        collectionCtrl.moveNotesInitialization();

        listViewSetup(allNotes);
        treeViewSetup();


        filesCtrl.setDashboardCtrl(this);
        filesCtrl.setReferences(filesView);

        viewAllNotes();

        // Temporary solution
        scheduler.scheduleAtFixedRate(() -> noteCtrl.saveAllPendingNotes(),
                10,10, TimeUnit.SECONDS);
    }

    public void noteAdditionSync() {
        server.registerForMessages("/topic/notes", Note.class, note -> {
            Platform.runLater(() -> {
                noteCtrl.updateViewAfterAdd(currentCollection, allNotes, collectionNotes, note);
            });
        });
    }

    public void noteDeletionSync() {
        server.registerForMessages("/topic/notes/delete", Note.class, note -> {
            Platform.runLater(() -> {
                noteCtrl.updateAfterDelete(note, allNotes, collectionNotes);
            });
        });
    }

    /**
     * Handles the current collection viewer setup
     */
    private void listViewSetup(ObservableList notes) {

        // Set required settings
        collectionView.setItems(notes);
        collectionView.setEditable(true);
        collectionView.setFixedCellSize(35);
        collectionView.setCellFactory(TextFieldListCell.forListView());

        // Set ListView entry as Title (editable)
        collectionView.setCellFactory(lv-> new NoteListItem(noteTitle, noteTitleMD, noteBody, this, noteCtrl));

        collectionView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (!isProgrammaticChange) actionHistory.clear();

                currentNote = (Note) newValue;
                noteCtrl.showCurrentNote(currentNote);

                markdownViewBlocker.setVisible(false);

                server.registerForEmbeddedFileUpdates(currentNote, embeddedFileId -> {
                    Platform.runLater(() -> {
//                        filesCtrl.showFiles(currentNote);
                        filesCtrl.updateViewAfterAdd(currentNote, embeddedFileId);
                    });
                });
                server.registerForEmbeddedFilesDeleteUpdates(currentNote, embeddedFileId -> {
                    Platform.runLater(() -> {
                        filesCtrl.updateViewAfterDelete(currentNote, embeddedFileId);
                    });
                });
                server.registerForEmbeddedFilesRenameUpdates(currentNote, newFileName -> {
                    Platform.runLater(() -> {
                        filesCtrl.updateViewAfterRename(currentNote, newFileName);
                    });
                });

            } else {
                currentNote = null;
                // Show content blockers when no item is selected
                contentBlocker.setVisible(true);
                markdownViewBlocker.setVisible(true);
                moveNotesButton.setText("Move Note");
                filesViewBlocker.setVisible(true);

                server.unregisterFromEmbeddedFileUpdates();
            }
        });

        collectionView.getSelectionModel().clearSelection();

    }

    /**
     * Method used to set up the treeView
     * This method should only be used once in the initialization.
     */
    private void treeViewSetup() {
        // Create a virtual root item (you can use this if you don't want the root to be visible)
        TreeItem<Object> virtualRoot = new TreeItem<>(null);
        virtualRoot.setExpanded(true); // Optional: if you want the root to be expanded by default

        syncTreeView(virtualRoot, collections, allNotes, false);

        // Add listener to the ObservableList to dynamically update the TreeView
        allNotes.addListener((ListChangeListener<Note>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    syncTreeView(virtualRoot, collections, allNotes, false);
                    selectNoteInTreeView(change.getAddedSubList().getLast());
                } else if (change.wasRemoved()) {
                    TreeItem<Object> toRemove = findItem(change.getRemoved().getFirst());
                    TreeItem<Object> toSelect = findValidItemInDirection(virtualRoot,toRemove, -1);
                    syncTreeView(virtualRoot, collections, allNotes, false);
                    if (toSelect != null && toSelect.getValue() instanceof Note) {
                        selectNoteInTreeView((Note) toSelect.getValue());
                    } else {
                        allNotesView.getSelectionModel().clearSelection();
                    }
                }
            }
        });

        collections.addListener((ListChangeListener<Collection>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    server.getWebSocketURL(change.getAddedSubList().getFirst().serverURL);
                    noteAdditionSync();
                    noteDeletionSync();
                }
                syncTreeView(virtualRoot, collections, allNotes, false);
            }
        });

        // Add selection change listener
        allNotesView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // If the selected item is a note, show it,
            // Content blockers otherwise
            if (newValue != null && ((TreeItem)newValue).getValue() instanceof Note note) {

                if (!isProgrammaticChange) actionHistory.clear();

                currentNote = note;
                noteCtrl.showCurrentNote(currentNote);

                markdownViewBlocker.setVisible(false);
                allNotesView.getFocusModel().focus(0);

                server.registerForEmbeddedFileUpdates(currentNote, embeddedFileId -> {
                    Platform.runLater(() -> {
//                        filesCtrl.showFiles(currentNote);
                        filesCtrl.updateViewAfterAdd(currentNote, embeddedFileId);
                    });
                });
                server.registerForEmbeddedFilesDeleteUpdates(currentNote, embeddedFileId -> {
                    Platform.runLater(() -> {
                        filesCtrl.updateViewAfterDelete(currentNote, embeddedFileId);
                    });
                });
                server.registerForEmbeddedFilesRenameUpdates(currentNote, newFileName -> {
                    Platform.runLater(() -> {
                        filesCtrl.updateViewAfterRename(currentNote, newFileName);
                    });
                });

            } else {
                currentNote = null;
                // Show content blockers when no item is selected
                contentBlocker.setVisible(true);
                markdownViewBlocker.setVisible(true);
                moveNotesButton.setText("Move Note");
                filesViewBlocker.setVisible(true);

                server.unregisterFromEmbeddedFileUpdates();
            }
        });

        // Set the virtual root as the root of the TreeView
        allNotesView.setRoot(virtualRoot);
        allNotesView.setShowRoot(false); // To hide the root item if it's just a container

        // Set custom TreeCell factory for NoteTreeItem
        allNotesView.setCellFactory(param -> new CustomTreeCell(this, noteCtrl, dialogStyler));

    }

    /**
     * This method refreshes the treeView
     * Any new, changed, or deleted items will be reflected in the tree view after this methood
     */
    public void refreshTreeView(List<Collection> filteredCollections, List<Note> filteredNotes, boolean expanded) {
        syncTreeView(allNotesView.getRoot(), filteredCollections, filteredNotes, expanded);
    }
    public void refreshTreeView() {
        refreshTreeView(collections, allNotes, false);
    }

    private void syncTreeView(TreeItem<Object> virtualRoot, List<Collection> filteredCollections, List<Note> filteredNotes, boolean expanded) {
        // Map existing tree items for quick lookup
        Map<Collection, TreeItem<Object>> existingCollections = new HashMap<>();
        Map<Note, TreeItem<Object>> existingNotes = new HashMap<>();

        // Build the mapping of current tree structure
        for (TreeItem<Object> collectionItem : virtualRoot.getChildren()) {
            if (collectionItem.getValue() instanceof Collection collection) {
                existingCollections.put(collection, collectionItem);
                for (TreeItem<Object> noteItem : collectionItem.getChildren()) {
                    if (noteItem.getValue() instanceof Note note) {
                        existingNotes.put(note, noteItem);
                    }
                }
            }
        }

        // Add or update collections
        for (Collection collection : filteredCollections) {
            TreeItem<Object> collectionItem = existingCollections.get(collection);
            if (collectionItem == null) {
                // Collection doesn't exist, add it
                collectionItem = new TreeItem<>(collection);
                collectionItem.setExpanded(expanded);
                virtualRoot.getChildren().add(collectionItem);
            }
            existingCollections.remove(collection); // Mark as handled

            // Add or update notes within the collection
            Map<Note, TreeItem<Object>> collectionNotes = new HashMap<>();
            for (TreeItem<Object> noteItem : collectionItem.getChildren()) {
                if (noteItem.getValue() instanceof Note note) {
                    collectionNotes.put(note, noteItem);
                }
            }

            boolean hasNotes = false;
            for (Note note : filteredNotes) {
                if (note.collection.equals(collection)) {
                    TreeItem<Object> noteItem = collectionNotes.get(note);
                    if (noteItem == null) {
                        // Note doesn't exist, add it
                        collectionItem.getChildren().add(new TreeItem<>(note));
                    } else {
                        collectionNotes.remove(note); // Mark as handled
                    }
                    hasNotes = true;
                }
            }

            // Remove notes that are no longer present
            for (TreeItem<Object> unusedNoteItem : collectionNotes.values()) {
                collectionItem.getChildren().remove(unusedNoteItem);
            }

            // Manage "no notes" item
            if (hasNotes) {
                collectionItem.getChildren().remove(noNotesItem);
            } else if (!collectionItem.getChildren().contains(noNotesItem)) {
                collectionItem.getChildren().add(noNotesItem);
            }
        }

        // Remove collections that are no longer present
        for (TreeItem<Object> unusedCollectionItem : new ArrayList<>(virtualRoot.getChildren())) {
            if (unusedCollectionItem.getValue() instanceof Collection collection &&
                    !filteredCollections.contains(collection)) {
                virtualRoot.getChildren().remove(unusedCollectionItem);
            }
        }


        // Reapply the custom cell factory
        allNotesView.setCellFactory(param -> new CustomTreeCell(this, noteCtrl, dialogStyler));
    }



    /**
     * This method is used to pars through and select a note from the tree view
     * @param targetNote the targeted note
     */
    public void selectNoteInTreeView(Note targetNote) {
        // Call the helper method to find and select the item
        TreeItem<Object> itemToSelect = findItem(targetNote);
        if (itemToSelect != null) {
            // Select the TreeItem
            allNotesView.getSelectionModel().select(itemToSelect);
        }
    }

    /**
     * This method takes in the current TreeItem that is selected in the treeView
     * And a target object that we want to find and it returns the targets respective TreeItem
     * @param currentItem currently selected TreeItem
     * @param targetObject object to be found
     * @return the TreeItem that represents the targetObject
     */
    private TreeItem<Object> findItem(TreeItem<Object> currentItem, Object targetObject) {
        // Check if the current item matches the target note
        if (currentItem.getValue() != null){
            if (currentItem.getValue() instanceof Note currentNote &&
                    targetObject instanceof Note targetNote &&
                    currentNote.title.equals(targetNote.title)) {
                return currentItem;
            }

            if (currentItem.getValue() instanceof Collection currentCollection &&
                    targetObject instanceof Collection targetCollection &&
                    currentCollection.title.equals(targetCollection.title)) {
                return currentItem;
            }
        }

        // Recursively check children of the current item
        for (TreeItem<Object> child : currentItem.getChildren()) {
            TreeItem<Object> result = findItem(child, targetObject);
            if (result != null) {
                return result; // Found in child subtree
            }
        }

        return null; // If no match is found
    }
    private TreeItem<Object> findItem(Object targetObject) {
        return findItem(allNotesView.getRoot(), targetObject);
    }

    public void addNote() {
        setSearchIsActive(false);
        noteCtrl.addNote(currentCollection,
                allNotes);
    }


    @FXML
    public void addCollection(){
        collectionCtrl.addCollection();
    }
    // This overloaded method is used when you already have the collection from the editCollections stage
    public void addCollection(Collection collection){
        currentCollection = collectionCtrl.addInputtedCollection(collection, currentCollection, collections);
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
    }


    public void openEditCollections() {
        collectionCtrl.editCollections();
    }

    public void viewAllNotes() {
        currentCollection = null;
        collectionNotes = collectionCtrl.viewNotes(null, allNotes);
    }

    @FXML
    public void onBodyChanged() {
        if (isUndoBodyChange) {
            isUndoBodyChange = false; // Reset the flag
            return; // Skip recording this change
        }

        String previousBody = currentNote != null? currentNote.getBody() : ""; // Get the current body before change
        noteCtrl.onBodyChanged(currentNote);
        String newBody = currentNote.getBody(); // Get the new body after change

        if (!previousBody.equals(newBody) && currentNote != null) {
            // Compute the diff between the previous body and the new body
            int startIndex = 0;
            int endIndexPrev = previousBody.length();
            int endIndexNew = newBody.length();

            // Find the first differing character from the start
            while (startIndex < endIndexPrev && startIndex < endIndexNew &&
                    previousBody.charAt(startIndex) == newBody.charAt(startIndex)) {
                startIndex++;
            }

            // Find the first differing character from the end
            while (endIndexPrev > startIndex && endIndexNew > startIndex &&
                    previousBody.charAt(endIndexPrev - 1) == newBody.charAt(endIndexNew - 1)) {
                endIndexPrev--;
                endIndexNew--;
            }

            String removedSegment = previousBody.substring(startIndex, endIndexPrev);
            String addedSegment = newBody.substring(startIndex, endIndexNew);

            boolean isWithinWord = removedSegment.matches("\\w*") && addedSegment.matches("\\w*");

            if (!newBody.isBlank() && (!actionHistory.isEmpty() && "editBody".equals(actionHistory.peek().getType()) &&
                    actionHistory.peek().getNote().equals(currentNote))) {

                if (isWithinWord) {
                    // Merge changes into the last action if they are within a word
                    Action lastAction = actionHistory.pop();
                    actionHistory.push(new Action(ActionType.EDIT_BODY, currentNote, lastAction.getPreviousState(), newBody));
                } else {
                    // Create a new action for changes across words or lines
                    actionHistory.push(new Action(ActionType.EDIT_BODY, currentNote, previousBody, newBody));
                }
            } else {
                // Create a new action for the first change
                actionHistory.push(new Action(ActionType.EDIT_BODY, currentNote, previousBody, newBody));
            }
        }

    }


    public void deleteSelectedNote() {
        noteCtrl.deleteSelectedNote(currentNote, collectionNotes, allNotes);
    }

    public void search() {
        searchCtrl.search(collectionNotes);
        searchCtrl.searchInTreeView(this, allNotes, collections);
    }
    public void setSearchIsActive(boolean b) {
        searchCtrl.setSearchIsActive(b, collectionNotes);
    }
    public void clearSearch() {
        searchCtrl.setSearchIsActive(false, collectionNotes);
        refreshTreeView();
    }

    public void refresh() {
        noteCtrl.saveAllPendingNotes();
        allNotes = FXCollections.observableArrayList(server.getAllNotes());
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
        filesCtrl.showFiles(currentNote);
        viewAllNotes();
        clearSearch();
        allNotesView.requestFocus();
    }

    @FXML
    public void deleteCollection() {
        if (collectionCtrl.showDeleteConfirmation()) {
            Collection collectionToDelete = currentCollection;
            viewAllNotes();
            collectionCtrl.removeCollectionFromClient(true, collectionToDelete, collections, collectionNotes, allNotes);
        }
    }

    public void connectToCollection(Collection collection) {
        collections.add(collection);
        List<Note> newCollectionNotes = server.getNotesByCollection(collection);
        for (Note note : newCollectionNotes) {
            allNotes.add(note);
        }

        if (currentCollection != null) {
            collectionNotes = FXCollections.observableArrayList(newCollectionNotes);
            currentCollection = collection;
        }

        config.writeToFile(collection);

        // add entry in collections menu
        //right now in createCollectionButton they are not added to any menu
        RadioMenuItem radioMenuItem = createCollectionButton(collection, currentCollectionTitle, collectionSelect);
        collectionSelect.selectToggle(radioMenuItem);

        collectionCtrl.viewNotes(currentCollection, allNotes);
    }

    public void addFile(){
        // Make sure notes are saved on the server
        noteCtrl.saveAllPendingNotes();
        EmbeddedFile newFile = filesCtrl.addFile(currentNote);
        if (newFile != null) {
            filesCtrl.showFiles(currentNote);
            // Save the file addition action to the history
            actionHistory.push(new Action(ActionType.ADD_FILE, currentNote, newFile, null));
        }
    }

    // Temporary solution
    @FXML
    public void onClose() {
        noteCtrl.saveAllPendingNotes();
        // Ensure the scheduler is shut down when the application closes
        scheduler.shutdown();
    }

    public RadioMenuItem createCollectionButton(Collection c, MenuButton currentCollectionTitle, ToggleGroup collectionSelect) {
        RadioMenuItem radioMenuItem = new RadioMenuItem(c.title);
        radioMenuItem.setOnAction(event -> {
            currentCollection = c;
            collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
        });
        radioMenuItem.setToggleGroup(collectionSelect);
        //here they are not added
        //currentCollectionTitle.getItems().addFirst(radioMenuItem);
        return radioMenuItem;
    }

    // ----------------------- HCI - Keyboard shortcuts -----------------------

    /**
     * ALT + RIGHT ARROW - cycles through collections
     */
    public void selectNextCollection() {
        if (currentCollection == null) {
            currentCollection = collections.getFirst();
        } else {
            if (collections.indexOf(currentCollection) == collections.size() - 1) {
                currentCollection = null;
            } else {
                currentCollection = collections.get(collections.indexOf(currentCollection) + 1);
            }
        }
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
    }

    /**
     * ALT + LEFT ARROW - cycles through collections
     */
    public void selectPreviousCollection() {
        if (currentCollection == null) {
            currentCollection = collections.getLast();
        } else {
            if (collections.indexOf(currentCollection) == 0) {
                currentCollection = null;
            } else {
                currentCollection = collections.get(collections.indexOf(currentCollection) - 1);
            }
        }
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
    }

    /**
     * ALT + DOWN ARROW - cycles through notes
     */
    public void selectNextNote() {
        selectNoteInDirection(1); // Direction 1 for "next"
    }

    /**
     * ALT + UP ARROW - cycles through notes
     */
    public void selectPreviousNote() {
        selectNoteInDirection(-1); // Direction -1 for "previous"
    }

    /**
     * ALT/CTRL + E - starts editing the current note name
     */
    public void editCurrentNoteName() {
        if (currentCollection == null) {
            // Handle TreeView selection
            TreeItem<Object> selectedItem = (TreeItem<Object>) allNotesView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getValue() instanceof Note) {
                CustomTreeCell customTreeCell = (CustomTreeCell) allNotesView.lookup(".tree-cell:selected");
                if (customTreeCell != null) {
                    customTreeCell.startEditing();
                }
            }
        } else {
            // Handle ListView selection
            Note selectedNote = (Note) collectionView.getSelectionModel().getSelectedItem();
            if (selectedNote != null) {
                NoteListItem noteListItem = (NoteListItem) collectionView.lookup(".list-cell:selected");
                if (noteListItem != null) {
                    noteListItem.startEditing();
                }
            }
        }
    }

    /**
     * CTRL + Z - Undoes the last action done to a note
     */
    public void undoLastAction(KeyEvent event) {
        if (actionHistory.isEmpty()) {
            return; // No actions to undo
        }
    
        Action lastAction = actionHistory.pop();
    
        switch (lastAction.getType()) {
            case ActionType.EDIT_BODY -> {
                isUndoBodyChange = true; // Set the flag
                currentNote.setBody((String) lastAction.getPreviousState());
                noteBody.setText(currentNote.getBody());
                // Add any edited but already existing note to the pending list
                if (!noteCtrl.getUpdatePendingNotes().contains(currentNote)) {
                    noteCtrl.getUpdatePendingNotes().add(currentNote);
                }
                noteBody.positionCaret(currentNote.getBody().length());
            }
            case ActionType.EDIT_TITLE -> {
                String oldTitle = (String) lastAction.getPreviousState();
                currentNote.setTitle(oldTitle);
                noteTitle.setText(oldTitle);
                noteTitleMD.setText(oldTitle);
                refreshTreeView();
                collectionView.setCellFactory(lv-> new NoteListItem(noteTitle, noteTitleMD, noteBody, this, noteCtrl));
            }
            case ActionType.ADD_FILE -> {
                EmbeddedFile addedFile = (EmbeddedFile) lastAction.getPreviousState();
                filesCtrl.deleteFile(currentNote, addedFile);
            }
            case ActionType.MOVE_NOTE -> {
                collectionCtrl.moveNoteFromCollection(currentNote, (Collection) lastAction.getPreviousState());
            }
            default -> throw new UnsupportedOperationException("Undo action not supported for type: " + lastAction.getType());
        }
        event.consume();
    }



    // ----------------------- HCI - Helper methods -----------------------

    private void selectNoteInDirection(int direction) {
        if (currentCollection == null) {
            TreeItem<Object> root = allNotesView.getRoot();
            if (root == null || root.getChildren().isEmpty()) {
                return; // No items in the TreeView, do nothing
            }

            TreeItem<Object> selectedItem = (TreeItem<Object>) allNotesView.getSelectionModel().getSelectedItem();
            TreeItem<Object> nextItem;

            if (selectedItem == null) {
                // No item selected, select the first valid item
                nextItem = findFirstValidItem(root);
            } else {
                // Find the next or previous valid item
                nextItem = findValidItemInDirection(root, selectedItem, direction);
            }

            if (nextItem != null) {
                allNotesView.getSelectionModel().select(nextItem);

                // If the selected item is of type Note, set it as the current note and show it
                if (nextItem.getValue() instanceof Note note) {
                    currentNote = note;
                    noteCtrl.showCurrentNote(currentNote);
                }
            }
        } else {
            // Collection-specific behavior
            if (currentNote == null) {
                currentNote = (direction > 0) ? collectionNotes.getFirst() : collectionNotes.getLast();
            } else {
                int currentIndex = collectionNotes.indexOf(currentNote);
                int nextIndex = (currentIndex + direction + collectionNotes.size()) % collectionNotes.size();
                currentNote = collectionNotes.get(nextIndex);
            }
            collectionView.getSelectionModel().select(currentNote);
            noteCtrl.showCurrentNote(currentNote);
        }
    }

    private TreeItem<Object> findFirstValidItem(TreeItem<Object> root) {
        for (TreeItem<Object> child : flattenTree(root)) {
            if (child.getValue() instanceof Note) {
                return child;
            }
        }
        return null; // No valid items found
    }

    private TreeItem<Object> findValidItemInDirection(TreeItem<Object> root, TreeItem<Object> currentItem, int direction) {
        List<TreeItem<Object>> flatList = flattenTree(root);

        // Find the index of the currently selected item
        int currentIndex = flatList.indexOf(currentItem);

        // Traverse the list in the specified direction
        for (int i = 1; i <= flatList.size(); i++) {
            int nextIndex = (currentIndex + (i * direction) + flatList.size()) % flatList.size();
            TreeItem<Object> nextItem = flatList.get(nextIndex);
            if (nextItem.getValue() instanceof Note && nextItem != currentItem) {
                return nextItem;
            }
        }

        return null; // No valid items found
    }

    private List<TreeItem<Object>> flattenTree(TreeItem<Object> root) {
        List<TreeItem<Object>> items = new ArrayList<>();
        flattenTreeRecursive(root, items);
        return items;
    }

    private void flattenTreeRecursive(TreeItem<Object> node, List<TreeItem<Object>> items) {
        if (node.getValue() != null) {
            items.add(node);
        }
        for (TreeItem<Object> child : node.getChildren()) {
            flattenTreeRecursive(child, items);
        }
    }
}
