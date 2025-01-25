package server.database;

import commons.EmbeddedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmbeddedFileRepository extends JpaRepository<EmbeddedFile, UUID> {
    List<EmbeddedFile> findByNoteId(UUID noteId);
    void deleteByNoteId(UUID noteId);
}
