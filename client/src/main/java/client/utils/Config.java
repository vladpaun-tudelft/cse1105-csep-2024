package client.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import client.ui.DialogStyler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import javafx.scene.control.Alert;
import org.apache.commons.io.FileUtils;

import commons.Collection;

public class Config {
    public File configFile;
    private DialogStyler dialogStyler;

    private boolean fileError = false;

    @Inject
    public Config(DialogStyler dialogStyler) {
        this.dialogStyler = dialogStyler;

        File appDataDir = getAppDataDirectory();
        configFile = new File(appDataDir, "config.json");

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                // Initialize the file with an empty list and no defaultCollectionId
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, new ConfigData(new ArrayList<>(), -1));
            } catch (IOException e) {
                if(!fileError) {
                    dialogStyler.createStyledAlert(
                            Alert.AlertType.ERROR,
                            "File Error.",
                            "File could not be created.",
                            "We have encountered an error while creating the config file. Please try again later."
                    ).showAndWait();
                    fileError = true;
                }
            }
        }
    }

    public boolean isFileErrorStatus() {
        return fileError;
    }

    public List<Collection> readFromFile() {
        try {
            if (configFile.length() == 0) {
                return new ArrayList<>();
            }
            String configJson = FileUtils.readFileToString(configFile, "UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            ConfigData configData = objectMapper.readValue(configJson, ConfigData.class);
            return configData.getCollections();
        } catch (IOException e) {
            dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "File Reading Error.",
                    "File could not be read.",
                    "We have encountered an error while trying to read the config file. Please try again later."
            ).showAndWait();
        }
        return null;
    }

    public void writeToFile(Collection collection) {
        List<Collection> collections = readFromFile();

        if (!collections.contains(collection)) {

            if (collections != null) {

                collections.add(collection);
                writeAllToFile(collections);
            }
        }
    }

    public void writeAllToFile(List<Collection> collections) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            long defaultId = readDefaultCollectionId(); // Preserve the existing default ID
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, new ConfigData(collections, defaultId));
        } catch (IOException e) {
            dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "File Writing Error.",
                    "We could not write the collections to file.",
                    "We have encountered an error while trying to write the collections to the config file." +
                            " Please try again later."
            ).showAndWait();
        }
    }

    public void setDefaultCollection(Collection defaultCollection) {
        List<Collection> collections = readFromFile();

        // Check if collections list is null
        if (collections == null) {
            dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "Default Collection Error",
                    "Configuration file error.",
                    "The configuration file could not be read. Please check the file and try again."
            ).showAndWait();
            return;
        }

        // Handle null defaultCollection
        long defaultCollectionId = -1;
        if (defaultCollection != null) {
            if (!collections.contains(defaultCollection)) {
                dialogStyler.createStyledAlert(
                        Alert.AlertType.ERROR,
                        "Default Collection Error",
                        "Collection not found.",
                        "The specified collection does not exist in the config file. Please try again."
                ).showAndWait();
                return;
            }
            defaultCollectionId = defaultCollection.id;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(configFile, new ConfigData(collections, defaultCollectionId));
        } catch (IOException e) {
            dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "File Writing Error.",
                    "Default collection could not be set.",
                    "We encountered an error while updating the config file. Please try again later."
            ).showAndWait();
        }
    }


    public Collection readDefaultCollection() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ConfigData configData = objectMapper.readValue(configFile, ConfigData.class);
            for (Collection collection : configData.getCollections()) {
                if (collection.id == configData.getDefaultCollectionId()) {
                    return collection;
                }
            }
        } catch (IOException e) {
            dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "File Reading Error.",
                    "Default collection could not be read.",
                    "We encountered an error while reading the config file. Please try again later."
            ).showAndWait();
        }
        return null;
    }

    private long readDefaultCollectionId() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ConfigData configData = objectMapper.readValue(configFile, ConfigData.class);
            return configData.getDefaultCollectionId();
        } catch (IOException e) {
            return -1; // Return -1 if there's an issue reading the file
        }
    }

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
        }

        return appDataDir;
    }

    // Inner class to represent the full config data
    private static class ConfigData {
        private List<Collection> collections;
        private long defaultCollectionId;

        // Default constructor for Jackson
        public ConfigData() {}

        public ConfigData(List<Collection> collections, long defaultCollectionId) {
            this.collections = collections;
            this.defaultCollectionId = defaultCollectionId;
        }

        public List<Collection> getCollections() {
            return collections;
        }

        public void setCollections(List<Collection> collections) {
            this.collections = collections;
        }

        public long getDefaultCollectionId() {
            return defaultCollectionId;
        }

        public void setDefaultCollectionId(long defaultCollectionId) {
            this.defaultCollectionId = defaultCollectionId;
        }
    }
}
