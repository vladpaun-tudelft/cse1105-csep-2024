package server.api;

import commons.Note;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.service.CollectionService;
import server.service.NoteService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService noteService;
    private final CollectionService collectionService;

    public NoteController(NoteService noteService, CollectionService collectionService) {
        this.noteService = noteService;
        this.collectionService = collectionService;
    }

    @PostMapping(path = {"/", ""})
    public ResponseEntity<Note> createNote(@RequestBody Note note) {
        if (note == null || note.collection == null || note.title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (!collectionService.getAllCollections().contains(note.collection)) {
            return ResponseEntity.badRequest().build();
        }
        Note createdNote = noteService.save(note);
        return ResponseEntity.ok(createdNote);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(@PathVariable long id, @RequestBody Note note) {
        if (note == null || note.collection == null) {
            return ResponseEntity.badRequest().body("Invalid request");
        } else if (note.title.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Note title cannot be blank");
        }
        Optional<Note> existingNote = noteService.findById(id);
        if (existingNote.isPresent()) {
            note.id = id; // Ensure the note's ID is set
            Note updatedNote = noteService.save(note);
            return ResponseEntity.ok(updatedNote);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable long id) {
        if (noteService.findById(id).isPresent()) {
            noteService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable long id) {
        Optional<Note> note = noteService.findById(id);
        return note.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(path={"","/"})
    public ResponseEntity<List<Note>> getAllNotes() {
        List<Note> notes = noteService.getAllNotes();
        return ResponseEntity.ok(notes);
    }
}
