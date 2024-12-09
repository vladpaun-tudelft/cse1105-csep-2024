package client.services;

import commons.Note;
import client.scenes.DashboardCtrl;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReferenceService {

    private final DashboardCtrl dashboardCtrl;

    public ReferenceService(DashboardCtrl dashboardCtrl) {
        this.dashboardCtrl = dashboardCtrl;
    }

    /**
     * Extracts references of the format [[...]] from markdown content.
     */
    public List<String> extractReferences(String markdown) {
        List<String> references = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[\\[(.*?)]]");
        Matcher matcher = pattern.matcher(markdown);
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
            Note referredNote = getReferredNoteByTitle(reference);
            if (referredNote == null) {
                markdown = highlightInvalidReference(markdown, reference);
            } else {
                markdown = replaceReferenceWithLink(markdown, reference, referredNote);
            }
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
     * Converts valid references to clickable links.
     */
    private String replaceReferenceWithLink(String markdown, String reference, Note referredNote) {
        String previewText = generatePreviewText(referredNote);
        String collectionName = referredNote.collection.title;

        return markdown.replace("[[" + reference + "]]",
                "<a href='#' class='note-link' data-note-title='" + reference + "' " +
                        "data-note-collection='" + collectionName + "' " +
                        "data-note-preview='" + previewText + "'>" + reference + "</a>");
    }

    /**
     * Generates preview text for the referred note.
     */
    private String generatePreviewText(Note referredNote) {
        if (referredNote.body.isBlank()) return "The note is blank.";
        // Prevent a note from referencing itself
        if (referredNote.title.equals(dashboardCtrl.getCurrentNote().title)) return "This note references itself.";
        // Get the first 20 characters of the body or the full body if it's shorter
        String previewText = referredNote.body.length() > 20
                ? referredNote.body.substring(0, 20) + "..."
                : referredNote.body;

        // Replace newlines with spaces to prevent breaking the link
        previewText = previewText.replace("\n", " ⏎ ");

        // Escape the preview text for HTML
        return escapeHtml(previewText);
    }

    /**
     * Finds a note by title in the current collection.
     */
    private Note getReferredNoteByTitle(String reference) {
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
        if (text == null) return "";
        return StringEscapeUtils.escapeHtml4(text)
                .replace("[[", "&#91;&#91;")
                .replace("]]", "&#93;&#93;");
    }
}
