package server.service;

import commons.EmbeddedFile;
import commons.Note;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import server.database.EmbeddedFileRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class EmbeddedFileService {
    private final EmbeddedFileRepository embeddedFileRepository;

    @Autowired
    public EmbeddedFileService(EmbeddedFileRepository embeddedFileRepository) {
        this.embeddedFileRepository = embeddedFileRepository;
    }

    public EmbeddedFile saveFile(Note note, MultipartFile file) throws IOException {
        byte[] fileContent = file.getBytes();

        EmbeddedFile embeddedFile = new EmbeddedFile(note, file.getOriginalFilename(), file.getContentType(), fileContent,
                convertMultipartFileToFile(file));

        return embeddedFileRepository.save(embeddedFile);
    }
    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = File.createTempFile("upload-", multipartFile.getOriginalFilename());
        multipartFile.transferTo(file);
        return file;
    }

    public Optional<EmbeddedFile> findById(Long fileId) {
        return embeddedFileRepository.findById(fileId);
    }

    public List<EmbeddedFile> getFilesByNoteId(Long noteId) {
        return embeddedFileRepository.findByNoteId(noteId);
    }

    public void deleteFile(Long id) {
        embeddedFileRepository.deleteById(id);
    }

    @Transactional
    public void deleteFilesByNoteId(Long noteId) {
        embeddedFileRepository.deleteByNoteId(noteId);
    }

    public EmbeddedFile save(EmbeddedFile embeddedFile) {
        return embeddedFileRepository.save(embeddedFile);
    }
}