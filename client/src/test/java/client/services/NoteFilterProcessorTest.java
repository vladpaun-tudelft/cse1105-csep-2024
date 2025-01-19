package client.services;

import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class NoteFilterProcessorTest {

    private NoteFilterProcessor noteFilterProcessor;
    private NoteFilter mockFilter1;
    private NoteFilter mockFilter2;
    private Note note1;
    private Note note2;

    @BeforeEach
    void setUp() {
        noteFilterProcessor = new NoteFilterProcessor();
        mockFilter1 = mock(NoteFilter.class);
        mockFilter2 = mock(NoteFilter.class);
        note1 = new Note("Note 1", "Body 1", null);
        note2 = new Note("Note 2", "Body 2", null);
    }

    @Test
    void testApplyFiltersWithMultipleFilters() {
        List<Note> notes = Arrays.asList(note1, note2);

        Mockito.when(mockFilter1.apply(notes)).thenReturn(Collections.singletonList(note1));
        Mockito.when(mockFilter2.apply(Collections.singletonList(note1))).thenReturn(Collections.singletonList(note1));

        noteFilterProcessor.addFilter(mockFilter1);
        noteFilterProcessor.addFilter(mockFilter2);

        List<Note> filteredNotes = noteFilterProcessor.applyFilters(notes);
        assertEquals(1, filteredNotes.size());
        assertEquals(note1, filteredNotes.get(0));
    }

    @Test
    void testApplyFiltersWithNoFilters() {
        List<Note> notes = Arrays.asList(note1, note2);

        List<Note> filteredNotes = noteFilterProcessor.applyFilters(notes);
        assertEquals(2, filteredNotes.size());
    }

    @Test
    void testApplyFiltersWithEmptyList() {
        List<Note> notes = List.of();

        List<Note> filteredNotes = noteFilterProcessor.applyFilters(notes);
        assertEquals(0, filteredNotes.size());
    }
}
