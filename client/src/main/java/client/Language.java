package client;

public enum Language {
    ENGLISH("en", "English", "/css/icons/en.jpg"),
    DUTCH("nl", "Nederlands", "/css/icons/nl.jpg"),
    ROMANIAN("ro", "Română", "/css/icons/ro.jpg");

    private final String code;
    private final String displayName;
    private final String imagePath;

    Language(String code, String displayName, String imagePath) {
        this.code = code;
        this.displayName = displayName;
        this.imagePath = imagePath;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.getCode().equals(code)) return lang;
        }
        return ENGLISH; // Default
    }
}
