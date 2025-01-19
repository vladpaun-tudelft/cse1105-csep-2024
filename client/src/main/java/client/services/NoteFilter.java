package client.services;

import commons.Note;

import java.util.List;

@FunctionalInterface
public interface NoteFilter {
    List<Note> apply(List<Note> notes);
}
