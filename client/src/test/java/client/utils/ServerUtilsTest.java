package client.utils;

import client.ui.DialogStyler;
import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import javafx.scene.control.Alert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServerUtilsTest {

//    @Mock
//    private Config mockConfig;
//    @Mock
//    private DialogStyler mockDialogStyler;
//    private ServerUtils serverUtils;
//
//    @Mock
//    private Client clientMock;
//    @Mock
//    private WebTarget targetMock;
//    @Mock
//    private Invocation.Builder builderMock;
//    @Mock
//    private Response responseMock;
//
//    private MockedStatic<ClientBuilder> clientBuilderMockStatic;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//        serverUtils = new ServerUtils(mockConfig, mockDialogStyler);
//
//        clientBuilderMockStatic = Mockito.mockStatic(ClientBuilder.class);
//        when(ClientBuilder.newClient(any())).thenReturn(clientMock);
//        when(clientMock.target(anyString())).thenReturn(targetMock);
//        when(targetMock.path(anyString())).thenReturn(targetMock);
//        when(targetMock.queryParam(anyString(), any())).thenReturn(targetMock);
//        when(targetMock.request(anyString())).thenReturn(builderMock);
//    }
//
//    @AfterEach
//    void tearDown() {
//        clientBuilderMockStatic.close();
//    }
//
//    @Test
//    void addNote() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        Note note = new Note("Note Title", "Note Body", collection);
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("api/notes")).thenReturn(targetMock);
//        when(builderMock.post(any(), eq(Note.class))).thenReturn(note);
//
//        Note result = serverUtils.addNote(note);
//
//        assertNotNull(result);
//        assertEquals("Note Title", result.title);
//        verify(builderMock).post(any(), eq(Note.class));
//    }
//
//    @Test
//    void getAllNotes() {
//        Collection collection1 = new Collection("Collection Title 1", "http://mock-server-1.com");
//        Collection collection2 = new Collection("Collection Title 2", "http://mock-server-2.com");
//        Note note1 = new Note("Note Title 1", "Note Body 1", collection1);
//        Note note2 = new Note("Note Title 2", "Note Body 2", collection2);
//        List<Collection> collections = Arrays.asList(collection1, collection2);
//        List<Note> notes1 = List.of(note1);
//        List<Note> notes2 = List.of(note2);
//
//        when(mockConfig.readFromFile()).thenReturn(collections);
//
//        // mock first server chain
//        WebTarget targetMock1 = mock(WebTarget.class);
//        WebTarget pathMock1 = mock(WebTarget.class);
//        WebTarget resolvedMock1 = mock(WebTarget.class);
//        Invocation.Builder builderMock1 = mock(Invocation.Builder.class);
//
//        when(clientMock.target("http://mock-server-1.com")).thenReturn(targetMock1);
//        when(targetMock1.path("/api/collection/{title}")).thenReturn(pathMock1);
//        when(pathMock1.resolveTemplate("title", "Collection Title 1")).thenReturn(resolvedMock1);
//        when(resolvedMock1.request(APPLICATION_JSON)).thenReturn(builderMock1);
//        when(builderMock1.get(any(GenericType.class))).thenReturn(notes1);
//
//        // mock second server chain
//        WebTarget targetMock2 = mock(WebTarget.class);
//        WebTarget pathMock2 = mock(WebTarget.class);
//        WebTarget resolvedMock2 = mock(WebTarget.class);
//        Invocation.Builder builderMock2 = mock(Invocation.Builder.class);
//
//        when(clientMock.target("http://mock-server-2.com")).thenReturn(targetMock2);
//        when(targetMock2.path("/api/collection/{title}")).thenReturn(pathMock2);
//        when(pathMock2.resolveTemplate("title", "Collection Title 2")).thenReturn(resolvedMock2);
//        when(resolvedMock2.request(APPLICATION_JSON)).thenReturn(builderMock2);
//        when(builderMock2.get(any(GenericType.class))).thenReturn(notes2);
//
//        // is server available to return true
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        List<Note> result = spyServerUtils.getAllNotes();
//
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        assertEquals("Note Title 1", result.get(0).title);
//        assertEquals("Note Title 2", result.get(1).title);
//    }
//
//    @Test
//    void updateNote() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        Note note = new Note("Updated Title", "Updated Body", collection);
//        note.id = 1L;
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("api/notes/" + note.id)).thenReturn(targetMock);
//        when(builderMock.put(any(), eq(Note.class))).thenReturn(note);
//
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        Note result = spyServerUtils.updateNote(note);
//        assertNotNull(result);
//        assertEquals("Updated Title", result.title);
//        verify(builderMock).put(any(), eq(Note.class));
//    }
//
//    @Test
//    void deleteNote() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        Note note = new Note("Note Title", "Note Body", collection);
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("api/notes/" + note.id)).thenReturn(targetMock);
//
//        serverUtils.deleteNote(note);
//
//        verify(builderMock).delete();
//    }
//
//    @Test
//    void getNotesByCollection() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        Note note1 = new Note("Note Title 1", "Note Body 1", collection);
//        Note note2 = new Note("Note Title 2", "Note Body 2", collection);
//        List<Note> notes = Arrays.asList(note1, note2);
//
//        WebTarget pathMock = mock(WebTarget.class);
//        WebTarget resolvedMock = mock(WebTarget.class);
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("/api/collection/{title}")).thenReturn(pathMock);
//        when(pathMock.resolveTemplate("title", "Collection Title")).thenReturn(resolvedMock);
//        when(resolvedMock.request(APPLICATION_JSON)).thenReturn(builderMock);
//        when(builderMock.get(any(GenericType.class))).thenReturn(notes);
//
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        List<Note> result = spyServerUtils.getNotesByCollection(collection);
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        assertEquals("Note Title 1", result.get(0).title);
//        verify(builderMock).get(any(GenericType.class));
//    }
//
//    @Test
//    void addCollection() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("/api/collection")).thenReturn(targetMock);
//        when(builderMock.post(any(), eq(Collection.class))).thenReturn(collection);
//
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        Collection result = spyServerUtils.addCollection(collection);
//        assertNotNull(result);
//        assertEquals("Collection Title", result.title);
//        verify(builderMock).post(any(), eq(Collection.class));
//    }
//
//    @Test
//    void updateCollection() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        collection.id = 1L;
//        Collection updatedCollection = new Collection("Updated Collection Title", "http://mock-server.com");
//        updatedCollection.id = 1L;
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("/api/collection/" + collection.id)).thenReturn(targetMock);
//        when(builderMock.put(any(), eq(Collection.class))).thenReturn(updatedCollection);
//
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        Collection result = spyServerUtils.updateCollection(collection);
//
//        assertNotNull(result);
//        assertEquals("Updated Collection Title", result.title);
//        assertEquals("http://mock-server.com", result.serverURL);
//        assertEquals(1L, result.id);
//        verify(builderMock).put(any(), eq(Collection.class));
//    }
//
//    @Test
//    void deleteCollection() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        collection.id = 1L;
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("/api/collection/" + collection.id)).thenReturn(targetMock);
//
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        spyServerUtils.deleteCollection(collection);
//
//        verify(builderMock).delete();
//    }
//
//    @Test
//    void addFile() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        Note note = new Note("Note", "Body", collection);
//        note.id = 1L;
//
//        File file = mock(File.class);
//        when(file.getName()).thenReturn("file.txt");
//        when(file.exists()).thenReturn(true);
//        when(file.length()).thenReturn(1024L);
//
//        EmbeddedFile expected = new EmbeddedFile(note, "file.txt", "text/plain", new byte[0]);
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("/api/notes/" + note.id + "/files")).thenReturn(targetMock);
//        when(builderMock.post(any(), eq(EmbeddedFile.class))).thenReturn(expected);
//
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        EmbeddedFile result = spyServerUtils.addFile(note, file);
//
//        assertNotNull(result);
//        assertEquals(expected.getFileName(), result.getFileName());
//        assertEquals(expected.getFileType(), result.getFileType());
//        verify(builderMock).post(any(), eq(EmbeddedFile.class));
//    }
//
//    @Test
//    void deleteFile() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        Note note = new Note("Note", "Body", collection);
//        note.id = 1L;
//        EmbeddedFile file = new EmbeddedFile(note, "file.txt", "text/plain", new byte[0]);
//        file.setId(1L);
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("api/notes/" + note.id + "/files/" + file.getId())).thenReturn(targetMock);
//
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        spyServerUtils.deleteFile(note, file);
//
//        verify(builderMock).delete();
//    }
//
//    @Test
//    void renameFile() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        Note note = new Note("Note", "Body", collection);
//        note.id = 1L;
//        EmbeddedFile file = new EmbeddedFile(note, "file.txt", "text/plain", new byte[0]);
//        file.setId(1L);
//
//        EmbeddedFile expected = new EmbeddedFile(note, "renamedFile.txt", "text/plain", new byte[0]);
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("api/notes/" + note.id + "/files/" + file.getId() + "/rename")).thenReturn(targetMock);
//        when(builderMock.put(any(), eq(EmbeddedFile.class))).thenReturn(expected);
//
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        EmbeddedFile result = spyServerUtils.renameFile(note, file, "renamedFile.txt");
//
//        assertNotNull(result);
//        assertEquals(expected.getFileName(), "renamedFile.txt");
//        verify(builderMock).put(any(), eq(EmbeddedFile.class));
//    }
//
//    @Test
//    void getFilesByNote() {
//        Collection collection = new Collection("Collection Title", "http://mock-server.com");
//        Note note = new Note("Note Title", "Note Body", collection);
//        note.id = 1L;
//
//        EmbeddedFile file1 = new EmbeddedFile(note, "file1.txt", "text/plain", new byte[0]);
//        EmbeddedFile file2 = new EmbeddedFile(note, "file2.txt", "text/plain", new byte[0]);
//        List<EmbeddedFile> expectedFiles = Arrays.asList(file1, file2);
//
//        when(clientMock.target("http://mock-server.com")).thenReturn(targetMock);
//        when(targetMock.path("/api/notes/" + note.id + "/files")).thenReturn(targetMock);
//        when(builderMock.get(any(GenericType.class))).thenReturn(expectedFiles);
//
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(true).when(spyServerUtils).isServerAvailableWithAlert(anyString());
//
//        List<EmbeddedFile> result = spyServerUtils.getFilesByNote(note);
//
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        assertEquals("file1.txt", result.get(0).getFileName());
//        assertEquals("file2.txt", result.get(1).getFileName());
//        verify(builderMock).get(any(GenericType.class));
//    }
//
//    @Test
//    void isServerAvailable_Success() {
//        when(clientMock.target("http://test.com")).thenReturn(targetMock);
//        when(targetMock.request(APPLICATION_JSON)).thenReturn(builderMock);
//        when(builderMock.get()).thenReturn(responseMock);
//
//        assertTrue(serverUtils.isServerAvailable("http://test.com"));
//    }
//
//    @Test
//    void isServerAvailable_ConnectionFails() {
//        when(clientMock.target("http://test.com")).thenReturn(targetMock);
//        when(targetMock.request(APPLICATION_JSON)).thenReturn(builderMock);
//        when(builderMock.get()).thenThrow(new ProcessingException(new ConnectException()));
//
//        assertFalse(serverUtils.isServerAvailable("http://test.com"));
//    }
//
//    @Test
//    void isServerAvailableWithAlert_ShowsDialogWhenUnavailable() {
//        Alert mockAlert = mock(Alert.class);
//        ServerUtils spyServerUtils = spy(serverUtils);
//        doReturn(false).when(spyServerUtils).isServerAvailable(anyString());
//        when(mockDialogStyler.createStyledAlert(any(), anyString(), anyString(), anyString()))
//                .thenReturn(mockAlert);
//
//        assertFalse(spyServerUtils.isServerAvailableWithAlert("http://test.com"));
//        verify(mockDialogStyler).createStyledAlert(
//                eq(Alert.AlertType.ERROR),
//                anyString(),
//                anyString(),
//                anyString()
//        );
//    }
}