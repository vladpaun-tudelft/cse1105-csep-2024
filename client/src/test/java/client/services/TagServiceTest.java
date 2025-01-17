package client.services;

import commons.Collection;
import commons.Note;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TagServiceTest {

    private final TagService tagService = new TagService();

    @Test
    public void testExtractTagsFromBody() {
        String body = "This is a note with #tag1 and #tag2.";

        List<String> tags = tagService.extractTagsFromBody(body);

        assertEquals(2, tags.size());
        assertTrue(tags.contains("tag1"));
        assertTrue(tags.contains("tag2"));
    }

    @Test
    public void testExtractTagsFromBodyWithNoTags() {
        String body = "This is a note without any tags.";

        List<String> tags = tagService.extractTagsFromBody(body);

        assertTrue(tags.isEmpty());
    }

    @Test
    public void testGetUniqueTags() {
        Collection collection = new Collection("test-collection");
        Note note1 = new Note("1", "First note with #tag1 and #tag2.", collection);
        Note note2 = new Note("2", "Second note with #tag2 and #tag3.", collection);
        List<Note> notes = List.of(note1, note2);

        List<String> uniqueTags = tagService.getUniqueTags(notes);

        assertEquals(3, uniqueTags.size());
        assertTrue(uniqueTags.contains("tag1"));
        assertTrue(uniqueTags.contains("tag2"));
        assertTrue(uniqueTags.contains("tag3"));
    }

    @Test
    public void testGetUniqueTagsWithNoTags() {
        Collection collection = new Collection("test-collection");
        Note note1 = new Note("1", "First note without any tags.", collection);
        Note note2 = new Note("2", "Second note also without tags.", collection);
        List<Note> notes = List.of(note1, note2);

        List<String> uniqueTags = tagService.getUniqueTags(notes);

        assertTrue(uniqueTags.isEmpty());
    }

    @Test
    public void testReplaceTagsInMarkdown() {
        String markdown = "This is a #tag1 and here is #tag2. Also, check #tag3.";

        String expected = "This is a <button class='custom-tag-button' data-tag='tag1' onclick='handleTagClick(\"tag1\")'>tag1</button> and here is <button class='custom-tag-button' data-tag='tag2' onclick='handleTagClick(\"tag2\")'>tag2</button>. Also, check <button class='custom-tag-button' data-tag='tag3' onclick='handleTagClick(\"tag3\")'>tag3</button>.";

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
}