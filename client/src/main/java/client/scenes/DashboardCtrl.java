package client.scenes;

import client.utils.Config;
import client.utils.ServerUtils;
import com.google.inject.Inject;
import commons.Collection;
import commons.Note;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.StringConverter;
import lombok.SneakyThrows;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.IOException;
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
    private WebView markdownView;
    @FXML
    private Label markdownViewBlocker;
    @FXML
    private Label noteTitle;
    @FXML
    private Label noteTitle_md;
    @FXML
    private ListView collectionView;
    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button deleteButton_md;
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
    private Button editCollectionButton;

    private ObservableList<Note> collectionNotes;
    private List<Note> filteredNotes = new ArrayList<>();
    private boolean searchIsActive = false;

    private List<Collection> collections;

    Config config = new Config();

    private final List<Note> createPendingNotes = new ArrayList<>();
    private final List<Note> updatePendingNotes = new ArrayList<>();

    private boolean pendingHideContentBlocker = true;

    @Inject
    public DashboardCtrl(ServerUtils server, MainCtrl mainCtrl) throws IOException {
        this.mainCtrl = mainCtrl;
        this.server = server;
    }

    @SneakyThrows
    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
        collectionNotes = FXCollections.observableArrayList(server.getAllNotes());

        listViewSetup(collectionNotes);

        updateMarkdownView("");

        deleteButton.setDisable(true);
        deleteButton_md.setDisable(true);

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
                viewCollection();
            });
            radioMenuItem.setStyle("-fx-text-fill: #000000");
            radioMenuItem.setToggleGroup(collectionSelect);
            collectionMenu.getItems().addFirst(radioMenuItem);
        }

        collectionSelect.selectToggle(allNotesButton);
        viewAllNotes();

        // Temporary solution
        scheduler.scheduleAtFixedRate(this::saveAllPendingNotes, 10,10, TimeUnit.SECONDS);

        // Listener for updating the markdown view
        noteBody.textProperty().addListener((observable, oldValue, newValue) -> {
            Note currentNote = (Note)collectionView.getSelectionModel().getSelectedItem();
            if (currentNote != null) {
                currentNote.setBody(newValue);
                String renderedHtml = convertMarkdownToHtml(newValue);
                markdownView.getEngine().loadContent(renderedHtml, "text/html");

                markdownViewBlocker.setVisible(newValue == null || newValue.isEmpty());
            }
        });
    }

    /**
     * Handles the current collection viewer setup
     */
    private void listViewSetup(ObservableList collectionNotes) {

        // Set required settings
        deleteButton.setDisable(true);
        contentBlocker.setVisible(true);
        collectionView.setItems(collectionNotes);
        collectionView.setEditable(true);
        collectionView.setFixedCellSize(35);
        collectionView.setCellFactory(TextFieldListCell.forListView());

        // Set ListView entry as Title (editable)
        setupCellFactory();

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
                if (newValue != null) {
                    updateMarkdownView(newValue.getBody());
                }
            }
        });

    }

    private void setupCellFactory() {
        collectionView.setCellFactory(lv -> {
            TextFieldListCell<Note> cell = new TextFieldListCell<>();   // Create basic TextField cell to edit
            cell.converterProperty().set(new StringConverter<>() {      // Edit converter which cell uses to display the custom object
                @Override
                public String toString(Note note) {                     // Override toString which the cell uses to display the object
                    return note != null ? note.getTitle() : "";
                    // We edit it such that it uses the cell uses the note title to display the note
                }

                @Override
                public Note fromString(String string) {                 // Say what properties need to be changed on title edit
                    Note note = cell.getItem();
                    if (note != null) {
                        note.setTitle(string);
                    }
                    return note;
                }
            });
            cell.setOnMouseClicked(event -> {                           // Handle on edit behaviour
                if (event != null) {
                    Note item = cell.getItem();
                    if(item != null) {
                        System.out.println("Cell selected: " + item.getTitle());
                        noteBody.setText((item).getBody());

                        noteTitle.setText((item).getTitle());
                        noteTitle_md.setText((item).getTitle());

                        deleteButton.setDisable(false);
                        deleteButton_md.setDisable(false);

                        handleContentBlocker();
                    }
                }
            });
            return cell;
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

    public void addNote() throws IOException {
        setSearchIsActive(false);

        Collection collection;
        if (collectionSelect.getSelectedToggle().equals(allNotesButton)) {
            collection = server.getCollections().stream().filter(c -> c.title.equals("Default")).toList().getFirst();
        }
        else {
            RadioMenuItem selectedCollection = (RadioMenuItem)(collectionSelect.getSelectedToggle());
            collection = server.getCollections().stream().filter(c -> c.title.equals(selectedCollection.getText())).toList().getFirst();
        }
        Note newNote = new Note("New Note", "", collection);
        collectionNotes.add(newNote);
        // Add the new note to a list of notes pending being sent to the server
        createPendingNotes.add(newNote);
        System.out.println("Note added to createPendingNotes: " + newNote.getTitle());

        collectionView.getSelectionModel().select(collectionNotes.size() - 1);
        collectionView.getFocusModel().focus(collectionNotes.size() - 1);
        collectionView.edit(collectionNotes.size() - 1);

        noteTitle.setText("New Note");
        noteTitle_md.setText("New Note");

        noteBody.setText("");

        updateMarkdownView("");
    }

    public void addCollection() throws IOException {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New collection");
        dialog.setContentText("Please enter the title for your new collection");
        Optional<String> collectionTitle = dialog.showAndWait();
        if (collectionTitle.isPresent()) {
            String s = collectionTitle.get();
            if (s.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("A collection needs a title");
                alert.showAndWait();
                return;
            }
            if (!server.getCollections().stream().filter(c -> c.title.equals(s)).toList().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("A collection with this title already exists");
                alert.showAndWait();
                return;
            }
            Collection addedCollection = server.addCollection(new Collection(s));
            config.writeToFile(addedCollection);
            collections.add(addedCollection);

            // add entry in collections menu
            RadioMenuItem radioMenuItem = new RadioMenuItem(s);
            radioMenuItem.setToggleGroup(collectionSelect);
            radioMenuItem.setStyle("-fx-text-fill: #000000");
            radioMenuItem.setOnAction(Event -> {
                viewCollection();
            });
            collectionMenu.getItems().addFirst(radioMenuItem);
            collectionSelect.selectToggle(radioMenuItem);
            viewCollection();
        }
    }

    /**
     * A method used to move note from one collection to the other
     *
     * @throws IOException exception
     */
    public void moveNoteFromCollection() throws IOException {
        // Get the currently selected note
        Note currentNote = (Note) collectionView.getSelectionModel().getSelectedItem();
        if (currentNote == null) {
            return;
        }
        // Get the title of current selected collection
        String selectedCollectionTitle = ((RadioMenuItem) collectionSelect.getSelectedToggle()).getText();

        // Find the collection
        Collection selectedCollection = server.getCollections()
                .stream()
                .filter(c -> c.title.equals(selectedCollectionTitle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        // Ask for a new title with a dialog
        TextInputDialog dialog = new TextInputDialog(selectedCollectionTitle);
        dialog.setTitle("Move Note");
        dialog.setContentText("Please enter the title of destination collection:");
        Optional<String> destinationCollectionTitle = dialog.showAndWait();

        // If user provided a title of destination collection
        if (destinationCollectionTitle.isPresent()) {
            String destinationTitle = destinationCollectionTitle.get().trim();

            // If user provided a title that is empty
            if (destinationTitle.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("A destination collection needs a title");
                alert.showAndWait();
                return;
            }

            // If user will choose the same collection
            if (destinationTitle.equals(selectedCollectionTitle)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setContentText("Cannot move the note to the same collection");
                alert.showAndWait();
                return;
            }

            // We get the destination collection
            Collection destinationCollection = server.getCollections()
                    .stream()
                    .filter(c -> c.title.equals(destinationTitle) && !c.title.equals(selectedCollection.title))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Destination collection not found"));

            //Move the note from previous collection to the new collection
            selectedCollection.notes.remove(currentNote);
            destinationCollection.notes.add(currentNote);

            // Update the note in server (we changed its collection)
            server.updateNote(currentNote);

            // Save collections locally
            config.writeAllToFile(collections);

            // Update menu
            viewCollection();
            viewAllNotes();

            // Information aller whether the note has been moved successfully
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Moved Note");
            alert.setContentText("Note moved successfully");
            alert.showAndWait();

        }
    }

    /**
     * Method that will change the title in collection
     *
     * @throws IOException exception
     */
    public void changeTitleInCollection() throws IOException {
        // Get the collection title
        String selectedCollectionTitle = ((RadioMenuItem) collectionSelect.getSelectedToggle()).getText();

        // Find the collection
        Collection selectedCollection = server.getCollections()
                .stream()
                .filter(c -> c.title.equals(selectedCollectionTitle))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        // Ask for a new title with a dialog
        TextInputDialog dialog = new TextInputDialog(selectedCollectionTitle);
        dialog.setTitle("Change Collection Title");
        dialog.setContentText("Please enter the new title for the collection:");

        Optional<String> newTitleOptional = dialog.showAndWait();

        // If we get a title
        if (newTitleOptional.isPresent()) {
            String newTitle = newTitleOptional.get().trim();

            // Validate the new title
            if (newTitle.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("The collection title cannot be empty.");
                alert.showAndWait();
                return;
            }

            // Check if a collection with the same title already exists
            if (server.getCollections()
                    .stream()
                    .anyMatch(c -> c.title.equals(newTitle))) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("A collection with this title already exists.");
                alert.showAndWait();
                return;
            }

            // Update the collection's title
            selectedCollection.title = newTitle;

            // Update the collection on the server
            Collection updatedCollection = server.updateCollection(selectedCollection);

            // Update the title locally
            for (int i = 0; i < collections.size(); i++) {
                if (collections.get(i).id == selectedCollection.id) {
                    collections.set(i, updatedCollection);
                    break;
                }
            }
            config.writeAllToFile(collections);

            config.writeAllToFile(collections);

            // update the menu item
            ((RadioMenuItem) collectionSelect.getSelectedToggle()).setText(newTitle);
            currentCollectionTitle.setText(newTitle);

            //information alert whether the collection title has been successfully changed
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Changed Collection Title");
            alert.setContentText("Collection title changed successfully");
            alert.showAndWait();
        }
    }

    public void deleteCollection() throws IOException {
        String selectedCollectionTitle = ((RadioMenuItem)collectionSelect.getSelectedToggle()).getText();
        Collection selectedCollection = server.getCollections().stream().filter(c -> c.title.equals(selectedCollectionTitle)).toList().getFirst();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete collection");
        alert.setContentText("Are you sure you want to delete this collection? All notes in the collection will be deleted as well.");
        Optional<ButtonType> buttonType = alert.showAndWait();
        if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
           List<Note> notesInCollection = server.getAllNotes()
                   .stream().filter(n -> n.collection.id == selectedCollection.id)
                   .toList();
           for (Note n : notesInCollection) {
               deleteNote(n);
           }
           // delete collection from server
           server.deleteCollection(selectedCollection.id);
           // delete collection from config file
           collections.remove(selectedCollection);
           config.writeAllToFile(collections);
           // delete collection from collections menu
           collectionMenu.getItems().remove(
                   collectionSelect.getSelectedToggle()
           );
           collectionSelect.selectToggle(allNotesButton);
           viewAllNotes();
        }
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
        deleteButton_md.setDisable(true);

        collectionView.getSelectionModel().clearSelection();

    }

    public void viewAllNotes() {
        saveAllPendingNotes();
        setSearchIsActive(false);
        contentBlocker.setVisible(true);
        collectionNotes = FXCollections.observableArrayList(server.getAllNotes());
        listViewSetup(collectionNotes);
        currentCollectionTitle.setText("All Notes");
        deleteCollectionButton.setDisable(true);
    }

    public void viewCollection() {
        saveAllPendingNotes();
        setSearchIsActive(false);
        contentBlocker.setVisible(true);
        String collectionTitle = ((RadioMenuItem)collectionSelect.getSelectedToggle()).getText();
        Collection currentCollection = server.getCollections().stream().filter(c -> c.title.equals(collectionTitle)).toList().getFirst();
        List<Note> notes = server.getAllNotes().stream().filter(n -> n.collection.id == currentCollection.id).toList();
        collectionNotes = FXCollections.observableArrayList(notes);
        listViewSetup(collectionNotes);
        currentCollectionTitle.setText(collectionTitle);
        if (currentCollection.title.equals("Default")) {
            deleteCollectionButton.setDisable(true);
        }
        else {
            deleteCollectionButton.setDisable(false);
        }
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

            // Update the Markdown view
            String renderedHtml = convertMarkdownToHtml(rawText);
            Platform.runLater(() -> markdownView.getEngine().loadContent(renderedHtml, "text/html"));

            // Add any edited but already existing note to the pending list
            if (!createPendingNotes.contains(currentNote) && !updatePendingNotes.contains(currentNote)) {
                updatePendingNotes.add(currentNote);
                System.out.println("Note added to updatePendingNotes: " + currentNote.getTitle());
            }
        }
    }

    private String convertMarkdownToHtml(String markdown) {
        URL cssUrl = getClass().getResource("/css/markdown.css");
        assert cssUrl != null;
        String cssPath = cssUrl.toExternalForm();

        if (markdown == null || markdown.isEmpty()) {
            return "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                        "<link rel='stylesheet' type='text/css' href='" + cssPath + "'>" +
                    "</head>" +
                    "<body></body>" +
                    "</html>";
        }

        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        String htmlContent = renderer.render(document);

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<link rel='stylesheet' type='text/css' href='" + cssPath + "'>" +
                "</head>" +
                "<body>" +
                htmlContent +
                "</body>" +
                "</html>";
    }

    @FXML
    private void updateMarkdownView(String markdown) {
        String renderedHtml = convertMarkdownToHtml(markdown);
        Platform.runLater(() -> markdownView.getEngine().loadContent(renderedHtml, "text/html"));
        markdownViewBlocker.setVisible(markdown == null || markdown.isEmpty());
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
                if (createPendingNotes.contains(currentNote)) {
                    createPendingNotes.remove(currentNote);
                }
                else {
                    deleteNote(currentNote);
                }
                collectionNotes.remove(currentNote);
                noteBody.clear();

                noteTitle.setText("");
                noteTitle_md.setText("");

                updateMarkdownView("");

                deleteButton.setDisable(true);
                deleteButton_md.setDisable(true);

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
