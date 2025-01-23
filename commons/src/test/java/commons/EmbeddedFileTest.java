package commons;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class EmbeddedFileTest {

    private Note note;
    private EmbeddedFile embeddedFile;
    private byte[] fileContent;

    @BeforeEach
    void setUp() {
        note = new Note("Test Note", "Sample Content", null);
        fileContent = new byte[]{1, 2, 3, 4};
        embeddedFile = new EmbeddedFile(note, "example.jpg", "image/jpeg", fileContent);
    }

    @Test
    void testEmbeddedFileCreation() {
        assertNotNull(embeddedFile);
        assertEquals("example.jpg", embeddedFile.getFileName());
        assertEquals("image/jpeg", embeddedFile.getFileType());
        assertArrayEquals(fileContent, embeddedFile.getFileContent());
        assertNotNull(embeddedFile.getUploadedAt());
        assertEquals(note, embeddedFile.getNote());
    }

    @Test
    void testSetFileName() {
        embeddedFile.setFileName("newfile.jpg");
        assertEquals("newfile.jpg", embeddedFile.getFileName());
    }

    @Test
    void testSetFileContent() {
        byte[] newContent = new byte[]{5, 6, 7, 8};
        embeddedFile.setFileContent(newContent);
        assertArrayEquals(newContent, embeddedFile.getFileContent());
    }

    @Test
    void testGetUploadedAt() {
        assertNotNull(embeddedFile.getUploadedAt());
        assertTrue(embeddedFile.getUploadedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testGetId() {
        assertNull(embeddedFile.getId());
    }
}
