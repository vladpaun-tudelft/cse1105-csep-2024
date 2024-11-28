package server.api;

import commons.Note;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import server.database.NoteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class TestNoteRepository implements NoteRepository {

    public List<Note> notes = new ArrayList<>();

    @Override
    public List<Note> findByCollectionTitle(String collectionTitle) {
        return List.of();
    }

    @Override
    public void flush() {

    }

    @Override
    public <S extends Note> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends Note> List<S> saveAllAndFlush(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public void deleteAllInBatch(Iterable<Note> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> longs) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public Note getOne(Long aLong) {
        return null;
    }

    @Override
    public Note getById(Long aLong) {
        return null;
    }

    @Override
    public Note getReferenceById(Long aLong) {
        return null;
    }

    @Override
    public <S extends Note> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Note> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends Note> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends Note> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends Note> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Note> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends Note, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public <S extends Note> S save(S entity) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id == entity.id) {
                notes.set(i, entity);
                return entity;
            }
        }
        entity.id = (long) (notes.size()+1);
        notes.add(entity);
        return entity;
    }

    @Override
    public <S extends Note> List<S> saveAll(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public Optional<Note> findById(Long aLong) {
        return notes.stream()
                .filter(note -> note.id == aLong)
                .findFirst();
    };

    @Override
    public boolean existsById(Long aLong) {
        return false;
    }

    @Override
    public List<Note> findAll() {
        return new ArrayList<>(notes);
    }

    @Override
    public List<Note> findAllById(Iterable<Long> longs) {
        return List.of();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Long aLong) {
        notes.removeIf(note -> aLong.equals(note.id));
    }

    @Override
    public void delete(Note entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Long> longs) {

    }

    @Override
    public void deleteAll(Iterable<? extends Note> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<Note> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<Note> findAll(Pageable pageable) {
        return null;
    }
}
