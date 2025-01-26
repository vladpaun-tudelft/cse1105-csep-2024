package server.database;

import commons.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findByCollectionTitle(String collectionTitle);
}
