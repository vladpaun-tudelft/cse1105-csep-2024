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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
    private final MarkdownCtrl markdownCtrl;
    private final CollectionCtrl collectionCtrl;
    private final NoteCtrl noteCtrl;
    private final SearchCtrl searchCtrl;
    private final FilesCtrl filesCtrl;

    // FXML Components
    @FXML
    private Label contentBlocker;
    @FXML
    @Getter private TextArea noteBody;
    @FXML
    private WebView markdownView;
    @FXML
    private Label markdownViewBlocker;
    @FXML
    private Label noteTitle;
    @FXML
    public ListView collectionView;
    @FXML
    public TreeView allNotesView;
    @FXML
    @Getter Button addButton;
    @FXML
    private Label noteTitleMD;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearSearchButton;
    @FXML
    private TextField searchField;
    @FXML
    private MenuButton currentCollectionTitle;
    @FXML
    private MenuItem allNotesButton;
    @FXML
    private ToggleGroup collectionSelect;
    @FXML
    private Button deleteCollectionButton;
    @FXML
    private MenuItem editCollectionTitle;
    @FXML
    private MenuButton moveNotesButton;
    @FXML
    private Button addFileButton;
    @FXML
    private HBox filesView;
    @FXML
    private Label filesViewBlocker;


    // Variables
    @Getter @Setter private Note currentNote = null;
    @Getter @Setter private Collection currentCollection = null;
    @Getter @Setter private Collection defaultCollection = null;
    @Getter @Setter private Collection destinationCollection = null;
    @Getter @Setter public List<Collection> collections;
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

    public void treeViewSetup() {
        // Create a virtual root item (you can use this if you don't want the root to be visible)
        TreeItem<Object> virtualRoot = new TreeItem<>(null);
        virtualRoot.setExpanded(true); // Optional: if you want the root to be expanded by default

        // Map each collection to its TreeItem
        Map<Collection, TreeItem<Object>> collectionItems = new HashMap<>();
        TreeItem<Object> noNotesItem = new TreeItem<>(" - no notes in collection.");

        // Initialize TreeView with current data
        for (Collection collection : collections) {
            TreeItem<Object> collectionItem = new TreeItem<>(collection);
            collectionItems.put(collection, collectionItem);
            virtualRoot.getChildren().add(collectionItem);
            collectionItem.getChildren().add(noNotesItem);

            // Eh, idk
            // collectionItem.setExpanded(true);
        }

        // Populate TreeItems with existing notes in allNotes
        for (Note note : allNotes) {
            TreeItem<Object> collectionItem = collectionItems.get(note.collection);
            if (collectionItem != null) {
                TreeItem<Object> noteItem = new TreeItem<>(note);
                if (collectionItem.getChildren().get(0).equals(noNotesItem)) {
                    collectionItem.getChildren().remove(0);
                }
                collectionItem.getChildren().add(noteItem);
            }
        }

        // Add listener to the ObservableList to dynamically update the TreeView
        allNotes.addListener((ListChangeListener<Note>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Note note : change.getAddedSubList()) {
                        TreeItem<Object> noteItem = new TreeItem<>(note);
                        TreeItem<Object> collectionItem = collectionItems.get(note.collection);
                        if (collectionItem != null) {
                            if (collectionItem.getChildren().size() ==  1 && collectionItem.getChildren().get(0).equals(noNotesItem)) {
                                collectionItem.getChildren().remove(0);
                            }
                            collectionItem.getChildren().add(noteItem);
                        }
                    }
                    selectNoteInTreeView(change.getAddedSubList().getLast());
                }
                if (change.wasRemoved()) {
                    for (Note note : change.getRemoved()) {
                        TreeItem<Object> collectionItem = collectionItems.get(note.collection);
                        if (collectionItem != null) {
                            collectionItem.getChildren().removeIf(item -> item.getValue().equals(note));
                            if (collectionItem.getChildren().isEmpty()) {
                                collectionItem.getChildren().add(noNotesItem);
                            }
                        }
                    }
                }
            }
        });

        // Add selection change listener
        allNotesView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
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
    public void refreshTreeView() {
        allNotesView.setCellFactory(param -> new CustomTreeCell(this, noteCtrl));
    }

    public void moveNoteInTreeView(Note currentNote, Collection selectedCollection) {
        // Ensure the current note and destination collection are not null
        if (currentNote == null || selectedCollection == null || currentNote.collection.equals(selectedCollection)) {
            return; // No action needed if note or collection is null or if the note is already in the selected collection
        }

        // Locate the current TreeItem for the note
        TreeItem<Object> root = allNotesView.getRoot();
        TreeItem<Object> currentCollectionItem = findItem(root, currentNote.collection);
        TreeItem<Object> noteItem = findItem(root, currentNote);

        // Locate the destination collection TreeItem
        TreeItem<Object> destinationCollectionItem = findItem(root, selectedCollection);

        // Perform the move if all items are found
        if (currentCollectionItem != null && noteItem != null && destinationCollectionItem != null) {
            // Remove the note from the current collection's children
            currentCollectionItem.getChildren().remove(noteItem);

            // If the current collection is now empty, optionally add a placeholder
            if (currentCollectionItem.getChildren().isEmpty()) {
                currentCollectionItem.getChildren().add(new TreeItem<>(" - no notes in collection."));
            }

            // Add the note to the destination collection
            if (destinationCollectionItem.getChildren().size() == 1 &&
                    destinationCollectionItem.getChildren().get(0).getValue().equals(" - no notes in collection.")) {
                // Remove placeholder if present
                destinationCollectionItem.getChildren().clear();
            }
            destinationCollectionItem.getChildren().add(noteItem);

            // Update the collection reference in the note
            currentNote.collection = selectedCollection;

            // Optionally select the moved note in the TreeView
            allNotesView.getSelectionModel().select(noteItem);
        }
    }

    public void selectNoteInTreeView(Note targetNote) {
        // Call the helper method to find and select the item
        TreeItem<Object> itemToSelect = findItem(allNotesView.getRoot(), targetNote);
        if (itemToSelect != null) {
            // Select the TreeItem
            allNotesView.getSelectionModel().select(itemToSelect);
        }
    }

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

    public FilesCtrl getFilesCtrl() {
        return this.filesCtrl;
    }

    public MarkdownCtrl getMarkdownCtrl() {
        return this.markdownCtrl;
    }

    public NoteCtrl getNoteCtrl() {
        return this.noteCtrl;
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
        searchCtrl.searchInTreeView(allNotesView, allNotes, collections);
    }
    public void setSearchIsActive(boolean b) {
        searchCtrl.setSearchIsActive(b, collectionNotes);
    }
    public void clearSearch() {
        searchCtrl.setSearchIsActive(false, collectionNotes);
        searchCtrl.resetSearch(allNotes);
        treeViewSetup();
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

    public Note getCurrentNote() {
        return currentNote;
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
}
