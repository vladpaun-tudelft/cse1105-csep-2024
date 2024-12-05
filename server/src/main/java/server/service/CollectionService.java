package server.service;

import commons.Collection;
import org.springframework.stereotype.Service;
import server.database.CollectionRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;

    public CollectionService(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    public Collection save(Collection collection) {
        return collectionRepository.save(collection);
    }

    public void deleteById(long id) {
        collectionRepository.deleteById(id);
    }

    public Optional<Collection> findById(long id) {
        return collectionRepository.findById(id);
    }

    public List<Collection> getAllCollections() {
        return collectionRepository.findAll();
    }

    public Collection getCollectionByTitle(String title) {
        return collectionRepository.findByTitle(title);
    }

    public Optional<Collection> getCollectionById(long id) {
        return collectionRepository.findById(id);
    }
}
