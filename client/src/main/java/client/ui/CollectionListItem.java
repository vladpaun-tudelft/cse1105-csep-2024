package client.ui;

import client.controllers.CollectionCtrl;
import client.scenes.EditCollectionsCtrl;
import commons.Collection;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class CollectionListItem extends ListCell<Collection> {

    // List cell content
    private final Label collectionTitle;
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

        // Create layout
        hBox = new HBox(0);
        hBox.getChildren().addAll(collectionTitle);
        hBox.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(Collection item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            collectionTitle.setText(null);
        }else {
            collectionTitle.setText(item.title);
            collectionTitle.maxWidthProperty().bind(controller.listView.widthProperty().subtract(10));
        }
        // Set the cell content
        setGraphic(hBox);
    }


}
