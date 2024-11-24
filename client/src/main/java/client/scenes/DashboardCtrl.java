package client.scenes;

import com.google.inject.Inject;

import client.utils.ServerUtils;
import commons.Note;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controls all logic for the main dashboard.
 */
public class DashboardCtrl implements Initializable {

    private final ServerUtils server;
    private final MainCtrl mainCtrl;

    @FXML
    private VBox notes;
    @FXML
    private TextArea noteBody;
    @FXML
    private Label noteTitle;
    @FXML
    private ListView collectionView;
    @FXML
    private Button addButton;
    @FXML
    private VBox root;

    private ObservableList<Note> collectionNotes;

    @Inject
    public DashboardCtrl(ServerUtils server, MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
    }

    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
        listViewSetup();
    }

    /**
     * Handles the current collection viewer setup
     */
    private void listViewSetup() {
//        // Fill list with current entries
//        collectionNotes= FXCollections.observableArrayList();
//        var noteList = server.getNotes();
//        for (var note : noteList) {
//            // TO DO LOAD NOTES
//        }
        collectionNotes = FXCollections.observableArrayList(
                new Note("Note 1", "This is the body of Note 1.", null),
                new Note("Note 2", "This is the body of Note 2.", null)
        );

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

    }

    public void addNote() {
        collectionNotes.add(new Note("New Note", "", null));
        collectionView.getSelectionModel().select(collectionNotes.size() - 1);
        collectionView.getFocusModel().focus(collectionNotes.size() - 1);
        collectionView.edit(collectionNotes.size() - 1);

        noteTitle.setText("New Note");
        noteBody.setText("");
    }

    @FXML
    public void onEditCommit() {
        Note currentNote = (Note)collectionView.getSelectionModel().getSelectedItem();
        noteTitle.setText(currentNote.getTitle());
    }

    @FXML
    public void onBodyChanged() {
        Note currentNote = (Note)collectionView.getSelectionModel().getSelectedItem();
        currentNote.setBody(noteBody.getText());
    }

}
