package client.controllers;

import client.scenes.DashboardCtrl;
import client.utils.Config;
import client.utils.ServerUtils;
import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CollectionCtrlTest {
    private ServerUtils serverMock;
    private Config configMock;
    private NoteCtrl noteCtrlMock;
    private DashboardCtrl dashboardCtrlMock;
    private CollectionCtrl collectionCtrl;

    @BeforeEach
    public void setUp() {
        serverMock = mock(ServerUtils.class);
        configMock = mock(Config.class);
        noteCtrlMock = mock(NoteCtrl.class);
        dashboardCtrlMock = mock(DashboardCtrl.class);

        collectionCtrl = new CollectionCtrl(serverMock, configMock, noteCtrlMock);
        collectionCtrl.setDashboardCtrl(dashboardCtrlMock);

        when(serverMock.isServerAvailable(anyString())).thenReturn(true);
        when(noteCtrlMock.isTitleDuplicate(any(), any(), any(), anyBoolean())).thenReturn(false);
        when(noteCtrlMock.getUpdatePendingNotes()).thenReturn(new ArrayList<>());
        doNothing().when(noteCtrlMock).saveAllPendingNotes();
    }

    @Test
    public void testMoveNoteFromCollection() {
        /*
        // Arrange
        serverMock = mock(ServerUtils.class);
        configMock = mock(Config.class);
        noteCtrlMock = mock(NoteCtrl.class);
        dashboardCtrlMock = mock(DashboardCtrl.class);

        collectionCtrl = new CollectionCtrl(serverMock, configMock, noteCtrlMock);
        collectionCtrl.setDashboardCtrl(dashboardCtrlMock);

        when(serverMock.isServerAvailable(anyString())).thenReturn(true);
        when(noteCtrlMock.isTitleDuplicate(any(), any(), any(), anyBoolean())).thenReturn(false);
        when(noteCtrlMock.getUpdatePendingNotes()).thenReturn(new ArrayList<>());
        doNothing().when(noteCtrlMock).saveAllPendingNotes();

        Collection collection1 = new Collection("testCollection1", "https://localhost:8080/");
        Collection collection2 = new Collection("testCollection2", "https://localhost:8080/");
        Note note = new Note("test1", "body", collection1);
        note.setEmbeddedFiles(List.of(new EmbeddedFile(note, "file.txt", "text/plain", new byte[0])));

        // Mock the necessary interactions
        ObservableList<Note> allNotesMock = FXCollections.observableArrayList();
        ObservableList<Note> collectionNotesMock = FXCollections.observableArrayList();
        when(dashboardCtrlMock.getAllNotes()).thenReturn(allNotesMock);
        when(dashboardCtrlMock.getCollectionNotes()).thenReturn(collectionNotesMock);

        doAnswer(invocation -> {
            allNotesMock.remove(note);
            collectionNotesMock.remove(note);
            return null;
        }).when(noteCtrlMock).deleteNote(eq(note), eq(collectionNotesMock), eq(allNotesMock));

        // Act
        collectionCtrl.moveNote(note, collection2);

        // Assert
        // Verify the note was moved to the new collection
        assertEquals(collection2, note.collection);

        // Verify the note is added to the dashboard
        assertEquals(1, allNotesMock.size());
        assertEquals(note, allNotesMock.get(0));

        // Verify the embedded file was restored
        assertEquals(1, note.getEmbeddedFiles().size());
        assertEquals("file.txt", note.getEmbeddedFiles().get(0).getFileName());

        // Verify server interaction for sending the note
        verify(serverMock).send(eq("/app/notes"), eq(note), eq("https://localhost:8080/"));

        // Verify UI updates
        verify(dashboardCtrlMock).selectNoteInTreeView(note);

         */
    }

}