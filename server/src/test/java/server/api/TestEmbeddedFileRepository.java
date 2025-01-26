package server.api;

import commons.EmbeddedFile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import server.database.EmbeddedFileRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class TestEmbeddedFileRepository implements EmbeddedFileRepository {

    private List<EmbeddedFile> embeddedFiles = new ArrayList<>();

    @Override
    public List<EmbeddedFile> findByNoteId(UUID  noteId) {
        return embeddedFiles.stream()
                .filter(file -> file.getNote().getId().equals(noteId))
                .toList();
    }

    @Override
    public void deleteByNoteId(UUID noteId) {

    }

    @Override
    public void flush() {

    }

    @Override
    public <S extends EmbeddedFile> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> List<S> saveAllAndFlush(Iterable<S> entities) {
        return null;
    }

    @Override
    public void deleteAllInBatch(Iterable<EmbeddedFile> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<UUID > longs) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public EmbeddedFile getOne(UUID  aUUID ) {
        return null;
    }

    @Override
    public EmbeddedFile getById(UUID  aUUID ) {
        return null;
    }

    @Override
    public EmbeddedFile getReferenceById(UUID  aUUID ) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends EmbeddedFile> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends EmbeddedFile> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends EmbeddedFile> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends EmbeddedFile, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public EmbeddedFile save(EmbeddedFile embeddedFile) {
        embeddedFiles.add(embeddedFile);
        return embeddedFile;
    }

    @Override
    public <S extends EmbeddedFile> List<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<EmbeddedFile> findById(UUID  id) {
        return embeddedFiles.stream()
                .filter(file -> file.getId().equals(id))
                .findFirst();
    }

    @Override
    public boolean existsById(UUID  aUUID ) {
        return false;
    }

    @Override
    public List<EmbeddedFile> findAll() {
        return embeddedFiles;
    }

    @Override
    public List<EmbeddedFile> findAllById(Iterable<UUID > longs) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    public void deleteById(UUID  id) {
        embeddedFiles.removeIf(file -> file.getId().equals(id));
    }

    @Override
    public void delete(EmbeddedFile entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends UUID > longs) {

    }

    @Override
    public void deleteAll(Iterable<? extends EmbeddedFile> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<EmbeddedFile> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<EmbeddedFile> findAll(Pageable pageable) {
        return null;
    }
}
