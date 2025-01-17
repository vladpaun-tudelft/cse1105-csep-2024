package client.controllers;

import client.services.TagService;
import commons.Note;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls logic for tag management.
 */
public class TagCtrl {

    private HBox tagsBox;
    private ObservableList<Note> allNotes;

    private TagService tagService;

    public TagCtrl() {
        this.tagService = new TagService();
    }

    public void setReferences(HBox tagsBox, ObservableList<Note> allNotes) {
        this.tagsBox = tagsBox;
        this.allNotes = allNotes;

        ComboBox<String> initialComboBox = createTagComboBox();
        tagsBox.getChildren().add(1, initialComboBox);
        initialComboBox.setOnAction(event -> onTagSelectionChanged(initialComboBox));

        updateTagList();
    }

    /**
     * Clears the tag list by removing all the tag dropdowns except the initial one.
     * It resets all tag selections and the initial combo box is added back to the list.
     */
    @FXML
    public void clearTags() {
        // keep the label and button and clear only the combo boxes
        List<javafx.scene.Node> children = tagsBox.getChildren();
        children.removeIf(node -> node instanceof ComboBox);

        // Add the initial combo box back
        ComboBox<String> initialComboBox = createTagComboBox();
        tagsBox.getChildren().add(1, initialComboBox);
        initialComboBox.setOnAction(event -> onTagSelectionChanged(initialComboBox));

        updateTagList();
    }

    /**
     * Updates the tag list displayed for each dropdown whenever the content of any note changes.
     * It ensures that each combo box reflects only the available tags, i.e.,
     * tags that haven't yet been selected.
     * <p>
     * The method also re selects the previously selected tags if they are still available
     */
    public void updateTagList() {
        List<String> uniqueTags = tagService.getUniqueTags(allNotes);
        List<String> selectedTags = new ArrayList<>();

        for (int i = 0; i < tagsBox.getChildren().size(); i++) {
            if (tagsBox.getChildren().get(i) instanceof ComboBox) {
                ComboBox<String> comboBox = (ComboBox<String>) tagsBox.getChildren().get(i);

                String selectedItem = comboBox.getSelectionModel().getSelectedItem();

                List<String> availableTags = new ArrayList<>(uniqueTags);
                availableTags.removeAll(selectedTags);

                comboBox.getItems().clear();
                comboBox.getItems().addAll(availableTags);

                // reselect the previously selected item if it's still in the new list
                if (selectedItem != null && uniqueTags.contains(selectedItem)) {
                    comboBox.getSelectionModel().select(selectedItem);
                }

                // exclude this selected tag from the next dropdowns
                if (selectedItem != null) {
                    selectedTags.add(selectedItem);
                }
            }
        }
    }

    private void onTagSelectionChanged(ComboBox<String> comboBox) {
        if (isLastDropdown(comboBox) && comboBox.getSelectionModel().getSelectedItem() != null) {
            if (isLastDropdown(comboBox)) {
                addTagDropdown(comboBox);
            }
            updateTagList();
        }
    }

    private boolean isLastDropdown(ComboBox<String> dropdown) {
        return tagsBox.getChildren().indexOf(dropdown) == tagsBox.getChildren().size() - 2;
    }

    @FXML
    private void addTagDropdown(ComboBox<String> triggeringDropdown) {
        ComboBox<String> newDropdown = createTagComboBox();
        updateTagList();

        int index = tagsBox.getChildren().indexOf(triggeringDropdown) + 1;
        tagsBox.getChildren().add(index, newDropdown);
        newDropdown.setOnAction(event -> onTagSelectionChanged(newDropdown));
    }

    private static ComboBox<String> createTagComboBox() {
        ComboBox<String> newDropdown = new ComboBox<>();
        newDropdown.setPrefWidth(200.0);
        newDropdown.setPromptText("Select a tag");
        newDropdown.getStyleClass().add("tag-dropdown");

        Tooltip dropdownTooltip = new Tooltip("Select a tag from the list or type to add a new one.");
        newDropdown.setTooltip(dropdownTooltip);
        return newDropdown;
    }
}
