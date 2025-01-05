package client.controllers;

import client.scenes.DashboardCtrl;
import client.ui.DialogStyler;
import client.utils.ServerUtils;
import commons.EmbeddedFile;
import commons.Note;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FilesCtrlTest {

    private FilesCtrl filesCtrl;
    private ServerUtils serverUtils;
    private DashboardCtrl dashboardCtrl;
    private DialogStyler dialogStyler;
    private HBox filesView;

    private Note sampleNote;
    private EmbeddedFile sampleFile;

    @BeforeEach
    void setup() {
        serverUtils = mock(ServerUtils.class);
        dashboardCtrl = mock(DashboardCtrl.class);
        dialogStyler = mock(DialogStyler.class);
        filesView = new HBox();

        filesCtrl = new FilesCtrl(serverUtils, new FileChooser());
        filesCtrl.setDashboardCtrl(dashboardCtrl);
        filesCtrl.setReferences(filesView);
        filesCtrl.setDialogStyler(dialogStyler);

        sampleNote = new Note("Sample Note", "This is a test note.", null);
        sampleFile = new EmbeddedFile(sampleNote, "test.txt", "text/plain", new byte[]{});
        sampleNote.getEmbeddedFiles().add(sampleFile);
    }

    @Test
    void checkFileNameTrue() {
        when(serverUtils.getFilesByNote(sampleNote)).thenReturn(List.of());

        assertTrue(filesCtrl.checkFileName(sampleNote, "newfile.txt"));
    }

    @Test
    void checkFileNameFalse() {
        when(serverUtils.getFilesByNote(sampleNote)).thenReturn(List.of(sampleFile));

        assertFalse(filesCtrl.checkFileName(sampleNote, "test.txt"));
    }

    @Test
    void addFile() throws IOException {
        FileChooser mockFileChooser = mock(FileChooser.class);
        File mockFile = mock(File.class);

        when(mockFileChooser.showOpenDialog(any())).thenReturn(mockFile); // Simulate file selection
        when(mockFile.getName()).thenReturn("valid.txt");
        when(mockFile.isDirectory()).thenReturn(false);

        EmbeddedFile embeddedFile = new EmbeddedFile(sampleNote, "valid.txt", "text/plain", new byte[]{});
        when(serverUtils.addFile(eq(sampleNote), eq(mockFile))).thenReturn(embeddedFile);

        filesCtrl.setFileChooser(mockFileChooser);

        EmbeddedFile result = filesCtrl.addFile(sampleNote);

        assertNotNull(result);
        assertEquals("valid.txt", result.getFileName());
        assertTrue(sampleNote.getEmbeddedFiles().contains(result));

        verify(serverUtils, times(1)).addFile(eq(sampleNote), eq(mockFile));
    }

    @Test
    void addFileNullNote() throws IOException {
        Alert mockAlert = mock(Alert.class);
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);

        EmbeddedFile result = filesCtrl.addFile(null);

        assertNull(result);
        verify(mockAlert, times(1)).showAndWait();
    }

    @Test
    void addFileDirectory() throws IOException {
        Alert mockAlert = mock(Alert.class);
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);

        FileChooser mockFileChooser = mock(FileChooser.class);
        File mockFile = mock(File.class);

        when(mockFileChooser.showOpenDialog(any())).thenReturn(mockFile);
        when(mockFile.getName()).thenReturn("valid.txt");
        when(mockFile.isDirectory()).thenReturn(true);

        filesCtrl.setFileChooser(mockFileChooser);

        EmbeddedFile result = filesCtrl.addFile(sampleNote);

        assertNull(result);
        verify(mockAlert, times(1)).showAndWait();
    }

    @Test
    void showFilesNullNote() {
        filesView = mock(HBox.class);
        filesCtrl.setReferences(filesView);
        filesCtrl.showFiles(null);
        assertNull(filesView.getChildren());
    }

    @Test
    void showFiles_ShouldAddEntries_WhenNoteHasFiles() {
        FilesCtrl filesCtrlSpy = spy(filesCtrl);
        doReturn(new HBox()).when(filesCtrlSpy).createFileEntry(any(Note.class), any(EmbeddedFile.class));

        // Mock HBox and its children list
        filesView = mock(HBox.class);
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(filesView.getChildren()).thenReturn(mockChildren);  // mock getChildren method

        // Inject the mock filesView into FilesCtrl
        filesCtrlSpy.setReferences(filesView);

        // Mock serverUtils to return files
        when(serverUtils.getFilesByNote(sampleNote)).thenReturn(List.of(sampleFile));

        // Mock createFileEntry to bypass actual implementation
        doReturn(new HBox()).when(filesCtrlSpy).createFileEntry(sampleNote, sampleFile);

        // Call the method under test
        filesCtrlSpy.showFiles(sampleNote);

        // Verify that the children list of filesView is modified correctly
        verify(filesView, times(3)).getChildren();  // Verify getChildren() is called
        verify(filesView.getChildren(), times(1)).clear();  // Verify clear() is called
        verify(filesView.getChildren(), times(2)).add(any());  // Verify add() is called
    }


    @Test
    void deleteFileCancelled() {
        Alert mockAlert = mock(Alert.class);
        when(mockAlert.showAndWait()).thenReturn(Optional.of(ButtonType.CANCEL));
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);

        filesCtrl.deleteFile(sampleNote, sampleFile);

        verify(serverUtils, never()).deleteFile(any(), any());
    }

    @Test
    void deleteFile() {
        Alert mockAlert = mock(Alert.class);
        when(mockAlert.showAndWait()).thenReturn(Optional.of(ButtonType.OK));
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);

        filesCtrl.deleteFile(sampleNote, sampleFile);

        assertFalse(sampleNote.getEmbeddedFiles().contains(sampleFile));
        verify(serverUtils, times(1)).deleteFile(eq(sampleNote), eq(sampleFile));
    }

    @Test
    void renameFileExistingFilename() {
        Alert mockAlert = mock(Alert.class);
        when(dialogStyler.createStyledAlert(any(), any(), any(), any())).thenReturn(mockAlert);
        TextInputDialog mockDialog = mock(TextInputDialog.class);
        when(mockDialog.showAndWait()).thenReturn(Optional.of("test.txt"));
        when(dialogStyler.createStyledTextInputDialog(any(), any(), any())).thenReturn(mockDialog);
        when(serverUtils.getFilesByNote(sampleNote)).thenReturn(List.of(sampleFile));

        filesCtrl.renameFile(sampleNote, sampleFile);

        verify(dialogStyler, times(1)).createStyledAlert(
                eq(Alert.AlertType.INFORMATION), any(), any(), eq("A file with this name already exists!")
        );
    }

    @Test
    void renameFile() {
        TextInputDialog mockDialog = mock(TextInputDialog.class);
        when(mockDialog.showAndWait()).thenReturn(Optional.of("newfile.txt"));
        when(dialogStyler.createStyledTextInputDialog(any(), any(), any())).thenReturn(mockDialog);

        EmbeddedFile renamedFile = new EmbeddedFile(sampleNote, "newfile.txt", "text/plain", new byte[]{});
        when(serverUtils.renameFile(eq(sampleNote), eq(sampleFile), eq("newfile.txt"))).thenReturn(renamedFile);

        FilesCtrl filesCtrlSpy = spy(filesCtrl);
        doNothing().when(filesCtrlSpy).persistFileName(any(), any(), any());

        filesCtrlSpy.renameFile(sampleNote, sampleFile);

        assertFalse(
                sampleNote.getEmbeddedFiles()
                        .stream().filter(e -> e.getFileName().equals("newfile.txt"))
                        .toList().isEmpty()
        );
        assertTrue(
                sampleNote.getEmbeddedFiles()
                        .stream().filter(e -> e.getFileName().equals(sampleFile.getFileName()))
                        .toList().isEmpty()
        );
    }

    @Test
    void persistFileName() {
        sampleNote.setBody("![File](test.txt)");

        NoteCtrl noteCtrlMock = mock(NoteCtrl.class);
        when(dashboardCtrl.getNoteCtrl()).thenReturn(noteCtrlMock);
        doNothing().when(noteCtrlMock).showCurrentNote(any());

        filesCtrl.persistFileName(sampleNote, "test.txt", "newfile.txt");

        assertEquals("![File](newfile.txt)", sampleNote.getBody());
    }

    @Test
    void notPersistFileName() {
        sampleNote.setBody("![File](otherfile.txt)");

        NoteCtrl noteCtrlMock = mock(NoteCtrl.class);
        when(dashboardCtrl.getNoteCtrl()).thenReturn(noteCtrlMock);
        doNothing().when(noteCtrlMock).showCurrentNote(any());

        filesCtrl.persistFileName(sampleNote, "test.txt", "newfile.txt");

        assertEquals("![File](otherfile.txt)", sampleNote.getBody());
    }

}
