package client.controllers;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebView;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.net.URL;

public class MarkdownCtrl {

    private WebView markdownView;
    private Label markdownViewBlocker;
    private TextArea noteBody;

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final String cssPath;

    public MarkdownCtrl() {
        parser = Parser.builder().build();
        renderer = HtmlRenderer.builder().build();

        URL cssUrl = getClass().getResource("/css/markdown.css");
        if (cssUrl != null) {
            cssPath = cssUrl.toExternalForm();
        } else {
            throw new RuntimeException("Markdown CSS file not found.");
        }
    }

    public String getCssPath() {
        return cssPath;
    }

    public void initialize(WebView markdownView, Label markdownViewBlocker, TextArea noteBody) {
        this.markdownView = markdownView;
        this.markdownViewBlocker = markdownViewBlocker;
        this.noteBody = noteBody;

        this.markdownView.getEngine().setJavaScriptEnabled(true);

        updateMarkdownView(""); // Initialize view

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
     * Updates the WebView with the rendered markdown.
     *
     * @param markdown the markdown content
     */
    public void updateMarkdownView(String markdown) {

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
