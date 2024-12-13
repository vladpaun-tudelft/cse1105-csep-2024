package server.service;

import commons.Note;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import server.database.NoteRepository;

import java.util.List;
import java.util.Optional;

@Service
public class NoteService {
    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public Note save(Note note) {
        return noteRepository.save(note);
    }

    public Optional<Note> findById(long id) {
        return noteRepository.findById(id);
    }

    @Transactional
    public void deleteById(long id) {
        noteRepository.deleteById(id);
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public List<Note> getNotesByCollection(String collectionTitle) {
        return noteRepository.findByCollectionTitle(collectionTitle);
    }
}
