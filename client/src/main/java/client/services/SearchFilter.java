package client.services;

import commons.Note;

import java.util.List;
import java.util.stream.Collectors;

public class SearchFilter implements NoteFilter {
    private final String searchText;

    public SearchFilter(String searchText) {
        this.searchText = searchText.toLowerCase().trim();
    }

    /**
     * Checks if the note's title or body contains the given search text.
     *
     * @param note       Note to check
     * @param searchText Text to search for
     * @return true if the note contains the text, false otherwise
     */
    private boolean containsText(Note note, String searchText) {
        return note.title.toLowerCase().contains(searchText) ||
                note.body.toLowerCase().contains(searchText);
    }

    @Override
    public List<Note> apply(List<Note> notes) {
        if (searchText.isEmpty()) return notes;
        return notes.stream()
                .filter(note -> containsText(note, searchText))
                .collect(Collectors.toList());
    }
}
