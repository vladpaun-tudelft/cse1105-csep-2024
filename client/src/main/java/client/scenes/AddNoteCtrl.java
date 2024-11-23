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
import org.w3c.dom.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class AddNoteCtrl {

    private final ServerUtils server;
    private final MainCtrl mainCtrl;

    @FXML
    private TextField noteTitle;

    @Inject
    public AddNoteCtrl(ServerUtils server, MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
    }

    public void cancel() {
        noteTitle.clear();
        mainCtrl.showDashboard();
    }

    public void addNote() {
        //TODO Implement once we have the proper backend
        noteTitle.clear();
        mainCtrl.showDashboard();
    }
}
