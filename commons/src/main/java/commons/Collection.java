package commons;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id",
        scope = Collection.class
)

public class Collection {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;

    @Column(unique = true, nullable = false)
    public String title;

    @Column(nullable = false)
    public String serverURL;

//    @OneToMany(mappedBy = "collection",cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OneToMany(mappedBy = "collection", fetch = FetchType.EAGER)
    public Set<Note> notes;

    @SuppressWarnings("unused")
    private Collection() {
    }

    public Collection(String title, String serverURL) {
        this.title = title;
        this.serverURL = serverURL;
        this.notes = new HashSet<>();
    }

    @Override
    public boolean equals(Object obj) {
        // return EqualsBuilder.reflectionEquals(this, obj);
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        Collection that = (Collection) obj;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        // return HashCodeBuilder.reflectionHashCode(this);
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }

}
