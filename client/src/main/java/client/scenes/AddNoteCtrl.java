package client.scenes;

import com.google.inject.Inject;

import client.utils.ServerUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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
