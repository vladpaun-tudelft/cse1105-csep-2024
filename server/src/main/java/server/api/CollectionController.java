package server.api;

import commons.Collection;
import commons.Note;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.service.CollectionService;
import server.service.NoteService;

import java.util.List;
import java.util.Optional;

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
    public ResponseEntity<List<Note>> getNotesInCollection(@PathVariable String collectionTitle) {
        List<Note> notes = noteService.getNotesByCollection(collectionTitle);
        return ResponseEntity.ok(notes);
    }

    @GetMapping(path = {"/", ""})
    public ResponseEntity<List<Collection>> getAllCollections() {
        List<Collection> collections = collectionService.getAllCollections();
        return ResponseEntity.ok(collections);
    }

    /**
     * endpoint for deleting the collection
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(@PathVariable long id) {
        if (collectionService.findById(id).isPresent()) {
            collectionService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * endpoint for saving the collection
     */

    @PutMapping("/{id}")
    public ResponseEntity<Collection> updateCollection(
            @PathVariable long id,
            @RequestBody Collection updatedCollection) {
        Optional<Collection> existingCollection = collectionService.findById(id);
        if (existingCollection.isPresent()) {
            updatedCollection.id = id;
            //Should we wrap it in a try and catch block if changed title will already exist in database
            //Or database will automatically handle this?
            Collection savedCollection = collectionService.save(updatedCollection);
            return ResponseEntity.ok(savedCollection);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * endpoint for reading the collection
     */
    @GetMapping("/{id}")
    public ResponseEntity<Collection> getCollectionById(@PathVariable long id) {
        Optional<Collection> collectionOptional = collectionService.findById(id);

        return collectionOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());

    }


}



