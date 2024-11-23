package client.scenes;

import com.google.inject.Inject;

import client.utils.ServerUtils;
import commons.Note;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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
    private Button addNoteButton;

    @Inject
    public DashboardCtrl(ServerUtils server, MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
    }

    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
        var noteList = server.getNotes();
        for (var note : noteList) {
            notes.getChildren().add(createNoteButton(note));
        }
    }

    public void addNote() {
        mainCtrl.showAddNote();
    }

    private Button createNoteButton(Note note) {
        Button b = new Button(note.title);
        b.setPrefWidth(notes.getPrefWidth());
        b.setStyle("-fx-background-color: transparent");
        b.setCursor(Cursor.HAND);
        b.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                noteTitle.setText(note.title);
                noteBody.setText(note.body);
            }
        });
        return b;
    }

}
