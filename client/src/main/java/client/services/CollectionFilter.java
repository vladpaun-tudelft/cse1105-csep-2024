package client.services;

import commons.Collection;
import commons.Note;

import java.util.List;
import java.util.stream.Collectors;

public class CollectionFilter implements NoteFilter {
    private final Collection currentCollection;

    public CollectionFilter(Collection currentCollection) {
        this.currentCollection = currentCollection;
    }

    @Override
    public List<Note> apply(List<Note> notes) {
        if (currentCollection == null) return notes;
        return notes.stream()
                .filter(note -> note.collection.equals(currentCollection))
                .collect(Collectors.toList());
    }
}
