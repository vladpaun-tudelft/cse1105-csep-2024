package client.entities;

import commons.Note;

/**
 * Represents an undoable action.
 */
public class Action {
    private final ActionType type;
    private final Note note;
    private final Object previousState;
    private final Object previousState2;
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

    public Action(ActionType type, Note note, Object previousState, Object previousState2,  Object newState) {
        this.type = type;
        this.note = note;
        this.previousState = previousState;
        this.previousState2 = previousState2;
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
    public Object getPreviousState2() {
        return previousState2;
    }

    public Object getNewState() {
        return newState;
    }

    public void setNewState(Object newState) {
        this.newState = newState;
    }
}
