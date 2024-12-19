package client.utils;

import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServerUtilsTest {

    private ServerUtils serverUtils;
    private Client clientMock;
    private WebTarget targetMock;
    private Invocation.Builder builderMock;
    private Response responseMock;
    private MockedStatic<ClientBuilder> clientBuilderMockStatic;

    @BeforeEach
    void setUp() {
        serverUtils = new ServerUtils();
        clientMock = mock(Client.class);
        targetMock = mock(WebTarget.class);
        builderMock = mock(Invocation.Builder.class);
        responseMock = mock(Response.class);

        clientBuilderMockStatic = Mockito.mockStatic(ClientBuilder.class);
        when(ClientBuilder.newClient(any())).thenReturn(clientMock);
        when(clientMock.target(anyString())).thenReturn(targetMock);
        when(targetMock.path(anyString())).thenReturn(targetMock);
        when(targetMock.queryParam(anyString(), any())).thenReturn(targetMock);
        when(targetMock.request(anyString())).thenReturn(builderMock);
    }

    @AfterEach
    void tearDown() {
        clientBuilderMockStatic.close(); // Ensure static mocking is deregistered
    }

    @Test
    void addNote() {
        Collection collection = new Collection("Collection Title");
        Note note = new Note("Note Title", "Note Body", collection);

        when(builderMock.post(any(), eq(Note.class))).thenReturn(note);

        Note result = serverUtils.addNote(note);
        assertNotNull(result);
        assertEquals("Note Title", result.title);
        verify(builderMock).post(any(), eq(Note.class));
    }

    @Test
    void getAllNotes() {
        Collection collection = new Collection("Collection Title");
        Note note1 = new Note("Note Title 1", "Note Body 1", collection);
        Note note2 = new Note("Note Title 2", "Note Body 2", collection);
        List<Note> notes = Arrays.asList(note1, note2);

        when(builderMock.get(any(GenericType.class))).thenReturn(notes);

        List<Note> result = serverUtils.getAllNotes();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Note Title 1", result.get(0).title);
        verify(builderMock).get(any(GenericType.class));
    }

    @Test
    void getNoteById() {
        Collection collection = new Collection("Collection Title");
        Note note = new Note("Note Title", "Note Body", collection);

        when(builderMock.get(eq(Note.class))).thenReturn(note);

        Note result = serverUtils.getNoteById(1L);
        assertNotNull(result);
        assertEquals("Note Title", result.title);
        verify(builderMock).get(eq(Note.class));
    }

    @Test
    void updateNote() {
        Collection collection = new Collection("Collection Title");
        Note note = new Note("Updated Title", "Updated Body", collection);

        when(builderMock.put(any(), eq(Note.class))).thenReturn(note);

        Note result = serverUtils.updateNote(note);
        assertNotNull(result);
        assertEquals("Updated Title", result.title);
        verify(builderMock).put(any(), eq(Note.class));
    }

    @Test
    void deleteNote() {
        serverUtils.deleteNote(1L);

        verify(builderMock).delete();
    }

    @Test
    void getNotesByCollection() {
        Collection collection = new Collection("Collection Title");
        Note note1 = new Note("Note Title 1", "Note Body 1", collection);
        Note note2 = new Note("Note Title 2", "Note Body 2", collection);
        List<Note> notes = Arrays.asList(note1, note2);

        // Update mocks to match the expected chain
        when(targetMock.path("/api/collection/{title}")).thenReturn(targetMock);
        when(targetMock.resolveTemplate("title", "Collection Title")).thenReturn(targetMock);
        when(builderMock.get(any(GenericType.class))).thenReturn(notes);

        List<Note> result = serverUtils.getNotesByCollection(collection);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Note Title 1", result.get(0).title);
        verify(builderMock).get(any(GenericType.class));
    }

    @Test
    void addCollection() {
        Collection collection = new Collection("Collection Title");

        when(builderMock.post(any(), eq(Collection.class))).thenReturn(collection);

        Collection result = serverUtils.addCollection(collection);
        assertNotNull(result);
        assertEquals("Collection Title", result.title);
        verify(builderMock).post(any(), eq(Collection.class));
    }

    @Test
    void getCollections() {
        Collection collection1 = new Collection("Collection Title 1");
        Collection collection2 = new Collection("Collection Title 2");
        List<Collection> collections = Arrays.asList(collection1, collection2);

        when(builderMock.get(any(GenericType.class))).thenReturn(collections);

        List<Collection> result = serverUtils.getCollections();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Collection Title 1", result.get(0).title);
        verify(builderMock).get(any(GenericType.class));
    }

    @Test
    void updateCollection() {
        Collection collection = new Collection("Updated Collection Title");

        when(builderMock.put(any(), eq(Collection.class))).thenReturn(collection);

        Collection result = serverUtils.updateCollection(collection);
        assertNotNull(result);
        assertEquals("Updated Collection Title", result.title);
        verify(builderMock).put(any(), eq(Collection.class));
    }

    @Test
    void deleteCollection() {
        serverUtils.deleteCollection(1L);

        verify(builderMock).delete();
    }

    @Test
    void addFile() throws IOException {
        Note note = new Note("Note", "Body", new Collection("Collection"));
        File file = mock(File.class);
        when(file.getName()).thenReturn("file.txt");
        when(file.exists()).thenReturn(true);
        when(file.length()).thenReturn(1024L);

        EmbeddedFile expected = new EmbeddedFile(note, "file.txt", "text/plain", new byte[0]);
        when(builderMock.post(any(), eq(EmbeddedFile.class))).thenReturn(expected);
        EmbeddedFile result = serverUtils.addFile(note, file);

        assertNotNull(result);
        assertEquals(expected.getFileName(), result.getFileName());
        assertEquals(expected.getFileType(), result.getFileType());
        verify(builderMock).post(any(), eq(EmbeddedFile.class));
    }

    @Test
    void deleteFile() {
        Note note = new Note("Note", "Body", new Collection("Collection"));
        EmbeddedFile file = new EmbeddedFile(note, "file.txt", "text/plain", new byte[0]);

        serverUtils.deleteFile(note, file);

        verify(builderMock).delete();
    }

    @Test
    void renameFile() {
        Note note = new Note("Note", "Body", new Collection("Collection"));
        EmbeddedFile file = new EmbeddedFile(note, "file.txt", "text/plain", new byte[0]);

        EmbeddedFile expected = new EmbeddedFile(note, "renamedFile.txt", "text/plain", new byte[0]);
        when(builderMock.put(any(), eq(EmbeddedFile.class))).thenReturn(expected);

        EmbeddedFile result = serverUtils.renameFile(note, file, "renamedFile.txt");

        assertNotNull(result);
        assertEquals(expected, result);
        verify(builderMock).put(any(), eq(EmbeddedFile.class));
    }

    @Test
    void testIsServerAvailable_ServerAvailable() {
        when(builderMock.get()).thenReturn(responseMock);
        when(responseMock.getStatus()).thenReturn(200);

        boolean result = serverUtils.isServerAvailable();
        assertTrue(result);
        verify(builderMock).get();
    }

    @Test
    void testIsServerAvailable_ServerUnavailable() {
        when(builderMock.get()).thenThrow(new jakarta.ws.rs.ProcessingException(new ConnectException()));

        boolean result = serverUtils.isServerAvailable();
        assertFalse(result);
        verify(builderMock).get();
    }
}