package client.services;

import commons.Note;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Pattern pattern = Pattern.compile("#(\\w+)");
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

        for (Note note : allNotes) {
            if (note.getBody() != null) {
                List<String> noteTags = extractTagsFromBody(note.getBody());
                tags.addAll(noteTags);
            }
        }

        return new ArrayList<>(tags.stream().distinct().toList());
    }
}
