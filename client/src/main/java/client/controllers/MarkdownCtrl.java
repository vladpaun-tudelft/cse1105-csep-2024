package client.controllers;

import client.scenes.DashboardCtrl;
import client.services.ReferenceService;
import client.services.TagService;
import client.ui.DialogStyler;
import com.google.inject.Inject;
import commons.Note;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import lombok.Getter;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles markdown rendering, reference validation, tag visualization, and tooltip interactions in the WebView.
 */
public class MarkdownCtrl {

    // Dashboard reference
    private DashboardCtrl dashboardCtrl;
    private ReferenceService referenceService;
    private TagService tagService;
    private DialogStyler dialogStyler = new DialogStyler();

    // Markdown parser and renderer
    private final Parser parser;
    private final HtmlRenderer renderer;

    // UI references
    private ListView<Note> collectionView;
    private TreeView<Note> treeView;
    private WebView markdownView;
    private Label markdownViewBlocker;
    private TextArea noteBody;

    private ContextMenu recommendationsMenu;


    private Note currentNote;


    @Getter
    private final String cssPath;
    private String scriptPath = null;

    @Inject
    public MarkdownCtrl() {
        this.referenceService = new ReferenceService(dashboardCtrl, noteBody, recommendationsMenu);
        this.tagService = new TagService();
        var extensions = Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create()
        );
        parser = Parser.builder().extensions(extensions).build();
        renderer = HtmlRenderer.builder().extensions(extensions).build();

        // Get the external CSS file path
        File cssFile = getExternalCssFile();
        cssPath = cssFile.toURI().toString();

        // Script path can still be loaded from the resources
        URL scriptUrl = getClass().getResource("/script/referenceHandler.js");
        if (scriptUrl != null) {
            scriptPath = scriptUrl.toExternalForm();
        } else {
            dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "File error",
                    "JS file not found",
                    "Reference javascript file not found."
            );
        }
    }

    public void setCurrentNote(Note currentNote) {
        this.currentNote = currentNote;
    }

    public String getCssPath() {
        return cssPath;
    }

    /**
     * Sets UI component references and initializes markdown rendering.
     */
    public void setReferences(ListView<Note> collectionView, TreeView<Note> treeView, WebView markdownView,
                              Label markdownViewBlocker, TextArea noteBody) {
        this.collectionView = collectionView;
        this.treeView = treeView;
        this.markdownView = markdownView;
        this.markdownViewBlocker = markdownViewBlocker;
        this.noteBody = noteBody;

        this.markdownView.getEngine().setJavaScriptEnabled(true);

        // Initialize the WebView
        updateMarkdownView("");

        // Add listeners for note body changes and scrolling
        noteBody.textProperty().addListener((_, _, newValue) -> {
            updateMarkdownView(newValue);
            PauseTransition pause = new PauseTransition(Duration.millis(100)); // Adjust duration as needed
            pause.setOnFinished(event -> {
                referenceService.handleReferenceRecommendations();
            });
            pause.play();
        });
        noteBody.scrollTopProperty().addListener((_, _, _) -> synchronizeScroll());

        // Handle javascript alerts from the WebView
        markdownView.getEngine().setOnAlert(event -> {
            String url = event.getData();

            if (url.startsWith("tag://")) {
                // Handle tag clicks
                String tag = url.substring("tag://".length());
                dashboardCtrl.selectTag(tag);
            } else if (url.startsWith("note://")) {
                // Handle internal note links
                String noteTitle = url.substring("note://".length());
                dashboardCtrl.getCollectionNotes().stream()
                        .filter(note -> note.title.equals(noteTitle))
                        .findFirst()
                        .ifPresent(selectedNote -> {
                            collectionView.getSelectionModel().select(selectedNote);
                            dashboardCtrl.selectNoteInTreeView(selectedNote);
                        });
            } else {
                // Handle external urls
                openUrlInBrowser(url);
            }
        });
    }

    public void setDashboardCtrl(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
        this.referenceService = new ReferenceService(dashboardCtrl, noteBody, recommendationsMenu);
    }

    /**
     * Updates the markdown view with validated and rendered content.
     */
    public void updateMarkdownView(String markdown) {
        String validatedContent = tagService.replaceTagsInMarkdown(markdown);
        validatedContent = referenceService.validateAndReplaceReferences(validatedContent);
        String renderedHtml = convertMarkdownToHtml(validatedContent);

        Platform.runLater(() -> {
            markdownView.getEngine().loadContent(renderedHtml, "text/html");
            markdownViewBlocker.setVisible(markdown == null || markdown.isEmpty());
        });
    }

    /**
     * Converts markdown content to HTML, applying CSS and JavaScript for tooltips and interactivity.
     */
    private String convertMarkdownToHtml(String markdown) {
        markdown = convertFileNameToURL(markdown);
        String htmlContent = markdown == null || markdown.isEmpty() ? "" : renderer.render(parser.parse(markdown));

        return """
                <!DOCTYPE html>
                <html>
                    <head>
                        <link rel='stylesheet' type='text/css' href='""" + cssPath + "'>" +
                """
                    </head>
                    <body>
                """ + htmlContent  +
                """
                    <div id="tooltip" class="tooltip"></div>
                </body>
                <script src='""" + scriptPath + "'></script>" +
                """
            </html>""";
    }

    private String convertFileNameToURL(String markdown) {
        if (currentNote == null) {
            return markdown;
        }
        String regex = "!\\[([^)]+)]\\(([^)]+)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(markdown);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while(matcher.find()) {
            result.append(markdown, lastEnd, matcher.start());
            String altText = matcher.group(1);
            String fileName = matcher.group(2);

            URI uri = null;
            try {
                uri = new URI(null, null, fileName, null);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            String encodedFileName = uri.toASCIIString();

            String fileURL = currentNote.collection.serverURL + "api/notes/" + currentNote.getId() + "/files/" + encodedFileName;

            result.append(String.format("![%s](%s)", altText, fileURL));

            lastEnd = matcher.end();
        }

        result.append(markdown.substring(lastEnd));
        return result.toString();
    }

    /**
     * Synchronizes scrolling between the note body and markdown view.
     */
    private void synchronizeScroll() {
        double percentage = computeScrollPercentage(noteBody);
        Platform.runLater(() -> markdownView.getEngine().executeScript(
                "document.body.scrollTop = document.body.scrollHeight * " + percentage + ";"
        ));
    }

    private double computeScrollPercentage(TextArea noteBody) {
        double scrollTop = noteBody.getScrollTop();
        double contentHeight = noteBody.lookup(".content").getBoundsInLocal().getHeight();
        double viewportHeight = noteBody.getHeight();
        return scrollTop / (contentHeight - viewportHeight);
    }

    private void openUrlInBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                Alert alert = dialogStyler.createStyledAlert(Alert.AlertType.ERROR, "Error Opening URL",
                        "Failed to open the URL: " + url, "Please check the URL format (missing protocol) or your connection");
                alert.showAndWait();
            }
        } else {
            Alert alert = dialogStyler.createStyledAlert(Alert.AlertType.ERROR, "Desktop Not Supported",
                    "Unable to open the URL", "Desktop is not supported on this platform.");
            alert.showAndWait();
        }
    }

    // ----------------------- Helper methods for getting external css file -----------------------

    public File getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String appDataPath = null;
        String appData;
        File appDataDir = null;
        if (os.contains("win")) {
            appData = System.getenv("APPDATA");
            if (appData != null) {
                appDataPath = appData + File.separator + "NetNote";
            }
        } else {
            appData = System.getProperty("user.home");
            if (appData != null) {
                appDataPath = appData + File.separator + ".netnote";
            }
        }
        if (appDataPath != null) {
            appDataDir = new File(appDataPath);
        }
        if (appDataDir == null || !(appDataDir.exists()) && !appDataDir.mkdirs()) {

            dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "Environment error",
                    "Environment variable error:",
                    "Failed to create directory: " + appDataPath
            );
            dashboardCtrl.onClose();
        }

        return appDataDir;

    }

    public File getExternalCssFile() {
        File appDataDir = getAppDataDirectory();
        File externalCssFile = new File(appDataDir, "markdown.css");

        // CSS file in resources
        String packagedCssPath = "/css/markdown.css";

        if (externalCssFile.exists()) {
            // External file exists, use it and update the packaged version
            updatePackagedCssFile(externalCssFile, packagedCssPath);
        } else {
            // External file does not exist, copy it from resources
            try (InputStream in = getClass().getResourceAsStream(packagedCssPath);
                 OutputStream out = new FileOutputStream(externalCssFile)) {
                if (in == null) {
                    dialogStyler.createStyledAlert(
                            Alert.AlertType.ERROR,
                            "File error",
                            "Markdown CSS Missing",
                            "The CSS file for markdown could not be found in the application resources."
                    );
                    return null;
                }

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                dialogStyler.createStyledAlert(
                        Alert.AlertType.ERROR,
                        "File error",
                        "File Copy Error",
                        "Failed to copy markdown.css to the external directory."
                );
            }
        }
        return externalCssFile;
    }

    private void updatePackagedCssFile(File externalCssFile, String packagedCssPath) {
        // Locate the packaged resource
        URL packagedCssUrl = getClass().getResource(packagedCssPath);
        if (packagedCssUrl == null) {
            return;
        }

        try {
            // Convert the resource URL to a file path
            File packagedCssFile = new File(packagedCssUrl.toURI());

            if (!packagedCssFile.canWrite()) {
                return;
            }

            // Copy external CSS to the packaged resource location
            try (InputStream in = new FileInputStream(externalCssFile);
                 OutputStream out = new FileOutputStream(packagedCssFile)) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
