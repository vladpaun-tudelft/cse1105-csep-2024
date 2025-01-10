package client.services;

import commons.Note;
import client.scenes.DashboardCtrl;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.control.skin.TextAreaSkin;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReferenceService {

    private final DashboardCtrl dashboardCtrl;
    private final TextArea noteBody;
    private ContextMenu recommendationsMenu;

    public ReferenceService(DashboardCtrl dashboardCtrl, TextArea noteBody, ContextMenu recommendationsMenu) {
        this.dashboardCtrl = dashboardCtrl;
        this.noteBody = noteBody;
        this.recommendationsMenu = recommendationsMenu;
    }

    // ----------------------- Reference Extraction and Validation -----------------------

    /**
     * Extracts references of the format [[...]] from markdown content.
     */
    public List<String> extractReferences(String markdown) {
        Pattern pattern = Pattern.compile("\\[\\[(.*?)]]");
        Matcher matcher = pattern.matcher(markdown);
        List<String> references = new ArrayList<>();
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
        return references;
    }

    /**
     * Validates references and replaces valid ones with clickable links.
     */
    public String validateAndReplaceReferences(String markdown) {
        List<String> references = extractReferences(markdown);
        for (String reference : references) {
            Note referredNote = findNoteByTitle(reference);
            markdown = (referredNote == null)
                    ? highlightInvalidReference(markdown, reference)
                    : replaceReferenceWithLink(markdown, reference, referredNote);
        }
        return markdown;
    }

    /**
     * Highlights invalid references in markdown.
     */
    private String highlightInvalidReference(String markdown, String reference) {
        return markdown.replace(
                "[[" + reference + "]]",
                "<span class='red-squiggly'>[[" + reference + "]]</span>"
        );
    }

    /**
     * Replaces valid references with clickable links.
     */
    private String replaceReferenceWithLink(String markdown, String reference, Note referredNote) {
        String previewText = generatePreviewText(referredNote);
        String collectionName = referredNote.collection.title;
        String noteUrl = "note://" + reference;

        return markdown.replace("[[" + reference + "]]",
                String.format("<a href='%s' class='note-link' data-note-title='%s' " +
                                "data-note-collection='%s' data-note-preview='%s'>%s</a>",
                        noteUrl, reference, collectionName, previewText, reference)
        );
    }

    /**
     * Generates preview text for the referred note.
     */
    private String generatePreviewText(Note referredNote) {
        if (referredNote.body.isBlank()) return "The note is blank.";
        if (referredNote.title.equals(dashboardCtrl.getCurrentNote().title)) return "This note references itself.";

        String previewText = referredNote.body.length() > 20
                ? referredNote.body.substring(0, 20) + "..."
                : referredNote.body;
        return escapeHtml(previewText.replace("\n", " ⏎ "));
    }

    /**
     * Finds a note by title in the current collection.
     */
    private Note findNoteByTitle(String reference) {
        return dashboardCtrl.getCollectionNotes().stream()
                .filter(note -> dashboardCtrl.getCurrentNote().collection.equals(note.collection))
                .filter(note -> note.title.equals(reference))
                .findFirst()
                .orElse(null);
    }

    /**
     * Escapes HTML characters and special sequences in a string.
     */
    private String escapeHtml(String text) {
        return StringEscapeUtils.escapeHtml4(Optional.ofNullable(text).orElse(""))
                .replace("[[", "&#91;&#91;")
                .replace("]]", "&#93;&#93;");
    }

    // ----------------------- Recommendations Handling -----------------------

    public void handleReferenceRecommendations() {
        String text = noteBody.getText();
        int caretPosition = noteBody.getCaretPosition();

        if (!isValidCaretPosition(text, caretPosition)) {
            hideRecommendationsMenu();
            return;
        }

        Optional<int[]> bracketPositions = findBrackets(text, caretPosition);
        if (bracketPositions.isEmpty()) {
            hideRecommendationsMenu();
            return;
        }

        String query = text.substring(bracketPositions.get()[0] + 2, caretPosition).strip();
        showRecommendations(query);
    }

    private boolean isValidCaretPosition(String text, int caretPosition) {
        return caretPosition >= 0 && caretPosition <= text.length();
    }

    private void hideRecommendationsMenu() {
        if (recommendationsMenu != null && recommendationsMenu.isShowing()) {
            recommendationsMenu.hide();
        }
    }

    /**
     * Displays recommendations in the context menu.
     */
    public void showRecommendations(String query) {
        if (recommendationsMenu == null) {
            recommendationsMenu = new ContextMenu();
        }
        recommendationsMenu.getItems().clear();

        List<String> matches = dashboardCtrl.getCollectionNotes().stream()
                .filter(note -> note.collection.equals(dashboardCtrl.getCurrentNote().collection))
                .map(Note::getTitle)
                .filter(title -> query.isEmpty() || title.toLowerCase().startsWith(query.toLowerCase()))
                .toList();

        if (matches.isEmpty()) {
            MenuItem noMatchesItem = new MenuItem("No Notes Found");
            noMatchesItem.setDisable(true);
            recommendationsMenu.getItems().add(noMatchesItem);
        } else {
            // Show the first 5 matches initially
            paginateRecommendations(matches, 0);
        }

        displayRecommendationsMenu();
    }

    private void paginateRecommendations(List<String> matches, int startIndex) {
        int pageSize = 5;
        int endIndex = Math.min(startIndex + pageSize, matches.size());

        // Clear the existing items
        recommendationsMenu.getItems().clear();

        // Add the "← Previous" button as the first item, if applicable
        if (startIndex > 0) {
            MenuItem previousPageItem = new MenuItem("← Previous");
            previousPageItem.setOnAction(e -> {
                paginateRecommendations(matches, startIndex - pageSize);
            });
            recommendationsMenu.getItems().add(previousPageItem);
        }

        // Add the current page of matches
        for (int i = startIndex; i < endIndex; i++) {
            String title = matches.get(i);
            MenuItem item = new MenuItem(title);
            item.setOnAction(e -> insertNoteReference(noteBody, title, noteBody.getCaretPosition()));
            recommendationsMenu.getItems().add(item);
        }

        // Add the "Next →" button as the last item, if applicable
        if (endIndex < matches.size()) {
            MenuItem nextPageItem = new MenuItem("Next →");
            nextPageItem.setOnAction(e -> {
                paginateRecommendations(matches, endIndex);
            });
            recommendationsMenu.getItems().add(nextPageItem);
        }

        // Re-show the updated ContextMenu
        displayRecommendationsMenu();
    }
    private void displayRecommendationsMenu() {
        Platform.runLater(() -> displayContextMenu(recommendationsMenu));
    }

    private void displayContextMenu(ContextMenu menu) {
        try {
            if (noteBody.getSkin() instanceof TextAreaSkin textAreaSkin) {
                Bounds caretBounds = textAreaSkin.getCaretBounds();
                if (caretBounds != null) {
                    Bounds screenBounds = noteBody.localToScreen(caretBounds);
                    if (screenBounds != null) {
                        menu.show(noteBody, screenBounds.getMinX(), screenBounds.getMaxY());
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error showing ContextMenu", e);
        }
    }

    // ----------------------- Text Manipulation -----------------------

    private void insertNoteReference(TextArea noteBody, String noteTitle, int caretPosition) {
        String text = noteBody.getText();
        Optional<int[]> bracketPositions = findBrackets(text, caretPosition);

        if (bracketPositions.isEmpty()) {
            return;
        }

        int lastOpenBrackets = bracketPositions.get()[0];
        int closingBrackets = bracketPositions.get()[1];

        String textBefore = text.substring(0, lastOpenBrackets + 2); // Include `[[`
        String textAfter = (closingBrackets != -1 && closingBrackets >= caretPosition)
                ? text.substring(closingBrackets) // Include `]]`
                : "]]" + text.substring(caretPosition);

        noteBody.setText(textBefore + noteTitle + textAfter);
        noteBody.positionCaret((textBefore + noteTitle + "]]").length());
        dashboardCtrl.getNoteCtrl().onBodyChanged(dashboardCtrl.getCurrentNote());

    }

    private Optional<int[]> findBrackets(String text, int caretPosition) {
        int lastOpenBrackets = text.lastIndexOf("[[", caretPosition - 1);
        if (lastOpenBrackets == -1 || lastOpenBrackets >= caretPosition) return Optional.empty();

        int closingBrackets = text.indexOf("]]", lastOpenBrackets + 2);
        if (closingBrackets != -1 && closingBrackets < caretPosition) return Optional.empty();

        return Optional.of(new int[]{lastOpenBrackets, closingBrackets});
    }
}
