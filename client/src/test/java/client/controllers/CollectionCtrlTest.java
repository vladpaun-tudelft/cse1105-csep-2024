package client.controllers;

import client.scenes.DashboardCtrl;
import client.utils.Config;
import client.utils.ServerUtils;
import commons.Collection;
import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(note.collection, collection1);
        collectionCtrl.moveNote(note, collection2);
        assertEquals(note.collection, collection2);
    }
}