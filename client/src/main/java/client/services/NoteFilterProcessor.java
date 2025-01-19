package client.services;

import commons.Note;

import java.util.ArrayList;
import java.util.List;

public class NoteFilterProcessor {
    private List<NoteFilter> filters = new ArrayList<>();

    public void addFilter(NoteFilter filter) {
        filters.add(filter);
    }

    public List<Note> applyFilters(List<Note> notes) {
        for (NoteFilter filter : filters) {
            notes = filter.apply(notes);
        }
        return notes;
    }
}
