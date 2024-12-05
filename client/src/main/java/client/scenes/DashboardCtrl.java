package client.scenes;

import client.controllers.CollectionCtrl;
import client.controllers.MarkdownCtrl;
import client.controllers.NoteCtrl;
import client.ui.NoteListItem;
import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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

    //TODO: This is just a temporary solution, to be changed with something smarter
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ServerUtils server;
    private final MainCtrl mainCtrl;

    private Collection currentCollection = null;
    
    @Inject
    private final MarkdownCtrl markdownCtrl;
    private final CollectionCtrl collectionCtrl;
    private final NoteCtrl noteCtrl;

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
    private Button addButton;
    @FXML
    private Label noteTitle_md;
    @FXML
    private Button searchButton;
    @FXML
    private TextField searchField;
    @FXML
    private Label currentCollectionTitle;
    @FXML
    private Menu collectionMenu;
    @FXML
    private RadioMenuItem allNotesButton;
    @FXML
    private ToggleGroup collectionSelect;
    @FXML
    private Button deleteCollectionButton;
    @FXML
    private VBox root;
    @FXML
    private MenuItem editCollectionTitle;

    @FXML
    private Button refreshButton;

    private ObservableList<Note> collectionNotes;
    private List<Note> filteredNotes = new ArrayList<>();
    private boolean searchIsActive = false;

    private List<Collection> collections;

    Config config = new Config();

    private final List<Note> createPendingNotes = new ArrayList<>();
    private final List<Note> updatePendingNotes = new ArrayList<>();

    public boolean pendingHideContentBlocker = true;

    @Inject
    public DashboardCtrl(ServerUtils server, MainCtrl mainCtrl, MarkdownCtrl markdownCtrl) throws IOException {
        this.mainCtrl = mainCtrl;
        this.server = server;
        this.markdownCtrl = markdownCtrl;
        this.collectionCtrl = new CollectionCtrl(server, config);
        this.noteCtrl = new NoteCtrl(server);

    }

    @SneakyThrows
    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
        markdownCtrl.initialize(markdownView, markdownViewBlocker, noteBody);
        collectionNotes = FXCollections.observableArrayList(server.getAllNotes());
        listViewSetup(collectionNotes);

        searchField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    search();
                }
                default -> {}
            }
        });

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
            RadioMenuItem radioMenuItem = new RadioMenuItem(c.title);
            radioMenuItem.setOnAction(Event -> {
                collectionCtrl.viewNotes(c, collectionView, collectionNotes,
                        currentCollectionTitle, deleteCollectionButton, editCollectionTitle);
            });
            radioMenuItem.setStyle("-fx-text-fill: #000000");
            radioMenuItem.setToggleGroup(collectionSelect);
            collectionMenu.getItems().addFirst(radioMenuItem);
        }

        collectionSelect.selectToggle(allNotesButton);
        viewAllNotes();


        // Temporary solution
        scheduler.scheduleAtFixedRate(this::saveAllPendingNotes, 10,10, TimeUnit.SECONDS);
    }

    /**
     * Handles the current collection viewer setup
     */
    private void listViewSetup(ObservableList collectionNotes) {

        // Set required settings
        contentBlocker.setVisible(true);
        collectionView.setItems(collectionNotes);
        collectionView.setEditable(true);
        collectionView.setFixedCellSize(35);
        collectionView.setCellFactory(TextFieldListCell.forListView());

        // Set ListView entry as Title (editable)
        collectionView.setCellFactory(lv-> new NoteListItem(noteTitle, noteBody, this));

        // Remove content blocker on select
        collectionView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Note>() {
            @Override
            public void changed(ObservableValue<? extends Note> observable, Note oldValue, Note newValue) {
                showCurrentNote();
            }
        });

    }

    public void setSearchIsActive(boolean b) {
        searchIsActive = b;
        if (!b) {
            searchField.clear();
            collectionView.setItems(collectionNotes);
            collectionView.getSelectionModel().clearSelection();
            contentBlocker.setVisible(true);
        }
    }

    public void addNote() throws IOException {
        setSearchIsActive(false);
        noteCtrl.addNote(collectionSelect, allNotesButton, collectionNotes,
                createPendingNotes, collectionView, noteTitle,
                noteTitle_md, noteBody, contentBlocker);
    }


    public void showCurrentNote() {
        noteCtrl.showCurrentNote(collectionView, noteTitle, noteTitle_md, noteBody, contentBlocker);
    }

    public void addCollection() throws IOException {
        currentCollection = collectionCtrl.addCollection(collections, collectionSelect, collectionMenu);
        collectionCtrl.viewNotes(currentCollection, collectionView, collectionNotes,
                currentCollectionTitle, deleteCollectionButton, editCollectionTitle);
    }

    public void moveNoteFromCollection() throws IOException {
        currentCollection = collectionCtrl.moveNoteFromCollection(currentCollection, collectionView, collections);
        collectionCtrl.viewNotes(currentCollection, collectionView, collectionNotes,
                currentCollectionTitle, deleteCollectionButton, editCollectionTitle);
    }

    public void changeTitleInCollection() throws IOException {
        collectionCtrl.changeTitleInCollection(currentCollection, collections, collectionSelect, currentCollectionTitle);

    }

    public void deleteCollection() throws IOException {
        collectionCtrl.deleteCollection(currentCollection, collections, collectionMenu, collectionSelect, allNotesButton);
        collectionCtrl.viewNotes(currentCollection, collectionView, collectionNotes,
                currentCollectionTitle, deleteCollectionButton, editCollectionTitle);
        currentCollection = null;
    }

    public void search() {
        if (searchField.getText().isEmpty()) {
            return;
        }
        searchIsActive = true;
        String searchText = searchField.getText().toLowerCase();
        filteredNotes = new ArrayList<>();
        for (Note note : collectionNotes) {
            String noteText = note.body.toLowerCase();
            String noteTitle = note.title.toLowerCase();
            if (noteTitle.contains(searchText) || noteText.contains(searchText)) {
                filteredNotes.add(note);
            }
        }
        collectionView.setItems(FXCollections.observableArrayList(filteredNotes));
        contentBlocker.setVisible(true);
        markdownViewBlocker.setVisible(true);
        collectionView.getSelectionModel().clearSelection();

    }

    public void viewAllNotes() {
        collectionCtrl.viewNotes(currentCollection, collectionView, collectionNotes,
                currentCollectionTitle, deleteCollectionButton, editCollectionTitle);

    }
    public void viewCollection() {
        collectionCtrl.viewNotes(currentCollection, collectionView, collectionNotes,
                currentCollectionTitle, deleteCollectionButton, editCollectionTitle);
    }


    @FXML
    public void onEditCommit() {
        Note currentNote = (Note)collectionView.getSelectionModel().getSelectedItem();
        if (currentNote != null) {
            noteTitle.setText(currentNote.getTitle());
            noteTitle_md.setText(currentNote.getTitle());

            if (!createPendingNotes.contains(currentNote) && !updatePendingNotes.contains(currentNote)) {
                updatePendingNotes.add(currentNote);
            }
        }

    }

    @FXML
    public void onBodyChanged() {
        Note currentNote = (Note)collectionView.getSelectionModel().getSelectedItem();
        if (currentNote != null) {
            String rawText = noteBody.getText();
            currentNote.setBody(rawText);

            // Add any edited but already existing note to the pending list
            if (!createPendingNotes.contains(currentNote) && !updatePendingNotes.contains(currentNote)) {
                updatePendingNotes.add(currentNote);
                System.out.println("Note added to updatePendingNotes: " + currentNote.getTitle());
            }
        }
    }

    public void deleteSelectedNote() {
        noteCtrl.deleteSelectedNote(collectionView, filteredNotes,
                createPendingNotes, collectionNotes,
                noteBody, noteTitle,
                noteTitle_md, contentBlocker, updatePendingNotes);

    }

    public void refresh() {
        collectionCtrl.viewNotes(currentCollection, collectionView, collectionNotes,
                currentCollectionTitle, deleteCollectionButton, editCollectionTitle);
    }

    // Temporary solution
    @FXML
    public void onClose() {
        saveAllPendingNotes();

        // Ensure the scheduler is shut down when the application closes
        scheduler.shutdown();
    }

    public void saveAllPendingNotes() {
        noteCtrl.saveAllPendingNotes(createPendingNotes, updatePendingNotes);
    }



}
