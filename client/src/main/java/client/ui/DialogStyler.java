package client.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;

public class DialogStyler {

    private final String stylesheet = "/css/styles.css";

    /**
     * Constructor to initialize the dialog styler with a specific stylesheet.
     *
     */
    public DialogStyler() {
    }

    /**
     * Applies the styles to a given dialog pane.
     *
     * @param dialogPane The dialog pane to style
     */
    public void styleDialog(DialogPane dialogPane) {
        dialogPane.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog"); // Optional custom style class
    }

    /**
     * Creates a styled Alert.
     *
     * @param alertType   The type of the alert
     * @param title       The title of the alert
     * @param headerText  The header text of the alert
     * @param contentText The content text of the alert
     * @return A styled Alert instance
     */
    public Alert createStyledAlert(Alert.AlertType alertType, String title, String headerText, String contentText) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        styleDialog(alert.getDialogPane());
        return alert;
    }

    /**
     * Creates a styled TextInputDialog.
     *
     * @param title       The title of the dialog
     * @param headerText  The header text of the dialog
     * @param contentText The content text of the dialog
     * @return A styled TextInputDialog instance
     */
    public TextInputDialog createStyledTextInputDialog(String title, String headerText, String contentText) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);
        styleDialog(dialog.getDialogPane());
        return dialog;
    }
}

