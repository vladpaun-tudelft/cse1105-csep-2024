package client.scenes;

import client.LanguageManager;
import client.controllers.CollectionCtrl;
import client.controllers.NoteCtrl;
import client.ui.CollectionListItem;
import client.ui.DialogStyler;
import client.utils.Config;
import client.utils.ServerUtils;
import commons.Collection;
import commons.Note;
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
    @FXML private Button addButton;

    public List<Collection> addPending;
    private ObservableList<Collection> knownCollections;

    private boolean isMigration = false; // Tracks if a migration is needed

    private Stage stage;
    private CollectionCtrl collectionCtrl;
    private ServerUtils serverUtils;

    private PauseTransition debounceTimer = new PauseTransition(Duration.millis(500));
    private NoteCtrl noteCtrl;
    private Config config;
    private LanguageManager languageManager;
    private ResourceBundle bundle;
    private DialogStyler dialogStyler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDraggableWindow();
        titleField.textProperty().addListener((observable, oldValue, newValue) -> onInputChanged());
        serverField.textProperty().addListener((observable, oldValue, newValue) -> onInputChanged());
        this.languageManager = LanguageManager.getInstance(this.config);
        this.bundle = this.languageManager.getBundle();
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
        if (createButton.isDisabled()) return;
        addPending.remove(currentCollection);

        currentCollection.title = titleField.getText().trim();
        currentCollection.serverURL = serverField.getText().trim();
        dashboardCtrl.addCollection(currentCollection);

        refreshListView(); // Refresh ListView to reflect updates

        collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
        collectionStatusLabel.setText(bundle.getString("collectionAddedSuccessfully.text"));

        Platform.runLater(this::onExitCollections);
    }

    @FXML
    public void saveCollection() {
        if (saveButton.isDisabled()) return;
        if (currentCollection == null || !checkStatus()) return;

        String newTitle = titleField.getText().trim();
        String newServerURL = serverField.getText().trim();

        if (isMigration) {
            // Prompt user for confirmation of migration
            if (!dialogStyler.createStyledAlert(
                    Alert.AlertType.CONFIRMATION,
                    bundle.getString("migrateNotes.text"),
                    bundle.getString("migrateNotes.text"),
                    bundle.getString("migrateNotesConfirmation.text")
            ).showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
                return;
            }

            migrateCollection(currentCollection, newServerURL);
        } else {
            // Standard save logic
            String oldCollectionTitile = currentCollection.title;
            currentCollection.title = newTitle;
            currentCollection.serverURL = newServerURL;

            collectionCtrl.updateCollection(currentCollection, collectionList, oldCollectionTitile);
            dashboardCtrl.refresh();
            refreshListView();

            collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
            collectionStatusLabel.setText(bundle.getString("collectionSaved.text"));
        }
    }

    private void migrateCollection(Collection collection, String newServerURL) {
        try {
            Collection collectionCopy = new Collection(collection.title, newServerURL);
            collectionCopy.id = collection.id;

            serverUtils.addCollection(collectionCopy);
            collectionCopy.id = serverUtils.getCollectionID(collectionCopy);

            // Check if the server is available
            if (serverUtils.isServerAvailable(newServerURL)) {
                serverUtils.getWebSocketURL(newServerURL);

                // Register for updates on this server
                dashboardCtrl.noteAdditionSync(newServerURL);
                dashboardCtrl.noteTitleSync(newServerURL);
                dashboardCtrl.noteDeletionSync(newServerURL);
            }

            // Fetch all notes from the old server
            List<Note> notesToMigrate = dashboardCtrl.getAllNotes().stream()
                    .filter(note -> note.collection.equals(collection))
                    .collect(Collectors.toList());

            for (Note note : notesToMigrate) {
                note.collection = collectionCopy;
                collectionCtrl.moveNote(note, collectionCopy);
            }

            // Delete collection from old server
            serverUtils.deleteCollection(collection);

            collection.serverURL = collectionCopy.serverURL;
            collection.title = collectionCopy.title;
            collection.id = collectionCopy.id;

            config.writeAllToFile(collectionList);

            collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
            collectionStatusLabel.setText(bundle.getString("migratedSuccessfully.text"));
            oldServerLabel.setText(newServerURL);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    public void deleteCollection() {
        if (deleteButton.isDisabled()) return;
        if (currentCollection == null) return;
        if(dashboardCtrl.getCollectionCtrl().showDeleteConfirmation()) {
            dashboardCtrl.getCollectionCtrl().removeCollectionFromClient(
                    true,
                    currentCollection,
                    dashboardCtrl.getCollections(),
                    dashboardCtrl.getCollectionNotes(),
                    dashboardCtrl.getAllNotes()
            );

            knownCollections.remove(currentCollection);
            currentCollection = null;
            dashboardCtrl.viewAllNotes();

            refreshListView();
        }
        dashboardCtrl.getActionHistory().clear();
    }
    @FXML
    public void forgetCollection() {
        if (forgetButton.isDisabled()) return;
        if (currentCollection == null) return;

        if (dashboardCtrl.getCollectionCtrl().showForgetConfirmation()) {
            dashboardCtrl.getCollectionCtrl().removeCollectionFromClient(
                    false,
                    currentCollection,
                    dashboardCtrl.getCollections(),
                    dashboardCtrl.getCollectionNotes(),
                    dashboardCtrl.getAllNotes()
            );

            knownCollections.remove(currentCollection);
            currentCollection = null;
            dashboardCtrl.viewAllNotes();

            refreshListView();
        }

    }

    @FXML
    public void connectToCollection() {
        if (connectButton.isDisabled()) return;
        if (currentCollection == null) return;
        if (!dialogStyler.createStyledAlert(
                Alert.AlertType.CONFIRMATION,
                bundle.getString("connectToCollection.text"),
                bundle.getString("connectToCollection.text"),
                bundle.getString("connectToCollectionConfirmation.text")
        ).showAndWait().filter(b -> b == ButtonType.OK).isPresent()) return;

        dashboardCtrl.connectToCollection(
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
            collectionStatusLabel.setText(bundle.getString("blankCollectionName.text"));
            createButton.setDisable(true);
            saveButton.setDisable(true);
            connectButton.setDisable(true);
            return false;
        }

        if (!serverUtils.isServerAvailable(newServerUrl)) {
            collectionStatusLabel.setText(bundle.getString("serverUnreachable.text"));
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
                collectionStatusLabel.setText(bundle.getString("collectionNameExists.text"));
                saveButton.setDisable(true);
                return false;
            } else {
                isMigration = true; // Mark as migration
                collectionStatusLabel.setTextFill(Paint.valueOf("ORANGE"));
                collectionStatusLabel.setText(bundle.getString("serverChanged.text"));
                saveButton.setDisable(false);
                return true;
            }
        } else {
            isMigration = false;
        }

        if (collectionNamesOnServer.contains(newTitle)) {
            collectionStatusLabel.setText(bundle.getString("collectionAlreadyExists.text"));
            saveButton.setDisable(true);
            return false;
        } else if (newTitle.equals(oldTitleLabel.getText()) && newServerUrl.equals(oldServerLabel.getText())) {
            collectionStatusLabel.setText(bundle.getString("nothingChanged.text"));
            saveButton.setDisable(true);
            return false;
        } else {
            collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
            collectionStatusLabel.setText(bundle.getString("saveUnderNewName.text"));
            saveButton.setDisable(false);
            return true;
        }
    }

    private boolean validateCreation(List<String> collectionNames, List<String> collectionNamesOnServer, String newTitle, String newServerUrl) {
        if (collectionNames.contains(newTitle)) {
            collectionStatusLabel.setTextFill(Paint.valueOf("RED"));
            collectionStatusLabel.setText(bundle.getString("youCannotCreateIt.text"));
            createButton.setDisable(true);

            return false;
        } else if (collectionNamesOnServer.contains(newTitle)) {
            createButton.setDisable(true);
            connectButton.setDisable(false);
            collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
            collectionStatusLabel.setText(bundle.getString("cannotConnectToIt.text"));

            return true;
        } else {
            createButton.setDisable(false);
            connectButton.setDisable(true);
            collectionStatusLabel.setTextFill(Paint.valueOf("GREEN"));
            collectionStatusLabel.setText(bundle.getString("collectionCanBeCreated.text"));
            return true;
        }
    }

    // This method shows the current collection as well as handling logic for what you can do with said collection
    private void showCurrentCollection() {
        if (currentCollection == null) return;

        Collection oldCollection = collectionList.stream().filter(collection -> collection.title.equals(currentCollection.title))
                .findFirst().orElse(null);
        if (oldCollection == null) {
            oldTitleLabel.setText(bundle.getString("thisIsANewCollection.text"));
            oldServerLabel.setText(bundle.getString("thisIsANewCollection.text"));
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
        currentCollection = null;
        listView.getSelectionModel().clearSelection();
        if (stage != null) {
            stage.close(); // Close the popup window
        }
    }

    public Collection getDefaultCollection() {
        return dashboardCtrl.getDefaultCollection();
    }

    public void setDefaultCollection(Collection item) {
        dashboardCtrl.setDefaultCollection(item);
    }

    public void refreshDefaultCollection() {
        Platform.runLater(() -> {listView.refresh();});
    }

    public void refreshTreeView() {
        Platform.runLater(() -> dashboardCtrl.refreshTreeView());
    }


    // ----------------------- HCI - Keyboard shortcuts -----------------------

    /**
     * DOWN ARROW - cycles through collections
     */
    public void selectNextCollection() {
        selectCollectionInDirection(1);
    }

    /**
     * ALT + UP ARROW - cycles through notes
     */
    public void selectPreviousCollection() {
        selectCollectionInDirection(-1);
    }

    /**
     * Selects the next JFX element.
     */
    public void selectNextJFXElement() {
        selectJFXElement(1);
    }

    /**
     * Selects the previous JFX element.
     */
    public void selectPreviousJFXElement() {
        selectJFXElement(-1);
    }

    /**
     * Handles navigation between UI elements in forward or backward direction.
     *
     * @param direction 1 for forward, -1 for backward
     */
    public void selectJFXElement(int direction) {

        if (currentCollection == null) {
            selectCollectionInDirection(direction);
            return;
        }

        // List of focusable elements in order
        List<Control> focusableElements = Arrays.asList(
                titleField,
                serverField,
                forgetButton,
                deleteButton,
                saveButton,
                connectButton,
                createButton
        );

        // Get the currently focused element
        Control focusedElement = (Control) focusableElements.stream()
                .filter(Control::isFocused)
                .findFirst()
                .orElse(null);

        // Find the index of the focused element
        int currentIndex = focusableElements.indexOf(focusedElement);

        // Loop through the elements in the specified direction
        for (int i = 1; i <= focusableElements.size(); i++) {
            int nextIndex = (currentIndex + (i * direction) + focusableElements.size()) % focusableElements.size();
            Control nextElement = focusableElements.get(nextIndex);

            // If the element is not disabled, request focus
            if (!nextElement.isDisabled()) {
                nextElement.requestFocus();
                break;
            }
        }
    }


    /**
     * Selects the next or previous collection in the list and handles edge cases.
     *
     * @param direction 1 for next, -1 for previous
     */
    public void selectCollectionInDirection(int direction) {
        if (knownCollections.isEmpty()) return; // No collections to navigate

        if (currentCollection == null) {
            if (addButton.isFocused()) {
                // Navigate from addButton to the first or last collection
                if (direction > 0) {
                    currentCollection = knownCollections.getFirst();
                } else {
                    currentCollection = knownCollections.getLast();
                }
                listView.getSelectionModel().select(currentCollection);
                titleField.requestFocus();
            } else {
                // If no collection is selected and addButton is not focused
                if (direction > 0) {
                    currentCollection = knownCollections.getFirst();
                    listView.getSelectionModel().select(currentCollection);
                } else {
                    addButton.requestFocus();
                }
            }
        } else {
            int currentIndex = knownCollections.indexOf(currentCollection);
            int nextIndex = currentIndex + direction;

            if (nextIndex >= knownCollections.size()) {
                // If navigating past the last collection, focus the addButton
                addButton.requestFocus();
                currentCollection = null;
                listView.getSelectionModel().clearSelection();
            } else if (nextIndex < 0) {
                // If navigating before the first collection, focus the addButton
                addButton.requestFocus();
                currentCollection = null;
                listView.getSelectionModel().clearSelection();
            } else {
                // Navigate to the next or previous collection
                currentCollection = knownCollections.get(nextIndex);
                listView.getSelectionModel().select(currentCollection);
            }
        }
    }



}
