package server.api;

import commons.Collection;
import commons.EmbeddedFile;
import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import server.service.CollectionService;
import server.service.EmbeddedFileService;
import server.service.NoteService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class NoteControllerTest {

    private NoteController noteController;
    private CollectionController collectionController;

    private NoteService noteService;
    private CollectionService collectionService;
    private EmbeddedFileService embeddedFileService;

    private TestNoteRepository noteRepo;
    private TestCollectionRepository collectionRepo;
    private TestEmbeddedFileRepository embeddedFileRepository;

    Collection collection1, collection2, collection3, collection4;
    Note note1, note2, note3, note4, note5, note6, note7, note8;
    EmbeddedFile embeddedFile;

    @BeforeEach
    void setUp() {
        noteRepo = new TestNoteRepository();
        collectionRepo = new TestCollectionRepository();
        embeddedFileRepository = new TestEmbeddedFileRepository();

        noteService = new NoteService(noteRepo);
        collectionService = new CollectionService(collectionRepo);
        embeddedFileService = new EmbeddedFileService(embeddedFileRepository);

        noteController = new NoteController(noteService, collectionService, embeddedFileService, noteRepo);
        collectionController = new CollectionController(noteService, collectionService);


        collection1 = new Collection("collection1", "http://localhost:8080/");
        collection2 = new Collection("collection2", "http://localhost:8080/");
        collection3 = new Collection("collection3", "http://localhost:8080/");
        collection4 = new Collection("collection4", "http://localhost:8080/");

        note1 = new Note("note1", "bla", collection1);
        note2 = new Note("note2", "bla", collection1);
        note3 = new Note("note3", "bla", collection2);
        note4 = new Note("note4", "bla", collection2);
        note5 = new Note("note5", "bla", collection3);
        note6 = new Note("note6", "bla", collection3);
        note7 = new Note("note7", "bla", collection4);
        note8 = new Note("note8", "bla", collection4);

        embeddedFile = new EmbeddedFile(note1, "file.txt", "text/plain", new byte[]{1, 2, 3, 4});
        embeddedFile.setId(UUID.randomUUID());
    }

    @Test
    public void createNoteTest() {
        collectionController.createCollection(collection1);
        var actual = noteController.createNote(note1);
        assertNotNull(actual);
        assertEquals(ResponseEntity.ok(note1), actual);
    }

    @Test
    public void cannotAddNullNote() {
        var actual = noteController.createNote(null);
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    public void cannotAddNoteWithNullCollection() {
        var actual = noteController.createNote(new Note("test", "bla", null));
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    public void cannotAddNoteWithUnknownCollection() {
        var actual = noteController.createNote(new Note("test", "bla", collection1));
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    public void cannotAddNoteWithEmptyTitle() {
        collectionController.createCollection(collection1);
        var actual = noteController.createNote(new Note("", "bla", collection1));
        assertEquals(BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    public void getNoteByIdTest() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;
        UUID id2 = noteController.createNote(note2).getBody().id;

        var actual1 = noteController.getNoteById(id1);
        var actual2 = noteController.getNoteById(id2);

        assertEquals(ResponseEntity.ok(note1), actual1);
        assertEquals(ResponseEntity.ok(note2), actual2);
    }

    @Test
    public void getNoteByIdNotFoundTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        var actual = noteController.getNoteById(UUID.randomUUID());

        assertEquals(ResponseEntity.notFound().build(), actual);
    }

    @Test
    public void deleteNoteTest() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        var response = noteController.deleteNote(id1);

        assertEquals(ResponseEntity.noContent().build(), response);

        var actual = noteController.getNoteById(id1);
        assertEquals(ResponseEntity.notFound().build(), actual);

    }

    @Test
    public void updateNoteTest() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        var response = noteController.updateNote(id1, note2);
        assertEquals(ResponseEntity.ok(note2), response);

        var actual = noteController.getNoteById(id1);
        assertEquals(ResponseEntity.ok(note2), actual);
    }

    @Test
    public void updateNoteNotFoundTest() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        var actual = noteController.updateNote(UUID.randomUUID(), note3);
        assertEquals(ResponseEntity.notFound().build(), actual);

        var actual2 = noteController.getNoteById(id1);
        assertEquals(ResponseEntity.ok(note1), actual2);
    }


    @Test
    void getAllNotesTest() {
        collectionController.createCollection(collection1);
        collectionController.createCollection(collection2);
        collectionController.createCollection(collection3);
        collectionController.createCollection(collection4);

        noteController.createNote(note1);
        noteController.createNote(note2);
        noteController.createNote(note3);
        noteController.createNote(note4);
        noteController.createNote(note5);
        noteController.createNote(note6);
        noteController.createNote(note7);
        noteController.createNote(note8);

        var response = noteController.getAllNotes();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(8, response.getBody().size());

        assertEquals("note1", response.getBody().get(0).title);
        assertEquals("note2", response.getBody().get(1).title);
        assertEquals("note3", response.getBody().get(2).title);
        assertEquals("note4", response.getBody().get(3).title);
        assertEquals("note5", response.getBody().get(4).title);
        assertEquals("note6", response.getBody().get(5).title);
        assertEquals("note7", response.getBody().get(6).title);
        assertEquals("note8", response.getBody().get(7).title);

        assertEquals(collection1, response.getBody().get(0).collection);
        assertEquals(collection1, response.getBody().get(1).collection);
        assertEquals(collection2, response.getBody().get(2).collection);
        assertEquals(collection2, response.getBody().get(3).collection);
        assertEquals(collection3, response.getBody().get(4).collection);
        assertEquals(collection3, response.getBody().get(5).collection);
        assertEquals(collection4, response.getBody().get(6).collection);
        assertEquals(collection4, response.getBody().get(7).collection);
    }

    @Test
    public void addMessage() {
        collectionController.createCollection(collection1);

        Note result = noteController.addMessage(note1);

        assertNotNull(result);
        assertEquals(note1, result);
    }

    @Test
    public void updateBody() {
        collectionController.createCollection(collection1);

        Note result = noteController.updateBody(note1);

        assertNotNull(result);
        assertEquals(note1, result);
    }

    @Test
    public void updateTitle() {
        collectionController.createCollection(collection1);

        Note result = noteController.updateTitle(note1);

        assertNotNull(result);
        assertEquals(note1, result);
    }

    @Test
    public void deleteNoteHandler() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        Note result = noteController.deleteNoteHandler(note1);

        assertNotNull(result);
        assertEquals(note1, result);
    }

    @Test
    public void sendEmbeddedFileUpdate() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        EmbeddedFile file = (EmbeddedFile) noteController.uploadFile(id1, mockFile).getBody();
        file.setId(UUID.randomUUID());

        UUID result = noteController.sendEmbeddedFileUpdate(note1.id, file.getId());

        assertEquals(file.getId(), result);
    }

    @Test
    public void sendMessageAfterDelete() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        EmbeddedFile file = (EmbeddedFile) noteController.uploadFile(id1, mockFile).getBody();
        file.setId(UUID.randomUUID());

        UUID result = noteController.sendMessageAfterDelete(note1.id, file.getId());

        assertEquals(file.getId(), result);
    }

    @Test
    public void sendMessageAfterRename() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        EmbeddedFile file = (EmbeddedFile) noteController.uploadFile(id1, mockFile).getBody();
        file.setId(UUID.randomUUID());

        String newFileName = "newName.txt";

        Object[] result = noteController.sendMessageAfterRename(note1.id, new Object[]{file.getId(), newFileName});
        UUID returnedId = (UUID) result[0];
        String returnedFileName = (String) result[1];

        assertEquals(file.getId(), returnedId);
        assertEquals(newFileName, returnedFileName);
    }

    @Test
    public void getFilesTest() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        noteController.uploadFile(id1, mockFile);

        var response = noteController.getFiles(id1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        EmbeddedFile retrievedFile = response.getBody().get(0);
        assertEquals("test.txt", retrievedFile.getFileName());
        assertEquals("text/plain", retrievedFile.getFileType());
    }

    @Test
    public void getFilesForNonExistentNoteTest() {
        var response = noteController.getFiles(UUID.randomUUID());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void getFileById() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        var uploadResponse = noteController.uploadFile(id1, mockFile);
        EmbeddedFile expected = (EmbeddedFile) uploadResponse.getBody();
        expected.setId(UUID.randomUUID());

        var response = noteController.getFileById(note1.id, expected.getId());
        EmbeddedFile actual = response.getBody();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expected, actual);
        assertEquals(expected.getFileName(), actual.getFileName());
        assertEquals(expected.getFileType(), actual.getFileType());
        assertEquals(expected.getFileContent(), actual.getFileContent());
    }

    @Test
    public void uploadFileTest() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        var response = noteController.uploadFile(id1, mockFile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        EmbeddedFile uploadedFile = (EmbeddedFile) response.getBody();
        assertNotNull(uploadedFile);
        assertEquals("test.txt", uploadedFile.getFileName());
        assertEquals("text/plain", uploadedFile.getFileType());
    }

    @Test
    public void uploadFileNoteNotFoundTest() {
        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        var response = noteController.uploadFile(UUID.randomUUID(), mockFile);

        assertEquals(ResponseEntity.notFound().build(), response);
    }

    @Test
    public void deleteFileTest() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;
        UUID fileId = embeddedFileRepository.save(embeddedFile).getId();

        var response = noteController.deleteFile(id1, fileId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(embeddedFileRepository.findById(fileId).isEmpty());
    }

    @Test
    public void renameFileTest() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;
        UUID fileId = embeddedFileRepository.save(embeddedFile).getId();

        var response = noteController.renameFile(id1, fileId, "new_name.txt");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        EmbeddedFile renamedFile = response.getBody();
        assertNotNull(renamedFile);
        assertEquals("new_name.txt", renamedFile.getFileName());
    }

    @Test
    public void renameFileNotFoundTest() {
        var response = noteController.renameFile(UUID.randomUUID(), UUID.randomUUID(), "new_name.txt");

        assertEquals(ResponseEntity.notFound().build(), response);
    }

    @Test
    public void updateNoteWithInvalidDataTest() {
        collectionController.createCollection(collection1);
        UUID id1 = noteController.createNote(note1).getBody().id;

        Note invalidNote = new Note("", "invalid", collection1);
        var response = noteController.updateNote(id1, invalidNote);

        assertEquals(BAD_REQUEST, response.getStatusCode());
        var actual = noteController.getNoteById(id1);
        assertEquals(ResponseEntity.ok(note1), actual);
    }

    @Test
    public void duplicateNoteTitlesInSameCollectionTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        Note duplicateNote = new Note("note1", "different content", collection1);
        var response = noteController.createNote(duplicateNote);

        assertEquals(BAD_REQUEST, response.getStatusCode());
    }
}
