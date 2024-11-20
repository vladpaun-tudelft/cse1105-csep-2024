package commons;

import jakarta.persistence.*;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
public class Note {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;


    @Column(unique = true, nullable = false)
    private String title;

    @Lob
    public String body;

    @ManyToOne
    @JoinColumn(name = "collectionId", nullable = false)
    public commons.Collection collection;

    @SuppressWarnings("unused")
    private Note() {
        // for object mapper
    }

    public Note(String title, String body) {
        this.title = title;
        this.body = body;
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
        return ToStringBuilder.reflectionToString(this);
    }
}
