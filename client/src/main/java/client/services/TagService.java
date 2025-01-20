package client.services;

import commons.Note;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for managing tags in the application.
 */
public class TagService {

    // Matches hashtags in the format "#tag"
    // consists of one or more of the following characters
    // - Letters: uppercase or lowercase, a to z or A to Z
    // - Numbers, 0 to 9
    // - Underscore, plus or hyphen
    private static final String TAG_PATTERN = "#([a-zA-Z0-9_+-]+)";

    /**
     * Extracts tags from a note's body using a regular expression.
     * Tags are identified as words prefixed with a `#` character.
     * Valid tag characters include letters, digits, underscores, plus signs and hyphens.
     *
     * @param body The body of the note.
     * @return A list of tags extracted from the body.
     */
    public List<String> extractTagsFromBody(String body) {
        List<String> tags = new ArrayList<>();
        Pattern pattern = Pattern.compile(TAG_PATTERN);
        Matcher matcher = pattern.matcher(body);

        while (matcher.find()) {
            String tag = matcher.group(1);
            tags.add(tag);
        }
        return tags;
    }

    /**
     * Returns a list of unique tags from a list of notes.
     *
     * @param allNotes The list of notes to extract tags from.
     * @return A list of unique tags.
     */
    public List<String> getUniqueTags(List<Note> allNotes) {
        ObservableList<String> tags = FXCollections.observableArrayList();
        if (allNotes == null) return tags;

        for (Note note : allNotes) {
            if (note.getBody() != null) {
                List<String> noteTags = extractTagsFromBody(note.getBody());
                tags.addAll(noteTags);
            }
        }

        return new ArrayList<>(tags.stream().distinct().toList());
    }

    /**
     * Filters the notes based on the currently selected tags.
     */
    public List<Note> filterNotesByTags(List<Note> notes, List<String> selectedTags) {
        return notes.stream()
                .filter(note -> noteHasAllSelectedTags(note, selectedTags))
                .collect(Collectors.toList());
    }

    /**
     * Returns the available tags for the remaining notes, excluding the selected ones.
     */
    public List<String> getAvailableTagsForRemainingNotes(List<Note> filteredNotes, List<String> selectedTags) {
        List<String> availableTags = new ArrayList<>(getUniqueTags(filteredNotes));

        // Remove all selected tags from the available tags
        availableTags.removeAll(selectedTags);

        return availableTags;
    }

    /**
     * Replaces tags in the markdown body with custom HTML elements for further processing.
     *
     * @param markdown The markdown body to process
     * @return The markdown with tags replaced by custom HTML
     */
    public String replaceTagsInMarkdown(String markdown) {
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile(TAG_PATTERN);
        Matcher matcher = pattern.matcher(markdown);

        while (matcher.find()) {
            String tag = matcher.group(1);
            String tagHtml = "<button class='custom-tag-button' data-tag='" + tag +
                    "' onclick='handleTagClick(\"" + tag + "\")'>" + tag + "</button>";
            matcher.appendReplacement(result, Matcher.quoteReplacement(tagHtml));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public boolean noteHasAllSelectedTags(Note note, List<String> selectedTags) {
        List<String> noteTags = extractTagsFromBody(note.getBody());
        return new HashSet<>(noteTags).containsAll(selectedTags);
    }
}
