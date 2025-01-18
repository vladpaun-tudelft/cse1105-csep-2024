package client;

import client.utils.Config;

import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageManager {
    private static LanguageManager instance;
    private ResourceBundle bundle;
    private Language currentLanguage;
    private final Config config;

    private LanguageManager(Config config) {
        this.config = config;
        this.currentLanguage = Language.fromCode(config.getCurrentLanguage());
        loadLanguageBundle();
    }

    public static LanguageManager getInstance(Config config) {
        if (instance == null) {
            instance = new LanguageManager(config);
        }
        return instance;
    }

    private void loadLanguageBundle() {
        bundle = ResourceBundle.getBundle("languages",
                new Locale(currentLanguage.getCode()));
    }

    public void switchLanguage(Language language) {
        currentLanguage = language;
        config.setLanguage(language.getCode());
        loadLanguageBundle();
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public Language getCurrentLanguage() {
        return currentLanguage;
    }
}
