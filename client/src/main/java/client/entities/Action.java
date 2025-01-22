package client.entities;

import commons.EmbeddedFile;
import commons.Note;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

/**
 * Represents an undoable action.
 */
public class Action {
    @Getter @Setter private final ActionType type;
    @Getter @Setter private final Note note;
    @Getter @Setter private final Object previousState;
    @Getter @Setter private Object newState;
    @Getter @Setter private Optional<EmbeddedFile> embeddedFile;

    @Override
    public String toString() {
        return "Action{" +
                "type='" + type + '\'' +
                ", note={" + note.title + ", " + note.collection.title + "}" +
                ", previousState=" + previousState +
                ", newState=" + newState +
                '}';
    }

    public Action(ActionType type, Note note, Object previousState, Object newState) {
        this.type = type;
        this.note = note;
        this.previousState = previousState;
        this.newState = newState;
        this.embeddedFile = Optional.empty();
    }
    public Action(ActionType type, Note note, Object previousState, Object newState, EmbeddedFile embeddedFile) {
        this.type = type;
        this.note = note;
        this.previousState = previousState;
        this.newState = newState;
        this.embeddedFile = Optional.of(embeddedFile);
    }

}
