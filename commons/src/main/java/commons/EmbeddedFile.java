package commons;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class EmbeddedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public EmbeddedFile() {}

    public EmbeddedFile(Note note, String fileName, String fileType, byte[] fileContent) {
        this.note = note;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileContent = fileContent;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddedFile that = (EmbeddedFile) o;
        return Objects.equals(id, that.id) /*&& Objects.equals(note, that.note) && Objects.equals(fileName, that.fileName) && Objects.equals(fileType, that.fileType) && Arrays.equals(fileContent, that.fileContent) && Objects.equals(uploadedAt, that.uploadedAt)*/;
    }

    @Override
    public int hashCode() {
        /* int result = Objects.hash(id, note, fileName, fileType, uploadedAt);
        result = 31 * result + Arrays.hashCode(fileContent);
        return result; */
        return Objects.hash(id);
    }
}
