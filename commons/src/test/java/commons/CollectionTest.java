package commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionTest {
    @Test
    public void checkConstructor() {
        var q = new Collection("title", "http://localhost:8080/");
        assertEquals("title", q.title);
        assertEquals("http://localhost:8080/", q.serverURL);
    }

    @Test
    public void equalsHashCode() {
        var a = new Collection("title", "http://localhost:8080/");
        var b = new Collection("title", "http://localhost:8080/");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualsHashCode() {
        var a = new Collection("title", "http://localhost:8080/");
        var b = new Collection("title2", "http://localhost:8080/");
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void hasToString() {
        var actual = new Collection("title", "http://localhost:8080/").toString();
        assertTrue(actual.contains(Collection.class.getSimpleName()));
        assertTrue(actual.contains("\n"));
        assertTrue(actual.contains("title"));
        assertTrue(actual.contains("http://localhost:8080/"));
    }
}
