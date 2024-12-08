package client.controllers;

import client.scenes.DashboardCtrl;
import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebView;
import lombok.Getter;
import lombok.Setter;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownCtrl {

    // Utilities
    @Setter
    private DashboardCtrl dashboardCtrl;
    private final NoteCtrl noteCtrl;
    private final Parser parser;
    private final HtmlRenderer renderer;

    // References
    private WebView markdownView;
    private Label markdownViewBlocker;
    private TextArea noteBody;

    // Variables
    @Getter
    private final String cssPath;

    @Inject
    public MarkdownCtrl(NoteCtrl noteCtrl) {
        this.noteCtrl = noteCtrl;

        var extensions = Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create()
        );

        parser = Parser.builder().extensions(extensions).build();
        renderer = HtmlRenderer.builder().extensions(extensions).build();

        URL cssUrl = getClass().getResource("/css/markdown.css");
        if (cssUrl != null) {
            cssPath = cssUrl.toExternalForm();
        } else {
            throw new RuntimeException("Markdown CSS file not found.");
        }
    }

    public void setReferences(WebView markdownView, Label markdownViewBlocker, TextArea noteBody) {
        this.markdownView = markdownView;
        this.markdownViewBlocker = markdownViewBlocker;
        this.noteBody = noteBody;

        this.markdownView.getEngine().setJavaScriptEnabled(true);

        updateMarkdownView(""); // Initialize view

        // Add listener for text changes to update markdown view and content blocker
        noteBody.textProperty().addListener((_, _, newValue) -> updateMarkdownView(newValue));

        // Add listener for synchronized scrolling
        noteBody.scrollTopProperty().addListener((_, _, _) -> scrollMarkdownView());

        // Add listener for clicking on references
        /*
        markdownView.getEngine().setOnAlert(event -> {
            String noteTitle = event.getData().replace("netnote://", "");
            dashboardCtrl.getCollectionNotes().stream()
                    .filter(note -> note.title.equals(noteTitle))
                    .findFirst().ifPresent(noteCtrl::showCurrentNote);

        }); */
    }

    /**
     * Updates the WebView with the rendered markdown.
     *
     * @param markdown the markdown content
     */
    private void updateMarkdownView(String markdown) {
        String validatedMarkdown = validateAndHighlightReferences(markdown);
        String renderedHtml = convertMarkdownToHtml(validatedMarkdown);
        Platform.runLater(() -> {
            markdownView.getEngine().loadContent(renderedHtml, "text/html");
            markdownViewBlocker.setVisible(markdown == null || markdown.isEmpty());
        });
    }

    private String validateAndHighlightReferences(String markdown) {
        List<String> references = extractReferences(markdown);
        for (String reference : references) {
            boolean isValid = dashboardCtrl.getCollectionNotes().stream()
                    .filter(note -> dashboardCtrl.getCurrentNote().collection.equals(note.collection))
                    .anyMatch(note -> note.title.equals(reference));

            if (!isValid) {
                markdown = markdown.replace(
                        "[[" + reference + "]]",
                        "<span class='red-squiggly'>[[" + reference + "]]</span>"
                );
            } else {
                markdown = markdown.replace("[[" + reference + "]]",
                        "<a href='netnote://" + reference + "'>" + reference + "</a>");
            }
        }
        return markdown;
    }

    private List<String> extractReferences(String markdown) {
        List<String> references = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[\\[(.*?)]]");
        Matcher matcher = pattern.matcher(markdown);
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
        return references;
    }

    /**
     * Converts a markdown string to an HTML string with the appropriate CSS included.
     *
     * @param markdown the markdown content
     * @return the HTML representation of the markdown
     */
    private String convertMarkdownToHtml(String markdown) {
        String htmlContent = markdown == null || markdown.isEmpty() ? "" : renderer.render(parser.parse(markdown));

        return """
                <!DOCTYPE html>
                <html>
                    <head>
                        <link rel='stylesheet' type='text/css' href='""" + cssPath + "'>\n" +
                """
                    </head>
                    <body>
                """ + htmlContent + "\n" +
                """
                    </body>
                </html>""";

    }

    /**
     * Forces the markdown view to scroll to a fixed percentage for now
     */
    // TODO: Make this smarter
    public void scrollMarkdownView() {
        double percentage = computeScrollPercentage(noteBody);
        Platform.runLater(() -> markdownView.getEngine().executeScript(
                "document.body.scrollTop = document.body.scrollHeight * " + percentage + ";"
        ));
    }
    /**
     * Gets the current scroll% in the note body
     * @param noteBody the note body
     * @return the scroll%
     */
    private double computeScrollPercentage(TextArea noteBody) {
        double scrollTop = noteBody.getScrollTop();
        // Use the Skin API to get the height of the content
        double contentHeight = noteBody.lookup(".content").getBoundsInLocal().getHeight();

        double viewportHeight = noteBody.getHeight();
        return scrollTop / (contentHeight - viewportHeight);
    }
}
