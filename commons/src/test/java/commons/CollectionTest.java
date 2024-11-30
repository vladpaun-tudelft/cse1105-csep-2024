package commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionTest {
    @Test
    public void checkConstructor() {
        var q = new Collection("title");
        assertEquals("title", q.title);
    }

    @Test
    public void equalsHashCode() {
        var a = new Collection("title");
        var b = new Collection("title");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualsHashCode() {
        var a = new Collection("title");
        var b = new Collection("title2");
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void hasToString() {
        var actual = new Collection("title").toString();
        assertTrue(actual.contains(Collection.class.getSimpleName()));
        assertTrue(actual.contains("\n"));
        assertTrue(actual.contains("title"));
    }
}
