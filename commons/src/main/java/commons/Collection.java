package commons;

import jakarta.persistence.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

@Entity
public class Collection {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;

    @Column(unique = true, nullable = false)
    public String title;

    // Refined the annotation here.
    //  mapped by specifies the field in the child entity (Note) that maps the relationship.
    // we can just use cascadeType.ALL for all the cascading types.
    // with orphanRemoval, if a Note is removed from a collection, it is deleted from the database.
    @OneToMany(mappedBy = "collection",cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<Note> notes;

    @SuppressWarnings("unused")
    private Collection() {
    }

    public Collection(String title) {
        this.title = title;
        this.notes = new HashSet<>();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }


}
