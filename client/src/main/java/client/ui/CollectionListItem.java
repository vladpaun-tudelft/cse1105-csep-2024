package client.ui;

import client.controllers.CollectionCtrl;
import client.scenes.EditCollectionsCtrl;
import commons.Collection;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class CollectionListItem extends ListCell<Collection> {

    // List cell content
    private final Label collectionTitle;
    private final Region spacer;
    private final Button favouriteButton;
    private final HBox hBox;
    private TextField textField;

    private EditCollectionsCtrl controller;
    private CollectionCtrl collectionCtrl;

    public CollectionListItem(EditCollectionsCtrl controller) {

        this.controller = controller;

        // Initialize the note title
        collectionTitle = new Label();
        collectionTitle.setTextOverrun(OverrunStyle.ELLIPSIS);
        collectionTitle.setWrapText(false);
        collectionTitle.setMinWidth(0);

        // Initialize the favourite button
        favouriteButton = new Button();
        favouriteButton.getStyleClass().addAll("icon", "collection_list_icon", "set_default_collection_icon");
        favouriteButton.setCursor(Cursor.HAND);

        // Create layout
        hBox = new HBox(0);
        spacer = new Region();
        hBox.getChildren().addAll(collectionTitle, spacer, favouriteButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hBox.setAlignment(Pos.CENTER_LEFT);

        configureEventHandlers();
    }

    private void configureEventHandlers() {
        favouriteButton.setOnAction(event -> {
            Collection item = getItem();
            if (item != null && !item.equals(controller.getDefaultCollection())) {
                // Update default collection in DashboardCtrl and Config
                controller.setDefaultCollection(item);

                // Update UI
                controller.refreshDefaultCollection();
            }
        });
    }

    @Override
    protected void updateItem(Collection item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            hBox.getChildren().clear();
        }else {
            hBox.getChildren().clear();
            hBox.getChildren().addAll(collectionTitle, spacer, favouriteButton);

            collectionTitle.setText(item.title);

            // Update favorite button state
            // Disable favorite button if the collection is pending
            if (controller.addPending.contains(item)) {
                favouriteButton.setDisable(true);
                favouriteButton.getStyleClass().removeAll("default_collection_icon", "set_default_collection_icon");
            } else if (controller.getDefaultCollection() != null && controller.getDefaultCollection().equals(item)) {
                favouriteButton.setDisable(true);
                favouriteButton.getStyleClass().add("default_collection_icon");
                favouriteButton.getStyleClass().remove("set_default_collection_icon");

                collectionTitle.setText(collectionTitle.getText() + " - Default");
            } else {
                favouriteButton.setDisable(false);
                favouriteButton.getStyleClass().add("set_default_collection_icon");
                favouriteButton.getStyleClass().remove("default_collection_icon");
            }

            collectionTitle.maxWidthProperty().bind(controller.listView.widthProperty().subtract(10));
        }
        // Set the cell content
        setGraphic(hBox);
    }


}
