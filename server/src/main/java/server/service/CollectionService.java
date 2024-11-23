package server.service;

import commons.Collection;
import org.springframework.stereotype.Service;
import server.database.CollectionRepository;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;

    public CollectionService(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    public Collection save(Collection collection) {
        return collectionRepository.save(collection);
    }
}
