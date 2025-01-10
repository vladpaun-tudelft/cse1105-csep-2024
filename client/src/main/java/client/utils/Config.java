package client.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import client.ui.DialogStyler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import javafx.scene.control.Alert;
import org.apache.commons.io.FileUtils;

import commons.Collection;

public class Config {
    public File configFile;
    private DialogStyler dialogStyler;

    @Inject
    public Config(DialogStyler dialogStyler) {
        configFile = new File("client/src/main/resources/config.json");
        this.dialogStyler = dialogStyler;
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                dialogStyler.createStyledAlert(
                        Alert.AlertType.ERROR,
                        "File Error.",
                        "File could not be created.",
                        "We have encountered an error while creating the config file. Please try again later."
                ).showAndWait();
            }
        }
    }

    public List<Collection> readFromFile(){
        try {
            if (configFile.length() == 0) {
                return new ArrayList<>();
            }
            String collectionsJson = FileUtils.readFileToString(configFile, "UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(collectionsJson, new TypeReference<List<Collection>>() {});
        } catch (IOException e) {
            dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "File Reading Error.",
                    "File could not be read.",
                    "We have encountered an error while trying to read the config file. Please try again later."
            );
        }
        return null;
    }

    public void writeToFile(Collection collection){
        List<Collection> collections = readFromFile();
        collections.add(collection);
        writeAllToFile(collections);
    }

    public void writeAllToFile(List<Collection> collections){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, collections);
        } catch (IOException e) {
            dialogStyler.createStyledAlert(
                    Alert.AlertType.ERROR,
                    "File Writing Error.",
                    "We could not write the collections to file..",
                    "We have encountered an error while trying to write the collections to the config file." +
                            " Please try again later."
            );
        }

    }

}
