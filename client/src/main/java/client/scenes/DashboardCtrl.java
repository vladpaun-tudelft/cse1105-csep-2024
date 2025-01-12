package client.scenes;

import client.controllers.*;
import client.ui.CustomTreeCell;
import client.ui.NoteListItem;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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

/**
 * Controls all logic for the main dashboard.
 */
@SuppressWarnings("rawtypes")
public class DashboardCtrl implements Initializable {

    // Utilities
    //TODO: This is just a temporary solution, to be changed with something smarter
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ServerUtils server;
    private final MainCtrl mainCtrl;
    private final Config config;

    // Controllers
    @Inject
    @Getter private final MarkdownCtrl markdownCtrl;
    private final CollectionCtrl collectionCtrl;
    @Getter private final NoteCtrl noteCtrl;
    private final SearchCtrl searchCtrl;
    @Getter private final FilesCtrl filesCtrl;

    // FXML Components
    @FXML private Label contentBlocker;
    @FXML @Getter private TextArea noteBody;
    @FXML private WebView markdownView;
    @FXML private Label markdownViewBlocker;
    @FXML private Label noteTitle;
    @FXML public ListView collectionView;
    @FXML public TreeView allNotesView;
    @FXML @Getter Button addButton;
    @FXML private Label noteTitleMD;
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


    @Inject
    public DashboardCtrl(ServerUtils server,
                         MainCtrl mainCtrl,
                         Config config,
                         MarkdownCtrl markdownCtrl,
                         CollectionCtrl collectionCtrl,
                         NoteCtrl noteCtrl,
                         SearchCtrl searchCtrl,
                         FilesCtrl filesCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
        this.config = config;
        this.markdownCtrl = markdownCtrl;
        this.collectionCtrl = collectionCtrl;
        this.noteCtrl = noteCtrl;
        this.searchCtrl = searchCtrl;
        this.filesCtrl = filesCtrl;
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
                collectionSelect,
                allNotesButton,
                editCollectionTitle,
                deleteCollectionButton,
                moveNotesButton
        );
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
            } else {
                // Show content blockers when no item is selected
                contentBlocker.setVisible(true);
                markdownViewBlocker.setVisible(true);
                moveNotesButton.setText("Move Note");
                filesViewBlocker.setVisible(true);
            }
        });

        collectionView.getSelectionModel().clearSelection();

    }

    /**
     * Method used to set up the treeView
     * This method should only be used once in the initialization.
     */
    public void treeViewSetup() {
        // Create a virtual root item (you can use this if you don't want the root to be visible)
        TreeItem<Object> virtualRoot = new TreeItem<>(null);
        virtualRoot.setExpanded(true); // Optional: if you want the root to be expanded by default

        populateTreeView(virtualRoot);

        // Add listener to the ObservableList to dynamically update the TreeView
        allNotes.addListener((ListChangeListener<Note>) change -> {
            while (change.next()) {
                refreshTreeView();
                if (change.wasAdded()) {
                    selectNoteInTreeView(change.getAddedSubList().getLast());
                }
            }
        });

        // Add selection change listener
        allNotesView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // If the selected item is a note, show it,
            // Content blockers otherwise
            if (newValue != null && ((TreeItem)newValue).getValue() instanceof Note) {
                currentNote = (Note)((TreeItem)newValue).getValue();
                noteCtrl.showCurrentNote(currentNote);
            } else {
                // Show content blockers when no item is selected
                contentBlocker.setVisible(true);
                markdownViewBlocker.setVisible(true);
                moveNotesButton.setText("Move Note");
                filesViewBlocker.setVisible(true);
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
        TreeItem<Object> noNotesItem = new TreeItem<>(" - no notes in collection.");

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
        TreeItem<Object> itemToSelect = findItem(allNotesView.getRoot(), targetNote);
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

    public void addNote() {
        setSearchIsActive(false);
        noteCtrl.addNote(currentCollection,
                allNotes,
                collectionNotes);
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
        noteCtrl.onBodyChanged(currentNote);
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
        currentCollection = collectionCtrl.deleteCollection(currentCollection, collections,
                collectionNotes, allNotes);
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
    }

    public void conectToCollection(Collection collection) {
        List<Note> newCollectionNotes = server.getNotesByCollection(collection);
        collectionNotes = FXCollections.observableArrayList(newCollectionNotes);
        allNotes.addAll(newCollectionNotes);
        currentCollection = collection;

        collections.add(collection);
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

}
