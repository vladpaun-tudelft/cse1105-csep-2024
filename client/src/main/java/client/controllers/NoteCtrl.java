package client.controllers;

import client.utils.ServerUtils;
import commons.Collection;
import commons.Note;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class NoteCtrl {

    private final ServerUtils server;

    public NoteCtrl(ServerUtils server) {
        this.server = server;
    }


    public void addNote(ToggleGroup collectionSelect,
                        RadioMenuItem allNotesButton, ObservableList<Note> collectionNotes,
                        List<Note> createPendingNotes, ListView collectionView,
                        Label noteTitle, Label noteTitle_md,
                        TextArea noteBody, Label contentBlocker) throws IOException {


        Collection collection;
        if (collectionSelect.getSelectedToggle().equals(allNotesButton)) {
            collection = server.getCollections().stream().filter(c -> c.title.equals("Default")).toList().getFirst();
        } else {
            RadioMenuItem selectedCollection = (RadioMenuItem) (collectionSelect.getSelectedToggle());
            collection = server.getCollections().stream().filter(c -> c.title.equals(selectedCollection.getText())).toList().getFirst();
        }
        Note newNote = new Note("New Note", "", collection);
        collectionNotes.add(newNote);
        // Add the new note to a list of notes pending being sent to the server
        createPendingNotes.add(newNote);
        System.out.println("Note added to createPendingNotes: " + newNote.getTitle());

        collectionView.getSelectionModel().select(collectionNotes.size() - 1);
        collectionView.getFocusModel().focus(collectionNotes.size() - 1);
        collectionView.edit(collectionNotes.size() - 1);

        noteTitle.setText("New Note");
        noteTitle_md.setText("New Note");

        noteBody.setText("");
        contentBlocker.setVisible(false);
    }

    public void showCurrentNote(ListView collectionView, Label noteTitle,
                                Label noteTitle_md, TextArea noteBody,
                                Label contentBlocker) {
        Note note = (Note) collectionView.getSelectionModel().getSelectedItem();
        if (note == null) return;
        noteTitle.setText(note.getTitle());
        noteTitle_md.setText(note.getTitle());
        noteBody.setText(note.getBody());
        contentBlocker.setVisible(false);
        Platform.runLater(() -> noteBody.requestFocus());
    }

    public void deleteSelectedNote(ListView collectionView, List<Note> filteredNotes,
                                   List<Note> createPendingNotes,
                                   ObservableList<Note> collectionNotes,
                                   TextArea noteBody, Label noteTitle,
                                   Label noteTitle_md, Label contentBlocker, List<Note> updatePendingNotes) {
        Note currentNote = (Note) collectionView.getSelectionModel().getSelectedItem();
        if (filteredNotes.contains(currentNote)) {
            filteredNotes.remove(currentNote);
            collectionView.setItems(FXCollections.observableArrayList(filteredNotes));
        }
        if (currentNote != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm deletion");
            alert.setContentText("Do you really want to delete this note?");
            Optional<ButtonType> buttonType = alert.showAndWait();
            if (buttonType.isPresent() && buttonType.get().equals(ButtonType.OK)) {
                if (createPendingNotes.contains(currentNote)) {
                    createPendingNotes.remove(currentNote);
                } else {
                    deleteNote(currentNote, collectionNotes, createPendingNotes, updatePendingNotes, contentBlocker);
                }
                collectionNotes.remove(currentNote);
                noteBody.clear();

                noteTitle.setText("");
                noteTitle_md.setText("");


                contentBlocker.setVisible(true);
                System.out.println("Note deleted: " + currentNote.getTitle());
                collectionView.getSelectionModel().clearSelection();
            }
        }
    }

    public void deleteNote(Note note, ObservableList<Note> collectionNotes,
                           List<Note> createPendingNotes,
                           List<Note> updatePendingNotes, Label contentBlocker) {
        collectionNotes.remove(note);
        createPendingNotes.remove(note);
        updatePendingNotes.remove(note);

        if (collectionNotes.isEmpty()) contentBlocker.setVisible(true);

        server.deleteNote(note.id);
    }

    public void saveAllPendingNotes(List<Note> createPendingNotes,
                                    List<Note> updatePendingNotes) {
        try {
            for (Note note : createPendingNotes) {
                Note savedNote = server.addNote(note);
                note.id = savedNote.id;
            }
            createPendingNotes.clear();

            for (Note note : updatePendingNotes) {
                server.updateNote(note);
            }
            updatePendingNotes.clear();
            System.out.println("Saved all notes on server");
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception to debug
        }
    }


}
