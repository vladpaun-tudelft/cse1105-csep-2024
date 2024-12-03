package client.controllers;

/*
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
*/

class MarkdownCtrlTest {
    /*
    private MarkdownCtrl markdownCtrl;
    private WebView mockedWebView;
    private Label mockedLabel;
    private TextArea mockedTextArea;

    private WebEngine mockedWebEngine;
    private DoubleProperty scrollTopProperty;

    private String EMPTY_HTML_DOC;
    private String cssPath;

    @BeforeAll
    static void initJavaFX() {
        javafx.application.Platform.startup(() -> {});
    }

    @BeforeEach
    void setUp() {
        mockedWebView = mock(WebView.class);
        mockedLabel = mock(Label.class);
        mockedTextArea = mock(TextArea.class);
        mockedWebEngine = mock(WebEngine.class);

        when(mockedWebView.getEngine()).thenReturn(mockedWebEngine);

        scrollTopProperty = new SimpleDoubleProperty(0.0);
        when(mockedTextArea.scrollTopProperty()).thenReturn(scrollTopProperty);

        markdownCtrl = new MarkdownCtrl();

        markdownCtrl.initialize(mockedWebView, mockedLabel, mockedTextArea);

        cssPath = markdownCtrl.getCssPath();
        EMPTY_HTML_DOC = """
                <!DOCTYPE html>
                <html>
                    <head>
                        <link rel='stylesheet' type='text/css' href='""" + cssPath + "'>\n" +
                """
                    </head>
                    <body>
                
                    </body>
                </html>""";
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void verifyJavascriptSetup(){
        verify(mockedWebView.getEngine(), times(1)).setJavaScriptEnabled(true);
    }

    @Test
    void MDIsValidTest() throws InterruptedException {
        String markdown = "# Hello World";
        markdownCtrl.updateMarkdownView(markdown);

        waitForFXEvents();

        verify(mockedWebView.getEngine(), times(2)).loadContent(anyString(), eq("text/html"));
        verify(mockedLabel, times(1)).setVisible(false);
    }

    @Test
    void MDNullTest() throws InterruptedException {
        markdownCtrl.updateMarkdownView(null);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebView.getEngine(), times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertEquals(EMPTY_HTML_DOC, renderedHtml);
    }

    @Test
    void MDEmptyTest() throws InterruptedException {
        markdownCtrl.updateMarkdownView("");
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebView.getEngine(), times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertEquals(EMPTY_HTML_DOC, renderedHtml);
    }

    @Test
    void MDHeadingTest() throws InterruptedException {
        String markdown = """
                # Hello World
                ## Hello World
                ### Hello World
                #### Hello World
                ##### Hello World
                ###### Hello World
                ####### Hello World""";

        markdownCtrl.updateMarkdownView(markdown);

        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<h1>Hello World</h1>"), "Expected <h1> tag not found.");
        assertTrue(renderedHtml.contains("<h2>Hello World</h2>"), "Expected <h2> tag not found.");
        assertTrue(renderedHtml.contains("<h3>Hello World</h3>"), "Expected <h3> tag not found.");
        assertTrue(renderedHtml.contains("<h4>Hello World</h4>"), "Expected <h4> tag not found.");
        assertTrue(renderedHtml.contains("<h5>Hello World</h5>"), "Expected <h5> tag not found.");
        assertTrue(renderedHtml.contains("<h6>Hello World</h6>"), "Expected <h6> tag not found.");
        assertTrue(renderedHtml.contains("<p>####### Hello World</p>"), "Unexpected behaviour");
    }

    @Test
    void MDOrderedListTest() throws InterruptedException {
        String markdown = """
            1. First item
            2. Second item
            3. Third item""";

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<ol>"), "Expected <ol> tag not found.");
        assertTrue(renderedHtml.contains("<li>First item</li>"), "Expected <li>First item</li> not found.");
        assertTrue(renderedHtml.contains("<li>Second item</li>"), "Expected <li>Second item</li> not found.");
        assertTrue(renderedHtml.contains("<li>Third item</li>"), "Expected <li>Third item</li> not found.");
    }

    @Test
    void MDUnorderedListTest() throws InterruptedException {
        String markdown = """
            - First item
            * Second item
            - Third item""";

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<ul>"), "Expected <ul> tag not found.");
        assertTrue(renderedHtml.contains("<li>First item</li>"), "Expected <li>First item</li> not found.");
        assertTrue(renderedHtml.contains("<li>Second item</li>"), "Expected <li>Second item</li> not found.");
        assertTrue(renderedHtml.contains("<li>Third item</li>"), "Expected <li>Third item</li> not found.");
    }

    @Test
    void MDMultipleNewlinesTest() throws InterruptedException {
        String markdown = """
            Paragraph one.


            Paragraph two.""";

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<p>Paragraph one.</p>"), "Expected <p>Paragraph one.</p> not found.");
        assertTrue(renderedHtml.contains("<p>Paragraph two.</p>"), "Expected <p>Paragraph two.</p> not found.");
    }

    @Test
    void MDCodeBlockTest() throws InterruptedException {
        String markdown = """
            ```
            code block
            ```""";

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<pre><code>code block\n</code></pre>"), "Expected <pre><code>code block</code></pre> not found.");
    }

    @Test
    void MDInlineFormattingTest() throws InterruptedException {
        String markdown = """
            **Bold text**
            *Italic text*
            `Inline code`""";

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<strong>Bold text</strong>"), "Expected <strong>Bold text</strong> not found.");
        assertTrue(renderedHtml.contains("<em>Italic text</em>"), "Expected <em>Italic text</em> not found.");
        assertTrue(renderedHtml.contains("<code>Inline code</code>"), "Expected <code>Inline code</code> not found.");
    }

    @Test
    void MDLinkTest() throws InterruptedException {
        String markdown = "[NetNote](https://NetNote.com)";

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<a href=\"https://NetNote.com\">NetNote</a>"), "Expected link <a> tag not found.");
    }

    @Test
    void MDImageTest() throws InterruptedException {
        String markdown = "![NetNote Logo](https://NetNote.com/logo.png)";

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<img src=\"https://NetNote.com/logo.png\" alt=\"NetNote Logo\""), "Expected <img> tag not found.");
    }

    @Test
    void MDBlockquoteTest() throws InterruptedException {
        String markdown = """
            > This is a blockquote\s\s
            > with multiple lines.""";

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<blockquote>\n<p>This is a blockquote<br />\nwith multiple lines.</p>\n</blockquote>"), "Expected blockquote not found.");
    }

    @Test
    void MDTableTest() throws InterruptedException {
        String markdown = """
        | Header 1 | Header 2 |
        |----------|----------|
        | Cell 1   | Cell 2   |
        | Cell 3   | Cell 4   |
        """;

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<table>"), "Expected <table> tag not found.");
        assertTrue(renderedHtml.contains("<tr>"), "Expected <tr> tag not found.");
        assertTrue(renderedHtml.contains("<th>Header 1</th>"), "Expected <th>Header 1</th> not found.");
        assertTrue(renderedHtml.contains("<th>Header 2</th>"), "Expected <th>Header 2</th> not found.");
        assertTrue(renderedHtml.contains("<td>Cell 1</td>"), "Expected <td>Cell 1</td> not found.");
        assertTrue(renderedHtml.contains("<td>Cell 2</td>"), "Expected <td>Cell 2</td> not found.");
        assertTrue(renderedHtml.contains("<td>Cell 3</td>"), "Expected <td>Cell 3</td> not found.");
        assertTrue(renderedHtml.contains("<td>Cell 4</td>"), "Expected <td>Cell 4</td> not found.");
    }

    @Test
    void MDStrikethroughTest() throws InterruptedException {
        String markdown = "~~Strikethrough text~~";

        markdownCtrl.updateMarkdownView(markdown);
        waitForFXEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockedWebEngine, times(2)).loadContent(captor.capture(), eq("text/html"));

        String renderedHtml = captor.getValue();

        assertTrue(renderedHtml.contains("<del>Strikethrough text</del>"), "Expected <del>Strikethrough text</del> not found.");
    }


    @Test
    void scrollMarkdownView() {
    }

    private void waitForFXEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timed out waiting for fx events");
        }
    }
    */
}