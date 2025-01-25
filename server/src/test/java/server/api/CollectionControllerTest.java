package server.api;

import commons.Collection;
import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import server.service.CollectionService;
import server.service.EmbeddedFileService;
import server.service.NoteService;

import static org.junit.jupiter.api.Assertions.*;

class CollectionControllerTest {
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

    @BeforeEach
    void setUp() {
        noteRepo = new TestNoteRepository();
        collectionRepo = new TestCollectionRepository();

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
    }

    @Test
    public void createCollectionTest() {
        var actual = collectionController.createCollection(collection1);
        assertNotNull(actual);
        assertEquals(ResponseEntity.ok(collection1), actual);
    }

    @Test
    public void cannotAddNullCollection() {
        var actual = collectionController.createCollection(null);
        assertEquals(ResponseEntity.badRequest().build(), actual);
    }

    @Test
    public void cannotAddCollectionWithEmptyTitle() {
        var actual = collectionController.createCollection(new Collection("", "http://localhost:8080/"));
        assertEquals(ResponseEntity.badRequest().body("A collection needs a title."), actual);
    }

    @Test
    public void cannotAddCollectionWithEmptyServer() {
        var actual = collectionController.createCollection(new Collection("bla bla", ""));
        assertEquals(ResponseEntity.badRequest().body("A collection needs a serverURL."), actual);
    }

    @Test
    public void cannotAddDuplicateCollectionTitles() {
        collectionController.createCollection(collection1);
        var actual = collectionController.createCollection(new Collection("collection1", "http://localhost:8080/"));
        assertEquals(ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicated collection title."), actual);
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

        var response = collectionController.getAllNotes();

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
    }

    @Test
    public void getNotesInCollectionTest() {
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

        var response1 = collectionController.getNotesInCollection(collection1.title);
        var response2 = collectionController.getNotesInCollection(collection2.title);
        var response3 = collectionController.getNotesInCollection(collection3.title);
        var response4 = collectionController.getNotesInCollection(collection4.title);

        assertEquals(200, response1.getStatusCodeValue());
        assertEquals(200, response2.getStatusCodeValue());
        assertEquals(200, response3.getStatusCodeValue());
        assertEquals(200, response4.getStatusCodeValue());

        assertNotNull(response1.getBody());
        assertNotNull(response2.getBody());
        assertNotNull(response3.getBody());
        assertNotNull(response4.getBody());

        assertEquals(2, response1.getBody().size());
        assertEquals(2, response2.getBody().size());
        assertEquals(2, response3.getBody().size());
        assertEquals(2, response4.getBody().size());

        assertEquals("note1", response1.getBody().get(0).title);
        assertEquals("note2", response1.getBody().get(1).title);
        assertEquals("note3", response2.getBody().get(0).title);
        assertEquals("note4", response2.getBody().get(1).title);
        assertEquals("note5", response3.getBody().get(0).title);
        assertEquals("note6", response3.getBody().get(1).title);
        assertEquals("note7", response4.getBody().get(0).title);
        assertEquals("note8", response4.getBody().get(1).title);
    }

    @Test
    public void getAllCollectionsTest() {
        collectionController.createCollection(collection1);
        collectionController.createCollection(collection2);
        collectionController.createCollection(collection3);
        collectionController.createCollection(collection4);

        var response = collectionController.getAllCollections();
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().size());
        assertEquals("collection1", response.getBody().get(0).title);
        assertEquals("collection2", response.getBody().get(1).title);
        assertEquals("collection3", response.getBody().get(2).title);
        assertEquals("collection4", response.getBody().get(3).title);
    }

    @Test
    public void getCollectionByIdTest() {
        collectionController.createCollection(collection1);
        collectionController.createCollection(collection2);
        collectionController.createCollection(collection3);
        collectionController.createCollection(collection4);

        var actual1 = collectionController.getCollectionById(1);
        var actual2 = collectionController.getCollectionById(2);
        var actual3 = collectionController.getCollectionById(3);
        var actual4 = collectionController.getCollectionById(4);

        assertEquals(ResponseEntity.ok(collection1), actual1);
        assertEquals(ResponseEntity.ok(collection2), actual2);
        assertEquals(ResponseEntity.ok(collection3), actual3);
        assertEquals(ResponseEntity.ok(collection4), actual4);
    }

    @Test
    public void getCollectionByIdNotFoundTest() {
        collectionController.createCollection(collection1);

        var actual = collectionController.getCollectionById(2);

        assertEquals(ResponseEntity.notFound().build(), actual);
    }

    @Test
    public void deleteCollectionTest() {
        collectionController.createCollection(collection1);

        var response = collectionController.deleteCollection(1);
        assertEquals(ResponseEntity.noContent().build(), response);

        var actual = collectionController.getCollectionById(1);

        assertEquals(ResponseEntity.notFound().build(), actual);
    }

    @Test
    public void deleteCollectionNotFoundTest() {
        collectionController.createCollection(collection1);

        var actual = collectionController.deleteCollection(2);
        assertEquals(ResponseEntity.notFound().build(), actual);
    }

    @Test
    public void updateCollectionTest() {
        collectionController.createCollection(collection1);

        var response = collectionController.updateCollection(1, collection2);
        assertEquals(ResponseEntity.ok(collection2), response);

        var actual = collectionController.getCollectionById(1);
        assertEquals(ResponseEntity.ok(collection2), actual);
    }

    @Test
    public void updateCollectionNotFoundTest() {
        collectionController.createCollection(collection1);

        var actual = collectionController.updateCollection(2, collection1);
        assertEquals(ResponseEntity.notFound().build(), actual);
    }

    @Test
    public void updateCollectionInvalidTest() {
        collectionController.createCollection(collection1);
        collectionController.createCollection(collection2);

        var actual = collectionController.updateCollection(1, new Collection("", "http://localhost:8080/"));

        assertEquals(ResponseEntity.badRequest().body("A collection needs a title."), actual);

        var actual2 = collectionController.updateCollection(1, new Collection(null, "http://localhost:8080/"));

        assertEquals(ResponseEntity.badRequest().body("A collection needs a title."), actual2);

        var actual3 = collectionController.updateCollection(1, new Collection("collection2", "http://localhost:8080/"));
        assertEquals(ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicated collection title"), actual3);
    }
}