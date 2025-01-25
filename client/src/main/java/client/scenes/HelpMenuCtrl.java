package client.scenes;

import client.LanguageManager;
import client.controllers.MarkdownCtrl;
import client.utils.Config;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class HelpMenuCtrl implements Initializable {

    @FXML private HBox menuBar;
    private double dragStartX, dragStartY;
    @FXML private WebView webView;
    private Stage stage;

    private Config config;
    private LanguageManager languageManager;
    private ResourceBundle bundle;
    private MarkdownCtrl markdownCtrl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.languageManager = LanguageManager.getInstance(this.config);
        this.bundle = this.languageManager.getBundle();
    }
    public void setReferences(Stage stage, MarkdownCtrl markdownCtrl) {
        this.stage = stage;
        this.markdownCtrl = markdownCtrl;
        setupDraggableWindow();
        addText();
    }

    private void addText() {
        try {
            String path = "HELP_" +
                switch (languageManager.getCurrentLanguage()) {
                    case ENGLISH:
                        yield "EN";
                    case DUTCH:
                        yield "NL";
                    case ROMANIAN:
                        yield "RO";
                    case POLISH:
                        yield "PL";
                }
                + ".md";
            // Read the README.md file
            String markdownContent = Files.readString(Path.of(path));

            // Convert Markdown to HTML
            String htmlContent = markdownCtrl.convertMarkdownToHtml(markdownContent);

            // Load the HTML into the WebView
            webView.getEngine().loadContent(htmlContent);
        } catch (IOException e) {
            e.printStackTrace();
            webView.getEngine().loadContent("<h1>Error</h1><p>Unable to load the README file.</p>");
        }
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
    private void onExitMenu() {
        if (stage != null) {
            stage.close(); // Close the popup window
        }
    }
}
