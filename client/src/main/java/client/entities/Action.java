package client.entities;

import commons.Note;

/**
 * Represents an undoable action.
 */
public class Action {
    private final ActionType type;
    private final Note note;
    private final Object previousState;
    private Object newState;

    @Override
    public String toString() {
        return "Action{" +
                "type='" + type + '\'' +
                ", note=" + note.title + ", " + note.collection.title +
                ", previousState=" + previousState +
                ", newState=" + newState +
                '}';
    }

    public Action(ActionType type, Note note, Object previousState, Object newState) {
        this.type = type;
        this.note = note;
        this.previousState = previousState;
        this.newState = newState;
    }

    public ActionType getType() {
        return type;
    }

    public Note getNote() {
        return note;
    }

    public Object getPreviousState() {
        return previousState;
    }

    public Object getNewState() {
        return newState;
    }

    public void setNewState(Object newState) {
        this.newState = newState;
    }
}
