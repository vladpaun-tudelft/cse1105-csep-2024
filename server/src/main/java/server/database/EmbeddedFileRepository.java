package server.database;

import commons.EmbeddedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmbeddedFileRepository extends JpaRepository<EmbeddedFile, Long> {
    List<EmbeddedFile> findByNoteId(Long noteId);
    void deleteByNoteId(Long noteId);
}
