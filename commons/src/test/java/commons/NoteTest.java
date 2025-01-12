package commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NoteTest {
    private static final Collection SOME_COLLECTION = new Collection("title", "http://localhost:8080/");

    @Test
    public void checkConstructor() {
        var q = new Note("title", "body", SOME_COLLECTION);
        assertEquals(SOME_COLLECTION, q.collection);
        assertEquals("title", q.title);
        assertEquals("body", q.body);
    }

    @Test
    public void equalsHashCode() {
        var a = new Note("title", "body", SOME_COLLECTION);
        var b = new Note("title", "body", SOME_COLLECTION);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualsHashCode() {
        var a = new Note("title", "body", SOME_COLLECTION);
        var b = new Note("title2", "body", SOME_COLLECTION);
        // assertNotEquals(a,b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void hasToString() {
        var actual = new Note("title", "body", SOME_COLLECTION).toString();
        assertTrue(actual.contains(Note.class.getSimpleName()));
        assertTrue(actual.contains("\n"));
        assertTrue(actual.contains(Collection.class.getSimpleName()));
        assertTrue(actual.contains("title"));
        assertTrue(actual.contains("body"));
    }

}
