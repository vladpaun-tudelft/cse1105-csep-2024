package client.services;

import commons.Collection;
import commons.Note;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TagServiceTest {

    private final TagService tagService = new TagService();

    @Test
    public void testExtractTagsFromBody() {
        String body = "This is a note with #tag1 and #tag2 .";

        List<String> tags = tagService.extractTagsFromBody(body);

        assertEquals(2, tags.size());
        assertTrue(tags.contains("tag1"));
        assertTrue(tags.contains("tag2"));
    }

    @Test
    public void testExtractTagsFromBodySpecialCharacters() {
        String body = "This is a note with special #tags-@1234";

        List<String> tags = tagService.extractTagsFromBody(body);

        assertEquals(1, tags.size());
        assertTrue(tags.contains("tags-@1234"));
    }

    @Test
    public void testExtractTagsFromBodyWithNoTags() {
        String body = "This is a note without any tags.";

        List<String> tags = tagService.extractTagsFromBody(body);

        assertTrue(tags.isEmpty());
    }

    @Test
    public void testGetUniqueTags() {
        Collection collection = new Collection("test-collection", "url");
        Note note1 = new Note("1", "First note with #tag1 and #tag2 .", collection);
        Note note2 = new Note("2", "Second note with #tag2 and #tag3 .", collection);
        List<Note> notes = List.of(note1, note2);

        List<String> uniqueTags = tagService.getUniqueTags(notes);

        assertEquals(3, uniqueTags.size());
        assertTrue(uniqueTags.contains("tag1"));
        assertTrue(uniqueTags.contains("tag2"));
        assertTrue(uniqueTags.contains("tag3"));
    }

    @Test
    public void testGetUniqueTagsWithNoTags() {
        Collection collection = new Collection("test-collection", "url");
        Note note1 = new Note("1", "First note without any tags.", collection);
        Note note2 = new Note("2", "Second note also without tags.", collection);
        List<Note> notes = List.of(note1, note2);

        List<String> uniqueTags = tagService.getUniqueTags(notes);

        assertTrue(uniqueTags.isEmpty());
    }

    @Test
    public void testReplaceTagsInMarkdown() {
        String markdown = "This is a #tag1 and here is #tag2 . Also, check #tag3 .";

        String expected = "This is a <button class='custom-tag-button' data-tag='tag1' onclick='handleTagClick(\"tag1\")'>tag1</button> and here is <button class='custom-tag-button' data-tag='tag2' onclick='handleTagClick(\"tag2\")'>tag2</button> . Also, check <button class='custom-tag-button' data-tag='tag3' onclick='handleTagClick(\"tag3\")'>tag3</button> .";

        String result = tagService.replaceTagsInMarkdown(markdown);

        assertEquals(expected, result);
    }

    @Test
    public void testReplaceTagsInMarkdownWithEmptyTags() {
        String markdown = "This is a markdown with no tags.";

        String expected = "This is a markdown with no tags.";

        String result = tagService.replaceTagsInMarkdown(markdown);

        assertEquals(expected, result);
    }

    @Test
    public void testReplaceTagsInMarkdownWithBlankTag() {
        String markdown = "This is a markdown with a # tag.";

        String expected = "This is a markdown with a # tag.";

        String result = tagService.replaceTagsInMarkdown(markdown);

        assertEquals(expected, result);
    }

    @Test
    public void testNoteHasAllSelectedTags() {
        Collection collection = new Collection("test-collection", "url");
        ObservableList<Note> allNotes = FXCollections.observableArrayList(
                new Note("Note 1", "This is a note with #tag1 and #tag2 .", collection),
                new Note("Note 2", "This is another note with #tag2 only.", collection),
                new Note("Note 3", "No tags here!", collection)
        );

        List<String> selectedTags = List.of("tag1", "tag2");

        Note note1 = allNotes.get(0);
        assertTrue(tagService.noteHasAllSelectedTags(note1, selectedTags));

        Note note2 = allNotes.get(1);
        assertFalse(tagService.noteHasAllSelectedTags(note2, selectedTags));

        Note note3 = allNotes.get(2);
        assertFalse(tagService.noteHasAllSelectedTags(note3, selectedTags));
    }

    @Test
    public void testFilterNotesByTags() {
        Collection collection = new Collection("test-collection", "url");
        Note note1 = new Note("1", "First note with #tag1 and #tag2 .", collection);
        Note note2 = new Note("2", "Second note with #tag2 and #tag3 .", collection);
        Note note3 = new Note("3", "Third note with #tag1 only.", collection);
        List<Note> notes = List.of(note1, note2, note3);

        List<String> selectedTags = List.of("tag1", "tag2");

        List<Note> filteredNotes = tagService.filterNotesByTags(notes, selectedTags);

        assertEquals(1, filteredNotes.size());
        assertTrue(filteredNotes.contains(note1));
        assertFalse(filteredNotes.contains(note2));
        assertFalse(filteredNotes.contains(note3));
    }

    @Test
    public void testGetAvailableTagsForRemainingNotes() {
        Collection collection = new Collection("test-collection", "url");
        Note note1 = new Note("1", "First note with #tag1 and #tag2 .", collection);
        Note note2 = new Note("2", "Second note with #tag2 and #tag3 .", collection);
        Note note3 = new Note("3", "Third note with #tag1 only.", collection);
        List<Note> filteredNotes = List.of(note1, note2, note3);

        List<String> selectedTags = List.of("tag1");

        List<String> availableTags = tagService.getAvailableTagsForRemainingNotes(filteredNotes, selectedTags);

        assertEquals(2, availableTags.size());
        assertTrue(availableTags.contains("tag2"));
        assertTrue(availableTags.contains("tag3"));
    }

    @Test
    public void testGetAvailableTagsForRemainingNotesWithNoTagsSelected() {
        Collection collection = new Collection("test-collection", "url");
        Note note1 = new Note("1", "First note with #tag1 and #tag2 .", collection);
        Note note2 = new Note("2", "Second note with #tag2 and #tag3 .", collection);
        Note note3 = new Note("3", "Third note with #tag1 only.", collection);
        List<Note> filteredNotes = List.of(note1, note2, note3);

        List<String> selectedTags = List.of();

        List<String> availableTags = tagService.getAvailableTagsForRemainingNotes(filteredNotes, selectedTags);

        assertEquals(3, availableTags.size());
        assertTrue(availableTags.contains("tag1"));
        assertTrue(availableTags.contains("tag2"));
        assertTrue(availableTags.contains("tag3"));
    }

    @Test
    public void testGetAvailableTagsForRemainingNotesWithAllTagsSelected() {
        Collection collection = new Collection("test-collection", "url");
        Note note1 = new Note("1", "First note with #tag1 and #tag2 .", collection);
        Note note2 = new Note("2", "Second note with #tag2 and #tag3 .", collection);
        Note note3 = new Note("3", "Third note with #tag1 only.", collection);
        List<Note> filteredNotes = List.of(note1, note2, note3);

        List<String> selectedTags = List.of("tag1", "tag2", "tag3");

        List<String> availableTags = tagService.getAvailableTagsForRemainingNotes(filteredNotes, selectedTags);

        assertTrue(availableTags.isEmpty());
    }
}