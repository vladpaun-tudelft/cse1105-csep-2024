package client.controllers;

import client.scenes.DashboardCtrl;
import client.utils.ServerUtils;
import commons.EmbeddedFile;
import commons.Note;
import jakarta.inject.Inject;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class FilesCtrl {
    private final ServerUtils serverUtils;
    private DashboardCtrl dashboardCtrl;

    private HBox filesView;

    @Inject
    public FilesCtrl(ServerUtils serverUtils) {
        this.serverUtils = serverUtils;
    }

    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
    }

    public void setReferences(HBox filesView) {
        this.filesView = filesView;
    }

    public EmbeddedFile addFile(Note currentNote) throws IOException {
        if (currentNote == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setContentText("You don't have a note selected!");
            alert.showAndWait();
            return null;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload file");
        File uploadedFile = fileChooser.showOpenDialog(null);
        if (uploadedFile != null) {
            return serverUtils.addFile(currentNote, uploadedFile);
        }
        return null;
    }

    public void showFiles(List<EmbeddedFile> filesInNote) {
        filesView.getChildren().clear();
        for (EmbeddedFile file : filesInNote) {
            filesView.getChildren().add(
                    createFileEntry(file)
            );
        }
        filesView.getChildren().add(new Region());
    }

    public HBox createFileEntry(EmbeddedFile file) {
        HBox entry = new HBox();
        entry.getStyleClass().add("file-view-entry");
        entry.setSpacing(5);

        Label fileName = new Label(file.getFileName());
        fileName.getStyleClass().add("file-view-label");

        Button editButton = new Button();
        editButton.getStyleClass().add("file-view-edit-button");

        Button deleteButton = new Button();
        deleteButton.getStyleClass().add("file-view-delete-button");

        entry.getChildren().addAll(fileName, editButton, deleteButton);

        return entry;
    }
}
