package client.scenes;

import com.google.inject.Inject;

import client.utils.ServerUtils;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @FXML
    private Label contentBlocker;
    @FXML
    private TextArea noteBody;
    @FXML
    private Label noteTitle;
    @FXML
    private ListView collectionView;
    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button searchButton;
    @FXML
    private TextField searchField;
    @FXML
    private VBox root;

    private ObservableList<Note> collectionNotes;
    private List<Note> filteredNotes = new ArrayList<>();
    private boolean searchIsActive = false;

    private final List<Note> createPendingNotes = new ArrayList<>();
    private final List<Note> updatePendingNotes = new ArrayList<>();

    private boolean pendingHideContentBlocker = true;

    @Inject
    public DashboardCtrl(ServerUtils server, MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
    }

    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
        // Gets all the notes in the db into the list of notes in the client
        // TODO: To be changed with server.getNotesByCollection when we implement collections
        collectionNotes = FXCollections.observableArrayList(server.getAllNotes());

        listViewSetup(collectionNotes);

        Image img = new Image("client/icons/trash.png");
        ImageView imgView = new ImageView(img);
        deleteButton.setGraphic(imgView);
        deleteButton.setDisable(true);

        img = new Image("client/icons/search.png");
        imgView = new ImageView(img);
        searchButton.setGraphic(imgView);
        searchField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    search();
                }
                default -> {}
            }
        });

        if (server.getCollections().stream().filter(c -> c.title.equals("All")).toList().isEmpty()) {
            server.addCollection(new Collection("All"));
        }

        // Temporary solution
        scheduler.scheduleAtFixedRate(this::saveAllPendingNotes, 10,10, TimeUnit.SECONDS);
    }

    /**
     * Handles the current collection viewer setup
     */
    private void listViewSetup(ObservableList collectionNotes) {

        // Set required settings
        collectionView.setItems(collectionNotes);
        collectionView.setEditable(true);
        collectionView.setFixedCellSize(35);
        collectionView.setCellFactory(TextFieldListCell.forListView());

        // Set ListView entry as Title (editable)
        collectionView.setCellFactory(lv -> {
            TextFieldListCell<Note> cell = new TextFieldListCell<>();
            cell.converterProperty().set(new StringConverter<>() {
                @Override
                public String toString(Note note) {
                    return note != null ? note.getTitle() : "";
                }

                @Override
                public Note fromString(String string) {
                    // Create a new Note object or update an existing one based on the edited text
                    Note note = cell.getItem();
                    if (note != null) {
                        note.setTitle(string);
                    }
                    return note;
                }
            });
            cell.setOnMouseClicked(event -> {
                if (event != null) {
                    Note item = cell.getItem();
                    if(item != null) {
                        System.out.println("Cell selected: " + item.getTitle());
                        noteBody.setText((item).getBody());
                        noteTitle.setText((item).getTitle());
                        deleteButton.setDisable(false);
                        handleContentBlocker();
                    }
                }
            });
            return cell;
        });

        // Reset edit on click anywhere
        root.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if(!addButton.contains(event.getX(), event.getY())) {
                int selected = collectionView.getSelectionModel().getSelectedIndex();
                collectionView.getSelectionModel().clearSelection();
                collectionView.getSelectionModel().select(selected);
            }
        });

        // Remove content blocker on select
        collectionView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Note>() {

            @Override
            public void changed(ObservableValue<? extends Note> observable, Note oldValue, Note newValue) {
                pendingHideContentBlocker = (newValue!=null);
            }
        });

    }

    public void setSearchIsActive(boolean b) {
        searchIsActive = b;
        if (!b) {
            searchField.clear();
            listViewSetup(collectionNotes);
            collectionView.getSelectionModel().clearSelection();
            contentBlocker.setVisible(true);
        }
    }

    /**
     * Handles content blocker when new Note is loaded
     */
    private void handleContentBlocker() {
        pendingHideContentBlocker = !pendingHideContentBlocker;
        contentBlocker.setVisible(pendingHideContentBlocker);
    }

    public void addNote() {
        setSearchIsActive(false);
        Note newNote = new Note("New Note", "", server.getCollections().getFirst());
        collectionNotes.add(newNote);
        // Add the new note to a list of notes pending being sent to the server
        createPendingNotes.add(newNote);
        System.out.println("Note added to createPendingNotes: " + newNote.getTitle());

        collectionView.getSelectionModel().select(collectionNotes.size() - 1);
        collectionView.getFocusModel().focus(collectionNotes.size() - 1);
        collectionView.edit(collectionNotes.size() - 1);

        noteTitle.setText("New Note");
        noteBody.setText("");
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
        listViewSetup(FXCollections.observableArrayList(filteredNotes));
        contentBlocker.setVisible(true);
        deleteButton.setDisable(true);
        collectionView.getSelectionModel().clearSelection();

    }

    @FXML
    public void onEditCommit() {
        Note currentNote = (Note)collectionView.getSelectionModel().getSelectedItem();
        if (currentNote != null) {
            noteTitle.setText(currentNote.getTitle());

            if (!createPendingNotes.contains(currentNote) && !updatePendingNotes.contains(currentNote)) {
                updatePendingNotes.add(currentNote);
            }
        }

    }

    @FXML
    public void onBodyChanged() {
        Note currentNote = (Note)collectionView.getSelectionModel().getSelectedItem();
        if (currentNote != null) {
            currentNote.setBody(noteBody.getText());

            // Add any edited but already existing note to the pending list
            if (!createPendingNotes.contains(currentNote) && !updatePendingNotes.contains(currentNote)) {
                updatePendingNotes.add(currentNote);
                System.out.println("Note added to updatePendingNotes: " + currentNote.getTitle());
            }
        }
    }

    public void deleteSelectedNote() {
        Note currentNote = (Note) collectionView.getSelectionModel().getSelectedItem();
        if (filteredNotes.contains(currentNote)) {
            filteredNotes.remove(currentNote);
            listViewSetup(FXCollections.observableArrayList(filteredNotes));
        }
        if (currentNote != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm deletion");
            alert.setContentText("Do you really want to delete this note?");
            Optional<ButtonType> buttonType = alert.showAndWait();
            if(buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
                deleteNote(currentNote);
                noteBody.clear();
                noteTitle.setText("");
                deleteButton.setDisable(true);
                contentBlocker.setVisible(true);
                System.out.println("Note deleted: " + currentNote.getTitle());
            }
        }
    }

    // Temporary solution
    @FXML
    public void onClose() {
        saveAllPendingNotes();

        // Ensure the scheduler is shut down when the application closes
        scheduler.shutdown();
    }

    public void saveAllPendingNotes() {
            try {
                for (Note note : createPendingNotes) {
                    Note savedNote = server.addNote(note);
                    note.id = savedNote.id;
                }
                createPendingNotes.clear();

                for (Note note : updatePendingNotes) {
                    server.updateNote(note);
                }
                updatePendingNotes.clear();
                System.out.println("Saved all notes on server");
            } catch (Exception e) {
                e.printStackTrace(); // Log the exception to debug
            }
    }

    public void deleteNote(Note note) {
        collectionNotes.remove(note);
        createPendingNotes.remove(note);
        updatePendingNotes.remove(note);

        server.deleteNote(note.id);
    }

}
