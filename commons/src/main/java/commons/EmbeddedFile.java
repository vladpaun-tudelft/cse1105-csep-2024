package commons;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
public class EmbeddedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "note_id", nullable = false)
    @JsonBackReference  // required to prevent infinite recursion
    private Note note;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Lob
    @Column(nullable = false)
    private byte[] fileContent;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Lob
    private File file;

    public EmbeddedFile() {}

    public EmbeddedFile(Note note, String fileName, String fileType, byte[] fileContent, File file) {
        this.note = note;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileContent = fileContent;
        this.uploadedAt = LocalDateTime.now();
        this.file = file;
    }

    public EmbeddedFile(Note note, String fileName, String fileType, byte[] fileContent) {
        this.note = note;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileContent = fileContent;
        this.uploadedAt = LocalDateTime.now();
        this.file = null;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Note getNote() {
        return note;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddedFile that = (EmbeddedFile) o;
        return Objects.equals(id, that.id) && Objects.equals(note, that.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, note);
    }

}