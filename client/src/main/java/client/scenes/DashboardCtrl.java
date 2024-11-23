package client.scenes;

import com.google.inject.Inject;

import client.utils.ServerUtils;
import commons.Note;
import commons.Person;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlls all logic for the main dashboard.
 */
public class DashboardCtrl implements Initializable {

    private final ServerUtils server;
    private final MainCtrl mainCtrl;

    @FXML
    private TreeView treeView;
    
    @Inject
    public DashboardCtrl(ServerUtils server, MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
    }

    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
        TreeItem<Note> rootItem = new TreeItem<>(new Note("John", "e", null));
        treeView.setRoot(rootItem);

        // TODO: Display note title as TreeView element -> Note class change required (communicate with backend?)
    }
    /**
     * Creates a note and immediately selects it
     */
    public void onAddButtonClicked() {
        treeView.getRoot().getChildren().add(new TreeItem<>(new Note("John", "e", null)));
    }

}
