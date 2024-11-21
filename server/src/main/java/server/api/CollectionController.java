package server.api;

import commons.Note;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.service.NoteService;

import java.util.List;

@RestController
@RequestMapping("/api/collection")
public class CollectionController {
    private final NoteService noteService;

    public CollectionController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/all-notes")
    public ResponseEntity<List<Note>> getAllNotes() {
        List<Note> notes = noteService.getAllNotes();
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/{collectionTitle}")
    public ResponseEntity<List<Note>> getAllNotes(@PathVariable String collectionTitle) {
        List<Note> notes = noteService.getNotesByCollection(collectionTitle);
        return ResponseEntity.ok(notes);
    }
}
