package commons;

import jakarta.persistence.*;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

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
    @JoinColumn(name = "collection_id", nullable = false)
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

    public void setId(long id) {
        this.id = id;
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
