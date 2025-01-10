package client.scenes;

import client.controllers.CollectionCtrl;
import client.controllers.NoteCtrl;
import client.ui.CollectionListItem;
import client.ui.DialogStyler;
import client.utils.Config;
import client.utils.ServerUtils;
import commons.Collection;
import commons.Note;
import jakarta.ws.rs.ClientErrorException;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class EditCollectionsCtrl implements Initializable {

    private Collection currentCollection;
    private DashboardCtrl dashboardCtrl;

    @FXML private Label contentBlocker;
    @FXML private HBox menuBar;
    private double dragStartX, dragStartY;
    @FXML public ListView listView;
    private List<Collection> collectionList;

    @FXML private TextField titleField;
    @FXML private TextField serverField;

    @FXML private Label collectionStatusLabel;
    @FXML private Label oldTitleLabel;
    @FXML private Label oldServerLabel;

    @FXML private Button forgetButton;
    @FXML private Button deleteButton;
    @FXML private Button saveButton;
    @FXML private Button createButton;
    @FXML private Button connectButton;

    private List<Collection> addPending;
    private ObservableList<Collection> knownCollections;

    private boolean isMigration = false; // Tracks if a migration is needed

    private Stage stage;
    private CollectionCtrl collectionCtrl;
    private ServerUtils serverUtils;

    private PauseTransition debounceTimer = new PauseTransition(Duration.millis(500));
    private NoteCtrl noteCtrl;
    private Config config;
    private DialogStyler dialogStyler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDraggableWindow();
        titleField.textProperty().addListener((observable, oldValue, newValue) -> onInputChanged());
        serverField.textProperty().addListener((observable, oldValue, newValue) -> onInputChanged());

    }

    public void setReferences(Stage stage, CollectionCtrl collectionCtrl,
                              DashboardCtrl dashboardCtrl,
                              ServerUtils serverUtils,
                              NoteCtrl noteCtrl,
                              Config config,
                              DialogStyler dialogStyler) { // Call this from the main controller
        this.stage = stage;
        this.collectionCtrl = collectionCtrl;
        this.dashboardCtrl = dashboardCtrl;
        this.serverUtils = serverUtils;
        this.noteCtrl = noteCtrl;
        this.config = config;
        this.dialogStyler = dialogStyler;
    }

    public void setCollectionList(List<Collection> collectionList) {
        this.collectionList = collectionList;
        addPending = new ArrayList<>();
        knownCollections = FXCollections.observableArrayList(new ArrayList<>(collectionList));
        setupListView();
    }

    private void setupListView() {

        // Set required settings
        listView.setItems(knownCollections);
        listView.setEditable(true);
        listView.setFixedCellSize(35);
        listView.setCellFactory(TextFieldListCell.forListView());

        listView.setCellFactory(lv -> new CollectionListItem(this));

        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                currentCollection = (Collection) newValue;
                showCurrentCollection();
                contentBlocker.setVisible(false);
            } else {
                // Show content blockers when no item is selected
                contentBlocker.setVisible(true);
            }
        });
        listView.getSelectionModel().clearSelection();
        contentBlocker.setVisible(true);
    }

    @FXML
    public void addCollection() {
        Collection newCollection = new Collection("New Collection", "");
        addPending.add(newCollection);
        currentCollection = newCollection;
        listView.getItems().add(newCollection);
        listView.getSelectionModel().select(newCollection);
        titleField.requestFocus();
    }

    @FXML public void createCollection() {
        addPending.remove(currentCollection);

        currentCollection.title = titleField.getText().trim();
        currentCollection.serverURL = serverField.getText().trim();
        dashboardCtrl.addCollection(currentCollection);

        refreshListView(); // Refresh ListView to reflect updates

        collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
        collectionStatusLabel.setText("Collection added successfully!");

        Platform.runLater(this::onExitCollections);
    }

    @FXML
    public void saveCollection() {
        if (currentCollection == null || !checkStatus()) return;

        String newTitle = titleField.getText().trim();
        String newServerURL = serverField.getText().trim();

        if (isMigration) {
            // Prompt user for confirmation of migration
            if (!dialogStyler.createStyledAlert(
                    Alert.AlertType.CONFIRMATION,
                    "Migrate Collection",
                    "Migrate Notes",
                    "Changing the server will migrate all notes to the new server. Are you sure?"
            ).showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
                return;
            }

            migrateCollection(currentCollection, newServerURL);
        } else {
            // Standard save logic
            currentCollection.title = newTitle;
            currentCollection.serverURL = newServerURL;

            collectionCtrl.updateCollection(currentCollection, collectionList);
            dashboardCtrl.refresh();
            refreshListView();

            collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
            collectionStatusLabel.setText("Collection saved successfully!");
        }
    }

    private void migrateCollection(Collection collection, String newServerURL) {
        // Fetch all notes from the old server
        List<Note> notesToMigrate = dashboardCtrl.getAllNotes().stream()
                .filter(note -> note.collection.equals(collection))
                .collect(Collectors.toList());

        // Delete notes from the old server
        for (Note note : notesToMigrate) {
            noteCtrl.deleteNote(note, FXCollections.observableArrayList(notesToMigrate), dashboardCtrl.getAllNotes());
        }

        // Delete collection from old server
        serverUtils.deleteCollection(collection);

        // Update collection details for the new server
        collection.serverURL = newServerURL;
        serverUtils.addCollection(collection);
        collection.id = serverUtils.getCollectionID(collection);
        config.writeAllToFile(collectionList);

        // Add notes to the new server
        for (Note note : notesToMigrate) {
            try {
                Note savedNote = serverUtils.addNote(note);
                note.id = savedNote.id;
            } catch (ClientErrorException e) {
                System.out.println(e.getResponse().readEntity(String.class));
            }


        }

        collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
        collectionStatusLabel.setText("Collection and notes migrated successfully!");
        oldServerLabel.setText(newServerURL);
    }

    @FXML
    public void deleteCollection() {
        if (currentCollection == null) return;

        if (!dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                "Delete collection",
                "Delete collection",
                "Are you sure you want to delete this collection? All notes in the collection will be deleted as well."
        ).showAndWait().filter(b -> b == ButtonType.OK).isPresent()) return;

        dashboardCtrl.setAllNotes(
                FXCollections.observableArrayList(
                        dashboardCtrl.getAllNotes().stream().filter(note ->
                                !note.collection.title.equals(currentCollection.title)
                        ).collect(Collectors.toList())
                )
        );

        List<Note> notesToDelete = dashboardCtrl.getAllNotes().stream()
                .filter(note -> note.collection.equals(currentCollection)).collect(Collectors.toList());
        for (Note n : notesToDelete) {
            noteCtrl.deleteNote(n, FXCollections.observableArrayList(notesToDelete), dashboardCtrl.getAllNotes());
        }
        // delete collection from server
        serverUtils.deleteCollection(currentCollection);

        collectionList.remove(currentCollection);
        knownCollections.remove(currentCollection);

        dashboardCtrl.setCurrentCollection(null);
        dashboardCtrl.viewAllNotes();

        // delete collection from config file
        config.writeAllToFile(collectionList);

        refreshListView();
    }
    @FXML
    public void forgetCollection() {
        if (currentCollection == null) return;

        if (!dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                "Forget collection",
                "Forget collection",
                "Are you sure you want to forget this collection?" +
                        "\nYou will lose access to it's notes, but may reconnect to it later."
        ).showAndWait().filter(b -> b == ButtonType.OK).isPresent()) return;

        dashboardCtrl.setAllNotes(
                FXCollections.observableArrayList(
                    dashboardCtrl.getAllNotes().stream().filter(note ->
                            !note.collection.title.equals(currentCollection.title)
                    ).collect(Collectors.toList())
                )
        );

        collectionList.remove(currentCollection);

        knownCollections.remove(currentCollection);
        refreshListView();

        dashboardCtrl.setCurrentCollection(null);
        dashboardCtrl.viewAllNotes();


        // delete collection from config file
        config.writeAllToFile(collectionList);
    }

    @FXML
    public void connectToCollection() {
        if (currentCollection == null) return;
        if (!dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                "Connect to collection",
                "Connect to collection",
                "Are you sure you want to connect to this collection? All notes in the collection will be copied as well."
        ).showAndWait().filter(b -> b == ButtonType.OK).isPresent()) return;

        dashboardCtrl.conectToCollection(
                serverUtils.getCollectionsOnServer(serverField.getText().trim()).stream()
                        .filter(collection -> collection.title.equals(titleField.getText().trim()))
                        .findFirst().get()
        );

        Platform.runLater(this::onExitCollections);
    }


    @FXML
    private void onInputChanged() {
        debounceTimer.setOnFinished(event -> checkStatus());
        debounceTimer.playFromStart();
    }

    private boolean checkStatus() {
        if (currentCollection == null) return false;

        titleField.setEditable(true);

        String newTitle = titleField.getText().trim();
        String newServerUrl = serverField.getText().trim();

        collectionStatusLabel.setTextFill(Paint.valueOf("RED"));

        List<String> collectionNames = collectionList.stream()
                .map(collection -> collection.title)
                .collect(Collectors.toList());

        if (newTitle.isBlank()) {
            collectionStatusLabel.setText("Collection name cannot be blank.");
            createButton.setDisable(true);
            saveButton.setDisable(true);
            connectButton.setDisable(true);
            return false;
        }

        if (!serverUtils.isServerAvailable(newServerUrl)) {
            collectionStatusLabel.setText("Server is unreachable.");
            saveButton.setDisable(true);
            connectButton.setDisable(true);
            createButton.setDisable(true);
            return false;
        }

        List<String> collectionNamesOnServer = serverUtils.getCollectionsOnServer(newServerUrl)
                .stream().map(collection -> collection.title)
                .filter(name -> !name.equals(oldTitleLabel.getText()))
                .toList();

        if (addPending.contains(currentCollection)) return validateCreation(collectionNames, collectionNamesOnServer, newTitle, newServerUrl);
        else return validateModification(collectionNamesOnServer, newTitle, newServerUrl);

    }

    private boolean validateModification(List<String> collectionNamesOnServer, String newTitle, String newServerUrl) {

        // Check if server URL has changed for existing collections
        if (!newServerUrl.equals(oldServerLabel.getText())) {
            collectionNamesOnServer = serverUtils.getCollectionsOnServer(newServerUrl)
                    .stream().map(collection -> collection.title)
                    .toList();

            titleField.setText(currentCollection.title);
            titleField.setEditable(false);

            if (collectionNamesOnServer.contains(newTitle)) {
                collectionStatusLabel.setTextFill(Paint.valueOf("ORANGE"));
                collectionStatusLabel.setText("A collection with this name already exists on the new server. Edit name before migrating.");
                saveButton.setDisable(true);
                return false;
            } else {
                isMigration = true; // Mark as migration
                collectionStatusLabel.setTextFill(Paint.valueOf("ORANGE"));
                collectionStatusLabel.setText("Server has changed. Saving will migrate notes to the new server.");
                saveButton.setDisable(false);
                return true;
            }
        } else {
            isMigration = false;
        }

        if (collectionNamesOnServer.contains(newTitle)) {
            collectionStatusLabel.setText("Collection already exists on the server. Cannot change name to this.");
            saveButton.setDisable(true);
            return false;
        } else if (newTitle.equals(oldTitleLabel.getText()) && newServerUrl.equals(oldServerLabel.getText())) {
            collectionStatusLabel.setText("Nothing changed.");
            saveButton.setDisable(true);
            return false;
        } else {
            collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
            collectionStatusLabel.setText("Collection can be saved under new name.");
            saveButton.setDisable(false);
            return true;
        }
    }

    private boolean validateCreation(List<String> collectionNames, List<String> collectionNamesOnServer, String newTitle, String newServerUrl) {
        if (collectionNames.contains(newTitle)) {
            collectionStatusLabel.setTextFill(Paint.valueOf("RED"));
            collectionStatusLabel.setText("Collection already exists on the server. You cannot create it.");
            createButton.setDisable(true);

            return false;
        } else if (collectionNamesOnServer.contains(newTitle)) {
            createButton.setDisable(true);
            connectButton.setDisable(false);
            collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
            collectionStatusLabel.setText("Collection exists on the server. You can connect to it.");

            return true;
        } else {
            createButton.setDisable(false);
            connectButton.setDisable(true);
            collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
            collectionStatusLabel.setText("Collection can be created.");
            return true;
        }
    }

    // This method shows the current collection as well as handling logic for what you can do with said collection
    private void showCurrentCollection() {
        if (currentCollection == null) return;

        Collection oldCollection = collectionList.stream().filter(collection -> collection.title.equals(currentCollection.title))
                .findFirst().orElse(null);
        if (oldCollection == null) {
            oldTitleLabel.setText("This is a new collection.");
            oldServerLabel.setText("This is a new collection.");
        } else {
            oldTitleLabel.setText(oldCollection.title);
            oldServerLabel.setText(oldCollection.serverURL);
        }

        titleField.setText(currentCollection.title);
        serverField.setText(currentCollection.serverURL);

        if (addPending.contains(currentCollection)) {
            forgetButton.setDisable(true);
            deleteButton.setDisable(true);
            saveButton.setDisable(true);
            connectButton.setDisable(false);
            createButton.setDisable(false);
        } else {
            forgetButton.setDisable(false);
            deleteButton.setDisable(false);
            saveButton.setDisable(false);
            connectButton.setDisable(true);
            createButton.setDisable(true);
        }

        checkStatus();
    }

    private void refreshListView() {
        listView.setItems(null);
        listView.setItems(knownCollections);
    }



    private void setupDraggableWindow() {
        menuBar.setOnMousePressed(event -> {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
        });

        menuBar.setOnMouseDragged(event -> {
            if (stage != null) { // Ensure stage is set
                stage.setX(event.getScreenX() - dragStartX);
                stage.setY(event.getScreenY() - dragStartY);
            }
        });
    }

    @FXML
    public void onExitCollections()
    {
        if (stage != null) {
            stage.close(); // Close the popup window
        }
    }
}
