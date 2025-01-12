package commons;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;


@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id",
        scope = Note.class
)

public class Note {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;


    @Column(nullable = false)
    public String title;

    @Lob
    public String body;

    @ManyToOne
    @JoinColumn(name = "collection_id", nullable = false)
    public commons.Collection collection;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true/*, fetch = FetchType.EAGER*/)
    @JsonManagedReference  // required to prevent infinite recursion
    public List<EmbeddedFile> embeddedFiles;

    @SuppressWarnings("unused")
    private Note() {
        // for object mapper
    }

    public Note(String title, String body, Collection collection) {
        this.title = title;
        this.body = body;
        this.collection = collection;
        this.embeddedFiles = new ArrayList<>();
    }

    // region Getters and Setters
    public long getId() {
        return id;
    }

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

    /**
     * @return The list of embedded files for this note
     */
    public List<EmbeddedFile> getEmbeddedFiles() {
        return embeddedFiles;
    }

    /**
     * Sets the embedded files list of this note
     * @param embeddedFiles The list of embedded files
     */
    public void setEmbeddedFiles(List<EmbeddedFile> embeddedFiles) {
        this.embeddedFiles = embeddedFiles;
    }

    // endregion

    /**
     * Equals implementation for Note by id
     * @param obj Object to compare to
     * @return Result of the equals comparison
     */
    @Override
    public boolean equals(Object obj) {
        // return EqualsBuilder.reflectionEquals(this, obj);
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        Note that = (Note) obj;
        return Objects.equals(this.id, that.id);
    }

    /**
     * Hashcode implementation for Note
     * @return HashCode of this object
     */
    @Override
    public int hashCode() {
        // return HashCodeBuilder.reflectionHashCode(this);
        return Objects.hash(id);
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
