package client.scenes;

import com.google.inject.Inject;

import client.utils.ServerUtils;
import commons.Note;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controls all logic for the main dashboard.
 */
@SuppressWarnings("rawtypes")
public class DashboardCtrl implements Initializable {

    private final ServerUtils server;
    private final MainCtrl mainCtrl;

    @FXML
    private Label contentBlocker;
    @FXML
    private TextArea noteBody;
    @FXML
    private Label noteTitle;
    @FXML
    private ListView collectionView;
    @FXML
    private Button addButton;
    @FXML
    private VBox root;

    private ObservableList<Note> collectionNotes;

    private boolean pendingHideContentBlocker = true;

    @Inject
    public DashboardCtrl(ServerUtils server, MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
        this.server = server;
    }

    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {

        listViewSetup();
    }

    /**
     * Handles the current collection viewer setup
     */
    private void listViewSetup() {
        collectionNotes = FXCollections.observableArrayList(
                new Note("Note 1", "This is the body of Note 1.", null),
                new Note("Note 2", "This is the body of Note 2.", null),
                new Note("Lorem Ipsum", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam massa dolor, viverra eget lectus id, laoreet egestas sem. Cras facilisis sed sem nec posuere. Interdum et malesuada fames ac ante ipsum primis in faucibus. Vivamus eget egestas sem. Donec rhoncus aliquam finibus. Vestibulum sed nisl vitae arcu venenatis malesuada. Nulla a dignissim erat. Duis sodales faucibus luctus.\n" +
                        "\n" +
                        "Cras mattis dictum tempus. Sed et maximus mauris. Curabitur placerat urna non placerat convallis. Integer nec rutrum lacus. Nunc imperdiet quam eget ante tincidunt, vitae consequat urna pulvinar. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; In hac habitasse platea dictumst. Proin nec efficitur arcu, sed laoreet odio. Phasellus varius purus et odio lobortis pharetra a et nunc. Etiam ut iaculis velit, sit amet malesuada mi.\n" +
                        "\n" +
                        "Vestibulum in lacinia tortor. Vestibulum elementum sed nulla sed volutpat. Curabitur eleifend ornare tincidunt. Ut et leo vel mauris dignissim porta. Donec est eros, ullamcorper sed mi eu, lacinia tempus elit. Curabitur commodo eget metus semper hendrerit. Pellentesque magna orci, volutpat in justo nec, sollicitudin molestie lectus. Nulla finibus rhoncus luctus. Praesent dapibus ultricies purus. Integer varius ipsum a magna imperdiet, eget dictum felis euismod. Curabitur auctor ante sit amet pellentesque consectetur. Aliquam pretium porttitor porta.\n" +
                        "\n" +
                        "Pellentesque dignissim ac ligula nec finibus. Donec est nisi, tincidunt vel tellus nec, porta aliquet dui. Praesent id aliquet tortor. Aliquam risus massa, egestas in dolor aliquam, venenatis semper est. Fusce finibus, purus sit amet bibendum mollis, ipsum ipsum eleifend augue, quis egestas nunc felis nec velit. Curabitur imperdiet tellus at dictum eleifend. Vivamus id tristique lectus. Donec magna purus, mollis quis erat quis, iaculis porttitor enim.\n" +
                        "\n" +
                        "Donec rutrum ornare efficitur. Aliquam molestie tempus posuere. Curabitur vel lorem accumsan, elementum sem id, viverra velit. Etiam consectetur sapien a ante commodo, ut euismod elit aliquet. Donec sodales posuere dolor vel maximus. Ut et purus maximus, condimentum tortor in, consectetur odio. Aliquam venenatis ligula id consectetur aliquet. Nam iaculis fringilla faucibus. Praesent et neque quis lectus volutpat suscipit non ullamcorper velit.", null)
        );

        // Set required settings
        collectionView.setItems(collectionNotes);
        collectionView.setEditable(true);
        collectionView.setFixedCellSize(35);
        collectionView.setCellFactory(TextFieldListCell.forListView());

        // Set ListView entry as Title (editable)
        collectionView.setCellFactory(lv -> {
            TextFieldListCell<Note> cell = new TextFieldListCell<>();
            cell.converterProperty().set(new StringConverter<>() {
                @Override
                public String toString(Note note) {
                    return note != null ? note.getTitle() : "";
                }

                @Override
                public Note fromString(String string) {
                    // Create a new Note object or update an existing one based on the edited text
                    Note note = cell.getItem();
                    if (note != null) {
                        note.setTitle(string);
                    }
                    return note;
                }
            });
            cell.setOnMouseClicked(event -> {
                if (event != null) {
                    Note item = cell.getItem();
                    if(item != null) {
                        System.out.println("Cell selected: " + item.getTitle());
                        noteBody.setText((item).getBody());
                        noteTitle.setText((item).getTitle());
                        handleContentBlocker();
                    }
                }
            });
            return cell;
        });

        // Reset edit on click anywhere
        root.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if(!addButton.contains(event.getX(), event.getY())) {
                int selected = collectionView.getSelectionModel().getSelectedIndex();
                collectionView.getSelectionModel().clearSelection();
                collectionView.getSelectionModel().select(selected);
            }
        });

        // Remove content blocker on select
        collectionView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Note>() {

            @Override
            public void changed(ObservableValue<? extends Note> observable, Note oldValue, Note newValue) {
                pendingHideContentBlocker = (newValue!=null);
            }
        });

    }

    /**
     * Handles content blocker when new Note is loaded
     */
    private void handleContentBlocker() {
        pendingHideContentBlocker = !pendingHideContentBlocker;
        contentBlocker.setVisible(pendingHideContentBlocker);
    }

    public void addNote() {
        collectionNotes.add(new Note("New Note", "", null));
        collectionView.getSelectionModel().select(collectionNotes.size() - 1);
        collectionView.getFocusModel().focus(collectionNotes.size() - 1);
        collectionView.edit(collectionNotes.size() - 1);

        noteTitle.setText("New Note");
        noteBody.setText("");
    }

    @FXML
    public void onEditCommit() {
        Note currentNote = (Note)collectionView.getSelectionModel().getSelectedItem();
        noteTitle.setText(currentNote.getTitle());
    }

    @FXML
    public void onBodyChanged() {
        Note currentNote = (Note)collectionView.getSelectionModel().getSelectedItem();
        currentNote.setBody(noteBody.getText());
    }

}
