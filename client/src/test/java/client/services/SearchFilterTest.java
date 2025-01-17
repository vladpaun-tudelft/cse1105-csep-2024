package client.services;

import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchFilterTest {

    private SearchFilter searchFilter;
    private Note note1;
    private Note note2;
    private Note note3;

    @BeforeEach
    void setUp() {
        note1 = new Note("Title 1", "Body 1", null);
        note2 = new Note("Title 2", "Body 2", null);
        note3 = new Note("Title 3", "Body 3", null);
        searchFilter = new SearchFilter("body");
    }

    @Test
    void testApplyFilterWithMatchingText() {
        List<Note> notes = Arrays.asList(note1, note2, note3);
        List<Note> filteredNotes = searchFilter.apply(notes);
        assertEquals(3, filteredNotes.size());
    }

    @Test
    void testApplyFilterWithNoMatchingText() {
        searchFilter = new SearchFilter("nonexistent");
        List<Note> notes = Arrays.asList(note1, note2, note3);
        List<Note> filteredNotes = searchFilter.apply(notes);
        assertEquals(0, filteredNotes.size());
    }

    @Test
    void testApplyFilterWithEmptySearchText() {
        searchFilter = new SearchFilter("");
        List<Note> notes = Arrays.asList(note1, note2, note3);
        List<Note> filteredNotes = searchFilter.apply(notes);
        assertEquals(3, filteredNotes.size());
    }
}
