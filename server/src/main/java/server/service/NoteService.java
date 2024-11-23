package server.service;

import commons.Note;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import server.database.NoteRepository;

import java.util.List;

@Service
public class NoteService {
    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public List<Note> getNotesByCollection(String collectionTitle) {
        return noteRepository.findByCollectionTitle(collectionTitle);
    }
}
