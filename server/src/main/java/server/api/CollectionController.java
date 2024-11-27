package server.api;

import commons.Collection;
import commons.Note;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

    /**
     * A method to create a collection
     *
     * @param collection collection
     * @return a collection or throws an exception
     */
    @PostMapping
    public ResponseEntity<?> createCollection(@RequestBody Collection collection) {
        //handling the empty collection title
        if (collection.title == null || collection.title.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A collection needs a title.");
        }
        //duplicated titles or different errors
        try {
            Collection createdCollection = collectionService.save(collection);
            return ResponseEntity.ok(createdCollection);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicated collection title.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error, try again later.");
        }


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
     * two exceptions could be thrown
     * DataIntegrityViolationException when we have duplicated title
     */

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCollection(
            @PathVariable long id,
            @RequestBody Collection updatedCollection) {
        Optional<Collection> existingCollection = collectionService.findById(id);
        if (existingCollection.isPresent()) {
            updatedCollection.id = id;
            //handling the empty collection title
            if (updatedCollection.title == null || updatedCollection.title.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A collection needs a title.");
            }
            //handling duplication of titles or different errors
            try {
                Collection savedCollection = collectionService.save(updatedCollection);
                return ResponseEntity.ok(savedCollection);
            } catch (DataIntegrityViolationException e) { //Exception where collection has duplicated title
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicated collection title");
            } catch (Exception e) { //General exception
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error, try again later");
            }
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



