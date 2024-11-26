package commons;

import jakarta.persistence.*;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

@Entity
public class Note {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;


    @Column(nullable = false)
    public String title;

    @Lob
    public String body;

    @ManyToOne
    // To be changed to nullable = false when we implement collections
    @JoinColumn(name = "collection_id", nullable = true)
    public commons.Collection collection;

    @SuppressWarnings("unused")
    private Note() {
        // for object mapper
    }

    public Note(String title, String body, Collection collection) {
        this.title = title;
        this.body = body;
        this.collection = collection;
    }

    // region Getters and Setters
    /**
     * Returns the title of this Note
     * @return Value of title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of this Note
     * @param title New value of title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the body of this Note
     * @return Value of body
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the body of this Note
     * @param body New value of title
     */
    public void setBody(String body) {
        this.body = body;
    }
    // endregion

    /**
     * Equals implementation for Note by id
     * @param obj Object to compare to
     * @return Result of the equals comparison
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if(obj == null || (this.getClass() != obj.getClass())) return false;
        return this.id == ((Note) obj).id;
    }

    /**
     * Hashcode implementation for Note
     * @return HashCode of this object
     */
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /**
     * Returns this Note as a human-readable String object
     * @return String containing information about this Note
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }
}
