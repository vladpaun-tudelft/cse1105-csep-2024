package client.controllers;

import client.scenes.DashboardCtrl;
import client.utils.ServerUtils;
import commons.Note;
import jakarta.inject.Inject;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;


public class FilesCtrl {
    private final ServerUtils serverUtils;
    private DashboardCtrl dashboardCtrl;

    private ListView filesView;

    @Inject
    public FilesCtrl(ServerUtils serverUtils) {
        this.serverUtils = serverUtils;
    }

    public void setRefrences(ListView filesView) {
        this.filesView = filesView;
    }

    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
    }

    public void addFile(Note currentNote) throws IOException {
        if (currentNote == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setContentText("You don't have a note selected!");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload file");
        File uploadedFile = fileChooser.showOpenDialog(null);
        if (uploadedFile != null) {
            serverUtils.addFile(currentNote, uploadedFile);
        }
    }
}
