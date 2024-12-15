package client.controllers;

import commons.Note;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebView;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;

import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownCtrl {

    private WebView markdownView;
    private Label markdownViewBlocker;
    private TextArea noteBody;

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final String cssPath;

    private Note currentNote;

    public MarkdownCtrl() {
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


    public void setCurrentNote(Note currentNote) {
        this.currentNote = currentNote;
    }

    public String getCssPath() {
        return cssPath;
    }

    public void setReferences(WebView markdownView, Label markdownViewBlocker, TextArea noteBody) {
        this.markdownView = markdownView;
        this.markdownViewBlocker = markdownViewBlocker;
        this.noteBody = noteBody;

        this.markdownView.getEngine().setJavaScriptEnabled(true);

        updateMarkdownView(""); // Initialize view

        // Add listener for text changes to update markdown view and content blocker
        noteBody.textProperty().addListener((observable, oldValue, newValue) -> {
            updateMarkdownView(newValue);
        });

        // Add listener for synchronized scrolling
        noteBody.scrollTopProperty().addListener((observable, oldValue, newValue) -> {
            scrollMarkdownView();
        });
    }

    /**
     * Converts a markdown string to an HTML string with the appropriate CSS included.
     *
     * @param markdown the markdown content
     * @return the HTML representation of the markdown
     */
    private String convertMarkdownToHtml(String markdown) {
        markdown = convertFileNameToURL(markdown);
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

            String fileURL = String.format("http://localhost:8080/api/notes/" + currentNote.getId() + "/files/" + fileName);

            result.append(String.format("![%s](%s)", altText, fileURL));

            lastEnd = matcher.end();
        }

        result.append(markdown.substring(lastEnd));
        return result.toString();
    }

    /**
     * Updates the WebView with the rendered markdown.
     *
     * @param markdown the markdown content
     */
    private void updateMarkdownView(String markdown) {
        String renderedHtml = convertMarkdownToHtml(markdown);
        Platform.runLater(() -> {
            markdownView.getEngine().loadContent(renderedHtml, "text/html");
            markdownViewBlocker.setVisible(markdown == null || markdown.isEmpty());
        });
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
