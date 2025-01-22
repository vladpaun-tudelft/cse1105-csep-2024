package client.controllers;

import client.LanguageManager;
import client.entities.Action;
import client.entities.ActionType;
import client.scenes.DashboardCtrl;
import client.ui.DialogStyler;
import client.utils.Config;
import client.utils.ServerUtils;
import commons.EmbeddedFile;
import commons.Note;
import jakarta.inject.Inject;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import lombok.Setter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


public class FilesCtrl {
    private final ServerUtils serverUtils;
    // for testing purposes
    @Setter private FileChooser fileChooser;
    @Setter private DashboardCtrl dashboardCtrl;

    private HBox filesView;
    @Setter private DialogStyler dialogStyler = new DialogStyler();

    private Config config;
    private LanguageManager languageManager;
    private ResourceBundle bundle;


    @Inject
    public FilesCtrl(ServerUtils serverUtils, FileChooser fileChooser, Config config) {
        this.serverUtils = serverUtils;
        this.fileChooser = fileChooser;

        this.config = config;
        this.languageManager = LanguageManager.getInstance(this.config);
        this.bundle = this.languageManager.getBundle();
    }

    public void setReferences(HBox filesView) {
        this.filesView = filesView;
    }

    public EmbeddedFile addFile(Note currentNote) {
        if (currentNote == null) {
            showNoNoteSelectedAlert();
            return null;
        }

        fileChooser.setTitle(bundle.getString("uploadFile.text"));
        File uploadedFile = fileChooser.showOpenDialog(null);
        if (uploadedFile != null) {
            if (!validateUploadedFile(currentNote, uploadedFile)) return null;

            EmbeddedFile embeddedFile = addFileInputted(currentNote, uploadedFile);
            return embeddedFile;
        }
        return null;
    }

    public EmbeddedFile addFileInputted(Note currentNote, File file) {

        try {
            EmbeddedFile embeddedFile = serverUtils.addFile(currentNote, file);
            serverUtils.send("/app/notes/" + currentNote.getId() + "/files", embeddedFile.getId());
            return embeddedFile;
        } catch (Exception exception) {
            showErrorUploadingFileAlert();
            return null;
        }

    }

    /**
     * Checks if a file with the name fileName already exists for the current note
     */
    public boolean checkFileName(Note currentNote, String fileName) {
        List<EmbeddedFile> files = currentNote.getEmbeddedFiles();
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

    public void updateView(Note currentNote) {
        List<EmbeddedFile> files = currentNote.getEmbeddedFiles();
        filesView.getChildren().clear();
        for (EmbeddedFile file : files) {
            filesView.getChildren().add(
                    createFileEntry(currentNote, file)
            );
        }
        filesView.getChildren().add(new Region());
    }

    public void updateViewAfterAdd(Note currentNote, Long fileId) {
        if (currentNote == null) {
            return;
        }
        EmbeddedFile e = serverUtils.getFileById(currentNote, fileId);
        currentNote.getEmbeddedFiles().add(e);
        updateView(currentNote);
    }

    public void updateViewAfterDelete(Note currentNote, Long fileId) {
        if (currentNote == null) {
            return;
        }
        EmbeddedFile fileToRemove = new EmbeddedFile(currentNote, "", "", null);
        fileToRemove.setId(fileId);
        currentNote.getEmbeddedFiles().remove(fileToRemove);
        updateView(currentNote);
    }

    public void updateViewAfterRename(Note currentNote, Object[] newFileName) {
        if (currentNote == null) {
            return;
        }
        Long fileId = ((Number) newFileName[0]).longValue();
        String fileName = (String) newFileName[1];
        for (EmbeddedFile file : currentNote.getEmbeddedFiles()) {
            if (Objects.equals(file.getId(), fileId)) {
                file.setFileName(fileName);
            }
        }
        updateView(currentNote);
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
            dashboardCtrl.getActionHistory().push(
                    new Action(ActionType.DELETE_FILE,currentNote,null,null, file)
            );
            deleteFile(currentNote, file);
        });

        entry.getChildren().addAll(fileName, editButton, deleteButton);
        return entry;
    }

    public void deleteFile(Note currentNote, EmbeddedFile file) {
        Alert alert = dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                bundle.getString("confirmDeletion.text"),
                bundle.getString("confirmDeletion.text"),
                bundle.getString("deleteFileConfirmation.text")
        );
        Optional<ButtonType> buttonType = alert.showAndWait();
        if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)){
            serverUtils.deleteFile(currentNote, file);
            serverUtils.send("/app/notes/" + currentNote.getId() + "/files/deleteFile", file.getId());
        }
    }

    public void renameFile(Note currentNote, EmbeddedFile file) {
        TextInputDialog dialog = dialogStyler.createStyledTextInputDialog(
                bundle.getString("renameFile.text"),
                bundle.getString("renameFile.text"),
                bundle.getString("pleaseEnterNewTitle.text")
        );
        Optional<String> fileName = dialog.showAndWait();
        if (fileName.isPresent()) {
            if (!checkFileName(currentNote, fileName.get())) {
                Alert alert = dialogStyler.createStyledAlert(
                        Alert.AlertType.INFORMATION,
                        bundle.getString("fileError.text"),
                        bundle.getString("fileError.text"),
                        bundle.getString("duplicateFile.text")
                );
                alert.showAndWait();
                return;
            }
            if (dashboardCtrl.getActionHistory() != null) {
                dashboardCtrl.getActionHistory().push(
                        new Action(ActionType.EDIT_FILE_NAME,currentNote,file.getFileName(),fileName.get(), file)
                );
            }

            // currentNote.getEmbeddedFiles().remove(file);
            renameFileInputted(file, fileName.get(), currentNote);
        }
    }
    public void renameFileInputted(EmbeddedFile file, String fileName, Note currentNote) {
        EmbeddedFile e = serverUtils.renameFile(currentNote, file, fileName);
        Object[] renameRequest = {e.getId(), fileName};
        serverUtils.send("/app/notes/" + currentNote.getId() + "/files/renameFile", renameRequest);
        persistFileName(currentNote, file.getFileName(), fileName);
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
        fileChooser.setTitle(bundle.getString("saveFile.text"));
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
                        bundle.getString("saveFileError.text"),
                        bundle.getString("saveFileError.text"),
                        bundle.getString("fileCouldNotBeSaved.text")
                );
                alert.showAndWait();
            }
        }
    }

    private void showErrorUploadingFileAlert() {
        Alert alert = dialogStyler.createStyledAlert(
                Alert.AlertType.INFORMATION,
                bundle.getString("uploadError.text"),
                bundle.getString("uploadError.text"),
                bundle.getString("errorUploadingFile.text")
        );
        alert.showAndWait();
    }

    private boolean validateUploadedFile(Note currentNote, File uploadedFile) {
        if (uploadedFile.isDirectory()) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.INFORMATION,
                    bundle.getString("fileError.text"),
                    bundle.getString("fileError.text"),
                    bundle.getString("directoriesCannotBeUploaded.text")
            );
            alert.showAndWait();
            return false;
        }

        if (!checkFileName(currentNote, uploadedFile.getName())) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.INFORMATION,
                    bundle.getString("fileError.text"),
                    bundle.getString("fileError.text"),
                    bundle.getString("fileWithNameAlreadyExists.text")
            );
            alert.showAndWait();
            return false;
        }

        if (uploadedFile.length() > 10 * 1024 * 1024 /*10MB*/) {
            Alert alert = dialogStyler.createStyledAlert(
                    Alert.AlertType.INFORMATION,
                    bundle.getString("uploadError.text"),
                    bundle.getString("uploadError.text"),
                    bundle.getString("thisFileIsTooLarge.text")
            );
            alert.showAndWait();
            return false;
        }
        return true;
    }

    private void showNoNoteSelectedAlert() {
        Alert alert = dialogStyler.createStyledAlert(
                Alert.AlertType.INFORMATION,
                bundle.getString("error.text"),
                bundle.getString("error.text"),
                bundle.getString("noNoteSelected.text")
        );
        alert.showAndWait();
    }

}
