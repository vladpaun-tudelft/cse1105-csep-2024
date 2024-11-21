package server.api;

import commons.Collection;
import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import server.service.NoteService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoteServiceTest {

    private NoteController noteController;
    private CollectionController collectionController;
    private NoteService service;
    private TestNoteRepository repo;

    private Collection collection1;
    private Collection collection2;

    @BeforeEach
    void setUp() {
        repo = new TestNoteRepository();
        service = new NoteService(repo);
        noteController = new NoteController(service);
        collectionController = new CollectionController(service);


        collection1 = new Collection("collection1");
        collection2 = new Collection("collection2");

        Note note1 = new Note("note1", "bla", collection1);
        Note note2 = new Note("note2", "bla", collection1);
        Note note3 = new Note("note3", "bla", collection2);
        Note note4 = new Note("note4", "bla", collection2);

        repo.save(note1);
        repo.save(note2);
        repo.save(note3);
        repo.save(note4);
    }

    @Test
    void getAllNotesThroughNoteCtrlTest() {
        ResponseEntity<List<Note>> response = noteController.getAllNotes();

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

    @Test
    void getAllNotesThroughCollectionCtrlTest() {
        ResponseEntity<List<Note>> response = collectionController.getAllNotes();

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
}