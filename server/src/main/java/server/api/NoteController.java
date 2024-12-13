package server.api;

import commons.EmbeddedFile;
import commons.Note;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.service.CollectionService;
import server.service.EmbeddedFileService;
import server.service.NoteService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService noteService;
    private final CollectionService collectionService;
    private final EmbeddedFileService embeddedFileService;

    public NoteController(NoteService noteService, CollectionService collectionService, EmbeddedFileService embeddedFileService) {
        this.noteService = noteService;
        this.collectionService = collectionService;
        this.embeddedFileService = embeddedFileService;
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
        Optional<Note> note = noteService.findById(id);
        if (note.isPresent()) {
            try {
                embeddedFileService.deleteFilesByNoteId(id);
                noteService.deleteById(id);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
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

    @PostMapping(path = "/{id}/files", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Optional<Note> noteOpt = noteService.findById(id);
        if (noteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            EmbeddedFile savedFile = embeddedFileService.saveFile(noteOpt.get(), file);
            return ResponseEntity.ok(savedFile);
        } catch (IOException e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading file: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<List<EmbeddedFile>> getFiles(@PathVariable Long id) {
        List<EmbeddedFile> files = embeddedFileService.getFilesByNoteId(id);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{noteId}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long noteId, @PathVariable Long fileId) {
        embeddedFileService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{noteId}/files/{fileId}/rename")
    public ResponseEntity<EmbeddedFile> renameFile(@PathVariable Long noteId, @PathVariable Long fileId, @RequestParam String newFileName) {
        Optional<EmbeddedFile> embeddedFileOpt = embeddedFileService.findById(fileId);
        if (embeddedFileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EmbeddedFile embeddedFile = embeddedFileOpt.get();

        // rename the file by updating its name in the database
        embeddedFile.setFileName(newFileName);
        EmbeddedFile updatedFile = embeddedFileService.save(embeddedFile);

        return ResponseEntity.ok(updatedFile);
    }
}
