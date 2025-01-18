package client.controllers;

import client.scenes.DashboardCtrl;
import client.ui.DialogStyler;
import client.utils.Config;
import client.utils.ServerUtils;
import commons.Collection;
import commons.Note;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionCtrlTest {

    @Mock
    private ServerUtils serverMock;

    @Mock
    private Config configMock;

    @Mock
    private NoteCtrl noteCtrlMock;

    @Mock
    private SearchCtrl searchCtrlMock;

    @Mock
    private DashboardCtrl dashboardCtrlMock;

    @Mock
    private DialogStyler dialogStylerMock;

    @InjectMocks
    private CollectionCtrl collectionCtrl;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        collectionCtrl = new CollectionCtrl(serverMock, configMock, noteCtrlMock, searchCtrlMock);
        collectionCtrl.setDashboardCtrl(dashboardCtrlMock);
        noteCtrlMock.setUpdatePendingNotes(new ArrayList<>());
    }

    @Test
    public void testViewNotes_WithCurrentCollection() {

    }

    @Test
    public void testViewNotes_NoCurrentCollection() {

    }

    @Test
    public void testDeleteCollection_Success() {

    }

    @Test
    public void testDeleteCollection_Cancellation() {

    }

    @Test
    public void testMoveNoteFromCollection() {
        Collection collection1 = new Collection("testCollection1", "https://localhost:8080/");
        Collection collection2 = new Collection("testCollection2", "https://localhost:8080/");
        Note note = new Note("test1", "body", collection1);
        assertEquals(note.collection, collection1);
        collectionCtrl.moveNote(note, collection2);
        assertEquals(note.collection, collection2);
    }
}