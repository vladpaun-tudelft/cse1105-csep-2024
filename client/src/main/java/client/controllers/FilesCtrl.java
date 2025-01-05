package client.controllers;

import client.scenes.DashboardCtrl;
import client.ui.DialogStyler;
import client.utils.ServerUtils;
import commons.EmbeddedFile;
import commons.Note;
import jakarta.inject.Inject;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


public class FilesCtrl {
    private final ServerUtils serverUtils;
    private FileChooser fileChooser;
    private DashboardCtrl dashboardCtrl;

    private HBox filesView;
    private DialogStyler dialogStyler = new DialogStyler();

    @Inject
    public FilesCtrl(ServerUtils serverUtils, FileChooser fileChooser) {
        this.serverUtils = serverUtils;
        this.fileChooser = fileChooser;
    }

    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
    }

    public void setReferences(HBox filesView) {
        this.filesView = filesView;
    }

    // for testing purposes
    public void setFileChooser(FileChooser fileChooser) {
        this.fileChooser = fileChooser;
    }

    public void setDialogStyler(DialogStyler dialogStyler) {
        this.dialogStyler = dialogStyler;
    }

    public EmbeddedFile addFile(Note currentNote) throws IOException {
        if (currentNote == null) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.INFORMATION,
                    "Error",
                    "Error",
                    "You don't have a note selected!"
            );
            alert.showAndWait();
            return null;
        }

        fileChooser.setTitle("Upload file");
        File uploadedFile = fileChooser.showOpenDialog(null);
        if (uploadedFile != null) {
            if (uploadedFile.isDirectory()) {
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.INFORMATION,
                        "File error",
                        "File error",
                        "Directories cannot be uploaded!"
                );
                alert.showAndWait();
                return null;
            }

            if (!checkFileName(currentNote, uploadedFile.getName())) {
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.INFORMATION,
                        "File error",
                        "File error",
                        "A file with this name already exists!"
                );
                alert.showAndWait();
                return null;
            }
            EmbeddedFile e = serverUtils.addFile(currentNote, uploadedFile);
            currentNote.getEmbeddedFiles().add(e);
            showFiles(currentNote);
            return e;
        }
        return null;
    }

    /**
     * Checks if a file with the name fileName already exists for the current note
     */
    public boolean checkFileName(Note currentNote, String fileName) {
        List<EmbeddedFile> files = serverUtils.getFilesByNote(currentNote);
        List<EmbeddedFile> filteredFiles = files.stream().filter(e -> e.getFileName().equals(fileName)).toList();
        return filteredFiles.isEmpty();
    }

    public void showFiles(Note currentNote) {
        if (currentNote == null)
            return;
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
        fileName.setOnMouseReleased(event -> {
            downloadFile(currentNote, file);
        });

        Button editButton = new Button();
        editButton.getStyleClass().add("icon");
        editButton.getStyleClass().add("file-view-edit-button");
        editButton.setOnAction(event -> {
            renameFile(currentNote, file);
        });

        Button deleteButton = new Button();
        deleteButton.getStyleClass().add("icon");
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
            currentNote.getEmbeddedFiles().remove(file);
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
        Optional<String> fileName = dialog.showAndWait();
        if (fileName.isPresent()) {
            if (!checkFileName(currentNote, fileName.get())) {
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.INFORMATION,
                        "File error",
                        "File error",
                        "A file with this name already exists!"
                );
                alert.showAndWait();
                return;
            }
            currentNote.getEmbeddedFiles().remove(file);
            EmbeddedFile e = serverUtils.renameFile(currentNote, file, fileName.get());
            currentNote.getEmbeddedFiles().add(e);
            persistFileName(currentNote, file.getFileName(), fileName.get());
            showFiles(currentNote);
        }
    }

    public void persistFileName(Note currentNote, String oldName, String newName) {
        String body = currentNote.getBody();
        String regex = "\\!\\[(.*?)\\]\\(" + java.util.regex.Pattern.quote(oldName) + "\\)";
        String replacement = "![$1](" + newName + ")";
        String newBody = body.replaceAll(regex, replacement);
        currentNote.setBody(newBody);
        dashboardCtrl.getNoteCtrl().showCurrentNote(currentNote);
    }

    public void downloadFile(Note currentNote, EmbeddedFile embeddedFile) {
        fileChooser.setTitle("Save file");
        fileChooser.setInitialFileName(embeddedFile.getFileName());
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(embeddedFile.getFileType() + " files", "*." + embeddedFile.getFileType())
        );

        File fileToSave = fileChooser.showSaveDialog(null);

        if (fileToSave != null) {
            try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                fos.write(embeddedFile.getFileContent());
            }
            catch (IOException e) {
                e.printStackTrace();
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.INFORMATION,
                        "Save file error",
                        "Save file error",
                        "The file could not be saved"
                );
                alert.showAndWait();
            }
        }
    }
}
