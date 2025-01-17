package client.services;

import commons.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TagFilterTest {

    private TagFilter tagFilter;
    private TagService tagService;
    private Note note1;
    private Note note2;
    private Note note3;

    @BeforeEach
    void setUp() {
        tagService = mock(TagService.class);
        note1 = new Note("Note 1", "Body 1", null);
        note2 = new Note("Note 2", "Body 2", null);
        note3 = new Note("Note 3", "Body 3", null);
        tagFilter = new TagFilter(Arrays.asList("tag1", "tag2"), tagService);
    }

    @Test
    void testApplyFilterWithMatchingTags() {
        List<Note> notes = Arrays.asList(note1, note2, note3);
        Mockito.when(tagService.noteHasAllSelectedTags(note1, Arrays.asList("tag1", "tag2"))).thenReturn(true);
        Mockito.when(tagService.noteHasAllSelectedTags(note2, Arrays.asList("tag1", "tag2"))).thenReturn(false);
        Mockito.when(tagService.noteHasAllSelectedTags(note3, Arrays.asList("tag1", "tag2"))).thenReturn(true);

        List<Note> filteredNotes = tagFilter.apply(notes);
        assertEquals(2, filteredNotes.size());
        assertEquals(note1, filteredNotes.get(0));
        assertEquals(note3, filteredNotes.get(1));
    }

    @Test
    void testApplyFilterWithEmptySelectedTags() {
        tagFilter = new TagFilter(List.of(), tagService);
        List<Note> notes = Arrays.asList(note1, note2, note3);
        List<Note> filteredNotes = tagFilter.apply(notes);
        assertEquals(3, filteredNotes.size());
    }

    @Test
    void testApplyFilterWithNoMatchingTags() {
        List<Note> notes = Arrays.asList(note1, note2, note3);
        Mockito.when(tagService.noteHasAllSelectedTags(note1, Arrays.asList("tag1", "tag2"))).thenReturn(false);
        Mockito.when(tagService.noteHasAllSelectedTags(note2, Arrays.asList("tag1", "tag2"))).thenReturn(false);
        Mockito.when(tagService.noteHasAllSelectedTags(note3, Arrays.asList("tag1", "tag2"))).thenReturn(false);

        List<Note> filteredNotes = tagFilter.apply(notes);
        assertEquals(0, filteredNotes.size());
    }
}
