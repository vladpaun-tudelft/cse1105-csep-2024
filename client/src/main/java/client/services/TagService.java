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

    /**
     * Extracts tags from a note's body using a regular expression.
     *
     * @param body The body of the note.
     * @return A list of tags extracted from the body.
     */
    public List<String> extractTagsFromBody(String body) {
        List<String> tags = new ArrayList<>();
        Pattern pattern = Pattern.compile("#(\\S+)");
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
        List<String> tags = extractTagsFromBody(markdown);

        for (String tag : tags) {
            if (!tag.isBlank()) {
                String tagHtml = "<button class='custom-tag-button' data-tag='" + tag +
                        "' onclick='handleTagClick(\"" + tag + "\")'>" + tag + "</button>";
                markdown = markdown.replace("#" + tag, tagHtml);
            }
        }

        return markdown;
    }

    public boolean noteHasAllSelectedTags(Note note, List<String> selectedTags) {
        List<String> noteTags = extractTagsFromBody(note.getBody());
        return new HashSet<>(noteTags).containsAll(selectedTags);
    }
}
