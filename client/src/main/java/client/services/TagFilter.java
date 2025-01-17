package client.services;

import commons.Note;

import java.util.List;
import java.util.stream.Collectors;

public class TagFilter implements NoteFilter {

    private final List<String> selectedTags;
    private final TagService tagService;

    public TagFilter(List<String> selectedTags, TagService tagService) {
        this.selectedTags = selectedTags;
        this.tagService = tagService;
    }

    @Override
    public List<Note> apply(List<Note> notes) {
        if (selectedTags.isEmpty()) return notes;
        return notes.stream()
                .filter(note -> tagService.noteHasAllSelectedTags(note, selectedTags))
                .collect(Collectors.toList());
    }
}
