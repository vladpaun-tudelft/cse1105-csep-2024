package client.scenes;

import client.controllers.*;
import client.ui.NoteListItem;
import client.ui.NoteTreeItem;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import lombok.Getter;
import lombok.SneakyThrows;
import java.io.IOException;
import java.net.URL;
import java.util.List;
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
    private TextArea noteBody;
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
    private Button addButton;
    @FXML
    private Label noteTitleMD;
    @FXML
    private Button searchButton;
    @FXML
    private Button clearSearchButton;
    @FXML
    private TextField searchField;
    @FXML
    private MenuButton currentCollectionTitle;
    @FXML
    private Menu collectionMenu;
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
    @Getter
    private Note currentNote = null;
    @Getter
    private Collection currentCollection = null;
    @Getter
    private Collection destinationCollection = null;
    @Getter
    private List<Collection> collections;
    @Getter
    private ObservableList<Note> allNotes;
    @Getter
    private ObservableList<Note> collectionNotes;


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
        markdownCtrl.setReferences(collectionView, markdownView, markdownViewBlocker, noteBody);
        markdownCtrl.setDashboardCtrl(this);
        searchCtrl.setReferences(searchField, collectionView, noteBody);
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
                collectionMenu,
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

        collections = collectionCtrl.setUp();
       collectionCtrl.initializeDropoutCollectionLabel();


        moveNotesButton.disableProperty().bind(
                collectionView.getSelectionModel().selectedItemProperty().isNull()
                        .and(allNotesView.getSelectionModel().selectedItemProperty().isNull())
                        .or(Bindings.createBooleanBinding(() -> {
                            TreeItem<Note> selectedItem = (TreeItem<Note>)allNotesView.getSelectionModel().getSelectedItem();
                            return selectedItem == null || !selectedItem.isLeaf(); // Disable if no selection OR not a leaf
                        }, allNotesView.getSelectionModel().selectedItemProperty()))
        );

        collectionCtrl.moveNotesInitialization();

        collectionCtrl.moveNotesInitialization();

        collectionNotes = collectionCtrl.viewNotes(null, allNotes);
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
                moveNotesButton.setText(currentNote.collection.title);
                noteCtrl.showCurrentNote(currentNote);
                markdownViewBlocker.setVisible(false);
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
        TreeItem<Note> virtualRoot = new TreeItem<>(null);
        virtualRoot.setExpanded(true); // Optional: if you want the root to be expanded by default

        // Populate TreeView with TreeItems for each Note
        for (Collection collection : collections) {
            TreeItem<Note> collectionItem = new TreeItem<>(new Note(collection.title, null, collection));
            List<Note> collectionNotes = allNotes.stream().filter(n -> n.collection.equals(collection)).toList();
            if(collectionNotes.size() > 0) {
                for(Note note : collectionNotes) {
                    TreeItem<Note> noteItem = new TreeItem<>(note);
                    collectionItem.getChildren().add(noteItem);
                }
                virtualRoot.getChildren().add(collectionItem);
                collectionItem.setExpanded(true);

            }
        }

        allNotesView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && ((TreeItem)newValue).isLeaf()) {
                currentNote = (Note)((TreeItem)newValue).getValue();
                moveNotesButton.setText(currentNote.collection.title);
                noteCtrl.showCurrentNote(currentNote);
                markdownViewBlocker.setVisible(false);
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
        allNotesView.setCellFactory(param -> new NoteTreeItem(noteTitle, noteTitleMD, noteBody, this, noteCtrl));

    }

    public void selectNoteInTreeView(Note targetNote) {
        // Call the helper method to find and select the item
        TreeItem<Note> itemToSelect = findItem(allNotesView.getRoot(), targetNote);
        if (itemToSelect != null) {
            // Select the TreeItem
            allNotesView.getSelectionModel().select(itemToSelect);
            System.out.println("hey!");
        }
    }

    private TreeItem<Note> findItem(TreeItem<Note> currentItem, Note targetNote) {
        // Check if the current item matches the target note
        if (currentItem.getValue() != null && currentItem.getValue().equals(targetNote)) {
            return currentItem; // Found the matching item
        }

        // Recursively check children of the current item
        for (TreeItem<Note> child : currentItem.getChildren()) {
            TreeItem<Note> result = findItem(child, targetNote);
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
                collections,
                allNotes,
                collectionNotes);
    }


    public void addCollection() throws IOException {
        currentCollection = collectionCtrl.addCollection(currentCollection, collections);
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
    }

    public void changeTitleInCollection() throws IOException {
        currentCollection = collectionCtrl.changeTitleInCollection(currentCollection, collections);

    }

    public void deleteCollection() throws IOException {
        currentCollection = collectionCtrl.deleteCollection(currentCollection, collections,
                collectionNotes, allNotes);
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
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
    }
    public void setSearchIsActive(boolean b) {
        searchCtrl.setSearchIsActive(b, collectionNotes);
    }
    public void clearSearch() {
        searchCtrl.setSearchIsActive(false, collectionNotes);
    }

    public void refresh() {
        noteCtrl.saveAllPendingNotes();
        allNotes = FXCollections.observableArrayList(server.getAllNotes());
        collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
        filesCtrl.showFiles(currentNote);
        clearSearch();
    }

    public void addFile() throws IOException {
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

    public RadioMenuItem createCollectionButton(Collection c, Menu collectionMenu, ToggleGroup collectionSelect) {
        RadioMenuItem radioMenuItem = new RadioMenuItem(c.title);
        radioMenuItem.setOnAction(event -> {
            currentCollection = c;
            collectionNotes = collectionCtrl.viewNotes(currentCollection, allNotes);
        });
        radioMenuItem.setToggleGroup(collectionSelect);
        collectionMenu.getItems().addFirst(radioMenuItem);
        return radioMenuItem;
    }

}
