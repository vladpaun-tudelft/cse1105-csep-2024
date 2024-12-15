package client.controllers;

import client.scenes.DashboardCtrl;
import client.ui.DialogStyler;
import client.utils.ServerUtils;
import commons.EmbeddedFile;
import commons.Note;
import jakarta.inject.Inject;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


public class FilesCtrl {
    private final ServerUtils serverUtils;
    private DashboardCtrl dashboardCtrl;

    private HBox filesView;
    private DialogStyler dialogStyler = new DialogStyler();

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

    public void showFiles(Note currentNote) {
        List<EmbeddedFile> filesInNote = serverUtils.getFilesByNote(currentNote);
        filesView.getChildren().clear();
        for (EmbeddedFile file : filesInNote) {
            filesView.getChildren().add(
                    createFileEntry(currentNote, file)
            );
        }
        filesView.getChildren().add(new Region());
    }

    public HBox createFileEntry(Note currentNote, EmbeddedFile file) {
        HBox entry = new HBox();
        entry.getStyleClass().add("file-view-entry");
        entry.setSpacing(5);

        Label fileName = new Label(file.getFileName());
        fileName.getStyleClass().add("file-view-label");

        Button editButton = new Button();
        editButton.getStyleClass().add("file-view-edit-button");
        editButton.setOnAction(event -> {
            renameFile(currentNote, file);
        });

        Button deleteButton = new Button();
        deleteButton.getStyleClass().add("file-view-delete-button");
        deleteButton.setOnAction(event -> {
            deleteFile(currentNote, file);
        });

        entry.getChildren().addAll(fileName, editButton, deleteButton);
        return entry;
    }

    public void deleteFile(Note currentNote, EmbeddedFile file) {
        Alert alert = dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                "Confirm deletion",
                "Confirm deletion",
                "Are you sure you want to delete this file?"
        );
        Optional<ButtonType> buttonType = alert.showAndWait();
        if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)){
            serverUtils.deleteFile(currentNote, file);
            showFiles(currentNote);
        }
    }

    public void renameFile(Note currentNote, EmbeddedFile file) {
        TextInputDialog dialog = dialogStyler.createStyledTextInputDialog(
                "Rename file",
                "Rename file",
                "Please enter the new title for the file:"
        );
        Optional<String> collectionTitle = dialog.showAndWait();
        if (collectionTitle.isPresent()) {
            serverUtils.renameFile(currentNote, file, collectionTitle.get());
            showFiles(currentNote);
        }
    }
}
