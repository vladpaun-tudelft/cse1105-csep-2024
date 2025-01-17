package client.scenes;

import client.controllers.*;
import client.services.CollectionFilter;
import client.services.NoteFilterProcessor;
import client.services.SearchFilter;
import client.services.TagFilter;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
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
import java.util.stream.Collectors;

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
    @Getter private final TagCtrl tagCtrl;

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
    @FXML private HBox tagsBox;


    // Variables
    @Getter @Setter private Note currentNote = null;
    @Getter @Setter private Collection currentCollection = null;
    @Getter @Setter private Collection defaultCollection = null;
    @Getter @Setter private Collection destinationCollection = null;
    @Getter @Setter public ObservableList<Collection> collections;
    @Getter @Setter private ObservableList<Note> allNotes;
    @Getter @Setter public ObservableList<Note> collectionNotes;

    private TreeItem<Object> noNotesItem = new TreeItem<>(" - no notes in collection.");

    @Inject
    public DashboardCtrl(ServerUtils server,
                         MainCtrl mainCtrl,
                         Config config,
                         MarkdownCtrl markdownCtrl,
                         CollectionCtrl collectionCtrl,
                         NoteCtrl noteCtrl,
                         SearchCtrl searchCtrl,
                         FilesCtrl filesCtrl,
                         DialogStyler dialogStyler,
                         TagCtrl tagCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
        this.config = config;
        this.markdownCtrl = markdownCtrl;
        this.collectionCtrl = collectionCtrl;
        this.noteCtrl = noteCtrl;
        this.searchCtrl = searchCtrl;
        this.filesCtrl = filesCtrl;
        this.dialogStyler = dialogStyler;
        this.tagCtrl = tagCtrl;
    }

    @SneakyThrows
    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
        allNotes = FXCollections.observableArrayList(server.getAllNotes());
        collectionNotes = allNotes;
        markdownCtrl.setReferences(collectionView, allNotesView, markdownView, markdownViewBlocker, noteBody);
        markdownCtrl.setDashboardCtrl(this);
        searchCtrl.setReferences(this, searchField);
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

        tagCtrl.setReferences(this, tagsBox, allNotes);

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
                currentNote = (Note) newValue;
                noteCtrl.showCurrentNote(currentNote);

                markdownViewBlocker.setVisible(false);

                server.registerForEmbeddedFileUpdates(currentNote, embeddedFile -> {
                    Platform.runLater(() -> {
                        filesCtrl.showFiles(currentNote);
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

        populateTreeView(virtualRoot);

        // Add listener to the ObservableList to dynamically update the TreeView
        allNotes.addListener((ListChangeListener<Note>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    Note note = change.getAddedSubList().getLast();
                    TreeItem<Object> parent = findItem(note.collection);
                    if (parent.getChildren().size() == 1 && parent.getChildren().getFirst().equals(noNotesItem)) {
                        parent.getChildren().remove(noNotesItem);
                    }
                    parent.getChildren().addLast(new TreeItem<>(note));
                    selectNoteInTreeView(note);
                }
                if (change.wasRemoved()) {
                    TreeItem<Object> toRemove = findItem(change.getRemoved().getFirst());
                    TreeItem<Object> toSelect = findValidItemInDirection(virtualRoot,toRemove, -1);
                    if (toRemove != null) {
                        // Locate the parent of the item to remove
                        TreeItem<Object> parent = toRemove.getParent();
                        if (parent != null) {
                            parent.getChildren().removeIf(child -> Objects.equals(child.getValue(), toRemove.getValue()));
                        }
                        if (parent.getChildren().isEmpty()) {
                            parent.getChildren().add(noNotesItem);
                        }

                        Platform.runLater(() -> {
                            allNotesView.getSelectionModel().select(toSelect);
                        });
                    }
                }
                allNotesView.setCellFactory(param -> new CustomTreeCell(this, noteCtrl));
            }
        });

        collections.addListener((ListChangeListener<Collection>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    server.getWebSocketURL(change.getAddedSubList().getFirst().serverURL);
                    noteAdditionSync();
                    noteDeletionSync();

                    TreeItem<Object> collectionItem = new TreeItem<>(change.getAddedSubList().getFirst());
                    virtualRoot.getChildren().add(collectionItem);

                    List<Note> notes = allNotes.stream().filter(note -> note.collection.equals(collectionItem.getValue())).collect(Collectors.toList());
                    if (notes.isEmpty()) {
                        collectionItem.getChildren().add(noNotesItem);
                    } else {
                        for (Note note : notes) {
                            collectionItem.getChildren().add(new TreeItem<>(note));
                        }
                    }
                }
                if (change.wasRemoved()) {
                    TreeItem<Object> toRemove = findItem(change.getRemoved().getFirst());
                    TreeItem<Object> toSelect = findValidItemInDirection(virtualRoot,toRemove, -1);
                    if (toRemove != null) {
                        toRemove.getChildren().clear();
                        TreeItem<Object> parent = toRemove.getParent();
                        if (parent != null) {
                            parent.getChildren().removeIf(child -> Objects.equals(child.getValue(), toRemove.getValue()));
                        }
                    }
                    Platform.runLater(() -> {
                        allNotesView.getSelectionModel().select(toSelect);
                    });
                }
                allNotesView.setCellFactory(param -> new CustomTreeCell(this, noteCtrl));
            }
        });

        // Add selection change listener
        allNotesView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // If the selected item is a note, show it,
            // Content blockers otherwise
            if (newValue != null && ((TreeItem)newValue).getValue() instanceof Note) {
                currentNote = (Note)((TreeItem)newValue).getValue();
                noteCtrl.showCurrentNote(currentNote);

                markdownViewBlocker.setVisible(false);
                allNotesView.getFocusModel().focus(0);

                server.registerForEmbeddedFileUpdates(currentNote, embeddedFile -> {
                    Platform.runLater(() -> {
                        filesCtrl.showFiles(currentNote);
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
        allNotesView.setCellFactory(param -> new CustomTreeCell(this, noteCtrl));

    }

    /**
     * This method populates a treeItem root with the collections and notes in the app.
     * @param virtualRoot the root we want to populate
     */
    public void populateTreeView(TreeItem<Object> virtualRoot, List<Collection> filteredCollections, List<Note> filteredNotes, boolean expanded) {
        // Map each collection to its TreeItem
        Map<Collection, TreeItem<Object>> collectionItems = new HashMap<>();

        // Initialize TreeView with filtered data
        for (Collection collection : filteredCollections) {
            TreeItem<Object> collectionItem = new TreeItem<>(collection);
            collectionItems.put(collection, collectionItem);
            virtualRoot.getChildren().add(collectionItem);
            collectionItem.getChildren().add(noNotesItem);
            collectionItem.setExpanded(expanded);
        }

        // Populate TreeItems with existing notes in filteredNotes
        for (Note note : filteredNotes) {
            TreeItem<Object> collectionItem = collectionItems.get(note.collection);
            if (collectionItem != null) {
                TreeItem<Object> noteItem = new TreeItem<>(note);
                if (collectionItem.getChildren().get(0).equals(noNotesItem)) {
                    collectionItem.getChildren().remove(0);
                }
                collectionItem.getChildren().add(noteItem);
            }
        }
    }
    public void populateTreeView(TreeItem<Object> virtualRoot) {
        populateTreeView(virtualRoot, collections, allNotes, false);
    }

    /**
     * This method refreshes the treeView
     * Any new, changed, or deleted items will be reflected in the tree view after this methood
     */
    public void refreshTreeView(List<Collection> filteredCollections, List<Note> filteredNotes, boolean expanded) {
        allNotesView.getRoot().getChildren().clear();
        populateTreeView(allNotesView.getRoot(), filteredCollections, filteredNotes, expanded);
        allNotesView.setCellFactory(param -> new CustomTreeCell(this, noteCtrl));
    }
    public void refreshTreeView() {
        refreshTreeView(collections, allNotes, false);
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
        updateTagList();
    }
    // This overloaded method is used when you already have the collection from the editCollections stage
    public void addCollection(Collection collection){
        currentCollection = collectionCtrl.addInputtedCollection(collection, currentCollection, collections);
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
        updateTagList();
    }


    public void openEditCollections() {
        collectionCtrl.editCollections();
    }

    public void viewAllNotes() {
        currentCollection = null;
        collectionNotes = collectionCtrl.viewNotes(null, allNotes);
        updateTagList();
    }

    @FXML
    public void onBodyChanged() {
        noteCtrl.onBodyChanged(currentNote);
        updateTagList();
    }

    public void deleteSelectedNote() {
        noteCtrl.deleteSelectedNote(currentNote, collectionNotes, allNotes);
        updateTagList();
    }

    public void search() {
        filter();
    }
    public void setSearchIsActive(boolean b) {
        searchCtrl.setSearchIsActive(b);
    }
    public void clearSearch() {
        searchCtrl.setSearchIsActive(false);
        updateTagList();
    }

    public void refresh() {
        noteCtrl.saveAllPendingNotes();
        allNotes = FXCollections.observableArrayList(server.getAllNotes());
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
        filesCtrl.showFiles(currentNote);
        viewAllNotes();
        clearSearch();
        filter();
        updateTagList();
        allNotesView.requestFocus();
    }

    @FXML
    public void deleteCollection() {
        if (collectionCtrl.showDeleteConfirmation()) {
            Collection collectionToDelete = currentCollection;
            viewAllNotes();
            collectionCtrl.removeCollectionFromClient(true, collectionToDelete, collections, collectionNotes, allNotes);
        }
        filter();
        updateTagList();
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
            if (nextItem.getValue() instanceof Note) {
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

    // ----------------------- Tag - Methods -----------------------
    public void updateTagList() {
        tagCtrl.updateTagList();
    }

    public void clearTags(ActionEvent actionEvent) {
        tagCtrl.clearTags();
    }

    public void selectTag(String tag) {
        tagCtrl.selectTag(tag);
    }

    // ----------------------- Filtering - Collection & Search & Tag -----------------------
    /**
     * Updates the view with filtered notes using all filters
     */
    public List<Note> getFilteredNotes() {
        NoteFilterProcessor filterProcessor = new NoteFilterProcessor();
        filterProcessor.addFilter(new CollectionFilter(currentCollection));
        filterProcessor.addFilter(new TagFilter(tagCtrl.getSelectedTags(), tagCtrl.getTagService()));
        filterProcessor.addFilter(new SearchFilter(searchField.getText().trim().toLowerCase()));

        return filterProcessor.applyFilters(allNotes);
    }

    /**
     * Returns notes filtered by all criteria
     */
    public List<Note> getFilteredNotesWithCustomTags(List<String> tags) {
        NoteFilterProcessor filterProcessor = new NoteFilterProcessor();
        filterProcessor.addFilter(new CollectionFilter(currentCollection));
        filterProcessor.addFilter(new TagFilter(tags, tagCtrl.getTagService()));

        return filterProcessor.applyFilters(allNotes);
    }

    /**
     * Returns notes filtered by collection only
     */
    public List<Note> getFilteredNotesByCollection() {
        NoteFilterProcessor filterProcessor = new NoteFilterProcessor();
        filterProcessor.addFilter(new CollectionFilter(currentCollection));
        return filterProcessor.applyFilters(allNotes);
    }

    public void filter() {
        List<Note> filteredNotes = getFilteredNotes();
        List<Collection> filteredCollections = collections.stream()
                .filter(collection -> filteredNotes.stream()
                        .anyMatch(note -> note.collection.equals(collection)))
                .toList();

        refreshTreeView(filteredCollections, filteredNotes, true);

        collectionView.getSelectionModel().clearSelection();
    }
}
