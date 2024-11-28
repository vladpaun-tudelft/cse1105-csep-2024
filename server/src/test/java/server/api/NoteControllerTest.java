package server.api;

import commons.Collection;
import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import server.service.CollectionService;
import server.service.NoteService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class NoteControllerTest {

    private NoteController noteController;
    private CollectionController collectionController;

    private NoteService noteService;
    private CollectionService collectionService;

    private TestNoteRepository noteRepo;
    private TestCollectionRepository collectionRepo;

    Collection collection1, collection2;
    Note note1, note2, note3, note4;

    @BeforeEach
    void setUp() {
        noteRepo = new TestNoteRepository();
        collectionRepo = new TestCollectionRepository();

        noteService = new NoteService(noteRepo);
        collectionService = new CollectionService(collectionRepo);

        noteController = new NoteController(noteService,collectionService);
        collectionController = new CollectionController(noteService, collectionService);


        collection1 = new Collection("collection1");
        collection2 = new Collection("collection2");

        note1 = new Note("note1", "bla", collection1);
        note2 = new Note("note2", "bla", collection1);
        note3 = new Note("note3", "bla", collection2);
        note4 = new Note("note4", "bla", collection2);
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
        noteController.createNote(note1);
        noteController.createNote(note2);

        var actual1 = noteController.getNoteById(1);
        var actual2 = noteController.getNoteById(2);

        assertEquals(ResponseEntity.ok(note1), actual1);
        assertEquals(ResponseEntity.ok(note2), actual2);
    }

    @Test
    public void getNoteByIdNotFoundTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        var actual = noteController.getNoteById(2);

        assertEquals(ResponseEntity.notFound().build(), actual);
    }

    @Test
    public void deleteNoteTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        var response = noteController.deleteNote(1);

        assertEquals(ResponseEntity.noContent().build(), response);

        var actual = noteController.getNoteById(1);
        assertEquals(ResponseEntity.notFound().build(), actual);

    }

    @Test
    public void updateNoteTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        var response = noteController.updateNote(1, note2);
        assertEquals(ResponseEntity.ok(note2), response);

        var actual = noteController.getNoteById(1);
        assertEquals(ResponseEntity.ok(note2), actual);
    }

    @Test
    public void updateNoteNotFoundTest() {
        collectionController.createCollection(collection1);
        noteController.createNote(note1);

        var actual = noteController.updateNote(2, note3);
        assertEquals(ResponseEntity.notFound().build(), actual);

        var actual2 = noteController.getNoteById(1);
        assertEquals(ResponseEntity.ok(note1), actual2);
    }


    @Test
    void getAllNotesTest() {
        collectionController.createCollection(collection1);
        collectionController.createCollection(collection2);

        noteController.createNote(note1);
        noteController.createNote(note2);
        noteController.createNote(note3);
        noteController.createNote(note4);

        var response = noteController.getAllNotes();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().size());

        assertEquals("note1", response.getBody().get(0).title);
        assertEquals("note2", response.getBody().get(1).title);
        assertEquals("note3", response.getBody().get(2).title);
        assertEquals("note4", response.getBody().get(3).title);

        assertEquals(collection1, response.getBody().get(0).collection);
        assertEquals(collection1, response.getBody().get(1).collection);
        assertEquals(collection2, response.getBody().get(2).collection);
        assertEquals(collection2, response.getBody().get(3).collection);
    }

    /*
    @Test
    void getNotesByCollectionTest() {
        ResponseEntity<List<Note>> response1 = collectionController.getAllNotes(collection1.title);
        ResponseEntity<List<Note>> response2 = collectionController.getAllNotes(collection2.title);

        assertEquals(200, response1.getStatusCodeValue());
        assertEquals(200, response2.getStatusCodeValue());

        assertNotNull(response1.getBody());
        assertNotNull(response2.getBody());

        assertEquals(2, response1.getBody().size());
        assertEquals(2, response2.getBody().size());

        assertEquals("note1", response1.getBody().get(0).title);
        assertEquals("note2", response1.getBody().get(1).title);

        assertEquals("note3", response2.getBody().get(0).title);
        assertEquals("note4", response2.getBody().get(1).title);
    }
    */
}