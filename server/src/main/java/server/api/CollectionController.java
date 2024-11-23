package server.api;

import commons.Collection;
import commons.Note;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.service.CollectionService;
import server.service.NoteService;

import java.util.List;

@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    private final NoteService noteService;
    private final CollectionService collectionService;

    public CollectionController(NoteService noteService, CollectionService collectionService) {
        this.noteService = noteService;
        this.collectionService = collectionService;
    }

    @PostMapping
    public ResponseEntity<Collection> createCollection(@RequestBody Collection collection) {
        Collection createdCollection = collectionService.save(collection);
        return ResponseEntity.ok(createdCollection);
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
