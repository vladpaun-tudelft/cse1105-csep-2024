package client.services;

import commons.Collection;
import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CollectionFilterTest {

    private CollectionFilter collectionFilter;
    private Collection collection;
    private Note note1;
    private Note note2;
    private Note note3;

    @BeforeEach
    void setUp() {
        collection = new Collection("Test Collection");
        note1 = new Note("Note 1", "Body 1", collection);
        note2 = new Note("Note 2", "Body 2", collection);
        note3 = new Note("Note 3", "Body 3", new Collection("Another Collection"));
        collectionFilter = new CollectionFilter(collection);
    }

    @Test
    void testApplyFilterWithMatchingCollection() {
        List<Note> notes = Arrays.asList(note1, note2, note3);
        List<Note> filteredNotes = collectionFilter.apply(notes);
        assertEquals(2, filteredNotes.size());
        assertEquals(note1, filteredNotes.get(0));
        assertEquals(note2, filteredNotes.get(1));
    }

    @Test
    void testApplyFilterWithNoMatchingCollection() {
        collectionFilter = new CollectionFilter(new Collection("Non Matching Collection"));
        List<Note> notes = Arrays.asList(note1, note2, note3);
        List<Note> filteredNotes = collectionFilter.apply(notes);
        assertEquals(0, filteredNotes.size());
    }

    @Test
    void testApplyFilterWithNullCollection() {
        collectionFilter = new CollectionFilter(null);
        List<Note> notes = Arrays.asList(note1, note2, note3);
        List<Note> filteredNotes = collectionFilter.apply(notes);
        assertEquals(3, filteredNotes.size());
    }
}
